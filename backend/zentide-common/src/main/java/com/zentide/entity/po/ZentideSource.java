package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideSource {
    private Long sourceId;
    private String sourceName;
    private String sourceType;
    private String canonicalUrl;
    private String canonicalUrlHash;
    private String trustTier;
    private String adapterKey;
    private String status;
    private String policyJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
