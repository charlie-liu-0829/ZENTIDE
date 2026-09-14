package com.zentide.service;

import com.zentide.entity.vo.ZentideStanceSummary;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideChangeStanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class ZentideChangeStanceService {
    private static final Set<String> SUPPORTED_STANCES = Set.of("ACTIONED", "PLANNING", "WATCHING", "BLOCKED", "NOT_RELEVANT");
    private final ZentideChangeStanceMapper mapper;

    public ZentideChangeStanceService(ZentideChangeStanceMapper mapper) {
        this.mapper = mapper;
    }

    public ZentideStanceSummary summary(Long changeId) {
        requirePublished(changeId);
        return mapper.summarize(changeId);
    }

    public String mine(String userId, Long changeId) {
        requirePublished(changeId);
        return mapper.findMine(changeId, userId);
    }

    @Transactional
    public ZentideStanceSummary choose(String userId, Long changeId, String requestedStance) {
        requirePublished(changeId);
        String stance = requestedStance == null ? "" : requestedStance.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STANCES.contains(stance)) throw new BusinessException("社区状态不受支持");
        mapper.upsert(changeId, userId, stance);
        return mapper.summarize(changeId);
    }

    private void requirePublished(Long changeId) {
        if (changeId == null || mapper.countPublishedChange(changeId) != 1) {
            throw new BusinessException("潮变不存在或尚未确认");
        }
    }
}
