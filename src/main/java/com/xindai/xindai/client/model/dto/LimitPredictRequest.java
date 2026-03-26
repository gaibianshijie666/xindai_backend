package com.xindai.xindai.client.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 额度预测请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitPredictRequest {
    private String user_id;
    private Map<String, Float> features;
}
