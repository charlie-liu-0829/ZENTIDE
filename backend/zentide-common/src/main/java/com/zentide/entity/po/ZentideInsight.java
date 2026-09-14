package com.zentide.entity.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ZentideInsight {
    private Long insightId;
    private String ownerId;
    private Long changeId;
    private BigDecimal relevanceScore;
    private String title;
    private String body;
    private String whyJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String changeTitle;
    private String changeSummary;
    private String verificationStatus;
    private LocalDateTime occurredAt;
    private String feedbackType;
}
