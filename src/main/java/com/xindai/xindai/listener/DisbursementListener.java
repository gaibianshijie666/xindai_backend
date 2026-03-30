package com.xindai.xindai.listener;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.event.LoanApplicationApprovedEvent;
import com.xindai.xindai.modules.loan.entity.BankAccount;
import com.xindai.xindai.modules.loan.entity.DisbursementRecord;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.DisbursementStatus;
import com.xindai.xindai.modules.loan.mapper.BankAccountMapper;
import com.xindai.xindai.modules.loan.mapper.DisbursementRecordMapper;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借款申请通过事件监听器
 * 触发放款流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DisbursementListener {

    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanContractMapper loanContractMapper;
    private final DisbursementRecordMapper disbursementRecordMapper;
    private final BankAccountMapper bankAccountMapper;

    @RabbitListener(queues = "loan.disbursement.execute")
    public void onApplicationApproved(LoanApplicationApprovedEvent event,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Disbursement triggered for applicationId={}, userId={}, approvedAmount={}",
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

            // 2. Get the associated contract
            // Wait briefly for contract to be created (in case ContractListener hasn't processed yet)
            LoanContract contract = null;
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                contract = loanContractMapper.selectOne(
                        new LambdaQueryWrapper<LoanContract>()
                                .eq(LoanContract::getApplicationId, event.getApplicationId())
                );
                if (contract != null) {
                    break;
                }
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(100); // Wait 100ms before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (contract == null) {
                log.warn("Loan contract not found after {} retries for applicationId={}, " +
                        "ContractListener may not have processed yet. Requeueing...",
                        maxRetries, event.getApplicationId());
                // Requeue the message to try again later
                try {
                    channel.basicNack(tag, false, true);
                } catch (Exception ex) {
                    log.error("Failed to NACK message", ex);
                }
                return;
            }

            // 3. Get user's default bank account
            BankAccount bankAccount = bankAccountMapper.selectOne(
                    new LambdaQueryWrapper<BankAccount>()
                            .eq(BankAccount::getUserId, event.getUserId())
                            .eq(BankAccount::getIsDefault, 1)
                            .eq(BankAccount::getStatus, 1)
            );

            // If no default account, get the first active account
            if (bankAccount == null) {
                bankAccount = bankAccountMapper.selectOne(
                        new LambdaQueryWrapper<BankAccount>()
                                .eq(BankAccount::getUserId, event.getUserId())
                                .eq(BankAccount::getStatus, 1)
                                .orderByDesc(BankAccount::getCreatedAt)
                                .last("LIMIT 1")
                );
            }

            if (bankAccount == null) {
                log.warn("No valid bank account found for userId={}, disbursement pending manual processing",
                        event.getUserId());
                // Create pending disbursement record without bank account for manual processing
                createPendingDisbursement(contract.getId(), application.getId(),
                        event.getUserId(), contract.getPrincipal(), null);
                channel.basicAck(tag, false);
                return;
            }

            // 4. Create disbursement record and execute disbursement
            processDisbursement(contract, application, bankAccount);

            channel.basicAck(tag, false);
            log.info("Disbursement processed successfully for applicationId={}", event.getApplicationId());

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
     * Create a pending disbursement record (for cases without valid bank account)
     */
    private void createPendingDisbursement(Long contractId, Long applicationId,
                                          Long userId, BigDecimal amount, Long bankAccountId) {
        DisbursementRecord record = new DisbursementRecord();
        record.setContractId(contractId);
        record.setApplicationId(applicationId);
        record.setUserId(userId);
        record.setAmount(amount);
        record.setBankAccountId(bankAccountId);
        record.setStatus(DisbursementStatus.PENDING.getCode());
        record.setCreatedAt(LocalDateTime.now());

        disbursementRecordMapper.insert(record);
        log.info("Created pending disbursement record for manual processing: id={}, contractId={}",
                record.getId(), contractId);
    }

    /**
     * Process disbursement: create record and execute
     */
    private void processDisbursement(LoanContract contract, LoanApplication application,
                                    BankAccount bankAccount) {
        BigDecimal amount = contract.getPrincipal();

        // Create disbursement record
        DisbursementRecord record = new DisbursementRecord();
        record.setContractId(contract.getId());
        record.setApplicationId(application.getId());
        record.setUserId(application.getUserId());
        record.setAmount(amount);
        record.setBankAccountId(bankAccount.getId());
        record.setStatus(DisbursementStatus.PROCESSING.getCode());
        record.setCreatedAt(LocalDateTime.now());

        disbursementRecordMapper.insert(record);
        log.info("Created disbursement record: id={}, contractId={}, amount={}",
                record.getId(), contract.getId(), amount);

        // Execute disbursement (mock - set to COMPLETED since we don't have real payment gateway)
        executeDisbursement(record, contract);
    }

    /**
     * Execute disbursement - mock implementation
     * In production, this would integrate with a real payment gateway/bank API
     */
    private void executeDisbursement(DisbursementRecord record, LoanContract contract) {
        try {
            // Generate transaction number
            String transactionNo = "TXN" + IdUtil.getSnowflakeNextIdStr();

            // Mock: simulate bank processing
            log.info("Simulating bank transfer: transactionNo={}, amount={}, bankAccountId={}",
                    transactionNo, record.getAmount(), record.getBankAccountId());

            // Set status to COMPLETED (mock)
            record.setStatus(DisbursementStatus.COMPLETED.getCode());
            record.setTransactionNo(transactionNo);
            record.setCompletedAt(LocalDateTime.now());
            disbursementRecordMapper.updateById(record);

            // Update contract status to REPAYING
            contract.setStatus(ContractStatus.REPAYING.getCode());
            contract.setDisbursedAt(LocalDateTime.now());
            loanContractMapper.updateById(contract);

            log.info("Disbursement completed successfully: id={}, transactionNo={}, contractId={}",
                    record.getId(), transactionNo, contract.getId());

        } catch (Exception e) {
            log.error("Disbursement execution failed: id={}", record.getId(), e);
            record.setStatus(DisbursementStatus.FAILED.getCode());
            record.setFailedReason(e.getMessage());
            disbursementRecordMapper.updateById(record);
        }
    }
}
