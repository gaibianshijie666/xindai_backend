package com.xindai.xindai.client.model.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PredictRequest {
    private String userId;
    private Map<String, Float> features;
    private String modelType = "risk_scoring";
}
