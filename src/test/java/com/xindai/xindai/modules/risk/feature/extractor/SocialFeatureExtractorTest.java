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
 * SocialFeatureExtractor 单元测试
 */
@DisplayName("SocialFeatureExtractor 单元测试")
class SocialFeatureExtractorTest {

    private SocialFeatureExtractor extractor;
    private User testUser;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        extractor = new SocialFeatureExtractor();
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
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", 0.8);
            socialFeatures.put("contact_stability", 0.9);
            socialFeatures.put("friend_quality_score", 0.85);
            socialFeatures.put("group_activity_score", 0.7);
            socialFeatures.put("profile_completeness", 0.95);
            socialFeatures.put("account_age", 36);
            socialFeatures.put("verification_level", 3);
            socialFeatures.put("interaction_frequency", 0.8);
            socialFeatures.put("endorsement_count", 10);
            socialFeatures.put("social_trust_score", 0.9);
            socialFeatures.put("network_diversity", 0.7);
            socialFeatures.put("community_score", 0.8);
            socialFeatures.put("reputation_score", 0.85);
            socialFeatures.put("influence_score", 0.6);

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertNotNull(features);
            assertTrue(features.containsKey("social_network_score"));
            assertTrue(features.containsKey("contact_stability"));
            assertTrue(features.containsKey("friend_quality_score"));
            assertTrue(features.containsKey("group_activity_score"));
            assertTrue(features.containsKey("profile_completeness"));
            assertTrue(features.containsKey("account_age"));
            assertTrue(features.containsKey("verification_level"));
            assertTrue(features.containsKey("interaction_frequency"));
            assertTrue(features.containsKey("endorsement_count"));
            assertTrue(features.containsKey("social_trust_score"));
            assertTrue(features.containsKey("network_diversity"));
            assertTrue(features.containsKey("community_score"));
            assertTrue(features.containsKey("reputation_score"));
            assertTrue(features.containsKey("influence_score"));
            assertEquals(14, features.size());
        }

        @Test
        @DisplayName("特征值在有效范围内")
        void extract_FeaturesInValidRange() {
            // Arrange
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", 0.8);
            socialFeatures.put("contact_stability", 0.9);
            socialFeatures.put("friend_quality_score", 0.85);
            socialFeatures.put("profile_completeness", 0.95);
            socialFeatures.put("interaction_frequency", 0.8);
            socialFeatures.put("social_trust_score", 0.9);
            socialFeatures.put("network_diversity", 0.7);
            socialFeatures.put("community_score", 0.8);
            socialFeatures.put("reputation_score", 0.85);
            socialFeatures.put("influence_score", 0.6);

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert - 验证所有0-1范围的特征
            String[] ratioFeatures = {
                    "social_network_score", "contact_stability", "friend_quality_score",
                    "group_activity_score", "profile_completeness", "interaction_frequency",
                    "social_trust_score", "network_diversity", "community_score",
                    "reputation_score", "influence_score"
            };

            for (String feature : ratioFeatures) {
                float value = features.get(feature);
                assertTrue(value >= 0 && value <= 1,
                        feature + " value " + value + " is not in range [0, 1]");
            }

            // account_age 应该是非负数
            assertTrue(features.get("account_age") >= 0);

            // verification_level 应该是非负数
            assertTrue(features.get("verification_level") >= 0);

            // endorsement_count 应该是非负数
            assertTrue(features.get("endorsement_count") >= 0);
        }

        @Test
        @DisplayName("空画像返回默认特征值")
        void extract_NullProfile_ReturnsDefaultFeatures() {
            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, null);

            // Assert
            assertNotNull(features);
            assertEquals(14, features.size());
            assertEquals(0.7f, features.get("social_network_score"));
            assertEquals(0.85f, features.get("contact_stability"));
            assertEquals(0.75f, features.get("friend_quality_score"));
            assertEquals(0.6f, features.get("group_activity_score"));
            assertEquals(0.9f, features.get("profile_completeness"));
            assertEquals(24f, features.get("account_age"));
            assertEquals(2f, features.get("verification_level"));
            assertEquals(0.7f, features.get("interaction_frequency"));
            assertEquals(5f, features.get("endorsement_count"));
            assertEquals(0.8f, features.get("social_trust_score"));
            assertEquals(0.6f, features.get("network_diversity"));
            assertEquals(0.7f, features.get("community_score"));
            assertEquals(0.75f, features.get("reputation_score"));
            assertEquals(0.5f, features.get("influence_score"));
        }

        @Test
        @DisplayName("空社交特征返回默认值")
        void extract_NullSocialFeatures_ReturnsDefaultFeatures() {
            // Arrange
            userProfile.setSocialFeatures(null);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertNotNull(features);
            assertEquals(14, features.size());
            assertEquals(0.7f, features.get("social_network_score"));
        }

        @Test
        @DisplayName("部分特征缺失使用默认值")
        void extract_PartialFeatures_UsesDefaultsForMissing() {
            // Arrange
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", 0.9);
            socialFeatures.put("account_age", 48);
            // 其他特征缺失

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(0.9f, features.get("social_network_score"));
            assertEquals(48f, features.get("account_age"));
            assertEquals(0.85f, features.get("contact_stability")); // 默认值
            assertEquals(0.75f, features.get("friend_quality_score")); // 默认值
        }

        @Test
        @DisplayName("字符串数值正确转换")
        void extract_StringValues_ConvertsCorrectly() {
            // Arrange
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", "0.85");
            socialFeatures.put("account_age", "24");
            socialFeatures.put("endorsement_count", "8");

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(0.85f, features.get("social_network_score"), 0.01);
            assertEquals(24f, features.get("account_age"));
            assertEquals(8f, features.get("endorsement_count"));
        }

        @Test
        @DisplayName("无效字符串使用默认值")
        void extract_InvalidString_UsesDefault() {
            // Arrange
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", "invalid");

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(0.7f, features.get("social_network_score")); // 默认值
        }

        @Test
        @DisplayName("高社交信用用户特征提取")
        void extract_HighSocialCredit_ExtractsCorrectly() {
            // Arrange
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", 0.95);
            socialFeatures.put("social_trust_score", 0.95);
            socialFeatures.put("reputation_score", 0.95);
            socialFeatures.put("endorsement_count", 50);
            socialFeatures.put("verification_level", 5);

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(0.95f, features.get("social_network_score"), 0.01);
            assertEquals(0.95f, features.get("social_trust_score"), 0.01);
            assertEquals(0.95f, features.get("reputation_score"), 0.01);
            assertEquals(50f, features.get("endorsement_count"));
            assertEquals(5f, features.get("verification_level"));
        }

        @Test
        @DisplayName("低社交活跃度用户特征提取")
        void extract_LowSocialActivity_ExtractsCorrectly() {
            // Arrange
            Map<String, Object> socialFeatures = new HashMap<>();
            socialFeatures.put("social_network_score", 0.3);
            socialFeatures.put("group_activity_score", 0.2);
            socialFeatures.put("interaction_frequency", 0.1);
            socialFeatures.put("endorsement_count", 0);
            socialFeatures.put("account_age", 1);

            userProfile.setSocialFeatures(socialFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(0.3f, features.get("social_network_score"), 0.01);
            assertEquals(0.2f, features.get("group_activity_score"), 0.01);
            assertEquals(0.1f, features.get("interaction_frequency"), 0.01);
            assertEquals(0f, features.get("endorsement_count"));
            assertEquals(1f, features.get("account_age"));
        }
    }

    @Nested
    @DisplayName("元数据测试")
    class MetadataTests {

        @Test
        @DisplayName("特征组名称为social")
        void getFeatureGroup_ReturnsSocial() {
            assertEquals("social", extractor.getFeatureGroup());
        }

        @Test
        @DisplayName("执行顺序为2")
        void getOrder_ReturnsTwo() {
            assertEquals(2, extractor.getOrder());
        }
    }
}
