package com.zentide.service;

import com.zentide.entity.vo.ZentideResearchResult;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideRetrievalMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZentideResearchService {
    private final ZentideRetrievalMapper mapper;

    public ZentideResearchService(ZentideRetrievalMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideResearchResult> search(String requestedQuery, Integer requestedLimit) {
        String query = requestedQuery == null ? "" : requestedQuery.trim();
        if (query.length() < 2 || query.length() > 200) {
            throw new BusinessException("研究问题需要是 2 到 200 个字符");
        }
        int limit = requestedLimit == null ? 8 : Math.max(1, Math.min(20, requestedLimit));
        return mapper.searchVerified(query, limit);
    }
}
