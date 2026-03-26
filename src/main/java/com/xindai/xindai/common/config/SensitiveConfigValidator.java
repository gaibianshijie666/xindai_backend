package com.xindai.xindai.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SensitiveConfigValidator {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${encryption.key:}")
    private String encryptionKey;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @PostConstruct
    public void validate() {
        validateNotBlank("jwt.secret", jwtSecret);
        validateNotBlank("encryption.key", encryptionKey);
        validateNotBlank("spring.datasource.password", dbPassword);
        log.info("Sensitive configuration validation passed");
    }

    private void validateNotBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required configuration '" + name + "' is not set. " +
                "Please set it via environment variable or application properties.");
        }
    }
}
