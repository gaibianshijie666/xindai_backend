package com.xindai.xindai.listener;

import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.LoanApplicationApprovedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 借款申请通过事件监听器
 * 触发放款流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DisbursementListener {

    // TODO: Wire to DisbursementService in Layer 4

    @RabbitListener(queues = QueueConstants.LOAN_APPLICATION_APPROVED)
    public void onApplicationApproved(LoanApplicationApprovedEvent event,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Disbursement triggered for applicationId={}, userId={}, approvedAmount={}",
                    event.getApplicationId(), event.getUserId(), event.getApprovedAmount());
            // TODO: Wire to DisbursementService in Layer 4
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
