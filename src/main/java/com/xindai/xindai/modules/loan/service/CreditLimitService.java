package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.entity.CreditLimit;

import java.math.BigDecimal;

/**
 * 信用额度服务
 */
public interface CreditLimitService {

    /**
     * 扣减额度（借款批准时调用）
     * 使用数据库级别的原子更新确保线程安全
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 更新后的额度记录
     */
    CreditLimit deductLimit(Long userId, BigDecimal amount);

    /**
     * 恢复额度（贷款结清时调用）
     * 使用数据库级别的原子更新确保线程安全
     *
     * @param userId 用户ID
     * @param amount 恢复金额
     * @return 更新后的额度记录
     */
    CreditLimit recoverLimit(Long userId, BigDecimal amount);

    /**
     * 获取用户额度记录
     *
     * @param userId 用户ID
     * @return 额度记录，不存在则返回null
     */
    CreditLimit getByUserId(Long userId);
}
