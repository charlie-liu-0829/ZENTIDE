package com.zentide.mapper;

import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.po.ZentideSourceApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ZentideSourceApplicationMapper {
    ZentideSourceApplication find(Long applicationId);

    List<ZentideSourceApplication> list();

    Long findActiveTopic(Long topicId);

    int insert(ZentideSourceApplication application);

    int review(@Param("applicationId") Long applicationId, @Param("expected") String expected, @Param("status") String status, @Param("note") String note, @Param("actor") String actor, @Param("at") LocalDateTime at);

    int startTrial(Long applicationId);

    int finishTrial(@Param("applicationId") Long applicationId, @Param("status") String status, @Param("result") String result, @Param("at") LocalDateTime at);

    ZentideSource findSourceByUrlHash(String urlHash);

    int insertActiveSource(ZentideSource source);

    int attachTopicSource(@Param("topicId") Long topicId, @Param("sourceId") Long sourceId, @Param("platformKey") String platformKey, @Param("addedBy") String addedBy);

    int activate(@Param("applicationId") Long applicationId, @Param("sourceId") Long sourceId, @Param("actor") String actor, @Param("at") LocalDateTime at);

    int addAudit(@Param("applicationId") Long applicationId, @Param("actorType") String actorType, @Param("actorId") String actorId, @Param("action") String action, @Param("note") String note, @Param("detail") String detail);
}
