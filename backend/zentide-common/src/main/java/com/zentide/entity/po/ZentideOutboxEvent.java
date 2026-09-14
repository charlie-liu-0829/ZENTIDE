package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

/** Transactional outbox row. Payload is an immutable event envelope serialized as JSON. */
@Data
public class ZentideOutboxEvent {
    private String id;
    private String eventId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private Integer eventVersion;
    private String payloadJson;
    private String traceId;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
