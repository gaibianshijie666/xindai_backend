package com.xindai.xindai.client.model;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xindai.xindai.client.model.dto.PredictRequest;
import com.xindai.xindai.client.model.dto.PredictResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ModelServiceClient {

    @Value("${model.service.url:http://localhost:8001}")
    private String modelServiceUrl;

    @Value("${model.service.timeout:5000}")
    private int timeout;

    /**
     * 调用风险评估预测接口
     */
    @CircuitBreaker(name = "modelService", fallbackMethod = "predictFallback")
    @Retry(name = "modelService")
    public PredictResponse predict(String userId, Map<String, Float> features) {
        String url = modelServiceUrl + "/model/predict";

        PredictRequest request = new PredictRequest();
        request.setUserId(userId);
        request.setFeatures(features);
        request.setModelType("risk_scoring");

        try {
            long startTime = System.currentTimeMillis();

            try (HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(request))
                    .timeout(timeout)
                    .execute()) {

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("Model service predict call took {}ms", elapsed);

                if (!response.isOk()) {
                    log.error("Model service returned error: {}", response.getStatus());
                    throw new RuntimeException("Model service error: " + response.getStatus());
                }

                return JSONUtil.toBean(response.body(), PredictResponse.class);
            }

        } catch (Exception e) {
            log.error("Failed to call model service: {}", e.getMessage());
            throw new RuntimeException("Model service call failed", e);
        }
    }

    /**
     * 健康检查
     */
    public boolean healthCheck() {
        String url = modelServiceUrl + "/model/health";

        try (HttpResponse response = HttpRequest.get(url)
                .timeout(3000)
                .execute()) {
            return response.isOk();
        } catch (Exception e) {
            log.error("Model service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取模型版本
     */
    public String getModelVersion() {
        String url = modelServiceUrl + "/model/version";

        try (HttpResponse response = HttpRequest.get(url)
                .timeout(3000)
                .execute()) {

            if (response.isOk()) {
                cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(response.body());
                return jsonObject.getStr("version");
            }
        } catch (Exception e) {
            log.error("Failed to get model version: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 熔断器降级方法
     */
    public PredictResponse predictFallback(String userId, Map<String, Float> features, Exception e) {
        log.warn("Model service fallback triggered for user {}: {}", userId, e.getMessage());
        return createDefaultPredictResponse(userId);
    }

    /**
     * 创建默认预测响应
     */
    private PredictResponse createDefaultPredictResponse(String userId) {
        PredictResponse response = new PredictResponse();
        response.setCode(0);
        response.setMessage("success");
        response.setModelVersion("fallback");
        response.setProcessingTimeMs(0);

        // 构建data字段
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("risk_score", 50.0);  // 中等风险
        data.put("risk_level", 2);
        data.put("decision", "MANUAL_REVIEW");  // 需人工审核
        data.put("confidence", 0.5);
        data.put("factors", List.of(
            Map.of("factor", "service_unavailable", "impact", 0.0)
        ));
        response.setData(data);

        return response;
    }
}
