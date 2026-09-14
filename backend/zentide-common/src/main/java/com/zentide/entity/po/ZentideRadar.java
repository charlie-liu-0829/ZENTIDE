package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ZentideRadar {
    private Long radarId;
    private String ownerId;
    private String name;
    private String attentionLevel;
    private String notificationStrategy;
    private String changeTypesJson;
    private String sourcePreferencesJson;
    private String ignoreRulesJson;
    private String userContext;
    private String visibility;
    private String status;
    private Integer version;
    private Long sourceCount;
    private Long changeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ZentideRadarTopic> topics;
}
