package com.xindai.xindai.listener;

import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.LoanOverdueDetectedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 逾期检测事件监听器
 * 触发催收流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionListener {

    // TODO: Wire to CollectionTaskService in Layer 5

    @RabbitListener(queues = QueueConstants.LOAN_OVERDUE_DETECTED)
    public void onOverdueDetected(LoanOverdueDetectedEvent event,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Collection triggered for userId={}, contractId={}, overdueDays={}, overdueAmount={}",
                    event.getUserId(), event.getContractId(), event.getOverdueDays(), event.getOverdueAmount());
            // TODO: Wire to CollectionTaskService in Layer 5
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
