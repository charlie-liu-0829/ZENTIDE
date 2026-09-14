package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideEvidenceSubmission {
    private Long submissionId;
    private Long changeId;
    private String changeTitle;
    private String authorId;
    private String sourceUrl;
    private String excerpt;
    private String note;
    private String status;
    private String reviewedBy;
    private String reviewNote;
    private Long acceptedEvidenceId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime updatedAt;
}
