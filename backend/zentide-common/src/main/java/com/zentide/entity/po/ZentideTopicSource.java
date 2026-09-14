package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideTopicSource {
    private Long topicSourceId;
    private Long topicId;
    private Long sourceId;
    private String platformKey;
    private String observationStatus;
    private String addedBy;
    private String sourceName;
    private String sourceType;
    private String canonicalUrl;
    private String sourceStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
