package com.xindai.xindai.modules.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xindai.xindai.modules.loan.entity.CreditLimit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 信用额度Mapper
 */
@Mapper
public interface CreditLimitMapper extends BaseMapper<CreditLimit> {

    /**
     * 扣减额度（数据库级原子更新）
     * 使用SET used_limit = used_limit + #{amount}确保线程安全
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     */
    @Update("UPDATE credit_limit " +
            "SET used_limit = used_limit + #{amount}, " +
            "    available_limit = total_limit - (used_limit + #{amount}), " +
            "    updated_at = NOW() " +
            "WHERE user_id = #{userId} AND available_limit >= #{amount}")
    void deductLimit(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    /**
     * 恢复额度（数据库级原子更新）
     * 使用GREATEST确保used_limit不会变为负数
     *
     * @param userId 用户ID
     * @param amount 恢复金额
     */
    @Update("UPDATE credit_limit " +
            "SET used_limit = GREATEST(0, used_limit - #{amount}), " +
            "    available_limit = total_limit - GREATEST(0, used_limit - #{amount}), " +
            "    updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    void recoverLimit(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
}
