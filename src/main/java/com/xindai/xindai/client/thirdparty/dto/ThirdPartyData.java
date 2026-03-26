package com.xindai.xindai.client.thirdparty.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ThirdPartyData {
    private String sourceType;
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> rawData;
    private Map<String, Object> parsedData;
    private LocalDateTime collectTime;
    private Long processingTimeMs;
}
