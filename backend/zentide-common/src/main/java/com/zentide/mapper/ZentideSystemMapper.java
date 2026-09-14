package com.zentide.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-backed infrastructure queries shared by each Spring Boot service.
 */
@Mapper
public interface ZentideSystemMapper {

    int ping();
}
