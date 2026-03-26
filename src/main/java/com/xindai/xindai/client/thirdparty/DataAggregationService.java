package com.xindai.xindai.client.thirdparty;

import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;
import com.xindai.xindai.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataAggregationService {

    private final List<DataCollector> dataCollectors;

    /**
     * 并行采集所有数据源
     */
    public Map<String, ThirdPartyData> collectAll(Long userId, String phone, String idCard) {
        log.info("Starting data collection for userId: {}", userId);
        long startTime = System.currentTimeMillis();

        // 并行调用所有采集器
        List<CompletableFuture<Map.Entry<String, ThirdPartyData>>> futures = dataCollectors.stream()
                .filter(DataCollector::isAvailable)
                .map(collector -> CompletableFuture.supplyAsync(() -> {
                    ThirdPartyData data = collector.collect(userId, phone, idCard);
                    return Map.entry(collector.getSourceType(), data);
                }))
                .collect(Collectors.toList());

        // 等待所有采集完成
        Map<String, ThirdPartyData> results = new HashMap<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<Map.Entry<String, ThirdPartyData>> future : futures) {
            try {
                Map.Entry<String, ThirdPartyData> entry = future.get();
                if (entry.getValue() != null) {
                    results.put(entry.getKey(), entry.getValue());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                log.error("Data collection interrupted for userId: {}", userId, e);
                throw new BusinessException("数据采集被中断");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                log.warn("Data collector failed: {}", cause != null ? cause.getMessage() : e.getMessage());
                // 继续处理其他收集器，但记录失败
            }
        }

        log.info("Data collection completed in {}ms, sources: {}",
                System.currentTimeMillis() - startTime, results.keySet());

        return results;
    }

    /**
     * 提取特征用于模型输入
     */
    public Map<String, Float> extractFeatures(Map<String, ThirdPartyData> allData) {
        Map<String, Float> features = new HashMap<>();

        // 征信特征
        ThirdPartyData creditData = allData.get("CREDIT_BUREAU");
        if (creditData != null && creditData.isSuccess()) {
            Map<String, Object> data = creditData.getParsedData();
            extractValue(features, data, "creditScore", "credit_score");
            extractValue(features, data, "debtRatio", "debt_ratio");
            extractValue(features, data, "overdueCount", "overdue_count");
            extractValue(features, data, "accountCount", "account_count");
        }

        // 芝麻信用特征
        ThirdPartyData zhimaData = allData.get("ZHIMA_CREDIT");
        if (zhimaData != null && zhimaData.isSuccess()) {
            Map<String, Object> data = zhimaData.getParsedData();
            extractValue(features, data, "zhimaScore", "zhima_score");
            extractValue(features, data, "behaviorScore", "behavior_score");
            extractBoolean(features, data, "idVerified", "id_verified");
            extractBoolean(features, data, "fraudRisk", "fraud_risk");
        }

        // 运营商特征
        ThirdPartyData carrierData = allData.get("CARRIER");
        if (carrierData != null && carrierData.isSuccess()) {
            Map<String, Object> data = carrierData.getParsedData();
            extractValue(features, data, "networkAge", "network_age");
            extractValue(features, data, "networkAgeScore", "network_age_score");
            extractValue(features, data, "callCount30d", "call_count_30d");
            extractValue(features, data, "locationStability", "location_stability");
        }

        return features;
    }

    private void extractValue(Map<String, Float> features, Map<String, Object> data,
                             String sourceKey, String targetKey) {
        Object value = data.get(sourceKey);
        if (value instanceof Number) {
            features.put(targetKey, ((Number) value).floatValue());
        }
    }

    private void extractBoolean(Map<String, Float> features, Map<String, Object> data,
                                String sourceKey, String targetKey) {
        Object value = data.get(sourceKey);
        if (value instanceof Boolean) {
            features.put(targetKey, ((Boolean) value) ? 1.0f : 0.0f);
        }
    }
}
