package com.zentide.mapper;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideClaim;
import com.zentide.entity.po.ZentideEvidence;
import com.zentide.entity.po.ZentideSignal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZentideChangeCandidateMapper {
    int insertSignal(ZentideSignal signal);

    int attachSignalTopics(@Param("signalId") Long signalId, @Param("sourceId") Long sourceId);

    int insertChange(ZentideChange change);

    int insertClaim(ZentideClaim claim);

    int insertEvidence(ZentideEvidence evidence);

    int linkSignalToChange(@Param("signalId") Long signalId, @Param("changeId") Long changeId);

    int insertActivity(@Param("signalId") Long signalId, @Param("changeId") Long changeId,
                       @Param("sourceId") Long sourceId, @Param("stage") String stage,
                       @Param("status") String status, @Param("message") String message);
}
