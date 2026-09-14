package com.zentide.entity.po;

import lombok.Data;

@Data
public class ZentideInterestEventStats {
    private Long eventId;
    private Integer wantCount;
    private Integer attendingCount;
    private Integer attendedCount;
    private Integer totalParticipants;
}
