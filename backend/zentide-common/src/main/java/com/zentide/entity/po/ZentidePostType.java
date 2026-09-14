package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentidePostType {
    private Long postTypeId;
    private Long hubId;
    private String hubName;
    private String typeCode;
    private String displayName;
    private String description;
    private Boolean systemFixed;
    private String status;
    private Integer sortOrder;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
