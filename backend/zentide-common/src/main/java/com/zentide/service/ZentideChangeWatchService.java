package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideChangeWatchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZentideChangeWatchService {
    private final ZentideChangeWatchMapper mapper;

    public ZentideChangeWatchService(ZentideChangeWatchMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void watch(String userId, Long changeId, String requestedMode) {
        requirePublishedChange(changeId);
        mapper.watch(userId, changeId, normalizeMode(requestedMode));
    }

    @Transactional
    public void stopWatching(String userId, Long changeId) {
        if (changeId == null || mapper.stopWatching(userId, changeId) != 1) {
            throw new BusinessException("这条潮变不在你的续看里");
        }
    }

    public List<ZentideChange> list(String userId, Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(100, requestedLimit));
        return mapper.listActive(userId, limit);
    }

    private void requirePublishedChange(Long changeId) {
        if (changeId == null || mapper.countPublishedChange(changeId) != 1) {
            throw new BusinessException("潮变不存在或仍在形成，暂时不能续看");
        }
    }

    private String normalizeMode(String requestedMode) {
        return "IMMEDIATE".equalsIgnoreCase(requestedMode) ? "IMMEDIATE" : "FOLLOW_UP";
    }
}
