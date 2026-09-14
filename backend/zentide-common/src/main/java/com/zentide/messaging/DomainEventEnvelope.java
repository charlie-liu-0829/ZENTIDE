package com.zentide.messaging;

import java.time.Instant;
import java.util.Map;

/** Versioned envelope shared by all asynchronous domain messages. */
public record DomainEventEnvelope(String eventId, String eventType, int eventVersion,
                                  String aggregateType, String aggregateId, Instant occurredAt,
                                  String traceId, String producer, Map<String, Object> payload) {
    public DomainEventEnvelope {
        required(eventId, "eventId"); required(eventType, "eventType");
        required(aggregateType, "aggregateType"); required(aggregateId, "aggregateId");
        required(traceId, "traceId"); required(producer, "producer");
        if (eventVersion < 1 || occurredAt == null) throw new IllegalArgumentException("Invalid event version or occurrence time");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
    private static void required(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); }
}
