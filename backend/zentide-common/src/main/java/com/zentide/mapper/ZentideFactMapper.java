package com.zentide.mapper;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideClaim;
import com.zentide.entity.po.ZentideEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideFactMapper {
    List<ZentideChange> listPublishedChanges(@Param("limit") int limit);

    ZentideChange findPublishedChange(Long changeId);

    List<ZentideClaim> listClaims(Long changeId);

    List<ZentideEvidence> listEvidence(Long changeId);
}
