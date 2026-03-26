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
 * CreditFeatureExtractor 单元测试
 */
@DisplayName("CreditFeatureExtractor 单元测试")
class CreditFeatureExtractorTest {

    private CreditFeatureExtractor extractor;
    private User testUser;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        extractor = new CreditFeatureExtractor();
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
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("ficoRangeLow", 700);
            creditFeatures.put("ficoRangeHigh", 704);
            creditFeatures.put("openAcc", 12);
            creditFeatures.put("totalAcc", 25);
            creditFeatures.put("revolBal", 15000);
            creditFeatures.put("revolUtil", 35);
            creditFeatures.put("delinquency_2years", 0);

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertNotNull(features);
            assertTrue(features.containsKey("ficoRangeLow"));
            assertTrue(features.containsKey("ficoRangeHigh"));
            assertTrue(features.containsKey("openAcc"));
            assertTrue(features.containsKey("totalAcc"));
            assertTrue(features.containsKey("revolBal"));
            assertTrue(features.containsKey("revolUtil"));
            assertTrue(features.containsKey("delinquency_2years"));
            assertEquals(7, features.size());
        }

        @Test
        @DisplayName("特征值在有效范围内")
        void extract_FeaturesInValidRange() {
            // Arrange
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("ficoRangeLow", 700);
            creditFeatures.put("ficoRangeHigh", 704);
            creditFeatures.put("revolUtil", 35);
            creditFeatures.put("delinquency_2years", 0);

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            // FICO 分数范围 300-850
            assertTrue(features.get("ficoRangeLow") >= 300 && features.get("ficoRangeLow") <= 850);
            assertTrue(features.get("ficoRangeHigh") >= 300 && features.get("ficoRangeHigh") <= 850);

            // revolUtil 信用使用率 0-100
            assertTrue(features.get("revolUtil") >= 0 && features.get("revolUtil") <= 100);

            // delinquency_2years 应该是非负数
            assertTrue(features.get("delinquency_2years") >= 0);

            // openAcc 和 totalAcc 应该是正数
            assertTrue(features.get("openAcc") > 0);
            assertTrue(features.get("totalAcc") > 0);
        }

        @Test
        @DisplayName("空画像返回默认特征值")
        void extract_NullProfile_ReturnsDefaultFeatures() {
            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, null);

            // Assert
            assertNotNull(features);
            assertEquals(7, features.size());
            assertEquals(680f, features.get("ficoRangeLow"));
            assertEquals(684f, features.get("ficoRangeHigh"));
            assertEquals(10f, features.get("openAcc"));
            assertEquals(20f, features.get("totalAcc"));
            assertEquals(10000f, features.get("revolBal"));
            assertEquals(40f, features.get("revolUtil"));
            assertEquals(0f, features.get("delinquency_2years"));
        }

        @Test
        @DisplayName("空信用特征返回默认值")
        void extract_NullCreditFeatures_ReturnsDefaultFeatures() {
            // Arrange
            userProfile.setCreditFeatures(null);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertNotNull(features);
            assertEquals(7, features.size());
            assertEquals(680f, features.get("ficoRangeLow"));
        }

        @Test
        @DisplayName("部分特征缺失使用默认值")
        void extract_PartialFeatures_UsesDefaultsForMissing() {
            // Arrange
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("ficoRangeLow", 750);
            // 其他特征缺失

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(750f, features.get("ficoRangeLow"));
            assertEquals(684f, features.get("ficoRangeHigh")); // 默认值
            assertEquals(10f, features.get("openAcc")); // 默认值
        }

        @Test
        @DisplayName("字符串数值正确转换")
        void extract_StringValues_ConvertsCorrectly() {
            // Arrange
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("ficoRangeLow", "720");
            creditFeatures.put("revolUtil", "45.5");

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(720f, features.get("ficoRangeLow"));
            assertEquals(45.5f, features.get("revolUtil"), 0.01);
        }

        @Test
        @DisplayName("FICO低分边界测试")
        void extract_LowFicoScore_HandlesCorrectly() {
            // Arrange
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("ficoRangeLow", 350);
            creditFeatures.put("ficoRangeHigh", 354);

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(350f, features.get("ficoRangeLow"));
            assertEquals(354f, features.get("ficoRangeHigh"));
        }

        @Test
        @DisplayName("FICO高分边界测试")
        void extract_HighFicoScore_HandlesCorrectly() {
            // Arrange
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("ficoRangeLow", 845);
            creditFeatures.put("ficoRangeHigh", 850);

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(845f, features.get("ficoRangeLow"));
            assertEquals(850f, features.get("ficoRangeHigh"));
        }

        @Test
        @DisplayName("逾期记录特征正确提取")
        void extract_DelinquencyFeature_ExtractsCorrectly() {
            // Arrange
            Map<String, Object> creditFeatures = new HashMap<>();
            creditFeatures.put("delinquency_2years", 3);

            userProfile.setCreditFeatures(creditFeatures);

            // Act
            Map<String, Float> features = extractor.extract(1L, testUser, userProfile);

            // Assert
            assertEquals(3f, features.get("delinquency_2years"));
        }
    }

    @Nested
    @DisplayName("元数据测试")
    class MetadataTests {

        @Test
        @DisplayName("特征组名称为credit")
        void getFeatureGroup_ReturnsCredit() {
            assertEquals("credit", extractor.getFeatureGroup());
        }

        @Test
        @DisplayName("执行顺序为3")
        void getOrder_ReturnsThree() {
            assertEquals(3, extractor.getOrder());
        }
    }
}
