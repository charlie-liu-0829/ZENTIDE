package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideRetrievalChunk {
    private Long chunkId;
    private Long documentVersionId;
    private Integer chunkNo;
    private Integer contentStart;
    private Integer contentEnd;
    private String chunkText;
    private String contentHash;
    private String status;
    private LocalDateTime indexedAt;
    private LocalDateTime createdAt;
}
