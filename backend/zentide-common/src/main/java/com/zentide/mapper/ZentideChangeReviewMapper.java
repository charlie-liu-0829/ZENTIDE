package com.zentide.mapper;

import com.zentide.entity.po.ZentideChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideChangeReviewMapper {
    List<ZentideChange> listCandidates(@Param("limit") int limit);

    ZentideChange findChange(Long changeId);

    int countFactClaims(Long changeId);

    int countCoveredFactClaims(Long changeId);

    int publishVerified(Long changeId);

    int verifyClaims(Long changeId);

    int insertVerificationAudit(@Param("actorId") String actorId, @Param("changeId") Long changeId);

    int promoteLinkedSignals(Long changeId);

    int insertPromotionActivities(Long changeId);
}
