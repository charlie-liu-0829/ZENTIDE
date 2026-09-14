package com.zentide.task;

import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.config.ZentideFetchProperties;
import com.zentide.service.ZentideSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "zentide.fetch.scheduler-enabled", havingValue = "true")
public class ZentideSourceFetchScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZentideSourceFetchScheduler.class);
    private final ZentideSourceService service;
    private final ZentideFetchProperties properties;

    public ZentideSourceFetchScheduler(ZentideSourceService service, ZentideFetchProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${zentide.fetch.interval-ms:1800000}")
    public void fetchActiveSources() {
        service.list().stream()
                .filter(source -> "ACTIVE".equals(source.getStatus()))
                .limit(Math.max(1, Math.min(200, properties.getBatchSize())))
                .forEach(this::fetch);
    }

    private void fetch(ZentideSource source) {
        try {
            service.fetch(source.getSourceId());
        } catch (Exception e) {
            LOGGER.warn("ZENTIDE source fetch failed: sourceId={}", source.getSourceId());
        }
    }
}
