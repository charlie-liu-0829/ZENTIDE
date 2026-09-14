package com.zentide.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zentide.entity.po.ZentideBriefing;
import com.zentide.entity.po.ZentideInsight;
import com.zentide.mapper.ZentideBriefingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ZentideBriefingService {
    private final ZentideBriefingMapper mapper;
    private final ZentideInsightService insightService;

    public ZentideBriefingService(ZentideBriefingMapper mapper, ZentideInsightService insightService) {
        this.mapper = mapper;
        this.insightService = insightService;
    }

    @Transactional
    public ZentideBriefing daily(String ownerId) {
        List<ZentideInsight> insights = insightService.refreshAndList(ownerId, 7);
        ZentideBriefing briefing = new ZentideBriefing();
        briefing.setOwnerId(ownerId);
        briefing.setBriefingDate(LocalDate.now());
        briefing.setHeadline(insights.isEmpty() ? "今天没有需要额外关注的已验证变化" : "今天有 " + insights.size() + " 个值得关注的变化");
        briefing.setBodyJson(JSON.toJSONString(insights.stream().map(this::item).toList()));
        briefing.setItemCount(insights.size());
        mapper.upsertDaily(briefing);
        return mapper.findDaily(ownerId, briefing.getBriefingDate());
    }

    private JSONObject item(ZentideInsight insight) {
        JSONObject item = new JSONObject();
        item.put("insightId", insight.getInsightId());
        item.put("changeId", insight.getChangeId());
        item.put("title", insight.getTitle());
        item.put("body", insight.getBody());
        item.put("relevanceScore", insight.getRelevanceScore());
        item.put("why", insight.getWhyJson() == null || insight.getWhyJson().isBlank() ? new JSONObject() : JSON.parse(insight.getWhyJson()));
        return item;
    }
}
