package com.zentide.entity.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ZentideClaim {
    private Long claimId;
    private Long changeId;
    private String claimType;
    private String claimText;
    private BigDecimal confidence;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
