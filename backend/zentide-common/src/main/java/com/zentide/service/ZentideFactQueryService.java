package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideEvidence;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideFactMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZentideFactQueryService {
    private final ZentideFactMapper mapper;

    public ZentideFactQueryService(ZentideFactMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideChange> listChanges(Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(100, requestedLimit));
        return mapper.listPublishedChanges(limit);
    }

    public ZentideChange getChange(Long changeId) {
        if (changeId == null) {
            throw new BusinessException("Change ID 不能为空");
        }
        ZentideChange change = mapper.findPublishedChange(changeId);
        if (change == null) {
            throw new BusinessException("Change 不存在");
        }
        change.setClaims(mapper.listClaims(changeId));
        change.setEvidence(mapper.listEvidence(changeId));
        return change;
    }

    public List<ZentideEvidence> listEvidence(Long changeId) {
        if (changeId == null) {
            throw new BusinessException("Change ID 不能为空");
        }
        if (mapper.findPublishedChange(changeId) == null) {
            throw new BusinessException("Change 不存在");
        }
        return mapper.listEvidence(changeId);
    }
}
