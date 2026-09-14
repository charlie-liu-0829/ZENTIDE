package com.zentide.service;

import com.zentide.entity.po.ZentideObservationActivity;
import com.zentide.mapper.ZentideObservationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZentideObservationService {
    private final ZentideObservationMapper mapper;

    public ZentideObservationService(ZentideObservationMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideObservationActivity> list(Long signalId, Long changeId, Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(100, requestedLimit));
        return mapper.list(signalId, changeId, limit);
    }
}
