package com.zentide.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZentideResearchRequest(
        @NotBlank(message = "搜索问题不能为空") @Size(max = 200, message = "搜索问题不能超过 200 个字符") String query
) {}
