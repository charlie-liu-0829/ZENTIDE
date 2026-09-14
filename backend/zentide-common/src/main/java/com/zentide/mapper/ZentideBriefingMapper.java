package com.zentide.mapper;

import com.zentide.entity.po.ZentideBriefing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface ZentideBriefingMapper {
    ZentideBriefing findDaily(@Param("ownerId") String ownerId, @Param("briefingDate") LocalDate briefingDate);

    int upsertDaily(ZentideBriefing briefing);
}
