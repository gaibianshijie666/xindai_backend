package com.xindai.xindai.common.mybatis;

import java.lang.annotation.*;

/**
 * Marks a field for automatic encryption/decryption by the CryptoInterceptor.
 * Only String fields are supported. Null values are skipped.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CryptoField {
    /**
     * Encryption algorithm. Currently only AES is supported.
     */
    String algorithm() default "AES";
}
