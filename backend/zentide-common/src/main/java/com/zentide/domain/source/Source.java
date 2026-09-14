package com.zentide.domain.source;

/**
 * Source governance vocabulary shared by all future adapters.
 */
public record Source(Long sourceId, String sourceType, String canonicalUrl,
                     String trustTier, String status, String adapterKey) {
}
