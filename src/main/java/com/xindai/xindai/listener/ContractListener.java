package com.xindai.xindai.listener;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.LoanApplicationApprovedEvent;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 借款申请通过事件监听器
 * 触发电子合同生成和还款计划创建
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractListener {

    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanContractMapper loanContractMapper;
    private final RepaymentPlanMapper repaymentPlanMapper;

    @RabbitListener(queues = "loan.contract.create")
    public void onApplicationApproved(LoanApplicationApprovedEvent event,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Contract generation triggered for applicationId={}, userId={}, approvedAmount={}",
                    event.getApplicationId(), event.getUserId(), event.getApprovedAmount());

            // 1. Get the approved loan application
            LoanApplication application = loanApplicationMapper.selectById(event.getApplicationId());
            if (application == null) {
                log.warn("Loan application not found: applicationId={}", event.getApplicationId());
                channel.basicAck(tag, false);
                return;
            }

            // Verify the application is approved
            if (application.getStatus() != ApplicationStatus.APPROVED.getCode()) {
                log.warn("Loan application is not in APPROVED status: applicationId={}, status={}",
                        event.getApplicationId(), application.getStatus());
                channel.basicAck(tag, false);
                return;
            }

            // 2. Create LoanContract and RepaymentPlan
            createContractAndRepaymentPlan(application, event);

            channel.basicAck(tag, false);
            log.info("Contract and repayment plan created successfully for applicationId={}",
                    event.getApplicationId());

        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }

    /**
     * Create loan contract and repayment plan
     * Uses equal principal and interest installment method (等额本息)
     */
    private void createContractAndRepaymentPlan(LoanApplication application,
                                                LoanApplicationApprovedEvent event) {
        // Generate contract number
        String contractNo = "LC" + IdUtil.getSnowflakeNextIdStr();

        // Calculate interest rate (default 12% annual rate, can be adjusted based on credit grade)
        BigDecimal annualRate = new BigDecimal("0.12"); // 12% annual rate
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);

        // Calculate monthly payment using equal principal and interest formula
        // PMT = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal principal = event.getApprovedAmount() != null ? event.getApprovedAmount() : application.getAmount();
        int term = application.getTerm();

        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, monthlyRate, term);
        BigDecimal totalRepayment = monthlyPayment.multiply(BigDecimal.valueOf(term));

        // Calculate due date (term months from now)
        LocalDate dueDate = LocalDate.now().plusMonths(term);

        // Create LoanContract
        LoanContract contract = new LoanContract();
        contract.setContractNo(contractNo);
        contract.setApplicationId(application.getId());
        contract.setUserId(application.getUserId());
        contract.setPrincipal(principal);
        contract.setInterestRate(annualRate.multiply(new BigDecimal("100"))); // Store as percentage
        contract.setTotalRepayment(totalRepayment);
        contract.setTerm(term);
        contract.setStatus(ContractStatus.PENDING.getCode()); // 待放款
        contract.setDueDate(dueDate);
        contract.setCreatedAt(LocalDateTime.now());

        loanContractMapper.insert(contract);
        log.info("Created loan contract: contractNo={}, contractId={}, principal={}, term={}",
                contractNo, contract.getId(), principal, term);

        // Create RepaymentPlan entries for each period
        createRepaymentPlans(contract, principal, monthlyRate, monthlyPayment, term);
    }

    /**
     * Calculate monthly payment using equal principal and interest formula
     * PMT = P * r * (1+r)^n / ((1+r)^n - 1)
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int term) {
        // (1+r)^n
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal ratePowN = onePlusRate.pow(term);

        // P * r * (1+r)^n
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(ratePowN);

        // ((1+r)^n - 1)
        BigDecimal denominator = ratePowN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Create repayment plan entries
     * For equal principal and interest, each payment is the same amount
     */
    private void createRepaymentPlans(LoanContract contract, BigDecimal principal,
                                      BigDecimal monthlyRate, BigDecimal monthlyPayment, int term) {
        List<RepaymentPlan> plans = new ArrayList<>();
        LocalDate baseDate = LocalDate.now();

        // Remaining principal for calculation
        BigDecimal remainingPrincipal = principal;

        for (int period = 1; period <= term; period++) {
            LocalDate dueDate = baseDate.plusMonths(period);

            // Calculate interest for this period
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // Calculate principal portion (monthly payment - interest)
            BigDecimal principalPortion = monthlyPayment.subtract(interest);

            // Adjust for last period to avoid rounding errors
            if (period == term) {
                principalPortion = remainingPrincipal;
                // Recalculate total for last period
            }

            // Update remaining principal
            remainingPrincipal = remainingPrincipal.subtract(principalPortion);
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                remainingPrincipal = BigDecimal.ZERO;
            }

            RepaymentPlan plan = new RepaymentPlan();
            plan.setContractId(contract.getId());
            plan.setPeriod(period);
            plan.setDueDate(dueDate);
            plan.setPrincipal(principalPortion);
            plan.setInterest(interest);
            plan.setTotalAmount(monthlyPayment);
            plan.setStatus(RepaymentStatus.PENDING.getCode());
            plan.setPenaltyAmount(BigDecimal.ZERO);
            plan.setOverdueDays(0);

            plans.add(plan);
        }

        // Batch insert repayment plans
        for (RepaymentPlan plan : plans) {
            repaymentPlanMapper.insert(plan);
        }

        log.info("Created {} repayment plan entries for contractId={}", plans.size(), contract.getId());
    }
}
