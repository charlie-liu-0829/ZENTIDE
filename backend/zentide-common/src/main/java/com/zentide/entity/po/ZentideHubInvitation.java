package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideHubInvitation {
    private Long invitationId;
    private Long hubId;
    private String hubName;
    private String createdBy;
    private String inviteToken;
    private LocalDateTime expiresAt;
    private Integer maxUses;
    private Integer useCount;
    private String status;
}
