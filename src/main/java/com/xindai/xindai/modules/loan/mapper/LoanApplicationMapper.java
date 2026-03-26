package com.xindai.xindai.modules.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xindai.xindai.modules.enterprise.dto.DailyLoanStats;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoanApplicationMapper extends BaseMapper<LoanApplication> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM loan_application WHERE status = 2")
    BigDecimal sumApprovedAmount();

    @Select("SELECT COUNT(*) FROM loan_application WHERE status = 2")
    int countApproved();

    /**
     * 获取指定日期范围内的每日借款统计（按企业ID分组）
     * 用于企业看板趋势查询，解决N+1问题
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COUNT(*) as loanCount, " +
            "COALESCE(SUM(amount), 0) as loanAmount " +
            "FROM loan_application " +
            "WHERE enterprise_id = #{enterpriseId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date")
    List<DailyLoanStats> getDailyLoanStatsByEnterprise(
            @Param("enterpriseId") Long enterpriseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 按天分组统计申请数量和状态分布（管理端仪表盘用）
     */
    @Select("SELECT DATE(created_at) as date, " +
            "COUNT(*) as loanCount, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as approvedCount, " +
            "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as rejectedCount " +
            "FROM loan_application " +
            "WHERE created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date")
    List<DailyLoanStats> getDailyLoanStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
