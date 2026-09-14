package com.zentide.mapper;

import com.zentide.entity.po.ZentideObservationActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideObservationMapper {
    List<ZentideObservationActivity> list(@Param("signalId") Long signalId,
                                          @Param("changeId") Long changeId,
                                          @Param("limit") int limit);
}
