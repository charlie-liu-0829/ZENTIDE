package com.zentide.messaging;

import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Consumer;

/** Execute a handler at most once per consumer/event pair. The claim and business write should share a transaction. */
@Service
public class IdempotentEventConsumer {
    private final ProcessedEventStore store;

    public IdempotentEventConsumer(ProcessedEventStore store) { this.store = Objects.requireNonNull(store); }

    public boolean consume(String consumerName, DomainEventEnvelope event, Consumer<DomainEventEnvelope> handler) {
        if (consumerName == null || consumerName.isBlank() || event == null || handler == null) throw new IllegalArgumentException("Consumer, event and handler are required");
        if (!store.claim(consumerName, event.eventId())) return false;
        handler.accept(event);
        return true;
    }
}
