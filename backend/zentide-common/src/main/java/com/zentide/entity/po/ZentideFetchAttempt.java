package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideFetchAttempt {
    private Long attemptId;
    private Long sourceId;
    private String requestUrl;
    private String status;
    private Integer httpStatus;
    private String contentType;
    private String responseHash;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
