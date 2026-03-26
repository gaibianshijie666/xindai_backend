package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.dto.BankAccountDTO;
import com.xindai.xindai.modules.loan.dto.DisbursementVO;
import com.xindai.xindai.modules.loan.entity.BankAccount;
import com.xindai.xindai.modules.loan.entity.DisbursementRecord;

import java.util.List;

/**
 * 放款服务接口
 */
public interface DisbursementService {

    /**
     * 绑定银行卡
     *
     * @param userId 用户ID
     * @param dto    银行卡信息
     * @return 绑定的银行卡
     */
    BankAccount bindBankAccount(Long userId, BankAccountDTO dto);

    /**
     * 获取用户银行卡列表
     *
     * @param userId 用户ID
     * @return 银行卡列表
     */
    List<BankAccount> getBankAccounts(Long userId);

    /**
     * 获取用户默认银行卡
     *
     * @param userId 用户ID
     * @return 默认银行卡
     */
    BankAccount getDefaultBankAccount(Long userId);

    /**
     * 解绑银行卡
     *
     * @param userId      用户ID
     * @param bankAccountId 银行卡ID
     */
    void unbindBankAccount(Long userId, Long bankAccountId);

    /**
     * 创建放款记录
     *
     * @param contractId    合同ID
     * @param applicationId 申请ID
     * @param userId        用户ID
     * @param amount        放款金额
     * @param bankAccountId 银行卡ID
     * @return 放款记录
     */
    DisbursementRecord createDisbursement(Long contractId, Long applicationId,
                                          Long userId, java.math.BigDecimal amount, Long bankAccountId);

    /**
     * 执行放款
     *
     * @param disbursementId 放款记录ID
     */
    void executeDisbursement(Long disbursementId);

    /**
     * 获取放款记录列表（管理端）
     *
     * @param status 状态过滤（null表示全部）
     * @return 放款记录列表
     */
    List<DisbursementVO> getDisbursementRecords(Integer status);

    /**
     * 获取用户的放款记录
     *
     * @param userId 用户ID
     * @return 放款记录列表
     */
    List<DisbursementVO> getUserDisbursements(Long userId);
}
