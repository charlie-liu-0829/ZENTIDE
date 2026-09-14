package com.zentide.messaging;

import com.zentide.entity.po.ZentideOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventStore {
    void insert(ZentideOutboxEvent event);
    List<ZentideOutboxEvent> findDue(int limit, LocalDateTime now);
    boolean claim(String eventId, LocalDateTime now);
    void markSent(String eventId, LocalDateTime publishedAt);
    void reschedule(String eventId, int retryCount, LocalDateTime nextRetryAt);
    void markFailed(String eventId, int retryCount);
}
