package com.xindai.xindai.modules.risk.feature.extractor;

import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BehaviorFeatureExtractor 单元测试
 */
@DisplayName("BehaviorFeatureExtractor 单元测试")
class BehaviorFeatureExtractorTest {

    private BehaviorFeatureExtractor extractor;
    private User testUser;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        extractor = new BehaviorFeatureExtractor();
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        userProfile = new UserProfile();
        userProfile.setUserId(1L);
    }

    @Nested
    @DisplayName("特征提取测试")
    class ExtractTests {

        @Test
        @DisplayName("提取返回所有必需特征")
        void extract_ReturnsAllRequiredFeatures() {
            // Arrange
            Map<String, Object> behaviorFeatures = new HashMap<>();
            behaviorFeatures.put("active_days_30d", 28);
            behaviorFeatures.put("avg_daily_transactions", 5);
            behaviorFeatures.put("repayment_score", 0.95);
            behaviorFeatures.put("login_frequency", 20);
            behaviorFeatures.put("session_duration", 10);
            behaviorFeatures.put("device_changes", 0);
            behaviorFeatures.put("location_changes", 1);
            behaviorFeatures.put("night_activity_ratio", 0.05);
            behaviorFeatures.put("weekend_activity_ratio", 0.25);
            behaviorFeatures.put("app_usage_score", 0.9);

            userProfile.setBehaviorFeatures(behaviorFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertNotNull(features);
            assertTrue(features.containsKey("active_days_30d"));
            assertTrue(features.containsKey("avg_daily_transactions"));
            assertTrue(features.containsKey("repayment_score"));
            assertTrue(features.containsKey("login_frequency"));
            assertTrue(features.containsKey("session_duration"));
            assertTrue(features.containsKey("device_changes"));
            assertTrue(features.containsKey("location_changes"));
            assertTrue(features.containsKey("night_activity_ratio"));
            assertTrue(features.containsKey("weekend_activity_ratio"));
            assertTrue(features.containsKey("app_usage_score"));
            assertEquals(10, features.size());
        }

        @Test
        @DisplayName("特征值在有效范围内")
        void extract_FeaturesInValidRange() {
            // Arrange
            Map<String, Object> behaviorFeatures = new HashMap<>();
            behaviorFeatures.put("active_days_30d", 28);
            behaviorFeatures.put("repayment_score", 0.95);

            userProfile.setBehaviorFeatures(behaviorFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            // active_days_30d 应该在 0-31 范围内
            assertTrue(features.get("active_days_30d") >= 0 && features.get("active_days_30d") <= 31);

            // repayment_score 应该在 0-1 范围内
            assertTrue(features.get("repayment_score") >= 0 && features.get("repayment_score") <= 1);

            // night_activity_ratio 应该在 0-1 范围内
            assertTrue(features.get("night_activity_ratio") >= 0 && features.get("night_activity_ratio") <= 1);

            // weekend_activity_ratio 应该在 0-1 范围内
            assertTrue(features.get("weekend_activity_ratio") >= 0 && features.get("weekend_activity_ratio") <= 1);

            // app_usage_score 应该在 0-1 范围内
            assertTrue(features.get("app_usage_score") >= 0 && features.get("app_usage_score") <= 1);
        }

        @Test
        @DisplayName("空画像返回默认特征值")
        void extract_NullProfile_ReturnsDefaultFeatures() {
            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, null);

            // Assert
            assertNotNull(features);
            assertEquals(10, features.size());
            assertEquals(25f, features.get("active_days_30d"));
            assertEquals(3f, features.get("avg_daily_transactions"));
            assertEquals(0.9f, features.get("repayment_score"));
        }

        @Test
        @DisplayName("空行为特征返回默认值")
        void extract_NullBehaviorFeatures_ReturnsDefaultFeatures() {
            // Arrange
            userProfile.setBehaviorFeatures(null);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertNotNull(features);
            assertEquals(10, features.size());
            assertEquals(25f, features.get("active_days_30d"));
        }

        @Test
        @DisplayName("部分特征缺失使用默认值")
        void extract_PartialFeatures_UsesDefaultsForMissing() {
            // Arrange
            Map<String, Object> behaviorFeatures = new HashMap<>();
            behaviorFeatures.put("active_days_30d", 30);
            // 其他特征缺失

            userProfile.setBehaviorFeatures(behaviorFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(30f, features.get("active_days_30d"));
            assertEquals(3f, features.get("avg_daily_transactions")); // 默认值
            assertEquals(0.9f, features.get("repayment_score")); // 默认值
        }

        @Test
        @DisplayName("字符串数值正确转换")
        void extract_StringValues_ConvertsCorrectly() {
            // Arrange
            Map<String, Object> behaviorFeatures = new HashMap<>();
            behaviorFeatures.put("active_days_30d", "28");
            behaviorFeatures.put("repayment_score", "0.85");

            userProfile.setBehaviorFeatures(behaviorFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(28f, features.get("active_days_30d"));
            assertEquals(0.85f, features.get("repayment_score"), 0.01);
        }

        @Test
        @DisplayName("无效字符串使用默认值")
        void extract_InvalidString_UsesDefault() {
            // Arrange
            Map<String, Object> behaviorFeatures = new HashMap<>();
            behaviorFeatures.put("active_days_30d", "invalid");

            userProfile.setBehaviorFeatures(behaviorFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(25f, features.get("active_days_30d")); // 默认值
        }
    }

    @Nested
    @DisplayName("元数据测试")
    class MetadataTests {

        @Test
        @DisplayName("特征组名称为behavior")
        void getFeatureGroup_ReturnsBehavior() {
            assertEquals("behavior", extractor.getFeatureGroup());
        }

        @Test
        @DisplayName("执行顺序为1")
        void getOrder_ReturnsOne() {
            assertEquals(1, extractor.getOrder());
        }
    }
}
