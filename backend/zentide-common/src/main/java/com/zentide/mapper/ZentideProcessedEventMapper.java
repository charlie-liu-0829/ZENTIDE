package com.zentide.mapper;

import com.zentide.messaging.ProcessedEventStore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZentideProcessedEventMapper extends ProcessedEventStore {
    int insert(@Param("consumerName") String consumerName, @Param("eventId") String eventId);

    @Override
    default boolean claim(String consumerName, String eventId) { return insert(consumerName, eventId) == 1; }
}
