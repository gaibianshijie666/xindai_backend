package com.xindai.xindai.config;

import com.xindai.xindai.common.constants.QueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    // ==================== Exchanges ====================

    @Bean
    public DirectExchange xindaiExchange() {
        return new DirectExchange(QueueConstants.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(QueueConstants.DLX_EXCHANGE, true, false);
    }

    // ==================== Queues ====================

    private Queue buildQueue(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE);
        return new Queue(queueName, true, false, false, args);
    }

    @Bean
    public Queue loanApplicationSubmittedQueue() {
        return buildQueue(QueueConstants.LOAN_APPLICATION_SUBMITTED);
    }

    @Bean
    public Queue loanApplicationApprovedQueue() {
        return buildQueue(QueueConstants.LOAN_APPLICATION_APPROVED);
    }

    @Bean
    public Queue loanApplicationRejectedQueue() {
        return buildQueue(QueueConstants.LOAN_APPLICATION_REJECTED);
    }

    @Bean
    public Queue loanRepaymentCompletedQueue() {
        return buildQueue(QueueConstants.LOAN_REPAYMENT_COMPLETED);
    }

    @Bean
    public Queue loanOverdueDetectedQueue() {
        return buildQueue(QueueConstants.LOAN_OVERDUE_DETECTED);
    }

    @Bean
    public Queue riskAssessmentCompletedQueue() {
        return buildQueue(QueueConstants.RISK_ASSESSMENT_COMPLETED);
    }

    // ==================== Bindings ====================

    @Bean
    public Binding loanApplicationSubmittedBinding() {
        return BindingBuilder.bind(loanApplicationSubmittedQueue())
                .to(xindaiExchange())
                .with(QueueConstants.LOAN_APPLICATION_SUBMITTED);
    }

    @Bean
    public Binding loanApplicationApprovedBinding() {
        return BindingBuilder.bind(loanApplicationApprovedQueue())
                .to(xindaiExchange())
                .with(QueueConstants.LOAN_APPLICATION_APPROVED);
    }

    @Bean
    public Binding loanApplicationRejectedBinding() {
        return BindingBuilder.bind(loanApplicationRejectedQueue())
                .to(xindaiExchange())
                .with(QueueConstants.LOAN_APPLICATION_REJECTED);
    }

    @Bean
    public Binding loanRepaymentCompletedBinding() {
        return BindingBuilder.bind(loanRepaymentCompletedQueue())
                .to(xindaiExchange())
                .with(QueueConstants.LOAN_REPAYMENT_COMPLETED);
    }

    @Bean
    public Binding loanOverdueDetectedBinding() {
        return BindingBuilder.bind(loanOverdueDetectedQueue())
                .to(xindaiExchange())
                .with(QueueConstants.LOAN_OVERDUE_DETECTED);
    }

    @Bean
    public Binding riskAssessmentCompletedBinding() {
        return BindingBuilder.bind(riskAssessmentCompletedQueue())
                .to(xindaiExchange())
                .with(QueueConstants.RISK_ASSESSMENT_COMPLETED);
    }

    // ==================== Message Converter ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== RabbitTemplate ====================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(QueueConstants.EXCHANGE);
        return template;
    }

    // ==================== Listener Container Factory ====================

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }
}
