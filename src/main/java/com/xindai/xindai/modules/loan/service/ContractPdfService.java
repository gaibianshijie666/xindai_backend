package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.user.entity.User;

import java.util.List;

/**
 * 电子合同PDF生成服务接口
 */
public interface ContractPdfService {

    /**
     * 生成借款合同PDF
     *
     * @param contract       借款合同
     * @param user           借款人
     * @param repaymentPlans 还款计划列表
     * @return PDF字节数组
     */
    byte[] generateContract(LoanContract contract, User user, List<RepaymentPlan> repaymentPlans);
}
