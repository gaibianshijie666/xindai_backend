package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.dto.*;

import com.xindai.xindai.modules.loan.entity.CreditLimit;

import java.util.List;

public interface LoanService {
    CreditLimitVO getCreditLimit(Long userId);
    LoanApplicationVO apply(Long userId, LoanApplyDTO dto);
    List<LoanApplicationVO> getApplications(Long userId);
    LoanApplicationVO getApplicationDetail(Long userId, Long applicationId);
    CreditLimit getOrCreateCreditLimit(Long userId);
    List<RepaymentPlanVO> getPendingRepayment(Long userId);
    List<LoanContractVO> getContracts(Long userId);

    // 新增接口
    LoanContractVO getContractDetail(Long userId, Long contractId);
    List<RepaymentPlanVO> getRepaymentPlansByContract(Long userId, Long contractId);
    List<RepaymentPlanVO> getAllRepaymentPlans(Long userId);
    RepaymentResultVO repay(Long userId, RepayDTO dto);
    RepaymentResultVO repayByPeriod(Long userId, Long contractId, Integer period);
    LimitEstimateVO estimateLimit(Long userId, LimitEstimateDTO dto);
    LimitEstimateVO predictLimit(Long userId, LimitEstimateDTO dto);
    CreditLimitVO applyLimitIncrease(Long userId, LimitApplyDTO dto);
}
