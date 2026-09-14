package com.zentide.domain.evidence;

/**
 * A traceable excerpt or locator supporting a public fact.
 */
public record Evidence(Long evidenceId, Long sourceId, Long documentVersionId,
                       String locator, String excerptHash, String trustTier) {
}
