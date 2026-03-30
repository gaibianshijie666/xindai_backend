package com.xindai.xindai.modules.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.loan.entity.CreditLimit;
import com.xindai.xindai.modules.loan.mapper.CreditLimitMapper;
import com.xindai.xindai.modules.loan.service.CreditLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 信用额度服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditLimitServiceImpl implements CreditLimitService {

    private final CreditLimitMapper creditLimitMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditLimit deductLimit(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "扣减金额必须大于0");
        }

        // 先检查记录是否存在
        CreditLimit existing = getByUserId(userId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.LIMIT_NOT_FOUND, "用户额度记录不存在");
        }

        // 检查可用额度是否足够
        if (existing.getAvailableLimit().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.LIMIT_INSUFFICIENT,
                    "当前可用额度：" + existing.getAvailableLimit() + "，需要：" + amount);
        }

        // 执行数据库级别的原子更新，确保线程安全
        creditLimitMapper.deductLimit(userId, amount);

        // 清除缓存
        evictCreditLimitCache(userId);

        // 返回更新后的记录
        CreditLimit updatedLimit = getByUserId(userId);
        log.info("Deducted credit limit for user {}: -{}, new used: {}, new available: {}",
                userId, amount, updatedLimit.getUsedLimit(), updatedLimit.getAvailableLimit());

        return updatedLimit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditLimit recoverLimit(Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "恢复金额必须大于0");
        }

        CreditLimit limit = getByUserId(userId);
        if (limit == null) {
            throw new BusinessException(ErrorCode.LIMIT_NOT_FOUND, "用户额度记录不存在");
        }

        // 使用数据库级别的原子更新，确保线程安全
        // SET used_limit = GREATEST(0, used_limit - X), available_limit = total_limit - used_limit
        creditLimitMapper.recoverLimit(userId, amount);

        // 清除缓存
        evictCreditLimitCache(userId);

        // 返回更新后的记录
        CreditLimit updatedLimit = getByUserId(userId);
        log.info("Recovered credit limit for user {}: +{}, new used: {}, new available: {}",
                userId, amount, updatedLimit.getUsedLimit(), updatedLimit.getAvailableLimit());

        return updatedLimit;
    }

    @Override
    public CreditLimit getByUserId(Long userId) {
        return creditLimitMapper.selectOne(
                new LambdaQueryWrapper<CreditLimit>().eq(CreditLimit::getUserId, userId)
        );
    }

    /**
     * 清除用户额度缓存
     */
    private void evictCreditLimitCache(Long userId) {
        var cache = cacheManager.getCache("creditLimit");
        if (cache != null) {
            cache.evict(userId);
            log.debug("Evicted credit limit cache for user {}", userId);
        }
    }
}
