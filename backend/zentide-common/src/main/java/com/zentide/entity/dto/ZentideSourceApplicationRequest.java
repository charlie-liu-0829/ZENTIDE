package com.zentide.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZentideSourceApplicationRequest(
        @NotBlank(message = "来源名称不能为空") @Size(max = 200, message = "来源名称不能超过 200 个字符") String sourceName,
        @NotBlank(message = "来源类型不能为空") @Size(max = 32, message = "来源类型不能超过 32 个字符") String sourceType,
        @NotBlank(message = "来源地址不能为空") @Size(max = 2048, message = "来源地址不能超过 2048 个字符") String canonicalUrl
) {}
