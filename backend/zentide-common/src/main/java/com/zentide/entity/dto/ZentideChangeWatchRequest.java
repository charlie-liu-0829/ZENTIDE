package com.zentide.entity.dto;

import jakarta.validation.constraints.Size;

public record ZentideChangeWatchRequest(
        @Size(max = 20, message = "续看方式不能超过 20 个字符") String watchMode
) {}
