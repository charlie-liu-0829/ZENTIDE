package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ZentideChange {
    private Long changeId;
    private String entityType;
    private String entityKey;
    private Long entityId;
    private Long documentVersionId;
    private String changeKey;
    private String changeType;
    private String title;
    private String summary;
    private String whatHappened;
    private String importance;
    private String status;
    private String verificationStatus;
    private LocalDateTime occurredAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long correctedByChangeId;
    private Long supersedesChangeId;
    private Long discussionCount;
    private String watchMode;
    private LocalDateTime watchedAt;
    private List<ZentideClaim> claims;
    private List<ZentideEvidence> evidence;
}
