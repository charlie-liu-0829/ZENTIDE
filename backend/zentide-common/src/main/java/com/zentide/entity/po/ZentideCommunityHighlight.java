package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

/** A high-signal post or comment selected for the user's daily community digest. */
@Data
public class ZentideCommunityHighlight {
    private String kind;
    private Long postId;
    private Long commentId;
    private Long hubId;
    private String hubName;
    private String title;
    private String body;
    private String authorId;
    private String authorLabel;
    private Integer likeCount;
    private Integer commentCount;
    private Integer bookmarkCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
