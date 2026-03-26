package com.xindai.xindai.modules.risk.feature.extractor;

import com.xindai.xindai.modules.risk.feature.FeatureExtractor;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CreditFeatureExtractor implements FeatureExtractor {

    @Override
    public String getFeatureGroup() {
        return "credit";
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public Map<String, Float> extract(Long userId, User user, UserProfile profile) {
        Map<String, Float> features = new HashMap<>();

        if (profile != null && profile.getCreditFeatures() != null) {
            Map<String, Object> creditFeatures = profile.getCreditFeatures();
            features.put("ficoRangeLow", getFloatValue(creditFeatures, "ficoRangeLow", 680f));
            features.put("ficoRangeHigh", getFloatValue(creditFeatures, "ficoRangeHigh", 684f));
            features.put("openAcc", getFloatValue(creditFeatures, "openAcc", 10f));
            features.put("totalAcc", getFloatValue(creditFeatures, "totalAcc", 20f));
            features.put("revolBal", getFloatValue(creditFeatures, "revolBal", 10000f));
            features.put("revolUtil", getFloatValue(creditFeatures, "revolUtil", 40f));
            features.put("delinquency_2years", getFloatValue(creditFeatures, "delinquency_2years", 0f));
        } else {
            // 默认值
            features.put("ficoRangeLow", 680f);
            features.put("ficoRangeHigh", 684f);
            features.put("openAcc", 10f);
            features.put("totalAcc", 20f);
            features.put("revolBal", 10000f);
            features.put("revolUtil", 40f);
            features.put("delinquency_2years", 0f);
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
