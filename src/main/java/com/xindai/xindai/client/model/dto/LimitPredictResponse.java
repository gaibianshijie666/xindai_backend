package com.xindai.xindai.client.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 额度预测响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitPredictResponse {
    private int code;
    private String message;
    private Map<String, Object> data;
    private int processing_time_ms;

    /**
     * 获取预测额度
     */
    public Double getPredictedLimit() {
        if (data != null && data.get("predicted_limit") != null) {
            Object limit = data.get("predicted_limit");
            if (limit instanceof Number) {
                return ((Number) limit).doubleValue();
            }
        }
        return null;
    }

    /**
     * 获取预测方法
     */
    public String getMethod() {
        if (data != null && data.get("method") != null) {
            return data.get("method").toString();
        }
        return "unknown";
    }

    /**
     * 获取置信度
     */
    public Double getConfidence() {
        if (data != null && data.get("confidence") != null) {
            Object conf = data.get("confidence");
            if (conf instanceof Number) {
                return ((Number) conf).doubleValue();
            }
        }
        return 0.7;
    }
}
