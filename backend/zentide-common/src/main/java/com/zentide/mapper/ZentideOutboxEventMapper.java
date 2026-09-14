package com.zentide.mapper;

import com.zentide.entity.po.ZentideOutboxEvent;
import com.zentide.messaging.OutboxEventStore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ZentideOutboxEventMapper extends OutboxEventStore {
    @Override
    void insert(ZentideOutboxEvent event);

    @Override
    List<ZentideOutboxEvent> findDue(@Param("limit") int limit, @Param("now") LocalDateTime now);

    @Override
    boolean claim(@Param("eventId") String eventId, @Param("now") LocalDateTime now);

    @Override
    void markSent(@Param("eventId") String eventId, @Param("publishedAt") LocalDateTime publishedAt);

    @Override
    void reschedule(@Param("eventId") String eventId, @Param("retryCount") int retryCount, @Param("nextRetryAt") LocalDateTime nextRetryAt);

    @Override
    void markFailed(@Param("eventId") String eventId, @Param("retryCount") int retryCount);
}
