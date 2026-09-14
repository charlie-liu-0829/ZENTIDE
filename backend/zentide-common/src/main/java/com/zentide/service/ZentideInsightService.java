package com.zentide.service;

import com.alibaba.fastjson2.JSON;
import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideInsight;
import com.zentide.entity.po.ZentideRadarTopic;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideInsightMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ZentideInsightService {
    private static final List<String> DISMISSAL_FEEDBACK = List.of("IRRELEVANT", "KNOWN", "TOO_NOISY");
    private final ZentideInsightMapper mapper;

    public ZentideInsightService(ZentideInsightMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public List<ZentideInsight> refreshAndList(String ownerId, Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(50, requestedLimit));
        List<ZentideRadarTopic> topics = mapper.listActiveTopics(ownerId);
        if (topics.isEmpty()) return List.of();
        for (ZentideChange change : mapper.listVerifiedChanges(200)) {
            ZentideInsight insight = evaluate(ownerId, change, topics);
            if (insight != null) mapper.upsertInsight(insight);
        }
        return mapper.listPublished(ownerId, limit);
    }

    @Transactional
    public void feedback(String ownerId, Long insightId, String requestedFeedbackType, String note) {
        if (insightId == null || mapper.findOwned(ownerId, insightId) == null) {
            throw new BusinessException("Insight 不存在或不属于当前用户");
        }
        String feedbackType = requestedFeedbackType == null ? "" : requestedFeedbackType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("USEFUL", "IRRELEVANT", "KNOWN", "TOO_NOISY").contains(feedbackType)) {
            throw new BusinessException("不支持的 Insight 反馈类型");
        }
        String safeNote = note == null ? null : note.trim();
        if (safeNote != null && safeNote.length() > 500) throw new BusinessException("反馈说明不能超过 500 个字符");
        mapper.upsertFeedback(ownerId, insightId, feedbackType, safeNote);
        mapper.updateStatus(ownerId, insightId, DISMISSAL_FEEDBACK.contains(feedbackType) ? "DISMISSED" : "PUBLISHED");
    }

    private ZentideInsight evaluate(String ownerId, ZentideChange change, List<ZentideRadarTopic> topics) {
        String searchable = normalize(change.getTitle() + " " + change.getSummary() + " " + change.getWhatHappened() + " " + change.getEntityKey());
        List<String> matches = new ArrayList<>();
        BigDecimal score = importanceScore(change.getImportance());
        for (ZentideRadarTopic topic : topics) {
            String name = topic.getCanonicalName() == null ? "" : topic.getCanonicalName().trim();
            if (name.isBlank() || !searchable.contains(normalize(name))) continue;
            matches.add(name);
            score = score.add(topic.getWeight() == null ? new BigDecimal("20") : topic.getWeight().multiply(new BigDecimal("20")));
        }
        if (matches.isEmpty()) return null;
        score = score.min(new BigDecimal("100.00"));
        Map<String, Object> why = new LinkedHashMap<>();
        why.put("matchedTopics", matches);
        why.put("rule", "TOPIC_TEXT_MATCH");
        why.put("factStatus", "VERIFIED");
        why.put("changeId", change.getChangeId());

        ZentideInsight insight = new ZentideInsight();
        insight.setOwnerId(ownerId);
        insight.setChangeId(change.getChangeId());
        insight.setRelevanceScore(score);
        insight.setTitle(change.getTitle());
        insight.setBody("这条已验证的公共变化命中了你的 Radar Topic：" + String.join("、", matches) + "。" + safeSummary(change));
        insight.setWhyJson(JSON.toJSONString(why));
        return insight;
    }

    private BigDecimal importanceScore(String importance) {
        return switch (importance == null ? "MEDIUM" : importance.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> new BigDecimal("40");
            case "HIGH" -> new BigDecimal("30");
            case "LOW" -> new BigDecimal("10");
            default -> new BigDecimal("20");
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String safeSummary(ZentideChange change) {
        String summary = change.getWhatHappened();
        if (summary == null || summary.isBlank()) summary = change.getSummary();
        if (summary == null || summary.isBlank()) return "";
        return " 事实摘要：" + (summary.length() > 240 ? summary.substring(0, 240) + "…" : summary);
    }
}
