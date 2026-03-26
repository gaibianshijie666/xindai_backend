package com.xindai.xindai.modules.risk.feature;

import com.xindai.xindai.client.thirdparty.DataAggregationService;
import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureAggregationService {

    private final List<FeatureExtractor> extractors;
    private final DataAggregationService dataAggregationService;

    /**
     * 聚合所有特征
     */
    public Map<String, Float> collectFeatures(Long userId, User user, UserProfile profile) {
        Map<String, Float> features = new HashMap<>();

        // 1. 从第三方数据源收集特征
        Map<String, ThirdPartyData> allData = dataAggregationService.collectAll(
                userId, user.getPhone(), user.getIdCard()
        );
        features.putAll(dataAggregationService.extractFeatures(allData));

        // 2. 使用策略模式收集各类特征
        extractors.stream()
                .sorted(Comparator.comparingInt(FeatureExtractor::getOrder))
                .forEach(extractor -> {
                    Map<String, Float> extractedFeatures = extractor.extract(userId, user, profile);
                    features.putAll(extractedFeatures);
                    log.debug("Extracted {} features from {}", extractedFeatures.size(), extractor.getFeatureGroup());
                });

        log.info("Total features collected for user {}: {}", userId, features.size());
        return features;
    }
}
