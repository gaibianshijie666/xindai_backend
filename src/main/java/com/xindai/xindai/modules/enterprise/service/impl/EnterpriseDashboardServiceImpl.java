package com.xindai.xindai.modules.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xindai.xindai.common.constants.RiskConstants;
import com.xindai.xindai.modules.enterprise.dto.DailyCustomerStats;
import com.xindai.xindai.modules.enterprise.dto.DailyLoanStats;
import com.xindai.xindai.modules.enterprise.dto.DashboardOverviewVO;
import com.xindai.xindai.modules.enterprise.dto.DashboardTrendVO;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseCustomerMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseDashboardService;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业数据看板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseDashboardServiceImpl implements EnterpriseDashboardService {

    private final EnterpriseCustomerMapper customerMapper;
    private final LoanApplicationMapper loanApplicationMapper;

    @Override
    public DashboardOverviewVO getOverview(Long enterpriseId) {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 今日统计
        vo.setTodayNewCustomers(getCustomerCountSince(enterpriseId, todayStart));
        LoanStats todayStats = getLoanStatsSince(enterpriseId, todayStart);
        vo.setTodayLoanCount(todayStats.count());
        vo.setTodayLoanAmount(todayStats.amount());

        // 累计统计
        vo.setTotalCustomers(getTotalCustomerCount(enterpriseId));
        LoanStats totalStats = getTotalLoanStats(enterpriseId);
        vo.setTotalLoanCount(totalStats.count());
        vo.setTotalLoanAmount(totalStats.amount());

        // 风险分布
        vo.setRiskDistribution(getRiskDistribution(enterpriseId));

        return vo;
    }

    private int getCustomerCountSince(Long enterpriseId, LocalDateTime since) {
        return customerMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .ge(EnterpriseCustomer::getCreatedAt, since)
        ).intValue();
    }

    private int getTotalCustomerCount(Long enterpriseId) {
        return customerMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
        ).intValue();
    }

    private LoanStats getLoanStatsSince(Long enterpriseId, LocalDateTime since) {
        return getLoanStatsWithCondition(enterpriseId, since, null);
    }

    private LoanStats getTotalLoanStats(Long enterpriseId) {
        return getLoanStatsWithCondition(enterpriseId, null, null);
    }

    private LoanStats getLoanStatsWithCondition(Long enterpriseId, LocalDateTime since, LocalDateTime until) {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("enterprise_id", enterpriseId);
        if (since != null) {
            wrapper.ge("created_at", since);
        }
        if (until != null) {
            wrapper.lt("created_at", until);
        }
        wrapper.select("COUNT(*) as count", "COALESCE(SUM(amount), 0) as totalAmount");

        List<Map<String, Object>> result = loanApplicationMapper.selectMaps(wrapper);
        if (result.isEmpty()) {
            return new LoanStats(0, BigDecimal.ZERO);
        }

        Map<String, Object> row = result.get(0);
        Number count = (Number) row.get("count");
        BigDecimal amount = (BigDecimal) row.get("totalAmount");
        return new LoanStats(count != null ? count.intValue() : 0, amount != null ? amount : BigDecimal.ZERO);
    }

    private DashboardOverviewVO.RiskDistribution getRiskDistribution(Long enterpriseId) {
        DashboardOverviewVO.RiskDistribution distribution = new DashboardOverviewVO.RiskDistribution();
        distribution.setLow(getCustomerCountByRiskLevel(enterpriseId, RiskConstants.RISK_LEVEL_LOW));
        distribution.setMedium(getCustomerCountByRiskLevel(enterpriseId, RiskConstants.RISK_LEVEL_MEDIUM));
        distribution.setHigh(getCustomerCountByRiskLevel(enterpriseId, RiskConstants.RISK_LEVEL_HIGH));
        return distribution;
    }

    private int getCustomerCountByRiskLevel(Long enterpriseId, int riskLevel) {
        return customerMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getRiskLevel, riskLevel)
        ).intValue();
    }

    /**
     * 借款统计数据
     */
    private record LoanStats(int count, BigDecimal amount) {}

    @Override
    public DashboardTrendVO getTrend(Long enterpriseId, LocalDate startDate, LocalDate endDate) {
        DashboardTrendVO vo = new DashboardTrendVO();
        List<DashboardTrendVO.TrendItem> items = new ArrayList<>();

        // 限制查询范围，最多查询90天
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 90) {
            startDate = endDate.minusDays(90);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 使用聚合查询获取借款统计数据（解决N+1查询问题）
        List<DailyLoanStats> loanStatsList = loanApplicationMapper.getDailyLoanStatsByEnterprise(
                enterpriseId, startDateTime, endDateTime);

        // 使用聚合查询获取客户统计数据（解决N+1查询问题）
        List<DailyCustomerStats> customerStatsList = customerMapper.getDailyCustomerStats(
                enterpriseId, startDateTime, endDateTime);

        // 转换为Map方便快速查找
        Map<LocalDate, DailyLoanStats> loanStatsMap = loanStatsList.stream()
                .collect(Collectors.toMap(DailyLoanStats::getDate, stats -> stats, (a, b) -> a));
        Map<LocalDate, DailyCustomerStats> customerStatsMap = customerStatsList.stream()
                .collect(Collectors.toMap(DailyCustomerStats::getDate, stats -> stats, (a, b) -> a));

        // 按天统计趋势
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DashboardTrendVO.TrendItem item = new DashboardTrendVO.TrendItem();
            item.setDate(date.toString());

            // 从聚合结果中获取数据，如果没有则为0
            DailyLoanStats loanStats = loanStatsMap.get(date);
            if (loanStats != null) {
                item.setLoanCount(loanStats.getLoanCount());
                item.setLoanAmount(loanStats.getLoanAmount() != null ? loanStats.getLoanAmount() : BigDecimal.ZERO);
            } else {
                item.setLoanCount(0);
                item.setLoanAmount(BigDecimal.ZERO);
            }

            DailyCustomerStats customerStats = customerStatsMap.get(date);
            if (customerStats != null) {
                item.setNewCustomerCount(customerStats.getNewCustomerCount());
            } else {
                item.setNewCustomerCount(0);
            }

            items.add(item);
        }

        vo.setItems(items);
        return vo;
    }
}
