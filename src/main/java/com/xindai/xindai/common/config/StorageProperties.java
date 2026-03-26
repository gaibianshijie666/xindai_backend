package com.xindai.xindai.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class StorageProperties {
    private String path = "./uploads";
    private String maxSize = "10MB";
    private String allowedTypes = "image/jpeg,image/png,image/gif,application/pdf";
}
