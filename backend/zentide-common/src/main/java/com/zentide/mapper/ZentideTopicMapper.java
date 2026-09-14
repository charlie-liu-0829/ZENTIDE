package com.zentide.mapper;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideTopic;
import com.zentide.entity.po.ZentideTopicSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideTopicMapper {
    List<ZentideTopic> discover(@Param("query") String query, @Param("limit") int limit);

    List<ZentideTopic> listFollowing(String userId);

    ZentideTopic findTopic(Long topicId);

    ZentideTopic findByName(String canonicalName);

    int insertTopic(ZentideTopic topic);

    int follow(@Param("topicId") Long topicId, @Param("userId") String userId, @Param("notificationMode") String notificationMode);

    int unfollow(@Param("topicId") Long topicId, @Param("userId") String userId);

    List<ZentideTopicSource> listSources(Long topicId);

    int attachChangeForSource(@Param("sourceId") Long sourceId, @Param("changeId") Long changeId);

    List<ZentideChange> listFeed(@Param("topicId") Long topicId, @Param("limit") int limit);
}
