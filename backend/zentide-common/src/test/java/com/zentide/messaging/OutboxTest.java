package com.zentide.messaging;

import com.zentide.entity.po.ZentideOutboxEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), ZoneOffset.UTC);

    @Test
    void enqueuePersistsVersionedEnvelopeBeforeAnyTransportCall() {
        MemoryStore store = new MemoryStore();
        DomainEventEnvelope event = new OutboxService(store, CLOCK).enqueue("CHANGE_CANDIDATE_CREATED", "CHANGE", "31", 1,
                "zentide-change", Map.of("changeId", 31));
        assertEquals(1, store.events.size());
        assertEquals("PENDING", store.events.getFirst().getStatus());
        assertEquals(event.eventId(), store.events.getFirst().getEventId());
        assertTrue(store.events.getFirst().getPayloadJson().contains("CHANGE_CANDIDATE_CREATED"));
    }

    @Test
    void publisherClaimsThenMarksSentAndUsesStableRoutingKey() {
        MemoryStore store = new MemoryStore();
        new OutboxService(store, CLOCK).enqueue("CHANGE_CANDIDATE_CREATED", "CHANGE", "31", 1,
                "zentide-change", Map.of("changeId", 31));
        List<String> routes = new ArrayList<>();
        int count = new OutboxPublisher(store, (route, event) -> routes.add(route), CLOCK).publishDue(10);
        assertEquals(1, count);
        assertEquals(List.of("change.candidate.created"), routes);
        assertEquals("SENT", store.events.getFirst().getStatus());
    }

    @Test
    void failedTransportIsRescheduledWithIncrementedRetryCount() {
        MemoryStore store = new MemoryStore();
        new OutboxService(store, CLOCK).enqueue("CHANGE_CANDIDATE_CREATED", "CHANGE", "31", 1,
                "zentide-change", Map.of());
        int count = new OutboxPublisher(store, (route, event) -> { throw new IllegalStateException("offline"); }, CLOCK).publishDue(10);
        assertEquals(0, count);
        assertEquals("PENDING", store.events.getFirst().getStatus());
        assertEquals(1, store.events.getFirst().getRetryCount());
        assertEquals(LocalDateTime.of(2026, 8, 27, 9, 0, 5), store.events.getFirst().getNextRetryAt());
    }

    @Test
    void processedEventStorePreventsDuplicateBusinessHandling() {
        ProcessedEventStore store = new ProcessedEventStore() {
            final java.util.Set<String> seen = new java.util.HashSet<>();
            public boolean claim(String consumer, String eventId) { return seen.add(consumer + ":" + eventId); }
        };
        IdempotentEventConsumer consumer = new IdempotentEventConsumer(store);
        DomainEventEnvelope event = new DomainEventEnvelope("evt-1", "CHANGE_PUBLISHED", 1, "CHANGE", "31", CLOCK.instant(), "trace-1", "test", Map.of());
        int[] calls = {0};
        assertTrue(consumer.consume("personalization", event, ignored -> calls[0]++));
        assertEquals(false, consumer.consume("personalization", event, ignored -> calls[0]++));
        assertEquals(1, calls[0]);
    }

    private static final class MemoryStore implements OutboxEventStore {
        final List<ZentideOutboxEvent> events = new ArrayList<>();
        public void insert(ZentideOutboxEvent event) { events.add(event); }
        public List<ZentideOutboxEvent> findDue(int limit, LocalDateTime now) { return events.stream().filter(event -> "PENDING".equals(event.getStatus()) && (event.getNextRetryAt() == null || !event.getNextRetryAt().isAfter(now))).limit(limit).toList(); }
        public boolean claim(String id, LocalDateTime now) { ZentideOutboxEvent event = events.stream().filter(item -> item.getEventId().equals(id) && "PENDING".equals(item.getStatus())).findFirst().orElse(null); if (event == null) return false; event.setStatus("PUBLISHING"); return true; }
        public void markSent(String id, LocalDateTime at) { update(id, "SENT", at, null, null); }
        public void reschedule(String id, int retries, LocalDateTime at) { update(id, "PENDING", null, retries, at); }
        public void markFailed(String id, int retries) { update(id, "FAILED", null, retries, null); }
        private void update(String id, String status, LocalDateTime sent, Integer retries, LocalDateTime next) { ZentideOutboxEvent event = events.stream().filter(item -> item.getEventId().equals(id)).findFirst().orElseThrow(); event.setStatus(status); if (sent != null) event.setPublishedAt(sent); if (retries != null) event.setRetryCount(retries); event.setNextRetryAt(next); }
    }
}
