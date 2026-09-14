package com.zentide.entity.dto;

import jakarta.validation.constraints.Size;

public record ZentideSourceApplicationReviewRequest(
        @Size(max = 1000, message = "审核说明不能超过 1000 个字符") String note
) {}
