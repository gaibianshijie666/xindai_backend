package com.xindai.xindai.common.event.impl;

import com.xindai.xindai.common.event.DomainEvent;
import com.xindai.xindai.common.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "local")
public class LocalEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public LocalEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.info("Publishing local event: {}", event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }
}
