package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.constants.RiskConstants;
import com.xindai.xindai.modules.admin.service.AdminDashboardService;
import com.xindai.xindai.modules.admin.vo.DashboardOverviewVO;
import com.xindai.xindai.modules.admin.vo.RiskStatsVO;
import com.xindai.xindai.modules.enterprise.dto.DailyLoanStats;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanContractMapper loanContractMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;

    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    @Override
    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        vo.setTodayApplications(getApplicationCountSince(todayStart));
        vo.setTodayApproved(getApplicationCountByStatusSince(STATUS_APPROVED, todayStart));
        vo.setTodayRejected(getApplicationCountByStatusSince(STATUS_REJECTED, todayStart));

        int todayTotal = vo.getTodayApplications();
        if (todayTotal > 0) {
            BigDecimal rate = BigDecimal.valueOf(vo.getTodayApproved())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(todayTotal), 2, RoundingMode.HALF_UP);
            vo.setApprovalRate(rate);
        } else {
            vo.setApprovalRate(BigDecimal.ZERO);
        }

        vo.setTotalUsers(getTotalUserCount());
        LoanStats totalStats = getTotalLoanStats();
        vo.setTotalLoans(totalStats.count());
        vo.setTotalAmount(totalStats.amount());

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

    private int getApplicationCountSince(LocalDateTime since) {
        return loanApplicationMapper.selectCount(
                new LambdaQueryWrapper<LoanApplication>()
                        .ge(LoanApplication::getCreatedAt, since)
        ).intValue();
    }

    private int getApplicationCountByStatusSince(Integer status, LocalDateTime since) {
        return loanApplicationMapper.selectCount(
                new LambdaQueryWrapper<LoanApplication>()
                        .eq(LoanApplication::getStatus, status)
                        .ge(LoanApplication::getCreatedAt, since)
        ).intValue();
    }

    private int getTotalUserCount() {
        return userMapper.selectCount(new LambdaQueryWrapper<>()).intValue();
    }

    private LoanStats getTotalLoanStats() {
        return new LoanStats(
                loanApplicationMapper.countApproved(),
                loanApplicationMapper.sumApprovedAmount()
        );
    }

    private BigDecimal calculateOverdueRate() {
        long totalContracts = loanContractMapper.selectCount(
                new LambdaQueryWrapper<LoanContract>()
        );
        if (totalContracts == 0) {
            return BigDecimal.ZERO;
        }

        long overdueContracts = loanContractMapper.selectCount(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getStatus, ContractStatus.OVERDUE.getCode())
        );

        return BigDecimal.valueOf(overdueContracts)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalContracts), 2, RoundingMode.HALF_UP);
    }

    private DashboardOverviewVO.RiskDistribution getRiskDistribution() {
        DashboardOverviewVO.RiskDistribution distribution = new DashboardOverviewVO.RiskDistribution();
        distribution.setLow(getUserProfileCountByRiskLevel(RiskConstants.RISK_LEVEL_LOW));
        distribution.setMedium(getUserProfileCountByRiskLevel(RiskConstants.RISK_LEVEL_MEDIUM));
        distribution.setHigh(getUserProfileCountByRiskLevel(RiskConstants.RISK_LEVEL_HIGH));
        return distribution;
    }

    private int getUserProfileCountByRiskLevel(Integer riskLevel) {
        return userProfileMapper.selectCount(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getRiskLevel, riskLevel)
        ).intValue();
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
        List<RiskAssessment> assessments = riskAssessmentMapper.selectList(
                new LambdaQueryWrapper<RiskAssessment>()
                        .ge(RiskAssessment::getCreatedAt, since)
        );

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
        List<DailyLoanStats> loanStats = loanApplicationMapper.getDailyLoanStats(startDate, endDate);
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
        List<RiskAssessment> assessments = riskAssessmentMapper.selectList(
                new LambdaQueryWrapper<RiskAssessment>()
                        .ge(RiskAssessment::getCreatedAt, startDate)
                        .lt(RiskAssessment::getCreatedAt, endDate)
        );
        Map<String, List<BigDecimal>> dailyScores = assessments.stream()
                .filter(a -> a.getRiskScore() != null && a.getCreatedAt() != null)
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

    private record LoanStats(int count, BigDecimal amount) {}
}
