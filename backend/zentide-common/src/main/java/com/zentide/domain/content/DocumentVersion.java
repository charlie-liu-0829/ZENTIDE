package com.zentide.domain.content;

import java.time.LocalDateTime;

/**
 * Immutable normalized content version used as a fact boundary.
 */
public record DocumentVersion(Long documentId, int versionNo, Long rawSnapshotId,
                              String contentHash, String semanticHash,
                              LocalDateTime publishedAt, LocalDateTime createdAt) {
}
