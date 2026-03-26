package com.xindai.xindai.common.event;

import com.xindai.xindai.common.event.impl.RabbitMQEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@DisplayName("RabbitMQEventPublisher 单元测试")
class RabbitMQEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitMQEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new RabbitMQEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("发布贷款申请提交事件")
    void publish_ApplicationSubmitted() {
        LoanApplicationSubmittedEvent event = new LoanApplicationSubmittedEvent(1L, 100L, new BigDecimal("50000"), 12);
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(eq("loan.application.submitted"), eq(event));
    }

    @Test
    @DisplayName("发布风控评估完成事件")
    void publish_RiskAssessmentCompleted() {
        RiskAssessmentCompletedEvent event = new RiskAssessmentCompletedEvent(1L, 100L, "LOW", new BigDecimal("100000"), 1);
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(eq("risk.assessment.completed"), eq(event));
    }

    @Test
    @DisplayName("发布还款完成事件")
    void publish_RepaymentCompleted() {
        RepaymentCompletedEvent event = new RepaymentCompletedEvent(1L, 100L, 1, new BigDecimal("5000"));
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(eq("loan.repayment.completed"), eq(event));
    }

    @Test
    @DisplayName("发布逾期检测事件")
    void publish_LoanOverdueDetected() {
        LoanOverdueDetectedEvent event = new LoanOverdueDetectedEvent(1L, 100L, 30, new BigDecimal("10000"));
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(eq("loan.overdue.detected"), eq(event));
    }
}
