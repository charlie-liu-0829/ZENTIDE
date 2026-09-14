package com.zentide.mapper;

import com.zentide.entity.po.ZentideChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideChangeWatchMapper {
    int countPublishedChange(Long changeId);

    int watch(@Param("userId") String userId, @Param("changeId") Long changeId, @Param("watchMode") String watchMode);

    int stopWatching(@Param("userId") String userId, @Param("changeId") Long changeId);

    List<ZentideChange> listActive(@Param("userId") String userId, @Param("limit") int limit);
}
