package com.zentide.entity.po;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ZentideRadarTopic {
    private Long radarTopicId;
    private Long radarId;
    private Long topicId;
    private BigDecimal weight;
    private Boolean includeDescendants;
    private String note;
    private String canonicalName;
    private String topicType;
}
