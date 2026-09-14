package com.zentide.messaging;

/** Durable deduplication boundary for at-least-once message delivery. */
public interface ProcessedEventStore {
    /** Returns true only when this consumer successfully claims a previously unseen event. */
    boolean claim(String consumerName, String eventId);
}
