package com.xindai.xindai.modules.risk.feature.extractor;

import com.xindai.xindai.modules.risk.feature.FeatureExtractor;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BaseFeatureExtractor implements FeatureExtractor {

    @Override
    public String getFeatureGroup() {
        return "base";
    }

    @Override
    public int getOrder() {
        return 0;  // 最先执行
    }

    @Override
    public Map<String, Float> extract(Long userId, User user, UserProfile profile) {
        Map<String, Float> features = new HashMap<>();

        if (profile != null) {
            // 收入特征
            if (profile.getAnnualIncome() != null) {
                features.put("income", profile.getAnnualIncome().floatValue());
            } else {
                features.put("income", 15000f);
            }

            // 就业年限
            if (profile.getEmploymentYears() != null) {
                features.put("employment_years", profile.getEmploymentYears().floatValue());
            } else {
                features.put("employment_years", 3f);
            }

            // 债务收入比
            if (profile.getDti() != null) {
                features.put("dti", profile.getDti().floatValue());
            }

            // 信用等级转换为数值
            if (profile.getCreditGrade() != null) {
                features.put("grade", gradeToFloat(profile.getCreditGrade()));
            }
        } else {
            // 没有用户画像时使用默认值
            features.put("income", 15000f);
            features.put("employment_years", 3f);
        }

        return features;
    }

    private float gradeToFloat(String grade) {
        return switch (grade.toUpperCase()) {
            case "A" -> 1f;
            case "B" -> 2f;
            case "C" -> 3f;
            case "D" -> 4f;
            case "E" -> 5f;
            case "F" -> 6f;
            case "G" -> 7f;
            default -> 3f;
        };
    }
}
