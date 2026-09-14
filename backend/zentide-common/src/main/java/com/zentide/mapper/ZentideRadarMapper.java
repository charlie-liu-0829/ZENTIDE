package com.zentide.mapper;

import com.zentide.entity.po.ZentideRadar;
import com.zentide.entity.po.ZentideRadarTopic;
import com.zentide.entity.po.ZentideTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideRadarMapper {
    List<ZentideRadar> listByOwner(String ownerId);

    ZentideRadar findOwned(@Param("ownerId") String ownerId, @Param("radarId") Long radarId);

    ZentideTopic findActiveTopic(Long topicId);

    int insertRadar(ZentideRadar radar);

    int updateRadar(ZentideRadar radar);

    int deleteTopics(Long radarId);

    int touchRadar(@Param("ownerId") String ownerId, @Param("radarId") Long radarId);

    int insertTopic(ZentideTopic topic);

    int attachTopic(ZentideRadarTopic topic);

    int ensureTopicFollow(@Param("userId") String userId, @Param("topicId") Long topicId);

    List<ZentideRadarTopic> listTopics(Long radarId);
}
