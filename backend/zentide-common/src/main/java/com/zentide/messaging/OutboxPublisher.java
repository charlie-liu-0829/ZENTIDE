package com.zentide.messaging;

import com.alibaba.fastjson2.JSON;
import com.zentide.entity.po.ZentideOutboxEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** At-least-once publisher; consumers must deduplicate with processed_event. */
@Service
@ConditionalOnBean(DomainEventTransport.class)
public class OutboxPublisher {
    private static final int MAX_RETRIES = 8;
    private final OutboxEventStore store;
    private final DomainEventTransport transport;
    private final Clock clock;

    public OutboxPublisher(OutboxEventStore store, DomainEventTransport transport) { this(store, transport, Clock.systemUTC()); }
    OutboxPublisher(OutboxEventStore store, DomainEventTransport transport, Clock clock) { this.store = store; this.transport = transport; this.clock = clock; }

    public int publishDue(int requestedLimit) {
        int limit = Math.max(1, Math.min(100, requestedLimit));
        List<ZentideOutboxEvent> events = store.findDue(limit, now());
        int published = 0;
        for (ZentideOutboxEvent event : events) if (publishOne(event)) published++;
        return published;
    }

    @Transactional
    boolean publishOne(ZentideOutboxEvent event) {
        if (!store.claim(event.getEventId(), now())) return false;
        try {
            DomainEventEnvelope envelope = JSON.parseObject(event.getPayloadJson(), DomainEventEnvelope.class);
            if (envelope == null || !event.getEventId().equals(envelope.eventId())) throw new IllegalStateException("Invalid outbox envelope");
            transport.publish(event.getEventType().toLowerCase().replace('_', '.'), envelope);
            store.markSent(event.getEventId(), now());
            return true;
        } catch (Exception ignored) {
            int retries = event.getRetryCount() + 1;
            if (retries >= MAX_RETRIES) store.markFailed(event.getEventId(), retries);
            else store.reschedule(event.getEventId(), retries, now().plusSeconds(backoffSeconds(retries)));
            return false;
        }
    }

    private long backoffSeconds(int retryCount) { return Math.min(3600, 5L << Math.min(retryCount - 1, 9)); }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}
