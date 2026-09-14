package com.zentide.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZentideRadarCreateRequest(
        @NotBlank(message = "雷达名称不能为空") @Size(max = 120, message = "雷达名称不能超过 120 个字符") String name,
        @NotBlank(message = "雷达至少需要一个话题") @Size(max = 4000, message = "雷达话题配置不能超过 4000 个字符") String topics,
        @Size(max = 20, message = "关注强度不能超过 20 个字符") String attentionLevel,
        @Size(max = 24, message = "提醒策略不能超过 24 个字符") String notificationStrategy,
        @Size(max = 4000, message = "变化类型配置不能超过 4000 个字符") String changeTypes,
        @Size(max = 4000, message = "潮源偏好配置不能超过 4000 个字符") String sourcePreferences,
        @Size(max = 4000, message = "忽略规则不能超过 4000 个字符") String ignoreRules,
        @Size(max = 1000, message = "使用背景不能超过 1000 个字符") String userContext
) {}
