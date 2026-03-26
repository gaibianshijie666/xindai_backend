package com.xindai.xindai.common.mybatis;

import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis interceptor that automatically encrypts and decrypts fields annotated with {@link CryptoField}.
 * <p>
 * Intercepts:
 * <ul>
 *   <li>{@code Executor.update()} - encrypts parameters before INSERT/UPDATE</li>
 *   <li>{@code Executor.query()} - decrypts result objects after SELECT</li>
 * </ul>
 * <p>
 * Uses Hutool's {@link AES} for symmetric encryption with a 256-bit key.
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        })
})
public class CryptoInterceptor implements Interceptor {

    private final AES aes;
    private final Map<Class<?>, List<Field>> cryptoFieldCache = new ConcurrentHashMap<>();

    /**
     * Creates a CryptoInterceptor with the given Base64-encoded AES key.
     * The key must be 32 bytes (256 bits) when decoded.
     *
     * @param base64Key Base64-encoded AES-256 key
     */
    public CryptoInterceptor(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES key must be 16, 24, or 32 bytes after Base64 decoding, got " + keyBytes.length + " bytes");
        }
        this.aes = new AES(keyBytes);
        log.info("CryptoInterceptor initialized with AES-{} encryption", keyBytes.length * 8);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();

        if (target instanceof Executor) {
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];

            // Encrypt parameters for INSERT/UPDATE
            String sqlCommandType = ms.getSqlCommandType().name();
            if (("INSERT".equals(sqlCommandType) || "UPDATE".equals(sqlCommandType)) && parameter != null) {
                encryptParameters(parameter);
            }
        }

        // Execute the original method
        Object result = invocation.proceed();

        // Decrypt results for SELECT queries
        if (target instanceof Executor) {
            MappedStatement ms = (MappedStatement) args[0];
            if ("SELECT".equals(ms.getSqlCommandType().name()) && result != null) {
                decryptResults(result);
            }
        }

        return result;
    }

    /**
     * Encrypts all @CryptoField annotated String fields in the parameter object.
     */
    void encryptParameters(Object parameter) {
        if (parameter instanceof Map<?, ?> map) {
            // MyBatis-Plus wraps parameters in a Map with keys like "et", "param1", etc.
            for (Object value : map.values()) {
                if (value != null) {
                    processFields(value, this::encryptValue);
                }
            }
        } else {
            processFields(parameter, this::encryptValue);
        }
    }

    /**
     * Decrypts all @CryptoField annotated String fields in the result objects.
     */
    @SuppressWarnings("unchecked")
    void decryptResults(Object result) {
        if (result instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    processFields(item, this::decryptValue);
                }
            }
        } else {
            processFields(result, this::decryptValue);
        }
    }

    /**
     * Processes all @CryptoField annotated String fields on the given object
     * using the provided field transformer function.
     */
    void processFields(Object obj, FieldTransformer transformer) {
        if (obj == null) {
            return;
        }
        Class<?> clazz = obj.getClass();
        // Skip JDK types, MyBatis wrapper types, and primitive wrappers
        if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.")) {
            return;
        }

        List<Field> cryptoFields = getCryptoFields(clazz);
        for (Field field : cryptoFields) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof String strValue && strValue.length() > 0) {
                    String transformed = transformer.transform(strValue);
                    field.set(obj, transformed);
                }
            } catch (IllegalAccessException e) {
                log.warn("Failed to access field {} on {}: {}", field.getName(), clazz.getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Encrypts a plaintext string using AES.
     */
    String encryptValue(String plaintext) {
        try {
            return aes.encryptBase64(plaintext);
        } catch (Exception e) {
            log.error("Encryption failed for value: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt field value", e);
        }
    }

    /**
     * Decrypts a Base64-encoded AES ciphertext string.
     */
    String decryptValue(String ciphertext) {
        try {
            return aes.decryptStr(ciphertext);
        } catch (Exception e) {
            log.warn("Decryption failed (value may not be encrypted): {}", e.getMessage());
            return ciphertext;
        }
    }

    /**
     * Gets and caches the list of @CryptoField annotated fields for a class.
     * Checks all fields in the class hierarchy.
     */
    private List<Field> getCryptoFields(Class<?> clazz) {
        return cryptoFieldCache.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(CryptoField.class) && field.getType() == String.class) {
                        fields.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            log.debug("Found {} @CryptoField(s) in {}", fields.size(), c.getSimpleName());
            return fields;
        });
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // No additional properties needed; key is passed via constructor
    }

    /**
     * Functional interface for field value transformation (encrypt or decrypt).
     */
    @FunctionalInterface
    interface FieldTransformer {
        String transform(String value);
    }
}
