package com.zentide.domain.entity;

/**
 * A stable identity that can be monitored, such as a project, product or job.
 */
public record TrackedEntity(Long entityId, String entityType, String canonicalKey,
                            String displayName, String status) {
}
