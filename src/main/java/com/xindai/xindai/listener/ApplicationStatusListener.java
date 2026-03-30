package com.xindai.xindai.listener;

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

/**
 * 风险评估完成事件监听器
 *
 * NOTE: The synchronous risk assessment in LoanServiceImpl.apply() is the primary flow.
 * This listener is kept for edge cases and logging only, to avoid duplicate processing.
 * The loan application status is already set by the synchronous call in LoanServiceImpl.
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
            log.info("Risk assessment completed: applicationId={}, riskLevel={}, riskScore={}",
                    event.getApplicationId(), event.getRiskLevel(), event.getRiskScore());

            LoanApplication application = loanApplicationMapper.selectById(event.getApplicationId());
            if (application == null) {
                log.warn("Loan application not found: applicationId={}", event.getApplicationId());
                channel.basicAck(tag, false);
                return;
            }

            // Log the current application status for monitoring/troubleshooting
            // The actual status change was already done synchronously in LoanServiceImpl.apply()
            ApplicationStatus currentStatus = ApplicationStatus.fromCode(application.getStatus());
            log.info("Current application status: applicationId={}, status={}, statusDesc={}",
                    event.getApplicationId(), application.getStatus(), currentStatus.getDesc());

            // Edge case handling: if the application is still in PENDING status after
            // the synchronous processing, it means the risk assessment failed
            if (application.getStatus() == ApplicationStatus.PENDING.getCode()) {
                log.warn("Application still in PENDING status after risk assessment completed: " +
                        "applicationId={}, this may indicate a processing failure",
                        event.getApplicationId());
                // The application will remain in PENDING for manual review
            }

            // Acknowledge message - we've logged what we needed
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, false); // Don't requeue to avoid duplicate processing
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }
}
