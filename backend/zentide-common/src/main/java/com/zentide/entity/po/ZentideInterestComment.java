package com.zentide.entity.po;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class ZentideInterestComment { private Long commentId; private Long postId; private String postTitle; private String hubName; private String authorId; private String authorLabel; private String authorAvatar; private Long parentCommentId; private String body; private String status; private Integer likeCount; private Boolean liked; private LocalDateTime createdAt; }
