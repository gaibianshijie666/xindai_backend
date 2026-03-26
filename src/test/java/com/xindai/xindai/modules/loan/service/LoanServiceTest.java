package com.xindai.xindai.modules.loan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.client.model.CreditLimitPredictionClient;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.loan.dto.CreditLimitVO;
import com.xindai.xindai.modules.loan.dto.LoanApplicationVO;
import com.xindai.xindai.modules.loan.dto.LoanApplyDTO;
import com.xindai.xindai.modules.loan.entity.CreditLimit;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.mapper.CreditLimitMapper;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.loan.service.impl.LoanServiceImpl;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService 单元测试")
class LoanServiceTest {

    @Mock
    private CreditLimitMapper creditLimitMapper;

    @Mock
    private LoanApplicationMapper applicationMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @Mock
    private CreditLimitCalculator creditLimitCalculator;

    @Mock
    private CreditLimitPredictionClient limitPredictionClient;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private LoanServiceImpl loanService;

    private CreditLimit creditLimit;
    private LoanApplyDTO applyDTO;
    private UserProfile userProfile;
    private RiskAssessment riskAssessment;

    @BeforeEach
    void setUp() {
        creditLimit = new CreditLimit();
        creditLimit.setId(1L);
        creditLimit.setUserId(1L);
        creditLimit.setTotalLimit(new BigDecimal("50000"));
        creditLimit.setUsedLimit(new BigDecimal("10000"));
        creditLimit.setAvailableLimit(new BigDecimal("40000"));
        creditLimit.setStatus(1);

        applyDTO = new LoanApplyDTO();
        applyDTO.setAmount(new BigDecimal("20000"));
        applyDTO.setTerm(12);
        applyDTO.setPurpose("消费");

        userProfile = new UserProfile();
        userProfile.setUserId(1L);
        userProfile.setAnnualIncome(new BigDecimal("120000"));
        userProfile.setRiskScore(50.0);
        userProfile.setCreditGrade("B");

        riskAssessment = new RiskAssessment();
        riskAssessment.setId(1L);
        riskAssessment.setRiskScore(new BigDecimal("55"));
        riskAssessment.setDecision("APPROVE");
    }

    @Nested
    @DisplayName("获取额度测试")
    class GetCreditLimitTests {

        @Test
        @DisplayName("获取已存在的额度 - 成功")
        void getCreditLimit_Existing_Success() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("50000"));

            CreditLimitVO result = loanService.getCreditLimit(1L);

