package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideEvidence {
    private Long evidenceId;
    private Long changeId;
    private Long sourceId;
    private Long documentVersionId;
    private Long claimId;
    private String sourceName;
    private String sourceUrl;
    private String locator;
    private String excerpt;
    private String trustTier;
    private String excerptHash;
    private LocalDateTime createdAt;
}
