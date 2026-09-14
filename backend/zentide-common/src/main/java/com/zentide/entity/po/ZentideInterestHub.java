package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideInterestHub {
    private Long hubId;
    private String slug;
    private String ownerId;
    private String ownerName;
    private String name;
    private String description;
    private String category;
    private String coverUrl;
    private String joinPolicy;
    private String joinQuestion;
    private String joinAnswer;
    private String status;
    private String visibility;
    private Integer memberCount;
    private Integer postCount;
    private Boolean joined;
    private Boolean owned;
    private String membershipStatus;
    private String memberRole;
    private String memberPermissions;
    private Long personalCategoryId;
    private String personalCategoryName;
    private LocalDateTime createdAt;
}
