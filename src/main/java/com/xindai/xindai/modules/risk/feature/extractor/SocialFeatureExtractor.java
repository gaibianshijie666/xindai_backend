package com.xindai.xindai.modules.risk.feature.extractor;

import com.xindai.xindai.modules.risk.feature.FeatureExtractor;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SocialFeatureExtractor implements FeatureExtractor {

    @Override
    public String getFeatureGroup() {
        return "social";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public Map<String, Float> extract(Long userId, User user, UserProfile profile) {
        Map<String, Float> features = new HashMap<>();

        if (profile != null && profile.getSocialFeatures() != null) {
            Map<String, Object> socialFeatures = profile.getSocialFeatures();
            features.put("social_network_score", getFloatValue(socialFeatures, "social_network_score", 0.7f));
            features.put("contact_stability", getFloatValue(socialFeatures, "contact_stability", 0.85f));
            features.put("friend_quality_score", getFloatValue(socialFeatures, "friend_quality_score", 0.75f));
            features.put("group_activity_score", getFloatValue(socialFeatures, "group_activity_score", 0.6f));
            features.put("profile_completeness", getFloatValue(socialFeatures, "profile_completeness", 0.9f));
            features.put("account_age", getFloatValue(socialFeatures, "account_age", 24f));
            features.put("verification_level", getFloatValue(socialFeatures, "verification_level", 2f));
            features.put("interaction_frequency", getFloatValue(socialFeatures, "interaction_frequency", 0.7f));
            features.put("endorsement_count", getFloatValue(socialFeatures, "endorsement_count", 5f));
            features.put("social_trust_score", getFloatValue(socialFeatures, "social_trust_score", 0.8f));
            features.put("network_diversity", getFloatValue(socialFeatures, "network_diversity", 0.6f));
            features.put("community_score", getFloatValue(socialFeatures, "community_score", 0.7f));
            features.put("reputation_score", getFloatValue(socialFeatures, "reputation_score", 0.75f));
            features.put("influence_score", getFloatValue(socialFeatures, "influence_score", 0.5f));
        } else {
            // 默认值
            features.put("social_network_score", 0.7f);
            features.put("contact_stability", 0.85f);
            features.put("friend_quality_score", 0.75f);
            features.put("group_activity_score", 0.6f);
            features.put("profile_completeness", 0.9f);
            features.put("account_age", 24f);
            features.put("verification_level", 2f);
            features.put("interaction_frequency", 0.7f);
            features.put("endorsement_count", 5f);
            features.put("social_trust_score", 0.8f);
            features.put("network_diversity", 0.6f);
            features.put("community_score", 0.7f);
            features.put("reputation_score", 0.75f);
            features.put("influence_score", 0.5f);
        }

        return features;
    }

    private float getFloatValue(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
