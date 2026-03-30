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
     * Aggregate all features for a user.
     * Loan-specific features (loan_amount, loan_term, loan_interest_rate) are injected
     * by RiskAssessmentServiceImpl.injectLoanFeatures() since this service has no
     * dependency on the loan module.
     */
    public Map<String, Float> collectFeatures(Long userId, User user, UserProfile profile) {
        Map<String, Float> features = new HashMap<>();

        // 1. Collect features from third-party data sources
        try {
            Map<String, ThirdPartyData> allData = dataAggregationService.collectAll(
                    userId, user.getPhone(), user.getIdCard()
            );
            features.putAll(dataAggregationService.extractFeatures(allData));
        } catch (Exception e) {
            log.warn("Third-party data collection failed for user {}: {}", userId, e.getMessage());
        }

        // 2. Use strategy pattern to collect feature groups
        extractors.stream()
                .sorted(Comparator.comparingInt(FeatureExtractor::getOrder))
                .forEach(extractor -> {
                    try {
                        Map<String, Float> extractedFeatures = extractor.extract(userId, user, profile);
                        if (extractedFeatures != null) {
                            features.putAll(extractedFeatures);
                            log.debug("Extracted {} features from {}", extractedFeatures.size(), extractor.getFeatureGroup());
                        }
                    } catch (Exception e) {
                        log.warn("Feature extractor {} failed for user {}: {}",
                                extractor.getFeatureGroup(), userId, e.getMessage());
                    }
                });

        log.info("Total features collected for user {}: {}", userId, features.size());
        return features;
    }
}
