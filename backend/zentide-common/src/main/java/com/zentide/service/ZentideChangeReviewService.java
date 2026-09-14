package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideChangeReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZentideChangeReviewService {
    private final ZentideChangeReviewMapper mapper;

    public ZentideChangeReviewService(ZentideChangeReviewMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideChange> listCandidates(Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(100, requestedLimit));
        return mapper.listCandidates(limit);
    }

    @Transactional
    public void verifyAndPublish(Long changeId, String actorId) {
        if (changeId == null) throw new BusinessException("Change ID 不能为空");
        ZentideChange change = mapper.findChange(changeId);
        if (change == null || !"CANDIDATE".equals(change.getStatus()) || !"UNVERIFIED".equals(change.getVerificationStatus())) {
            throw new BusinessException("Change 不处于可验证状态");
        }
        int factClaims = mapper.countFactClaims(changeId);
        int coveredClaims = mapper.countCoveredFactClaims(changeId);
        if (factClaims == 0 || coveredClaims != factClaims) {
            throw new BusinessException("每条事实 Claim 都必须具备 Evidence，不能发布");
        }
        if (mapper.publishVerified(changeId) != 1) throw new BusinessException("Change 状态已变化，请刷新后重试");
        mapper.verifyClaims(changeId);
        mapper.insertVerificationAudit(actorId, changeId);
        mapper.promoteLinkedSignals(changeId);
        mapper.insertPromotionActivities(changeId);
    }
}
