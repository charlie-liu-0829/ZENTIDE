package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideRawSnapshot {
    private Long rawSnapshotId;
    private Long sourceId;
    private Long attemptId;
    private LocalDateTime capturedAt;
    private String contentType;
    private String contentHash;
    private String storageRef;
    private String body;
    private LocalDateTime createdAt;
}
