package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.ApplicationQueryDTO;
import com.xindai.xindai.modules.admin.dto.ApplicationReviewDTO;
import com.xindai.xindai.modules.admin.service.impl.AdminApplicationServiceImpl;
import com.xindai.xindai.modules.admin.vo.AdminApplicationVO;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminApplicationServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminApplicationServiceImpl 单元测试")
class AdminApplicationServiceImplTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RiskAssessmentMapper riskAssessmentMapper;

    @InjectMocks
    private AdminApplicationServiceImpl adminApplicationService;

    private LoanApplication testApplication;
    private User testUser;
    private RiskAssessment testRiskAssessment;

    @BeforeEach
    void setUp() {
        testApplication = new LoanApplication();
        testApplication.setId(1L);
        testApplication.setApplicationNo("LA202401010001");
        testApplication.setUserId(100L);
        testApplication.setAmount(new BigDecimal("50000"));
        testApplication.setTerm(12);
        testApplication.setPurpose("个人消费");
        testApplication.setStatus(0);
        testApplication.setCreatedAt(LocalDateTime.now());

        testUser = new User();
        testUser.setId(100L);
        testUser.setRealName("张三");
        testUser.setPhone("13800138000");

        testRiskAssessment = new RiskAssessment();
        testRiskAssessment.setId(1L);
        testRiskAssessment.setApplicationId(1L);
        testRiskAssessment.setRiskScore(new BigDecimal("35.5"));
    }

    @Nested
    @DisplayName("审核申请测试")
    class ReviewApplicationTests {

        @Test
        @DisplayName("审批通过 - 更新状态为已通过")
        void reviewApplication_Approve_UpdatesStatusToApproved() {
            // Arrange
            ApplicationReviewDTO reviewDTO = new ApplicationReviewDTO();
            reviewDTO.setApproved(true);
            reviewDTO.setReason("符合条件");

            when(loanApplicationMapper.selectById(1L)).thenReturn(testApplication);
            when(loanApplicationMapper.update(isNull(), any())).thenReturn(1);

            // Act
            adminApplicationService.reviewApplication(1L, reviewDTO);

            // Assert
            verify(loanApplicationMapper).update(isNull(), any());
        }

        @Test
        @DisplayName("审批拒绝 - 只更新状态")
        void reviewApplication_Reject_UpdatesStatusOnly() {
            // Arrange
            ApplicationReviewDTO reviewDTO = new ApplicationReviewDTO();
            reviewDTO.setApproved(false);
            reviewDTO.setReason("风险过高");

            when(loanApplicationMapper.selectById(1L)).thenReturn(testApplication);
            when(loanApplicationMapper.update(isNull(), any())).thenReturn(1);

            // Act
            adminApplicationService.reviewApplication(1L, reviewDTO);

            // Assert
            verify(loanApplicationMapper).update(isNull(), any());
        }

        @Test
        @DisplayName("已审核申请再次审核 - 抛出异常")
        void reviewApplication_AlreadyReviewed_ThrowsException() {
            // Arrange
            testApplication.setStatus(2); // 已通过状态
            ApplicationReviewDTO reviewDTO = new ApplicationReviewDTO();
            reviewDTO.setApproved(true);

            when(loanApplicationMapper.selectById(1L)).thenReturn(testApplication);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminApplicationService.reviewApplication(1L, reviewDTO));

            assertEquals(ErrorCode.APPLICATION_ALREADY_REVIEWED.getCode(), exception.getCode());
            verify(loanApplicationMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("申请不存在 - 抛出异常")
        void reviewApplication_ApplicationNotFound_ThrowsException() {
            // Arrange
            ApplicationReviewDTO reviewDTO = new ApplicationReviewDTO();
            reviewDTO.setApproved(true);

            when(loanApplicationMapper.selectById(1L)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> adminApplicationService.reviewApplication(1L, reviewDTO));

            assertEquals(ErrorCode.APPLICATION_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("审核中状态可以审核")
        void reviewApplication_ReviewingStatus_CanReview() {
            // Arrange
            testApplication.setStatus(1); // 审核中状态
            ApplicationReviewDTO reviewDTO = new ApplicationReviewDTO();
            reviewDTO.setApproved(true);

            when(loanApplicationMapper.selectById(1L)).thenReturn(testApplication);
            when(loanApplicationMapper.update(isNull(), any())).thenReturn(1);

            // Act
            adminApplicationService.reviewApplication(1L, reviewDTO);

            // Assert
            verify(loanApplicationMapper).update(isNull(), any());
        }
    }

    @Nested
    @DisplayName("获取申请列表测试")
    class GetApplicationListTests {

        @Test
        @DisplayName("带过滤条件查询 - 返回分页结果")
        void listApplications_WithFilters_ReturnsPagedResult() {
            // Arrange
            ApplicationQueryDTO queryDTO = new ApplicationQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setStatus(0);

            Page<LoanApplication> appPage = new Page<>(1, 10);
            appPage.setRecords(List.of(testApplication));
            appPage.setTotal(1);

            when(loanApplicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(appPage);
            when(userMapper.selectBatchIds(any())).thenReturn(List.of(testUser));
            when(riskAssessmentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(testRiskAssessment));

            // Act
            Page<AdminApplicationVO> result = adminApplicationService.getApplicationList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1, result.getTotal());
            AdminApplicationVO vo = result.getRecords().get(0);
            assertEquals("张三", vo.getUserName());
            assertEquals(new BigDecimal("50000"), vo.getAmount());
        }

        @Test
        @DisplayName("无过滤条件查询 - 返回所有结果")
        void listApplications_NoFilters_ReturnsAllResults() {
            // Arrange
            ApplicationQueryDTO queryDTO = new ApplicationQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setStatus(null);

            Page<LoanApplication> appPage = new Page<>(1, 10);
            appPage.setRecords(List.of(testApplication));
            appPage.setTotal(1);

            when(loanApplicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(appPage);
            when(userMapper.selectBatchIds(any())).thenReturn(List.of(testUser));
            when(riskAssessmentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(testRiskAssessment));

            // Act
            Page<AdminApplicationVO> result = adminApplicationService.getApplicationList(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("空列表查询 - 返回空结果")
        void listApplications_EmptyList_ReturnsEmptyResult() {
            // Arrange
            ApplicationQueryDTO queryDTO = new ApplicationQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<LoanApplication> appPage = new Page<>(1, 10);
            appPage.setRecords(Collections.emptyList());
            appPage.setTotal(0);

            when(loanApplicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(appPage);

            // Act
            Page<AdminApplicationVO> result = adminApplicationService.getApplicationList(queryDTO);

            // Assert
            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
            assertEquals(0, result.getTotal());
        }
    }

    @Nested
    @DisplayName("申请详情测试")
    class ApplicationDetailTests {

        @Test
        @DisplayName("获取申请详情 - 返回完整信息")
        void getApplicationDetail_ReturnsCompleteInfo() {
            // Arrange
            ApplicationQueryDTO queryDTO = new ApplicationQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<LoanApplication> appPage = new Page<>(1, 10);
            appPage.setRecords(List.of(testApplication));
            appPage.setTotal(1);

            when(loanApplicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(appPage);
            when(userMapper.selectBatchIds(any())).thenReturn(List.of(testUser));
            when(riskAssessmentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(testRiskAssessment));

            // Act
            Page<AdminApplicationVO> result = adminApplicationService.getApplicationList(queryDTO);
            AdminApplicationVO vo = result.getRecords().get(0);

            // Assert
            assertNotNull(vo);
            assertEquals(testApplication.getId(), vo.getId());
            assertEquals(testApplication.getApplicationNo(), vo.getApplicationNo());
            assertEquals(testApplication.getUserId(), vo.getUserId());
            assertEquals(testUser.getRealName(), vo.getUserName());
            assertEquals(testApplication.getAmount(), vo.getAmount());
            assertEquals(testApplication.getTerm(), vo.getTerm());
            assertEquals(testApplication.getPurpose(), vo.getPurpose());
            assertEquals(testApplication.getStatus(), vo.getStatus());
            assertEquals(testRiskAssessment.getRiskScore(), vo.getRiskScore());
        }
    }
}
