package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideObservationActivity {
    private Long activityId;
    private Long signalId;
    private Long changeId;
    private Long sourceId;
    private String sourceName;
    private String signalTitle;
    private String changeTitle;
    private String stage;
    private String status;
    private String message;
    private String detailJson;
    private LocalDateTime createdAt;
}
