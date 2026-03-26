package com.xindai.xindai.common.mybatis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link CryptoInterceptor} as a MyBatis plugin when encryption is enabled.
 * <p>
 * Requires the property {@code encryption.key} to be set. A default dev key is provided
 * for the development profile.
 */
@Configuration
public class MybatisCryptoConfig {

    @Bean
    @ConditionalOnProperty(name = "encryption.key")
    public CryptoInterceptor cryptoInterceptor(@Value("${encryption.key}") String key) {
        return new CryptoInterceptor(key);
    }
}
