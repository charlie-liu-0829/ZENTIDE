package com.zentide.service;

import com.zentide.entity.po.ZentideSignal;
import com.zentide.mapper.ZentideSignalMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZentideSignalService {
    private final ZentideSignalMapper mapper;

    public ZentideSignalService(ZentideSignalMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideSignal> listActive(Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(100, requestedLimit));
        return mapper.listActive(limit);
    }
}
