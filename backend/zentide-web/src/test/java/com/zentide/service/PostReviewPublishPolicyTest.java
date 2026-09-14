package com.zentide.service;

import com.zentide.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostReviewPublishPolicyTest {
    @Test
    void blockedReviewCannotBeForcePublished() {
        Map<String, Object> review = Map.of(
                "publish_blocked", true,
                "advice", "revise"
        );

        assertThrows(BusinessException.class, () -> PostReviewPublishPolicy.enforce(review, true));
    }

    @Test
    void ordinaryRevisionCanStillBeExplicitlyForced() {
        Map<String, Object> review = Map.of(
                "publish_blocked", false,
                "advice", "revise"
        );

        assertDoesNotThrow(() -> PostReviewPublishPolicy.enforce(review, true));
    }
}
