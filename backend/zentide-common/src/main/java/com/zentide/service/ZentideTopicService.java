package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideTopic;
import com.zentide.entity.po.ZentideTopicSource;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideTopicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ZentideTopicService {
    private final ZentideTopicMapper mapper;

    public ZentideTopicService(ZentideTopicMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideTopic> discover(String query, Integer requestedLimit) {
        String keyword = query == null ? "" : query.trim();
        return mapper.discover(keyword, boundedLimit(requestedLimit, 24));
    }

    public List<ZentideTopic> following(String userId) {
        return mapper.listFollowing(userId);
    }

    @Transactional
    public ZentideTopic createAndFollow(String userId, String requestedName, String topicType) {
        String name = required(requestedName, "话题名称", 120);
        ZentideTopic topic = mapper.findByName(name);
        if (topic == null) {
            topic = new ZentideTopic();
            topic.setCanonicalName(name);
            topic.setTopicType(normalizeTopicType(topicType));
            mapper.insertTopic(topic);
        }
        mapper.follow(topic.getTopicId(), userId, "DAILY");
        topic.setFollowed(true);
        return topic;
    }

    @Transactional
    public void follow(String userId, Long topicId, String notificationMode) {
        requireTopic(topicId);
        mapper.follow(topicId, userId, normalizeNotificationMode(notificationMode));
    }

    public void unfollow(String userId, Long topicId) {
        mapper.unfollow(topicId, userId);
    }

    public List<ZentideTopicSource> listSources(Long topicId) {
        requireTopic(topicId);
        return mapper.listSources(topicId);
    }

    public List<ZentideChange> feed(Long topicId, Integer requestedLimit) {
        requireTopic(topicId);
        return mapper.listFeed(topicId, boundedLimit(requestedLimit, 30));
    }

    private void requireTopic(Long topicId) {
        if (topicId == null || mapper.findTopic(topicId) == null) throw new BusinessException("话题不存在或已停用");
    }

    private int boundedLimit(Integer requested, int fallback) {
        return requested == null ? fallback : Math.max(1, Math.min(100, requested));
    }

    private String normalizeTopicType(String value) {
        String type = value == null || value.isBlank() ? "GENERAL" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("GENERAL", "TECHNOLOGY", "GAME", "LIFESTYLE", "FINANCE", "SPORTS", "CREATOR").contains(type)) {
            throw new BusinessException("不支持的话题类型");
        }
        return type;
    }

    private String normalizeNotificationMode(String value) {
        String mode = value == null || value.isBlank() ? "DAILY" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("REALTIME", "DAILY", "QUIET").contains(mode)) throw new BusinessException("不支持的提醒方式");
        return mode;
    }

    private String required(String value, String field, int maxLength) {
        String result = value == null ? "" : value.trim();
        if (result.isBlank() || result.length() > maxLength) throw new BusinessException(field + "不能为空且不能超过 " + maxLength + " 个字符");
        return result;
    }

}
