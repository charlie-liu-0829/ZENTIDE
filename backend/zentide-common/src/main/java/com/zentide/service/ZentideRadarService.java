package com.zentide.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.zentide.entity.po.ZentideRadar;
import com.zentide.entity.po.ZentideRadarTopic;
import com.zentide.entity.po.ZentideTopic;
import com.zentide.entity.dto.ZentideRadarCreateRequest;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideRadarMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ZentideRadarService {
    private static final Set<String> ATTENTION_LEVELS = Set.of("HIGH", "NORMAL", "LIGHT");
    private static final Set<String> NOTIFICATION_STRATEGIES = Set.of("REALTIME", "DAILY", "IMPORTANT_ONLY");
    private static final Set<String> CHANGE_TYPES = Set.of(
            "RELEASE", "BREAKING_CHANGE", "SECURITY", "PRICE", "API", "DOCUMENTATION", "COMMUNITY"
    );
    private static final Set<String> SOURCE_PREFERENCES = Set.of(
            "ALL", "OFFICIAL", "GITHUB", "RSS", "WEIBO", "XIAOHONGSHU", "BILIBILI", "XIAOHEIHE", "WEB"
    );
    private final ZentideRadarMapper mapper;

    public ZentideRadarService(ZentideRadarMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideRadar> list(String ownerId) {
        List<ZentideRadar> radars = mapper.listByOwner(ownerId);
        radars.forEach(radar -> radar.setTopics(mapper.listTopics(radar.getRadarId())));
        return radars;
    }

    @Transactional
    public ZentideRadar create(String ownerId, ZentideRadarCreateRequest request) {
        ZentideRadar radar = buildPlan(ownerId, null, request);
        mapper.insertRadar(radar);
        attachTopics(ownerId, radar.getRadarId(), parseTopics(request.topics()));
        radar.setTopics(mapper.listTopics(radar.getRadarId()));
        radar.setSourceCount(0L);
        radar.setChangeCount(0L);
        return radar;
    }

    @Transactional
    public ZentideRadar update(String ownerId, Long radarId, ZentideRadarCreateRequest request) {
        if (radarId == null || mapper.findOwned(ownerId, radarId) == null) {
            throw new BusinessException("雷达不存在或不属于当前用户");
        }
        ZentideRadar radar = buildPlan(ownerId, radarId, request);
        if (mapper.updateRadar(radar) != 1) {
            throw new BusinessException("雷达观察计划更新失败");
        }
        mapper.deleteTopics(radarId);
        attachTopics(ownerId, radarId, parseTopics(request.topics()));
        radar.setTopics(mapper.listTopics(radarId));
        return radar;
    }

    @Transactional
    public ZentideRadar addTopic(String ownerId, Long radarId, Long topicId) {
        ZentideRadar radar = mapper.findOwned(ownerId, radarId);
        if (radar == null || !"ACTIVE".equals(radar.getStatus())) {
            throw new BusinessException("雷达不存在或当前不可用");
        }
        ZentideTopic topic = mapper.findActiveTopic(topicId);
        if (topic == null) throw new BusinessException("话题不存在或已停用");
        ZentideRadarTopic link = new ZentideRadarTopic();
        link.setRadarId(radarId);
        link.setTopicId(topicId);
        mapper.attachTopic(link);
        mapper.ensureTopicFollow(ownerId, topicId);
        mapper.touchRadar(ownerId, radarId);
        radar.setTopics(mapper.listTopics(radarId));
        return radar;
    }

    private ZentideRadar buildPlan(String ownerId, Long radarId, ZentideRadarCreateRequest request) {
        List<String> topics = parseTopics(request.topics());
        if (topics.isEmpty()) throw new BusinessException("至少添加一个话题");
        ZentideRadar radar = new ZentideRadar();
        radar.setRadarId(radarId);
        radar.setOwnerId(ownerId);
        radar.setName(required(request.name(), "雷达名称", 120));
        radar.setAttentionLevel(enumValue(request.attentionLevel(), "NORMAL", ATTENTION_LEVELS, "关注强度"));
        radar.setNotificationStrategy(enumValue(request.notificationStrategy(), "DAILY", NOTIFICATION_STRATEGIES, "提醒策略"));
        radar.setChangeTypesJson(JSON.toJSONString(parseCodes(request.changeTypes(), List.of("RELEASE", "BREAKING_CHANGE", "SECURITY"), CHANGE_TYPES, "变化类型")));
        radar.setSourcePreferencesJson(JSON.toJSONString(parseCodes(request.sourcePreferences(), List.of("ALL"), SOURCE_PREFERENCES, "潮源偏好")));
        radar.setIgnoreRulesJson(JSON.toJSONString(parseTextList(request.ignoreRules(), 10, 100, "忽略规则")));
        radar.setUserContext(optional(request.userContext(), 1000, "使用背景"));
        return radar;
    }

    private void attachTopics(String ownerId, Long radarId, List<String> topics) {
        for (String topicName : topics) {
            ZentideTopic topic = new ZentideTopic();
            topic.setCanonicalName(topicName);
            topic.setTopicType("TECHNOLOGY");
            mapper.insertTopic(topic);
            ZentideRadarTopic link = new ZentideRadarTopic();
            link.setRadarId(radarId);
            link.setTopicId(topic.getTopicId());
            mapper.attachTopic(link);
            mapper.ensureTopicFollow(ownerId, topic.getTopicId());
        }
    }

    private List<String> parseTopics(String value) {
        return parseTextList(value, 20, 120, "话题");
    }

    private List<String> parseTextList(String value, int limit, int itemLength, String field) {
        try {
            JSONArray array = JSON.parseArray(value == null ? "[]" : value);
            List<String> items = array.stream().map(String::valueOf).map(String::trim)
                    .filter(item -> !item.isBlank()).distinct().toList();
            if (items.size() > limit || items.stream().anyMatch(item -> item.length() > itemLength)) {
                throw new BusinessException(field + "数量或长度超出限制");
            }
            return items;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(field + "必须是有效 JSON 数组");
        }
    }

    private List<String> parseCodes(String value, List<String> defaults, Set<String> supported, String field) {
        List<String> values = value == null || value.isBlank() ? defaults : parseTextList(value, 20, 40, field);
        List<String> normalized = values.stream().map(item -> item.toUpperCase(Locale.ROOT)).distinct().toList();
        if (normalized.isEmpty() || !supported.containsAll(normalized)) throw new BusinessException("不支持的" + field);
        return normalized;
    }

    private String enumValue(String value, String fallback, Set<String> supported, String field) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
        if (!supported.contains(normalized)) throw new BusinessException("不支持的" + field);
        return normalized;
    }

    private String required(String value, String field, int maxLength) {
        String result = value == null ? "" : value.trim();
        if (result.isBlank() || result.length() > maxLength) throw new BusinessException(field + "不能为空且不能超过 " + maxLength + " 个字符");
        return result;
    }

    private String optional(String value, int maxLength, String field) {
        String result = value == null ? "" : value.trim();
        if (result.length() > maxLength) throw new BusinessException(field + "不能超过 " + maxLength + " 个字符");
        return result.isBlank() ? null : result;
    }
}
