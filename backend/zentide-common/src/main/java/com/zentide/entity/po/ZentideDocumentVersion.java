package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideDocumentVersion {
    private Long documentVersionId;
    private Long documentId;
    private Integer versionNo;
    private Long rawSnapshotId;
    private String contentHash;
    private String semanticHash;
    private String title;
    private String normalizedContent;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
