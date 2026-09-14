package com.zentide.entity.po;

import lombok.Data;

@Data
public class ZentideInterestEntity {
    private Long entityId;
    private Long hubId;
    private String entityType;
    private String name;
    private String subtitle;
    private String metadataJson;
    private String status;
    private Integer followerCount;
    private Integer postCount;
    private Integer eventCount;
    private Boolean followed;
    private String myAction;
}
