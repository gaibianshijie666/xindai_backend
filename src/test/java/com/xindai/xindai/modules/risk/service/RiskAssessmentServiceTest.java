package com.xindai.xindai.modules.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.client.model.ModelServiceClient;
import com.xindai.xindai.client.model.dto.PredictResponse;
import com.xindai.xindai.client.thirdparty.DataAggregationService;
import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.feature.FeatureAggregationService;
import com.xindai.xindai.modules.risk.mapper.BlacklistMapper;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.risk.service.impl.RiskAssessmentServiceImpl;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAssessmentService 单元测试")
class RiskAssessmentServiceTest {

    @Mock
    private ModelServiceClient modelServiceClient;

    @Mock
    private RiskAssessmentMapper assessmentMapper;

    @Mock
    private BlacklistMapper blacklistMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private DataAggregationService dataAggregationService;

    @Mock
    private FeatureAggregationService featureAggregationService;

    @InjectMocks
    private RiskAssessmentServiceImpl riskAssessmentService;

    private User testUser;
    private PredictResponse mockResponse;
    private UserProfile userProfile;
    private Map<String, ThirdPartyData> mockThirdPartyData;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        testUser.setIdCard("110101199001011234");

        mockResponse = createPredictResponse(72.5, 2, "APPROVE", "v1.0.0", 100, null);

        userProfile = new UserProfile();
        userProfile.setUserId(1L);
        userProfile.setAnnualIncome(new BigDecimal("120000"));
        userProfile.setEmploymentYears(5);
        userProfile.setDti(new BigDecimal("0.25"));
        userProfile.setCreditGrade("B");

