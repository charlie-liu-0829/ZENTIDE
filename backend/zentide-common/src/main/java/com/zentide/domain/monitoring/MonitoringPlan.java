package com.zentide.domain.monitoring;

import java.time.LocalTime;
import java.util.Map;

/**
 * A versionable user intent for observing a part of the world.
 */
public interface MonitoringPlan {
    Long planId();

    String ownerId();

    Long scopeId();

    Long sourceId();

    String focusLevel();

    String rules();

    String deliveryFrequency();

    Map<String, String> personalization();

    String personalDeliveryFrequency();

    LocalTime quietHoursStart();

    LocalTime quietHoursEnd();
}
