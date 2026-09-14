package com.zentide.entity.vo;

import lombok.Data;

@Data
public class ZentideStanceSummary {
    private Long totalCount;
    private Long actionedCount;
    private Long planningCount;
    private Long watchingCount;
    private Long blockedCount;
    private Long notRelevantCount;
}
