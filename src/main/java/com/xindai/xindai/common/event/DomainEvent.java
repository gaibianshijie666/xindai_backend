package com.xindai.xindai.common.event;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public abstract class DomainEvent {
    private String eventId;
    private LocalDateTime occurredAt;
    private String traceId;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.traceId = org.slf4j.MDC.get("traceId");
    }
}
