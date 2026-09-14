package com.zentide.entity.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ZentideSignal {
    private Long signalId;
    private Long sourceId;
    private Long documentVersionId;
    private Long linkedChangeId;
    private Long topicId;
    private String topicNames;
    private String sourceName;
    private String signalKey;
    private String signalType;
    private String title;
    private String summary;
    private String status;
    private BigDecimal confidenceScore;
    private LocalDateTime discoveredAt;
    private LocalDateTime promotedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
