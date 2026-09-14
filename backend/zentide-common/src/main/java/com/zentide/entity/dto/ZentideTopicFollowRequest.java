package com.zentide.entity.dto;

import jakarta.validation.constraints.Size;

public record ZentideTopicFollowRequest(
        @Size(max = 20, message = "提醒方式不能超过 20 个字符") String notificationMode
) {}
