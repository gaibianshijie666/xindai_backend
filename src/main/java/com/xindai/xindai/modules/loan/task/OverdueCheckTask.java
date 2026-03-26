package com.xindai.xindai.modules.loan.task;

import com.xindai.xindai.common.event.EventPublisher;
import com.xindai.xindai.common.event.LoanOverdueDetectedEvent;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.loan.service.OverdueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OverdueCheckTask {
    private final OverdueService overdueService;
    private final EventPublisher eventPublisher;
    private final RepaymentPlanMapper repaymentPlanMapper;
    private final LoanContractMapper loanContractMapper;

    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdue() {
        log.info("开始执行逾期检查任务");
        try {
            overdueService.checkAndMarkOverdue();

            // 发布逾期检测事件：查找已标记为逾期的合同并发布事件
            publishOverdueDetectedEvents();

            log.info("逾期检查任务执行完成");
        } catch (Exception e) {
            log.error("逾期检查任务执行失败", e);
        }
    }

    private void publishOverdueDetectedEvents() {
        // 查找所有逾期合同
        List<LoanContract> overdueContracts = loanContractMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getStatus, ContractStatus.OVERDUE.getCode())
        );

        for (LoanContract contract : overdueContracts) {
            // 计算该合同的逾期信息
            List<RepaymentPlan> overduePlans = repaymentPlanMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RepaymentPlan>()
                            .eq(RepaymentPlan::getContractId, contract.getId())
                            .eq(RepaymentPlan::getStatus, RepaymentStatus.OVERDUE.getCode())
            );

            if (overduePlans.isEmpty()) {
                continue;
            }

            // 取最大逾期天数和总逾期金额
            int maxOverdueDays = overduePlans.stream()
                    .mapToInt(p -> p.getOverdueDays() != null ? p.getOverdueDays() : 0)
                    .max().orElse(0);

            BigDecimal totalOverdueAmount = overduePlans.stream()
                    .map(p -> p.getPrincipal()
                            .add(p.getInterest() != null ? p.getInterest() : BigDecimal.ZERO)
                            .add(p.getPenaltyAmount() != null ? p.getPenaltyAmount() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            eventPublisher.publish(new LoanOverdueDetectedEvent(
                    contract.getId(), contract.getUserId(), maxOverdueDays, totalOverdueAmount));

            log.info("Published LoanOverdueDetectedEvent for contractId={}, userId={}, overdueDays={}, amount={}",
                    contract.getId(), contract.getUserId(), maxOverdueDays, totalOverdueAmount);
        }
    }
}