        mockThirdPartyData = new HashMap<>();
    }

    private PredictResponse createPredictResponse(double riskScore, int riskLevel, String decision,
                                                   String modelVersion, long processingTimeMs,
                                                   List<Map<String, Object>> factors) {
        PredictResponse response = new PredictResponse();
        response.setCode(0);
        response.setMessage("success");
        response.setModelVersion(modelVersion);
        response.setProcessingTimeMs((int) processingTimeMs);

        Map<String, Object> data = new HashMap<>();
        data.put("risk_score", riskScore);
        data.put("risk_level", riskLevel);
        data.put("decision", decision);
        data.put("confidence", 0.85);
        if (factors != null) {
            data.put("factors", factors);
        }
        response.setData(data);

        return response;
    }

    @Nested
    @DisplayName("风险评估主流程测试")
    class AssessTests {

        @Test
        @DisplayName("评估成功 - 低风险用户")
        void assess_Success_LowRisk() {
            mockResponse = createPredictResponse(25.0, 1, "APPROVE", "v1.0.0", 100, null);

            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
            assertEquals("APPROVE", result.getDecision());
            assertEquals(1, result.getRiskLevel());
            verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object.class));
        }

        @Test
        @DisplayName("评估成功 - 中风险用户需要人工审核")
        void assess_Success_MediumRisk() {
            mockResponse = createPredictResponse(55.0, 2, "MANUAL_REVIEW", "v1.0.0", 100, null);

            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
            assertEquals("MANUAL_REVIEW", result.getDecision());
        }

        @Test
        @DisplayName("评估成功 - 高风险用户拒绝")
        void assess_Success_HighRisk() {
            mockResponse = createPredictResponse(85.0, 3, "REJECT", "v1.0.0", 100, null);

            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
            assertEquals("REJECT", result.getDecision());
            assertEquals(3, result.getRiskLevel());
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void assess_UserNotFound_ThrowsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
            when(userMapper.selectById(1L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> riskAssessmentService.assess(1L, null, 1));

            assertEquals("用户不存在", exception.getMessage());
            verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object.class));
        }

        @Test
        @DisplayName("获取分布式锁失败 - 抛出异常")
        void assess_LockFailed_ThrowsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> riskAssessmentService.assess(1L, null, 1));

            assertTrue(exception.getMessage().contains("正在进行中"));
        }

        @Test
        @DisplayName("评估完成后释放锁")
        void assess_LockReleased_AfterCompletion() {
            setupSuccessfulAssessMocks();

            riskAssessmentService.assess(1L, null, 1);

            verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object.class));
        }

        @Test
        @DisplayName("评估异常时仍释放锁")
        void assess_LockReleased_OnException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
            when(userMapper.selectById(1L)).thenThrow(new RuntimeException("数据库异常"));

            assertThrows(RuntimeException.class, () -> riskAssessmentService.assess(1L, null, 1));

            verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object.class));
        }
    }

    @Nested
    @DisplayName("黑名单检测测试")
    class BlacklistTests {

        @Test
        @DisplayName("手机号在黑名单中 - 自动拒绝")
        void assess_PhoneInBlacklist_AutoReject() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            Blacklist blacklist = new Blacklist();
            blacklist.setType(1);
            blacklist.setValue(testUser.getPhone());
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(blacklist);
            when(assessmentMapper.insert(any(RiskAssessment.class))).thenReturn(1);

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertEquals("REJECT", result.getDecision());
            assertEquals(new BigDecimal("100"), result.getRiskScore());
            verify(modelServiceClient, never()).predict(anyString(), any());
        }

        @Test
        @DisplayName("身份证在黑名单中 - 自动拒绝")
        void assess_IdCardInBlacklist_AutoReject() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            // 手机号不在黑名单
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null)  // 手机号查询返回null
                    .thenReturn(new Blacklist());  // 身份证查询返回存在
            when(assessmentMapper.insert(any(RiskAssessment.class))).thenReturn(1);

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertEquals("REJECT", result.getDecision());
        }

        @Test
        @DisplayName("用户无身份证 - 跳过身份证黑名单检查")
        void assess_NoIdCard_SkipIdCardCheck() {
            testUser.setIdCard(null);
            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
            assertEquals("APPROVE", result.getDecision());
        }
    }

    @Nested
    @DisplayName("特征收集测试")
    class FeatureCollectionTests {

        @Test
        @DisplayName("有用户画像时收集完整特征")
        void assess_WithProfile_CollectFullFeatures() {
            userProfile.setBehaviorFeatures(Map.of(
                    "active_days_30d", 28,
                    "repayment_score", 0.95
            ));
            userProfile.setSocialFeatures(Map.of(
                    "social_network_score", 0.8
            ));
            userProfile.setCreditFeatures(Map.of(
                    "ficoRangeLow", 700
            ));

            setupSuccessfulAssessMocks();
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
            verify(modelServiceClient).predict(anyString(), any(Map.class));
        }

        @Test
        @DisplayName("无用户画像时使用默认特征")
        void assess_NoProfile_UseDefaultFeatures() {
            setupSuccessfulAssessMocks();
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
        }

        @Test
        @DisplayName("画像字段为空时使用默认值")
        void assess_ProfileWithNullFields_UseDefaults() {
            userProfile.setAnnualIncome(null);
            userProfile.setEmploymentYears(null);
            userProfile.setDti(null);
            userProfile.setCreditGrade(null);

            setupSuccessfulAssessMocks();
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("模型调用测试")
    class ModelServiceTests {

        @Test
        @DisplayName("模型返回带因素列表")
        void assess_ModelReturnsFactors() {
            List<Map<String, Object>> factors = List.of(
                    Map.of("feature", "income", "importance", 0.3),
                    Map.of("feature", "credit_score", "importance", 0.25)
            );
            mockResponse = createPredictResponse(72.5, 2, "APPROVE", "v1.0.0", 100, factors);

            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
            assertNotNull(result.getFactors());
        }

        @Test
        @DisplayName("模型返回无因素列表")
        void assess_ModelReturnsNoFactors() {
            mockResponse = createPredictResponse(72.5, 2, "APPROVE", "v1.0.0", 100, null);

            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("评估历史查询测试")
    class AssessmentHistoryTests {

        @Test
        @DisplayName("获取用户评估历史")
        void getAssessmentHistory_Success() {
            RiskAssessment assessment1 = new RiskAssessment();
            assessment1.setId(1L);
            assessment1.setUserId(1L);

            RiskAssessment assessment2 = new RiskAssessment();
            assessment2.setId(2L);
            assessment2.setUserId(1L);

            when(assessmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 20, 2).setRecords(List.of(assessment1, assessment2)));

            var result = riskAssessmentService.getAssessmentHistory(1L, 1, 20);

            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
        }

        @Test
        @DisplayName("用户无评估历史返回空列表")
        void getAssessmentHistory_Empty() {
            when(assessmentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 20, 0).setRecords(List.of()));

            var result = riskAssessmentService.getAssessmentHistory(1L, 1, 20);

            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("模型服务降级测试")
    class ModelServiceFallbackTests {

        @Test
        @DisplayName("模型服务不可用时抛出异常")
        void assess_ModelServiceUnavailable_ThrowsException() {
            setupSuccessfulAssessMocks();
            when(modelServiceClient.predict(anyString(), any(Map.class)))
                    .thenThrow(new RuntimeException("Model service unavailable"));

            assertThrows(RuntimeException.class, () -> riskAssessmentService.assess(1L, null, 1));

            verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object.class));
        }
    }

    @Nested
    @DisplayName("并发请求处理测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("并发请求时锁机制正常工作")
        void assess_ConcurrentRequest_HandlesWithLock() {
            // 第一次获取锁成功
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);
            when(modelServiceClient.predict(anyString(), any(Map.class))).thenReturn(mockResponse);
            when(assessmentMapper.insert(any(RiskAssessment.class))).thenReturn(1);

            // 执行第一次评估
            RiskAssessment result1 = riskAssessmentService.assess(1L, null, 1);
            assertNotNull(result1);

            // 验证锁被释放（通过Lua脚本执行）
            verify(redisTemplate).execute(any(RedisScript.class), any(List.class), any(Object.class));
        }

        @Test
        @DisplayName("锁获取失败时返回友好提示")
        void assess_LockAcquisitionFailed_ReturnsFriendlyMessage() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> riskAssessmentService.assess(1L, null, 1));

            assertTrue(exception.getMessage().contains("正在进行中") || exception.getMessage().contains("稍后"));
        }
    }

    @Nested
    @DisplayName("决策逻辑测试")
    class DecisionLogicTests {

        @Test
        @DisplayName("高风险用户返回拒绝决策")
        void assess_HighRisk_ReturnsRejectDecision() {
            mockResponse = createPredictResponse(85.0, 3, "REJECT", "v1.0.0", 100, null);
            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertEquals("REJECT", result.getDecision());
            assertEquals(3, result.getRiskLevel());
        }

        @Test
        @DisplayName("低风险用户返回通过决策")
        void assess_LowRisk_ReturnsApproveDecision() {
            mockResponse = createPredictResponse(15.0, 1, "APPROVE", "v1.0.0", 100, null);
            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertEquals("APPROVE", result.getDecision());
            assertEquals(1, result.getRiskLevel());
        }

        @Test
        @DisplayName("有效请求保存正确决策和评估信息")
        void assess_ValidRequest_SavesAssessmentWithCorrectDecision() {
            List<Map<String, Object>> factors = List.of(
                    Map.of("feature", "credit_score", "importance", 0.35),
                    Map.of("feature", "income", "importance", 0.25)
            );
            mockResponse = createPredictResponse(45.0, 2, "MANUAL_REVIEW", "v2.0.0", 150, factors);
            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, 100L, 1);

            // 验证保存的信息正确
            assertEquals(1L, result.getUserId());
            assertEquals(100L, result.getApplicationId());
            assertEquals(1, result.getAssessmentType());
            assertEquals(new BigDecimal("45.0"), result.getRiskScore());
            assertEquals(2, result.getRiskLevel());
            assertEquals("MANUAL_REVIEW", result.getDecision());
            assertEquals("v2.0.0", result.getModelVersion());
            assertNotNull(result.getAssessmentNo());
            assertNotNull(result.getFactors());

            // 验证mapper被调用
            verify(assessmentMapper).insert(any(RiskAssessment.class));
        }
    }

    @Nested
    @DisplayName("评估详情查询测试")
    class AssessmentDetailTests {

        @Test
        @DisplayName("根据评估号获取详情 - 成功")
        void getByAssessmentNo_Success() {
            RiskAssessment assessment = new RiskAssessment();
            assessment.setAssessmentNo("RA123456");
            assessment.setUserId(1L);

            when(assessmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assessment);

            RiskAssessment result = riskAssessmentService.getByAssessmentNo("RA123456");

            assertNotNull(result);
            assertEquals("RA123456", result.getAssessmentNo());
        }

        @Test
        @DisplayName("评估号不存在返回null")
        void getByAssessmentNo_NotFound() {
            when(assessmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            RiskAssessment result = riskAssessmentService.getByAssessmentNo("NOT_EXIST");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("信用等级转换测试")
    class CreditGradeTests {

        @ParameterizedTest
        @CsvSource({
                "A, 1.0",
                "B, 2.0",
                "C, 3.0",
                "D, 4.0",
                "E, 5.0",
                "F, 6.0",
                "G, 7.0",
                "X, 3.0"  // 未知等级使用默认值
        })
        @DisplayName("不同信用等级转换为正确的数值")
        void assess_DifferentCreditGrades(String grade, float expectedValue) {
            userProfile.setCreditGrade(grade);
            setupSuccessfulAssessMocks();

            RiskAssessment result = riskAssessmentService.assess(1L, null, 1);

            assertNotNull(result);
        }
    }

    // ========== Helper Methods ==========

    private void setupSuccessfulAssessMocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(blacklistMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);
        when(featureAggregationService.collectFeatures(anyLong(), any(User.class), any(UserProfile.class)))
                .thenReturn(new HashMap<>());
        when(modelServiceClient.predict(anyString(), any(Map.class))).thenReturn(mockResponse);
        when(assessmentMapper.insert(any(RiskAssessment.class))).thenReturn(1);
    }
}
