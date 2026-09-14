package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideTopic {
    private Long topicId;
    private String canonicalName;
    private String topicType;
    private String aliasesJson;
    private String status;
    private Long followerCount;
    private Long sourceCount;
    private Boolean followed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
