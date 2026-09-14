package com.zentide.entity.po;

import lombok.Data;

@Data
public class ZentideInterestChangeContext {
    private Long hubId;
    private String hubName;
    private Long topicId;
    private String topicName;
    private Long changeId;
    private String changeTitle;
    private String changeSummary;
}