            assertNotNull(result);
            assertEquals(new BigDecimal("50000"), result.getTotalLimit());
            assertEquals(new BigDecimal("40000"), result.getAvailableLimit());
        }

        @Test
        @DisplayName("额度不存在时创建新额度")
        void getCreditLimit_NotExists_CreatesNew() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);
            when(limitPredictionClient.predictLimitSimple(anyLong(), any(BigDecimal.class), anyString(),
                    any(BigDecimal.class), any(BigDecimal.class))).thenReturn(new BigDecimal("30000"));
            when(creditLimitMapper.insert(any(CreditLimit.class))).thenReturn(1);

            CreditLimitVO result = loanService.getCreditLimit(1L);

            assertNotNull(result);
            verify(creditLimitMapper).insert(any(CreditLimit.class));
        }

        @Test
        @DisplayName("无用户画像时使用默认额度")
        void getCreditLimit_NoProfile_UseDefault() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(creditLimitMapper.insert(any(CreditLimit.class))).thenReturn(1);

            CreditLimitVO result = loanService.getCreditLimit(1L);

            assertNotNull(result);
            assertEquals(new BigDecimal("10000"), result.getTotalLimit());
        }
    }

    @Nested
    @DisplayName("借款申请测试")
    class ApplyTests {

        @Test
        @DisplayName("申请成功 - 自动审批通过")
        void apply_Success_AutoApproved() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                LoanApplication app = inv.getArgument(0);
                app.setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("48000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(new BigDecimal("20000"), result.getAmount());
            assertEquals(12, result.getTerm());
        }

        @Test
        @DisplayName("申请金额超过可用额度 - 抛出异常")
        void apply_ExceedLimit_ThrowsException() {
            applyDTO.setAmount(new BigDecimal("50000"));
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> loanService.apply(1L, applyDTO));

            assertEquals(ErrorCode.LIMIT_INSUFFICIENT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("申请金额等于可用额度 - 成功")
        void apply_EqualToLimit_Success() {
            applyDTO.setAmount(new BigDecimal("40000"));
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("40000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(new BigDecimal("40000"), result.getAmount());
        }

        @Test
        @DisplayName("风控评估拒绝申请")
        void apply_RiskRejected() {
            riskAssessment.setDecision("REJECT");
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("30000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(3, result.getStatus()); // 拒绝状态
        }

        @Test
        @DisplayName("风控评估需要人工审核")
        void apply_ManualReview() {
            riskAssessment.setDecision("MANUAL_REVIEW");
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("45000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(2, result.getStatus()); // 人工审核状态
        }

        @Test
        @DisplayName("风控评估失败时保持待审核状态")
        void apply_RiskAssessmentFailed_KeepPending() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt()))
                    .thenThrow(new RuntimeException("风控服务异常"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(0, result.getStatus()); // 待审核状态
        }
    }

    @Nested
    @DisplayName("获取申请列表测试")
    class GetApplicationsTests {

        @Test
        @DisplayName("获取申请列表 - 成功")
        void getApplications_Success() {
            LoanApplication app1 = new LoanApplication();
            app1.setId(1L);
            app1.setUserId(1L);
            app1.setAmount(new BigDecimal("10000"));
            app1.setTerm(12);
            app1.setStatus(1);

            when(applicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10, 1).setRecords(List.of(app1)));

            var resultPage = loanService.getApplications(1L, 1, 10);
            List<LoanApplicationVO> result = resultPage.getRecords();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(new BigDecimal("10000"), result.get(0).getAmount());
        }

        @Test
        @DisplayName("获取申请列表 - 空列表")
        void getApplications_EmptyList() {
            when(applicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10, 0).setRecords(Collections.emptyList()));

            var resultPage = loanService.getApplications(1L, 1, 10);
            List<LoanApplicationVO> result = resultPage.getRecords();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("获取申请详情测试")
    class GetApplicationDetailTests {

        @Test
        @DisplayName("获取申请详情 - 成功")
        void getApplicationDetail_Success() {
            LoanApplication application = new LoanApplication();
            application.setId(1L);
            application.setUserId(1L);
            application.setAmount(new BigDecimal("10000"));
            application.setTerm(12);
            application.setStatus(1);

            when(applicationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(application);

            LoanApplicationVO result = loanService.getApplicationDetail(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(new BigDecimal("10000"), result.getAmount());
        }

        @Test
        @DisplayName("申请不存在 - 抛出异常")
        void getApplicationDetail_NotFound_ThrowsException() {
            when(applicationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> loanService.getApplicationDetail(1L, 999L));

            assertEquals(ErrorCode.APPLICATION_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("申请不属于当前用户 - 抛出异常")
        void getApplicationDetail_NotOwner_ThrowsException() {
            LoanApplication application = new LoanApplication();
            application.setId(1L);
            application.setUserId(2L); // 不同用户

            when(applicationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class, () -> loanService.getApplicationDetail(1L, 1L));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @ParameterizedTest
        @CsvSource({
                "1000, 3",      // 最小金额和最短期限
                "40000, 24",    // 最大可用金额和最长期限
                "5000, 6",      // 中等金额和期限
        })
        @DisplayName("不同有效金额和期限组合")
        void apply_ValidAmountAndTerm_Success(BigDecimal amount, Integer term) {
            applyDTO.setAmount(amount);
            applyDTO.setTerm(term);

            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("45000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(amount, result.getAmount());
            assertEquals(term, result.getTerm());
        }

        @Test
        @DisplayName("额度恰好用完场景")
        void apply_UseAllAvailableLimit_Success() {
            creditLimit.setAvailableLimit(new BigDecimal("10000"));
            applyDTO.setAmount(new BigDecimal("10000"));

            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("10000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
            assertEquals(new BigDecimal("10000"), result.getAmount());
        }

        @ParameterizedTest
        @ValueSource(strings = {"消费", "装修", "教育", "医疗", "旅游", ""})
        @DisplayName("不同借款用途")
        void apply_DifferentPurposes_Success(String purpose) {
            applyDTO.setPurpose(purpose);

            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(applicationMapper.insert(any(LoanApplication.class))).thenAnswer(inv -> {
                inv.getArgument(0, LoanApplication.class).setId(1L);
                return 1;
            });
            when(riskAssessmentService.assess(anyLong(), anyLong(), anyInt())).thenReturn(riskAssessment);
            when(applicationMapper.updateById(any(LoanApplication.class))).thenReturn(1);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("45000"));

            LoanApplicationVO result = loanService.apply(1L, applyDTO);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("额度计算测试")
    class CreditLimitCalculationTests {

        @Test
        @DisplayName("创建或获取额度 - 已存在")
        void getOrCreateCreditLimit_Exists() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(creditLimit);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);
            when(creditLimitCalculator.adjustByRiskScore(any(BigDecimal.class), anyDouble()))
                    .thenReturn(new BigDecimal("50000"));

            CreditLimit result = loanService.getOrCreateCreditLimit(1L);

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
        }

        @Test
        @DisplayName("创建或获取额度 - 新建")
        void getOrCreateCreditLimit_CreateNew() {
            when(creditLimitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(userProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userProfile);
            when(limitPredictionClient.predictLimitSimple(anyLong(), any(BigDecimal.class), anyString(),
                    any(BigDecimal.class), any(BigDecimal.class))).thenReturn(new BigDecimal("30000"));
            when(creditLimitMapper.insert(any(CreditLimit.class))).thenReturn(1);

            CreditLimit result = loanService.getOrCreateCreditLimit(1L);

            assertNotNull(result);
            assertEquals(new BigDecimal("30000"), result.getTotalLimit());
        }
    }
}
