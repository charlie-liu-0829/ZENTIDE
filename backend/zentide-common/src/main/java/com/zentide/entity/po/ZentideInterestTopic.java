package com.zentide.entity.po;

import lombok.Data;

@Data
public class ZentideInterestTopic {
    private Long topicId;
    private Long hubId;
    private String canonicalName;
    private String topicType;
    private String status;
    private Integer postCount;
    private Integer hubCount;
    private Boolean featured;
}
