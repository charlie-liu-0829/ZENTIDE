package com.zentide.mapper;

import com.zentide.entity.vo.ZentideStanceSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZentideChangeStanceMapper {
    int countPublishedChange(Long changeId);

    int upsert(@Param("changeId") Long changeId, @Param("userId") String userId, @Param("stanceType") String stanceType);

    ZentideStanceSummary summarize(Long changeId);

    String findMine(@Param("changeId") Long changeId, @Param("userId") String userId);
}
