package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

/** A platform-managed direction used to classify interest hubs. */
@Data
public class ZentideHubDirection {
    private Long directionId;
    private String code;
    private String displayName;
    private String description;
    private String status;
    private Integer sortOrder;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
