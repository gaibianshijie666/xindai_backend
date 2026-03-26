package com.xindai.xindai.client.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PredictResponse {
    private Integer code;
    private String message;
    private Map<String, Object> data;
    private String modelVersion;
    private Integer processingTimeMs;

    // 便捷方法
    public Double getRiskScore() {
        if (data == null) return null;
        Object score = data.get("risk_score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        return null;
    }

    public Integer getRiskLevel() {
        if (data == null) return null;
        Object level = data.get("risk_level");
        if (level instanceof Number) {
            return ((Number) level).intValue();
        }
        return null;
    }

    public String getDecision() {
        if (data == null) return null;
        return (String) data.get("decision");
    }

    public Double getConfidence() {
        if (data == null) return null;
        Object conf = data.get("confidence");
        if (conf instanceof Number) {
            return ((Number) conf).doubleValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFactors() {
        if (data == null) return null;
        Object factors = data.get("factors");
        if (factors instanceof List) {
            return (List<Map<String, Object>>) factors;
        }
        return null;
    }
}
