package com.xindai.xindai.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.notification.channel.NotificationMessage;
import com.xindai.xindai.common.notification.channel.NotificationType;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled task that sends repayment reminders for plans due within 3 days.
 * Runs daily at 8:00 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentReminderTask {

    private final RepaymentPlanMapper repaymentPlanMapper;
    private final LoanContractMapper loanContractMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendRepaymentReminders() {
        log.info("开始执行还款提醒任务");

        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        try {
            // Find repayment plans that are PENDING and due within 3 days
            List<RepaymentPlan> upcomingPlans = repaymentPlanMapper.selectList(
                    new LambdaQueryWrapper<RepaymentPlan>()
                            .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode())
                            .ge(RepaymentPlan::getDueDate, today)
                            .le(RepaymentPlan::getDueDate, threeDaysLater)
            );

            if (upcomingPlans.isEmpty()) {
                log.info("没有即将到期的还款计划");
                return;
            }

            // Deduplicate per user per contract
            Set<String> remindedKeys = upcomingPlans.stream()
                    .map(plan -> plan.getContractId() + ":" + plan.getContractId())
                    .collect(Collectors.toSet());

            // Group by contractId to get one reminder per contract
            Map<Long, List<RepaymentPlan>> plansByContract = upcomingPlans.stream()
                    .collect(Collectors.groupingBy(RepaymentPlan::getContractId));

            int reminderCount = 0;
            for (Map.Entry<Long, List<RepaymentPlan>> entry : plansByContract.entrySet()) {
                Long contractId = entry.getKey();
                List<RepaymentPlan> plans = entry.getValue();

                // Get the contract
                LoanContract contract = loanContractMapper.selectById(contractId);
                if (contract == null) {
                    log.warn("Contract not found for contractId={}", contractId);
                    continue;
                }

                // Get the user
                User user = userMapper.selectById(contract.getUserId());
                if (user == null) {
                    log.warn("User not found for userId={}", contract.getUserId());
                    continue;
                }

                // Find the earliest due plan for this contract
                RepaymentPlan earliestPlan = plans.stream()
                        .min((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                        .orElse(plans.get(0));

                // Calculate total amount for upcoming plans
                java.math.BigDecimal totalAmount = plans.stream()
                        .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : java.math.BigDecimal.ZERO)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                String subject = "还款提醒 - " + contract.getContractNo();
                String content = String.format("您有%d笔还款即将到期，最早还款日期：%s，合计应还金额：%.2f元。请确保账户余额充足，及时还款。",
                        plans.size(),
                        earliestPlan.getDueDate(),
                        totalAmount);

                NotificationMessage message = NotificationMessage.builder()
                        .userId(contract.getUserId())
                        .userType("BORROWER")
                        .to(user.getPhone())
                        .subject(subject)
                        .content(content)
                        .type(NotificationType.REPAYMENT_REMINDER)
                        .relatedId(String.valueOf(contractId))
                        .params(Map.of(
                                "contractNo", contract.getContractNo() != null ? contract.getContractNo() : "",
                                "period", String.valueOf(earliestPlan.getPeriod()),
                                "dueDate", earliestPlan.getDueDate().toString(),
                                "totalAmount", totalAmount.toPlainString()
                        ))
                        .build();

                notificationService.sendMultiChannel(message);
                reminderCount++;
            }

            log.info("还款提醒任务执行完成，共发送{}条提醒", reminderCount);
        } catch (Exception e) {
            log.error("还款提醒任务执行失败", e);
        }
    }
}
