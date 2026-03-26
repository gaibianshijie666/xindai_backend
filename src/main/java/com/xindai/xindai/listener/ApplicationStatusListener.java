package com.xindai.xindai.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.RiskAssessmentCompletedEvent;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 风险评估完成事件监听器
 * 根据风险等级自动审批或拒绝借款申请
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStatusListener {

    private final LoanApplicationMapper loanApplicationMapper;

    @RabbitListener(queues = QueueConstants.RISK_ASSESSMENT_COMPLETED)
    public void onRiskAssessmentCompleted(RiskAssessmentCompletedEvent event,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Processing risk assessment result: applicationId={}, riskLevel={}",
                    event.getApplicationId(), event.getRiskLevel());

            LoanApplication application = loanApplicationMapper.selectById(event.getApplicationId());
            if (application == null) {
                log.warn("Loan application not found: applicationId={}", event.getApplicationId());
                channel.basicAck(tag, false);
                return;
            }

            String riskLevel = event.getRiskLevel();
            String reviewNote;
            int newStatus;

            switch (riskLevel) {
                case "LOW":
                    newStatus = ApplicationStatus.APPROVED.getCode();
                    reviewNote = "自动审批通过（低风险）";
                    break;
                case "MEDIUM":
                    newStatus = ApplicationStatus.APPROVED.getCode();
                    reviewNote = "自动审批通过（中风险）";
                    break;
                case "HIGH":
                    newStatus = ApplicationStatus.REJECTED.getCode();
                    reviewNote = "自动拒绝（高风险）";
                    break;
                default:
                    log.warn("Unknown risk level: {} for applicationId={}, skipping auto-review",
                            riskLevel, event.getApplicationId());
                    channel.basicAck(tag, false);
                    return;
            }

            LambdaUpdateWrapper<LoanApplication> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(LoanApplication::getId, event.getApplicationId())
                    .set(LoanApplication::getStatus, newStatus)
                    .set(LoanApplication::getReviewerId, null)
                    .set(LoanApplication::getReviewNote, reviewNote)
                    .set(LoanApplication::getReviewedAt, LocalDateTime.now());

            loanApplicationMapper.update(null, wrapper);
            log.info("Auto-reviewed loan application: applicationId={}, action={}, riskLevel={}",
                    event.getApplicationId(),
                    newStatus == ApplicationStatus.APPROVED.getCode() ? "APPROVED" : "REJECTED",
                    riskLevel);

            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }
}
