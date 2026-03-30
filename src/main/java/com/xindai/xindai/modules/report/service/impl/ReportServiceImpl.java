package com.xindai.xindai.modules.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.collection.entity.CollectionTask;
import com.xindai.xindai.modules.collection.enums.CollectionTaskStatus;
import com.xindai.xindai.modules.collection.mapper.CollectionTaskMapper;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.report.dto.*;
import com.xindai.xindai.modules.report.service.ReportService;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表导出服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final LoanContractMapper loanContractMapper;
    private final RepaymentPlanMapper repaymentPlanMapper;
    private final CollectionTaskMapper collectionTaskMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final UserMapper userMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void exportLoanLedger(HttpServletResponse response, String startDate, String endDate) {
        LambdaQueryWrapper<LoanContract> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(LoanContract::getCreatedAt, parseStartDateTime(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.lt(LoanContract::getCreatedAt, parseEndDateTime(endDate));
        }
        wrapper.orderByDesc(LoanContract::getCreatedAt);

        List<LoanContract> contracts = loanContractMapper.selectList(wrapper);

        List<LoanLedgerExcelVO> excelVOList = contracts.stream()
                .map(this::toLoanLedgerVO)
                .collect(Collectors.toList());

        writeExcel(response, "借款台账", LoanLedgerExcelVO.class, excelVOList);
    }

    @Override
    public void exportOverdueReport(HttpServletResponse response, String startDate, String endDate) {
        // 查询所有逾期的还款计划
        LambdaQueryWrapper<RepaymentPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepaymentPlan::getStatus, RepaymentStatus.OVERDUE.getCode());
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(RepaymentPlan::getDueDate, parseLocalDate(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(RepaymentPlan::getDueDate, parseLocalDate(endDate));
        }
        wrapper.orderByDesc(RepaymentPlan::getDueDate);

        List<RepaymentPlan> overduePlans = repaymentPlanMapper.selectList(wrapper);

        List<OverdueReportExcelVO> excelVOList = overduePlans.stream()
                .map(this::toOverdueReportVO)
                .collect(Collectors.toList());

        writeExcel(response, "逾期报表", OverdueReportExcelVO.class, excelVOList);
    }

    @Override
    public void exportRiskStats(HttpServletResponse response, String startDate, String endDate) {
        // Build date filter wrapper
        LambdaQueryWrapper<RiskAssessment> dateWrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            dateWrapper.ge(RiskAssessment::getCreatedAt, parseStartDateTime(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            dateWrapper.lt(RiskAssessment::getCreatedAt, parseEndDateTime(endDate));
        }

        List<RiskStatsExcelVO> statsList = new ArrayList<>();

        // 总评估数
        Long totalAssessments = riskAssessmentMapper.selectCount(dateWrapper);
        statsList.add(toRiskStatVO("总风控评估数", String.valueOf(totalAssessments), "系统累计完成的风控评估总数"));

        // 通过数
        LambdaQueryWrapper<RiskAssessment> approvedWrapper = dateWrapper.clone();
        approvedWrapper.eq(RiskAssessment::getDecision, "APPROVE");
        Long approvedCount = riskAssessmentMapper.selectCount(approvedWrapper);
        statsList.add(toRiskStatVO("自动通过数", String.valueOf(approvedCount), "风控模型自动决策通过的申请数"));

        // 拒绝数
        LambdaQueryWrapper<RiskAssessment> rejectedWrapper = dateWrapper.clone();
        rejectedWrapper.eq(RiskAssessment::getDecision, "REJECT");
        Long rejectedCount = riskAssessmentMapper.selectCount(rejectedWrapper);
        statsList.add(toRiskStatVO("自动拒绝数", String.valueOf(rejectedCount), "风控模型自动决策拒绝的申请数"));

        // 人工审核数
        LambdaQueryWrapper<RiskAssessment> manualWrapper = dateWrapper.clone();
        manualWrapper.eq(RiskAssessment::getDecision, "MANUAL_REVIEW");
        Long manualCount = riskAssessmentMapper.selectCount(manualWrapper);
        statsList.add(toRiskStatVO("人工审核数", String.valueOf(manualCount), "转由人工审核的申请数"));

        // 通过率
        if (totalAssessments > 0) {
            BigDecimal approveRate = new BigDecimal(approvedCount)
                    .divide(new BigDecimal(totalAssessments), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            statsList.add(toRiskStatVO("自动通过率", approveRate.toPlainString() + "%", "自动通过数 / 总评估数"));
        }

        // 平均风险评分
        List<RiskAssessment> assessments = riskAssessmentMapper.selectList(dateWrapper);
        if (!assessments.isEmpty()) {
            double avgScore = assessments.stream()
                    .filter(a -> a.getRiskScore() != null)
                    .mapToDouble(a -> a.getRiskScore().doubleValue())
                    .average()
                    .orElse(0);
            statsList.add(toRiskStatVO("平均风险评分", String.format("%.1f", avgScore), "所有评估的平均风险评分"));
        }

        writeExcel(response, "风控统计报表", RiskStatsExcelVO.class, statsList);
    }

    @Override
    public void exportCollectionPerformance(HttpServletResponse response, String startDate, String endDate) {
        // 查询所有催收任务
        LambdaQueryWrapper<CollectionTask> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(CollectionTask::getCreatedAt, parseStartDateTime(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.lt(CollectionTask::getCreatedAt, parseEndDateTime(endDate));
        }

        List<CollectionTask> tasks = collectionTaskMapper.selectList(wrapper);

        // 按催收员分组
        Map<Long, List<CollectionTask>> tasksByCollector = tasks.stream()
                .filter(t -> t.getCollectorId() != null)
                .collect(Collectors.groupingBy(CollectionTask::getCollectorId));

        List<CollectionPerformanceExcelVO> excelVOList = tasksByCollector.entrySet().stream()
                .map(entry -> toCollectionPerformanceVO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        writeExcel(response, "催收绩效报表", CollectionPerformanceExcelVO.class, excelVOList);
    }

    // === Private helper methods ===

    private LoanLedgerExcelVO toLoanLedgerVO(LoanContract contract) {
        LoanLedgerExcelVO vo = new LoanLedgerExcelVO();
        vo.setContractNo(contract.getContractNo());
        vo.setPrincipal(contract.getPrincipal());
        vo.setInterestRate(contract.getInterestRate());
        vo.setTerm(contract.getTerm());
        vo.setTotalRepayment(contract.getTotalRepayment());

        if (contract.getDisbursedAt() != null) {
            vo.setDisbursedAt(contract.getDisbursedAt().toLocalDate().format(DATE_FORMATTER));
        }
        if (contract.getDueDate() != null) {
            vo.setDueDate(contract.getDueDate().format(DATE_FORMATTER));
        }
        vo.setStatusDesc(getContractStatusDesc(contract.getStatus()));

        // 填充借款人信息
        User user = userMapper.selectById(contract.getUserId());
        if (user != null) {
            vo.setUserName(user.getRealName());
            vo.setIdCard(user.getIdCard());
        }

        return vo;
    }

    private OverdueReportExcelVO toOverdueReportVO(RepaymentPlan plan) {
        OverdueReportExcelVO vo = new OverdueReportExcelVO();
        vo.setPeriod(plan.getPeriod());
        vo.setOverdueAmount(plan.getTotalAmount());
        vo.setOverdueDays(plan.getOverdueDays());
        vo.setPenaltyAmount(plan.getPenaltyAmount());

        if (plan.getDueDate() != null) {
            vo.setDueDate(plan.getDueDate().format(DATE_FORMATTER));
        }

        // 逾期等级
        Integer days = plan.getOverdueDays() != null ? plan.getOverdueDays() : 0;
        if (days <= 30) {
            vo.setOverdueLevel("M1");
        } else if (days <= 60) {
            vo.setOverdueLevel("M2");
        } else if (days <= 90) {
            vo.setOverdueLevel("M3");
        } else {
            vo.setOverdueLevel("M4+");
        }

        // 填充合同和借款人信息
        LoanContract contract = loanContractMapper.selectById(plan.getContractId());
        if (contract != null) {
            vo.setContractNo(contract.getContractNo());
            User user = userMapper.selectById(contract.getUserId());
            if (user != null) {
                vo.setUserName(user.getRealName());
                vo.setUserPhone(user.getPhone());
            }
        }

        return vo;
    }

    private RiskStatsExcelVO toRiskStatVO(String itemName, String value, String description) {
        RiskStatsExcelVO vo = new RiskStatsExcelVO();
        vo.setItemName(itemName);
        vo.setValue(value);
        vo.setDescription(description);
        return vo;
    }

    private CollectionPerformanceExcelVO toCollectionPerformanceVO(Long collectorId, List<CollectionTask> tasks) {
        CollectionPerformanceExcelVO vo = new CollectionPerformanceExcelVO();
        vo.setCollectorId(collectorId);

        // 填充催收员姓名
        User collector = userMapper.selectById(collectorId);
        if (collector != null) {
            vo.setCollectorName(collector.getRealName());
        }

        vo.setTotalTasks(tasks.size());
        vo.setCompletedTasks((int) tasks.stream()
                .filter(t -> t.getStatus() == CollectionTaskStatus.COMPLETED.getCode()).count());
        vo.setInProgressTasks((int) tasks.stream()
                .filter(t -> t.getStatus() == CollectionTaskStatus.IN_PROGRESS.getCode()).count());
        vo.setClosedTasks((int) tasks.stream()
                .filter(t -> t.getStatus() == CollectionTaskStatus.CLOSED.getCode()).count());

        // 成功率
        long actionableTasks = tasks.stream()
                .filter(t -> t.getStatus() == CollectionTaskStatus.COMPLETED.getCode()
                        || t.getStatus() == CollectionTaskStatus.CLOSED.getCode())
                .count();
        if (actionableTasks > 0) {
            BigDecimal rate = new BigDecimal(vo.getCompletedTasks())
                    .divide(new BigDecimal(actionableTasks), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setSuccessRate(rate);
        } else {
            vo.setSuccessRate(BigDecimal.ZERO);
        }

        // 催收总金额
        BigDecimal totalAmount = tasks.stream()
                .map(CollectionTask::getOverdueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalOverdueAmount(totalAmount);

        // 已收回金额（已完成任务的金额作为已收回）
        BigDecimal collectedAmount = tasks.stream()
                .filter(t -> t.getStatus() == CollectionTaskStatus.COMPLETED.getCode())
                .map(CollectionTask::getOverdueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setCollectedAmount(collectedAmount);

        return vo;
    }

    private String getContractStatusDesc(Integer status) {
        if (status == null) return "未知";
        try {
            return ContractStatus.fromCode(status).getDesc();
        } catch (Exception e) {
            return "未知";
        }
    }

    private <T> void writeExcel(HttpServletResponse response, String fileName, Class<T> clazz, List<T> data) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

        try {
            EasyExcel.write(response.getOutputStream(), clazz)
                    .sheet(fileName)
                    .doWrite(data);
        } catch (IOException e) {
            log.error("Failed to export Excel: {}", fileName, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出Excel失败");
        }
    }

    private LocalDateTime parseStartDateTime(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER).atStartOfDay();
    }

    private LocalDateTime parseEndDateTime(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER).plusDays(1).atStartOfDay();
    }

    private LocalDate parseLocalDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
