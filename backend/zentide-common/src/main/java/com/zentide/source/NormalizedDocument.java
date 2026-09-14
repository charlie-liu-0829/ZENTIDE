package com.zentide.source;

import java.time.LocalDateTime;

public record NormalizedDocument(String canonicalKey, String title, String content,
                                 String contentHash, LocalDateTime publishedAt,
                                 String sourceUrl, String excerpt) {
}
