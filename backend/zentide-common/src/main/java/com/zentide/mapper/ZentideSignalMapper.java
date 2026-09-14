package com.zentide.mapper;

import com.zentide.entity.po.ZentideSignal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideSignalMapper {
    List<ZentideSignal> listActive(@Param("limit") int limit);
}
