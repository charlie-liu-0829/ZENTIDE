package com.zentide.entity.po;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ZentideSourceApplication {
    private Long applicationId;
    private Long topicId;
    private String applicantId;
    private String sourceName;
    private String sourceType;
    private String canonicalUrl;
    private String canonicalUrlHash;
    private String trustTier;
    private String status;
    private String reviewNote;
    private String reviewerAccount;
    private Long sourceId;
    private String trialResultJson;
    private LocalDateTime trialedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
