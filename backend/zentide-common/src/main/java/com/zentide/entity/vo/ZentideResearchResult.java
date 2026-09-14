package com.zentide.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideResearchResult {
    private Long chunkId;
    private String excerpt;
    private String title;
    private Long sourceId;
    private String sourceName;
    private String sourceUrl;
    private Long documentVersionId;
    private LocalDateTime publishedAt;
    private String contentHash;
    private Long evidenceId;
    private Long changeId;
}
