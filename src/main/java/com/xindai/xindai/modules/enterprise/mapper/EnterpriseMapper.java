package com.xindai.xindai.modules.enterprise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface EnterpriseMapper extends BaseMapper<Enterprise> {

    /**
     * 扣减企业已用额度（数据库级原子更新）
     * 使用SET used_limit = used_limit + #{amount}确保线程安全
     *
     * @param id 企业ID
     * @param amount 扣减金额
     * @return 影响行数，0表示剩余额度不足
     */
    @Update("UPDATE enterprise " +
            "SET used_limit = used_limit + #{amount}, " +
            "    updated_at = NOW() " +
            "WHERE id = #{id} AND credit_limit - used_limit >= #{amount}")
    int deductUsedLimit(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 恢复企业已用额度（数据库级原子更新）
     * 使用GREATEST确保used_limit不会变为负数
     *
     * @param id 企业ID
     * @param amount 恢复金额
     */
    @Update("UPDATE enterprise " +
            "SET used_limit = GREATEST(0, used_limit - #{amount}), " +
            "    updated_at = NOW() " +
            "WHERE id = #{id}")
    void recoverUsedLimit(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
