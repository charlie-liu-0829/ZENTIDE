package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideChangeDiscussion {
    private Long discussionId;
    private Long changeId;
    private String authorId;
    private String authorLabel;
    private Long parentDiscussionId;
    private Long evidenceId;
    private String body;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
