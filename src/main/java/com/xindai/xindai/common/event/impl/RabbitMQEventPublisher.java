package com.xindai.xindai.common.event.impl;

import com.xindai.xindai.common.event.*;
import com.xindai.xindai.common.constants.QueueConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        String routingKey = resolveRoutingKey(event);
        log.info("Publishing event: {} to routingKey: {}", event.getClass().getSimpleName(), routingKey);
        rabbitTemplate.convertAndSend(routingKey, event);
    }

    private String resolveRoutingKey(DomainEvent event) {
        return switch (event.getClass().getSimpleName()) {
            case "LoanApplicationSubmittedEvent" -> QueueConstants.LOAN_APPLICATION_SUBMITTED;
            case "LoanApplicationApprovedEvent" -> QueueConstants.LOAN_APPLICATION_APPROVED;
            case "LoanApplicationRejectedEvent" -> QueueConstants.LOAN_APPLICATION_REJECTED;
            case "RepaymentCompletedEvent" -> QueueConstants.LOAN_REPAYMENT_COMPLETED;
            case "LoanOverdueDetectedEvent" -> QueueConstants.LOAN_OVERDUE_DETECTED;
            case "RiskAssessmentCompletedEvent" -> QueueConstants.RISK_ASSESSMENT_COMPLETED;
            case "DisbursementCompletedEvent" -> QueueConstants.LOAN_APPLICATION_APPROVED;
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass());
        };
    }
}
