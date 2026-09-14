package com.zentide.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZentideEvidenceSubmissionRequest(
        @NotBlank(message = "潮源链接不能为空") @Size(max = 2048, message = "潮源链接不能超过 2048 个字符") String sourceUrl,
        @NotBlank(message = "原文摘录不能为空") @Size(max = 4000, message = "原文摘录不能超过 4000 个字符") String excerpt,
        @Size(max = 1000, message = "补充说明不能超过 1000 个字符") String note
) {}
