package com.zentide.service;

import com.zentide.entity.po.ZentideBriefing;
import com.zentide.entity.po.ZentideInsight;
import com.zentide.mapper.ZentideBriefingMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZentideBriefingServiceTest {
    @Test
    void dailyBriefingCompressesPublishedPersonalInsights() {
        ZentideBriefingMapper mapper = mock(ZentideBriefingMapper.class);
        ZentideInsightService insightService = mock(ZentideInsightService.class);
        ZentideInsight insight = new ZentideInsight();
        insight.setInsightId(6L);
        insight.setChangeId(7L);
        insight.setTitle("Spring Boot 发布新版本");
        insight.setBody("与你的 Java Radar 有关。");
        insight.setRelevanceScore(new BigDecimal("80.00"));
        insight.setWhyJson("{\"matchedTopics\":[\"Spring Boot\"]}");
        when(insightService.refreshAndList("u1", 7)).thenReturn(List.of(insight));
        ZentideBriefing persisted = new ZentideBriefing();
        persisted.setItemCount(1);
        when(mapper.findDaily(eq("u1"), any())).thenReturn(persisted);

        ZentideBriefing result = new ZentideBriefingService(mapper, insightService).daily("u1");

        assertEquals(1, result.getItemCount());
        verify(mapper).upsertDaily(argThat(briefing -> briefing.getHeadline().contains("1 个")
                && briefing.getBodyJson().contains("Spring Boot")));
    }
}
