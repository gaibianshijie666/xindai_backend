package com.xindai.xindai.modules.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.modules.loan.dto.OverdueInfoVO;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.loan.service.OverdueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueServiceImpl implements OverdueService {

    private final RepaymentPlanMapper repaymentPlanMapper;
    private final LoanContractMapper loanContractMapper;

    /** Daily penalty rate: 0.05% per day */
    private static final BigDecimal DAILY_PENALTY_RATE = new BigDecimal("0.0005");

    @Override
    public void checkAndMarkOverdue() {
        LocalDate today = LocalDate.now();

        // Find all PENDING repayment plans past due date
        List<RepaymentPlan> overduePlans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode())
                        .lt(RepaymentPlan::getDueDate, today)
        );

        if (overduePlans.isEmpty()) {
            log.info("No overdue repayment plans found");
            return;
        }

        log.info("Found {} overdue repayment plans", overduePlans.size());

        for (RepaymentPlan plan : overduePlans) {
            int overdueDays = (int) java.time.temporal.ChronoUnit.DAYS.between(plan.getDueDate(), today);
            BigDecimal penaltyBase = plan.getPrincipal()
                    .add(plan.getInterest() != null ? plan.getInterest() : BigDecimal.ZERO);
            BigDecimal penaltyAmount = penaltyBase
                    .multiply(DAILY_PENALTY_RATE)
                    .multiply(BigDecimal.valueOf(overdueDays))
                    .setScale(2, RoundingMode.HALF_UP);

            plan.setStatus(RepaymentStatus.OVERDUE.getCode());
            plan.setOverdueDays(overdueDays);
            plan.setPenaltyAmount(penaltyAmount);
            repaymentPlanMapper.updateById(plan);

            log.info("Marked plan id={} as overdue: days={}, penalty={}",
                    plan.getId(), overdueDays, penaltyAmount);
        }

        // Check contracts: if all unpaid plans are overdue, mark contract as OVERDUE
        List<Long> contractIds = overduePlans.stream()
                .map(RepaymentPlan::getContractId)
                .distinct()
                .toList();

        for (Long contractId : contractIds) {
            long pendingCount = repaymentPlanMapper.selectCount(
                    new LambdaQueryWrapper<RepaymentPlan>()
                            .eq(RepaymentPlan::getContractId, contractId)
                            .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode())
            );

            if (pendingCount == 0) {
                LoanContract contract = loanContractMapper.selectById(contractId);
                if (contract != null && contract.getStatus() == ContractStatus.REPAYING.getCode()) {
                    contract.setStatus(ContractStatus.OVERDUE.getCode());
                    loanContractMapper.updateById(contract);
                    log.info("Marked contract id={} as OVERDUE", contractId);
                }
            }
        }
    }

    @Override
    public List<OverdueInfoVO> getOverdueList(Long userId) {
        // Get user's contracts
        List<LoanContract> contracts = loanContractMapper.selectList(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getUserId, userId)
        );

        if (contracts.isEmpty()) {
            return List.of();
        }

        List<Long> contractIds = contracts.stream().map(LoanContract::getId).toList();
        Map<Long, String> contractNoMap = contracts.stream()
                .collect(Collectors.toMap(LoanContract::getId, LoanContract::getContractNo));

        // Get overdue repayment plans
        List<RepaymentPlan> overduePlans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .in(RepaymentPlan::getContractId, contractIds)
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.OVERDUE.getCode())
                        .orderByAsc(RepaymentPlan::getDueDate)
        );

        List<OverdueInfoVO> result = new ArrayList<>();
        for (RepaymentPlan plan : overduePlans) {
            OverdueInfoVO vo = new OverdueInfoVO();
            vo.setContractId(plan.getContractId());
            vo.setContractNo(contractNoMap.get(plan.getContractId()));
            vo.setPeriod(plan.getPeriod());
            vo.setDueDate(plan.getDueDate());
            vo.setPrincipal(plan.getPrincipal());
            vo.setInterest(plan.getInterest());
            vo.setPenaltyAmount(plan.getPenaltyAmount());
            vo.setOverdueDays(plan.getOverdueDays());

            BigDecimal total = plan.getPrincipal()
                    .add(plan.getInterest())
                    .add(plan.getPenaltyAmount() != null ? plan.getPenaltyAmount() : BigDecimal.ZERO);
            vo.setTotalOverdueAmount(total);

            result.add(vo);
        }

        return result;
    }
}
