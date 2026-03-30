package com.xindai.xindai.client.model;

import com.xindai.xindai.client.model.dto.LimitPredictRequest;
import com.xindai.xindai.client.model.dto.LimitPredictResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 额度预测模型客户端
 *
 * 调用Python风控服务的XGBoost额度预测模型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitPredictionClient {

    @Value("${model.service.url:http://localhost:8000}")
    private String modelServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * 预测用户可贷额度
     *
     * @param userId 用户ID
     * @param features 用户特征
     * @return 预测的额度
     */
    public LimitPredictResponse predictLimit(String userId, Map<String, Object> features) {
        String url = modelServiceUrl + "/model/limit";

        try {
            // 转换特征值为Float
            Map<String, Float> floatFeatures = new HashMap<>();
            for (Map.Entry<String, Object> entry : features.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    floatFeatures.put(entry.getKey(), ((Number) value).floatValue());
                } else if (value instanceof String) {
                    // 处理信用等级转换
                    if ("grade".equals(entry.getKey())) {
                        floatFeatures.put("grade_encoded", encodeGrade((String) value));
                    }
                }
            }

            LimitPredictRequest request = new LimitPredictRequest(userId, floatFeatures);
            LimitPredictResponse response = restTemplate.postForObject(url, request, LimitPredictResponse.class);

            log.info("Limit prediction for user {}: {} yuan (method: {})",
                    userId,
                    response != null ? response.getData().get("predicted_limit") : "N/A",
                    response != null ? response.getData().get("method") : "N/A");

            return response;

        } catch (Exception e) {
            log.error("Failed to predict limit for user {}: {}", userId, e.getMessage());
            return createFallbackResponse(userId, features);
        }
    }

    /**
     * 快速预测额度（简化参数）
     */
    @CircuitBreaker(name = "modelService", fallbackMethod = "predictLimitFallback")
    public BigDecimal predictLimitSimple(Long userId, BigDecimal annualIncome, String grade,
                                          BigDecimal dti, BigDecimal interestRate) {
        Map<String, Object> features = new HashMap<>();
        features.put("annualIncome", annualIncome);
        features.put("grade", grade);
        features.put("dti", dti);
        features.put("interestRate", interestRate);
        features.put("ficoRangeLow", 680);
        features.put("ficoRangeHigh", 684);
        features.put("delinquency_2years", 0);
        features.put("openAcc", 10);
        features.put("totalAcc", 20);
        features.put("loanAmnt", 10000);
        features.put("term", 3);
        features.put("installment", 300);
        features.put("employmentLength", 5);
        features.put("homeOwnership", 0);
        features.put("revolBal", 10000);
        features.put("revolUtil", 40);

        LimitPredictResponse response = predictLimit(String.valueOf(userId), features);

        if (response != null && response.getData() != null) {
            Object limit = response.getData().get("predicted_limit");
            if (limit instanceof Number) {
                return BigDecimal.valueOf(((Number) limit).doubleValue());
            }
        }

        // 降级到规则计算
        return calculateRuleBasedLimit(annualIncome, grade, dti, interestRate);
    }

    /**
     * 规则引擎计算（降级方案）
     */
    private BigDecimal calculateRuleBasedLimit(BigDecimal income, String grade,
                                                BigDecimal dti, BigDecimal rate) {
        // 基础额度 = 收入 × 倍数
        double incomeValue = income != null ? income.doubleValue() : 50000;
        double multiplier = incomeValue < 30000 ? 0.3 :
                           incomeValue < 60000 ? 0.4 :
                           incomeValue < 100000 ? 0.5 : 0.6;
        double baseLimit = incomeValue * multiplier;

        // 信用等级系数
        Map<String, Double> gradeFactors = Map.of(
            "A", 1.3, "B", 1.15, "C", 1.0, "D", 0.85, "E", 0.7, "F", 0.55, "G", 0.4
        );
        double gradeFactor = gradeFactors.getOrDefault(grade != null ? grade : "C", 1.0);

        // 利率系数
        double rateValue = rate != null ? rate.doubleValue() : 15;
        double rateFactor = rateValue < 10 ? 1.2 : rateValue < 15 ? 1.0 : 0.8;

        // DTI系数
        double dtiValue = dti != null ? dti.doubleValue() : 20;
        double dtiFactor = dtiValue < 20 ? 1.0 : dtiValue < 30 ? 0.9 : 0.7;

        double limit = baseLimit * gradeFactor * rateFactor * dtiFactor;
        limit = Math.max(1000, Math.min(500000, limit));

        return BigDecimal.valueOf(limit).setScale(0, java.math.RoundingMode.DOWN);
    }

    /**
     * 编码信用等级
     */
    private float encodeGrade(String grade) {
        return switch (grade != null ? grade.toUpperCase() : "C") {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            case "E" -> 5;
            case "F" -> 6;
            case "G" -> 7;
            default -> 3;
        };
    }

    /**
     * 熔断器降级方法
     */
    public BigDecimal predictLimitFallback(Long userId, BigDecimal annualIncome, String grade,
                                            BigDecimal dti, BigDecimal interestRate, Exception e) {
        log.warn("Limit prediction fallback for user {}: {}", userId, e.getMessage());
        return calculateRuleBasedLimit(annualIncome, grade, dti, interestRate);
    }

    /**
     * 创建降级响应
     */
    private LimitPredictResponse createFallbackResponse(String userId, Map<String, Object> features) {
        BigDecimal income = features.get("annualIncome") instanceof Number
                ? BigDecimal.valueOf(((Number) features.get("annualIncome")).doubleValue())
                : new BigDecimal("50000");
        String grade = features.get("grade") instanceof String
                ? (String) features.get("grade") : "C";
        BigDecimal dti = features.get("dti") instanceof Number
                ? BigDecimal.valueOf(((Number) features.get("dti")).doubleValue())
                : new BigDecimal("20");
        BigDecimal rate = features.get("interestRate") instanceof Number
                ? BigDecimal.valueOf(((Number) features.get("interestRate")).doubleValue())
                : new BigDecimal("15");

        BigDecimal limit = calculateRuleBasedLimit(income, grade, dti, rate);

        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("predicted_limit", limit);
        data.put("min_limit", 1000);
        data.put("max_limit", 500000);
        data.put("confidence", 0.7);
        data.put("method", "rule_based_fallback");

        return new LimitPredictResponse(0, "success", data, 1);
    }
}
