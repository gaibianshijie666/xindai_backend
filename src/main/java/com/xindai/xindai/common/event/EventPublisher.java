package com.xindai.xindai.common.event;

public interface EventPublisher {
    void publish(DomainEvent event);
}
