package com.zentide.mapper;

import com.zentide.entity.po.ZentideDirectMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideDirectMessageMapper {
    int countUser(String userId);

    int insertMessage(ZentideDirectMessage message);

    List<ZentideDirectMessage> listConversations(@Param("userId") String userId, @Param("limit") int limit);

    List<ZentideDirectMessage> listMessages(@Param("userId") String userId, @Param("peerId") String peerId, @Param("limit") int limit);

    int markConversationRead(@Param("userId") String userId, @Param("peerId") String peerId);

    int countUnread(String userId);

    List<ZentideDirectMessage> searchUsers(@Param("userId") String userId, @Param("query") String query, @Param("limit") int limit);
}
