package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ZentideBriefing {
    private Long briefingId;
    private String ownerId;
    private LocalDate briefingDate;
    private String briefingType;
    private String headline;
    private String bodyJson;
    private Integer itemCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
