package com.zentide.entity.enums;

public final class SourceStatuses {
    public static final String PENDING_CHECK = "PENDING_CHECK";
    public static final String CHECK_FAILED = "CHECK_FAILED";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String REJECTED = "REJECTED";
    public static final String APPROVED = "APPROVED";
    public static final String CONFIGURING = "CONFIGURING";
    public static final String TRIAL_RUNNING = "TRIAL_RUNNING";
    public static final String ACTIVE = "ACTIVE";
    public static final String DEGRADED = "DEGRADED";
    public static final String PAUSED = "PAUSED";
    public static final String DISABLED = "DISABLED";
    public static final String WITHDRAWN = "WITHDRAWN";

    public static final String CHECK_PENDING = "PENDING";
    public static final String CHECK_RUNNING = "RUNNING";
    public static final String CHECK_PASSED = "PASSED";
    public static final String CHECK_FAILED_STATUS = "FAILED";

    private SourceStatuses() {
    }
}
