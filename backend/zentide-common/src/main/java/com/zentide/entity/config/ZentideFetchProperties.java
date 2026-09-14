package com.zentide.entity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zentide.fetch")
public class ZentideFetchProperties {
    private boolean schedulerEnabled;
    private long intervalMs = 1_800_000L;
    private int batchSize = 50;
}
