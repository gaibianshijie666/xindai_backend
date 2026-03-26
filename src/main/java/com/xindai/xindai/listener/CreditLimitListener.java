package com.xindai.xindai.listener;

import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.RepaymentCompletedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 还款完成事件监听器
 * 恢复用户授信额度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitListener {

    // TODO: Restore credit limit on repayment

    @RabbitListener(queues = QueueConstants.LOAN_REPAYMENT_COMPLETED)
    public void onRepaymentCompleted(RepaymentCompletedEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            log.info("Credit limit restoration triggered for userId={}, contractId={}, period={}, amount={}",
                    event.getUserId(), event.getContractId(), event.getPeriod(), event.getAmount());
            // TODO: Restore credit limit on repayment
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
