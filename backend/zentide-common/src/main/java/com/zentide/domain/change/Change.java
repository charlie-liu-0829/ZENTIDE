package com.zentide.domain.change;

import java.util.Map;

/**
 * A verified public change in a monitored subject.
 */
public interface Change {
    Long changeId();

    Long subjectId();

    Long sourceId();

    Long scopeId();

    String kind();

    String importance();

    String title();

    String sourceUrl();

    String location();

    String category();

    Map<String, String> attributes();
}
