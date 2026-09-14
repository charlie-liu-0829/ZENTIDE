package com.zentide.source;

public record SourceFetchResult(String finalUrl, int httpStatus, String contentType, byte[] body) {
}
