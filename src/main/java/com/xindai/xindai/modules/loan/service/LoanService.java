package com.xindai.xindai.modules.loan.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.enterprise.dto.DailyLoanStats;
import com.xindai.xindai.modules.loan.dto.*;

import com.xindai.xindai.modules.loan.entity.CreditLimit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LoanService {
    CreditLimitVO getCreditLimit(Long userId);
    LoanApplicationVO apply(Long userId, LoanApplyDTO dto);
    Page<LoanApplicationVO> getApplications(Long userId, int page, int size);
    LoanApplicationVO getApplicationDetail(Long userId, Long applicationId);
    CreditLimit getOrCreateCreditLimit(Long userId);
    List<RepaymentPlanVO> getPendingRepayment(Long userId);
    Page<LoanContractVO> getContracts(Long userId, int page, int size);

    // 新增接口
    LoanContractVO getContractDetail(Long userId, Long contractId);
    List<RepaymentPlanVO> getRepaymentPlansByContract(Long userId, Long contractId);
    Page<RepaymentPlanVO> getAllRepaymentPlans(Long userId, int page, int size);
    RepaymentResultVO repay(Long userId, RepayDTO dto);
    RepaymentResultVO repayByPeriod(Long userId, Long contractId, Integer period);
    LimitEstimateVO estimateLimit(Long userId, LimitEstimateDTO dto);
    LimitEstimateVO predictLimit(Long userId, LimitEstimateDTO dto);
    CreditLimitVO applyLimitIncrease(Long userId, LimitApplyDTO dto);

    // 统计方法（供AdminDashboard使用）
    int countApplicationsSince(LocalDateTime since);
    int countApplicationsByStatusSince(Integer status, LocalDateTime since);
    int countApproved();
    BigDecimal sumApprovedAmount();
    List<DailyLoanStats> getDailyLoanStats(LocalDateTime startDate, LocalDateTime endDate);
    long countAllContracts();
    long countOverdueContracts();
}
