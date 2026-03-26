package com.xindai.xindai.listener;

import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.LoanApplicationSubmittedEvent;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 借款申请提交事件监听器
 * 触发风险评估流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskAssessmentListener {

    private final RiskAssessmentService riskAssessmentService;

    @RabbitListener(queues = QueueConstants.LOAN_APPLICATION_SUBMITTED)
    public void onApplicationSubmitted(LoanApplicationSubmittedEvent event,
                                       Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Triggering risk assessment for userId={}, applicationId={}",
                    event.getUserId(), event.getApplicationId());
            riskAssessmentService.assess(event.getUserId(), event.getApplicationId(), 2);
            log.info("Risk assessment completed for applicationId={}", event.getApplicationId());
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
