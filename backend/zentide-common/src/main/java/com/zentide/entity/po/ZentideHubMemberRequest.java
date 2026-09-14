package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideHubMemberRequest {
    private Long hubId;
    private String hubName;
    private String userId;
    private String nickName;
    private String avatar;
    private String membershipStatus;
    private String role;
    private String permissionsJson;
    private String applicationNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
