package com.xindai.xindai.modules.enterprise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xindai.xindai.modules.enterprise.dto.DailyCustomerStats;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EnterpriseCustomerMapper extends BaseMapper<EnterpriseCustomer> {

    /**
     * 获取指定日期范围内的每日新增客户统计（按企业ID分组）
     * 用于企业看板趋势查询，解决N+1问题
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COUNT(*) as newCustomerCount " +
            "FROM enterprise_customer " +
            "WHERE enterprise_id = #{enterpriseId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date")
    List<DailyCustomerStats> getDailyCustomerStats(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
