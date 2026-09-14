package com.zentide.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZentideTopicCreateRequest(
        @NotBlank(message = "话题名称不能为空") @Size(max = 120, message = "话题名称不能超过 120 个字符") String name,
        @Size(max = 24, message = "话题类型不能超过 24 个字符") String topicType
) {}
