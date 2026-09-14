package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideDocument {
    private Long documentId;
    private Long sourceId;
    private String canonicalKey;
    private Integer currentVersionNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
