package com.zentide.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ZentideChangeStanceRequest(
        @NotBlank(message = "请选择你的当前状态")
        @Pattern(regexp = "ACTIONED|PLANNING|WATCHING|BLOCKED|NOT_RELEVANT", message = "社区状态不受支持")
        String stanceType
) {}
