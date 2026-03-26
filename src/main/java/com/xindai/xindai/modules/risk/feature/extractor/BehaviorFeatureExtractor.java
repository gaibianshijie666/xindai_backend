package com.xindai.xindai.modules.risk.feature.extractor;

import com.xindai.xindai.modules.risk.feature.FeatureExtractor;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BehaviorFeatureExtractor implements FeatureExtractor {

    @Override
    public String getFeatureGroup() {
        return "behavior";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Map<String, Float> extract(Long userId, User user, UserProfile profile) {
        Map<String, Float> features = new HashMap<>();

        if (profile != null && profile.getBehaviorFeatures() != null) {
            Map<String, Object> behaviorFeatures = profile.getBehaviorFeatures();
            features.put("active_days_30d", getFloatValue(behaviorFeatures, "active_days_30d", 25f));
            features.put("avg_daily_transactions", getFloatValue(behaviorFeatures, "avg_daily_transactions", 3f));
            features.put("repayment_score", getFloatValue(behaviorFeatures, "repayment_score", 0.9f));
            features.put("login_frequency", getFloatValue(behaviorFeatures, "login_frequency", 15f));
            features.put("session_duration", getFloatValue(behaviorFeatures, "session_duration", 8f));
            features.put("device_changes", getFloatValue(behaviorFeatures, "device_changes", 1f));
            features.put("location_changes", getFloatValue(behaviorFeatures, "location_changes", 2f));
            features.put("night_activity_ratio", getFloatValue(behaviorFeatures, "night_activity_ratio", 0.1f));
            features.put("weekend_activity_ratio", getFloatValue(behaviorFeatures, "weekend_activity_ratio", 0.3f));
            features.put("app_usage_score", getFloatValue(behaviorFeatures, "app_usage_score", 0.8f));
        } else {
            // 默认值
            features.put("active_days_30d", 25f);
            features.put("avg_daily_transactions", 3f);
            features.put("repayment_score", 0.9f);
            features.put("login_frequency", 15f);
            features.put("session_duration", 8f);
            features.put("device_changes", 1f);
            features.put("location_changes", 2f);
            features.put("night_activity_ratio", 0.1f);
            features.put("weekend_activity_ratio", 0.3f);
            features.put("app_usage_score", 0.8f);
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
