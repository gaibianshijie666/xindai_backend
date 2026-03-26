package com.xindai.xindai.modules.admin.service.impl;

import com.xindai.xindai.common.constants.RiskConstants;
import com.xindai.xindai.modules.admin.service.AdminDashboardService;
import com.xindai.xindai.modules.admin.vo.DashboardOverviewVO;
import com.xindai.xindai.modules.admin.vo.RiskStatsVO;
import com.xindai.xindai.modules.enterprise.dto.DailyLoanStats;
import com.xindai.xindai.modules.loan.service.LoanService;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import com.xindai.xindai.modules.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final LoanService loanService;
    private final RiskAssessmentService riskAssessmentService;
    private final UserProfileService userProfileService;

    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    @Override
    @Cacheable(value = "dashboardStats", key = "'overview'")
    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        vo.setTodayApplications(loanService.countApplicationsSince(todayStart));
        vo.setTodayApproved(loanService.countApplicationsByStatusSince(STATUS_APPROVED, todayStart));
        vo.setTodayRejected(loanService.countApplicationsByStatusSince(STATUS_REJECTED, todayStart));

        int todayTotal = vo.getTodayApplications();
        if (todayTotal > 0) {
            BigDecimal rate = BigDecimal.valueOf(vo.getTodayApproved())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(todayTotal), 2, RoundingMode.HALF_UP);
            vo.setApprovalRate(rate);
        } else {
            vo.setApprovalRate(BigDecimal.ZERO);
        }

        vo.setTotalUsers((int) userProfileService.countUsers());
        vo.setTotalLoans(loanService.countApproved());
        vo.setTotalAmount(loanService.sumApprovedAmount());

        vo.setOverdueRate(calculateOverdueRate());
        vo.setRiskDistribution(getRiskDistribution());

        return vo;
    }

    @Override
    public RiskStatsVO getRiskStats(String range) {
        RiskStatsVO vo = new RiskStatsVO();

        int days = parseRange(range);
        vo.setRange(range != null ? range : "7d");

        LocalDateTime startDate = LocalDate.now().minusDays(days).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().plusDays(1).atStartOfDay();

        vo.setDistribution(calculateRiskDistribution(startDate));
        vo.setDailyStats(calculateDailyStats(startDate, endDate));

        return vo;
    }

    private BigDecimal calculateOverdueRate() {
        long totalContracts = loanService.countAllContracts();
        if (totalContracts == 0) {
            return BigDecimal.ZERO;
        }

        long overdueContracts = loanService.countOverdueContracts();

        return BigDecimal.valueOf(overdueContracts)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalContracts), 2, RoundingMode.HALF_UP);
    }

    private DashboardOverviewVO.RiskDistribution getRiskDistribution() {
        DashboardOverviewVO.RiskDistribution distribution = new DashboardOverviewVO.RiskDistribution();
        distribution.setLow(userProfileService.countProfilesByRiskLevel(RiskConstants.RISK_LEVEL_LOW));
        distribution.setMedium(userProfileService.countProfilesByRiskLevel(RiskConstants.RISK_LEVEL_MEDIUM));
        distribution.setHigh(userProfileService.countProfilesByRiskLevel(RiskConstants.RISK_LEVEL_HIGH));
        return distribution;
    }

    private int parseRange(String range) {
        if (range == null) {
            return 7;
        }
        return switch (range) {
            case "30d" -> 30;
            case "90d" -> 90;
            default -> 7;
        };
    }

    private RiskStatsVO.RiskDistribution calculateRiskDistribution(LocalDateTime since) {
        List<RiskAssessment> assessments = riskAssessmentService.getAssessmentsSince(since);

        int total = assessments.size();
        if (total == 0) {
            RiskStatsVO.RiskDistribution dist = new RiskStatsVO.RiskDistribution();
            dist.setLowPercent(BigDecimal.ZERO);
            dist.setMediumPercent(BigDecimal.ZERO);
            dist.setHighPercent(BigDecimal.ZERO);
            return dist;
        }

        long lowCount = assessments.stream()
                .filter(a -> a.getRiskLevel() != null && a.getRiskLevel() == 1).count();
        long mediumCount = assessments.stream()
                .filter(a -> a.getRiskLevel() != null && a.getRiskLevel() == 2).count();
        long highCount = assessments.stream()
                .filter(a -> a.getRiskLevel() != null && a.getRiskLevel() == 3).count();

        RiskStatsVO.RiskDistribution dist = new RiskStatsVO.RiskDistribution();
        dist.setLowPercent(BigDecimal.valueOf(lowCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        dist.setMediumPercent(BigDecimal.valueOf(mediumCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        dist.setHighPercent(BigDecimal.valueOf(highCount * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));

        return dist;
    }

    /**
     * 使用 GROUP BY 按天聚合查询，替代逐天循环查询（消除N+1问题）
     */
    private List<RiskStatsVO.DailyStats> calculateDailyStats(LocalDateTime startDate, LocalDateTime endDate) {
        // 一次性获取所有天的申请统计
        List<DailyLoanStats> loanStats = loanService.getDailyLoanStats(startDate, endDate);
        Map<String, DailyLoanStats> statsMap = loanStats.stream()
                .collect(Collectors.toMap(
                        s -> s.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        s -> s,
                        (a, b) -> {
                            a.setLoanCount(a.getLoanCount() + b.getLoanCount());
                            a.setApprovedCount((a.getApprovedCount() != null ? a.getApprovedCount() : 0)
                                    + (b.getApprovedCount() != null ? b.getApprovedCount() : 0));
                            a.setRejectedCount((a.getRejectedCount() != null ? a.getRejectedCount() : 0)
                                    + (b.getRejectedCount() != null ? b.getRejectedCount() : 0));
                            return a;
                        }
                ));

        // 一次性获取所有天的风险评估（用于计算平均风险分）
        List<RiskAssessment> assessments = riskAssessmentService.getAssessmentsSince(startDate);
        Map<String, List<BigDecimal>> dailyScores = assessments.stream()
                .filter(a -> a.getRiskScore() != null && a.getCreatedAt() != null)
                .filter(a -> a.getCreatedAt().isBefore(endDate))
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        Collectors.mapping(RiskAssessment::getRiskScore, Collectors.toList())
                ));

        int days = (int) java.time.Duration.between(startDate, endDate).toDays();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<RiskStatsVO.DailyStats> result = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = startDate.plusDays(i).toLocalDate();
            String dateStr = date.format(formatter);

            RiskStatsVO.DailyStats stats = new RiskStatsVO.DailyStats();
            stats.setDate(dateStr);

            DailyLoanStats loanStat = statsMap.get(dateStr);
            stats.setApplications(loanStat != null ? loanStat.getLoanCount() : 0);
            stats.setApproved(loanStat != null && loanStat.getApprovedCount() != null ? loanStat.getApprovedCount().intValue() : 0);
            stats.setRejected(loanStat != null && loanStat.getRejectedCount() != null ? loanStat.getRejectedCount().intValue() : 0);

            List<BigDecimal> scores = dailyScores.get(dateStr);
            if (scores != null && !scores.isEmpty()) {
                BigDecimal avgScore = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                stats.setAvgRiskScore(avgScore);
            } else {
                stats.setAvgRiskScore(BigDecimal.ZERO);
            }

            result.add(stats);
        }

        return result;
    }
}
