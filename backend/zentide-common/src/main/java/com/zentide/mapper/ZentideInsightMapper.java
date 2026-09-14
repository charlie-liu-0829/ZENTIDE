package com.zentide.mapper;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideInsight;
import com.zentide.entity.po.ZentideRadarTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideInsightMapper {
    List<ZentideChange> listVerifiedChanges(@Param("limit") int limit);

    List<ZentideRadarTopic> listActiveTopics(@Param("ownerId") String ownerId);

    int upsertInsight(ZentideInsight insight);

    List<ZentideInsight> listPublished(@Param("ownerId") String ownerId, @Param("limit") int limit);

    ZentideInsight findOwned(@Param("ownerId") String ownerId, @Param("insightId") Long insightId);

    int upsertFeedback(@Param("ownerId") String ownerId, @Param("insightId") Long insightId, @Param("feedbackType") String feedbackType, @Param("note") String note);

    int updateStatus(@Param("ownerId") String ownerId, @Param("insightId") Long insightId, @Param("status") String status);
}
