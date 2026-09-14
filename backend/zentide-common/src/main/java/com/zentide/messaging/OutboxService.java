package com.zentide.messaging;

import com.alibaba.fastjson2.JSON;
import com.zentide.entity.po.ZentideOutboxEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxEventStore store;
    private final Clock clock;

    @Autowired
    public OutboxService(OutboxEventStore store) { this(store, Clock.systemUTC()); }
    OutboxService(OutboxEventStore store, Clock clock) { this.store = store; this.clock = clock; }

    /** Invoke inside the transaction that persists the aggregate. */
    public DomainEventEnvelope enqueue(String eventType, String aggregateType, String aggregateId,
                                       int eventVersion, String producer, Map<String, Object> payload) {
        String eventId = id();
        String traceId = id();
        DomainEventEnvelope envelope = new DomainEventEnvelope(eventId, eventType, eventVersion,
                aggregateType, aggregateId, clock.instant(), traceId, producer, payload);
        ZentideOutboxEvent event = new ZentideOutboxEvent();
        event.setId(id()); event.setEventId(eventId); event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId); event.setEventType(eventType); event.setEventVersion(eventVersion);
        event.setPayloadJson(JSON.toJSONString(envelope)); event.setTraceId(traceId); event.setStatus("PENDING");
        event.setRetryCount(0); event.setCreatedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        store.insert(event);
        return envelope;
    }

    private static String id() { return UUID.randomUUID().toString().replace("-", ""); }
}
