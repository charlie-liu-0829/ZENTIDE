package com.zentide.service;

import com.zentide.exception.BusinessException;

import java.util.Map;

/** Server-side publication gate. Client flags can never bypass a blocked review. */
public final class PostReviewPublishPolicy {
    private PostReviewPublishPolicy() {
    }

    public static void enforce(Map<String, Object> review, boolean forcePublish) {
        if (Boolean.TRUE.equals(review.get("publish_blocked"))) {
            throw new BusinessException("检测到敏感信息，禁止发布；请移除或脱敏后重新检查");
        }
        String advice = String.valueOf(review.getOrDefault("advice", "revise"));
        if (!"ready".equals(advice) && !"manual_review".equals(advice) && !forcePublish) {
            throw new BusinessException("请先处理智能检查建议，或明确选择继续发布");
        }
    }
}
