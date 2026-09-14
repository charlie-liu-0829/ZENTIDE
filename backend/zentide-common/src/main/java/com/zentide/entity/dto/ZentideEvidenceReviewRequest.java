package com.zentide.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ZentideEvidenceReviewRequest(
        @NotNull(message = "文档版本不能为空")
        @Positive(message = "文档版本必须为正数")
        Long documentVersionId,
        @Size(max = 500, message = "审核说明不能超过 500 个字符")
        String reviewNote
) {}
