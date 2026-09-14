package com.zentide.source;

public class ZentideFetchException extends RuntimeException {
    private final String code;

    public ZentideFetchException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
