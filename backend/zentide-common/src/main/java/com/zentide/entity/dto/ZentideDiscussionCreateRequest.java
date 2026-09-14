package com.zentide.entity.dto;

import jakarta.validation.constraints.Size;

public record ZentideDiscussionCreateRequest(
        @jakarta.validation.constraints.NotBlank(message = "讨论内容不能为空")
        @Size(max = 2000, message = "讨论内容不能超过 2000 个字符") String body,
        Long parentDiscussionId,
        Long evidenceId
) {}
