package com.zentide.mapper;

import com.zentide.entity.po.UserInfo;
import com.zentide.entity.query.UserInfoQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAccountMapper {

    List<UserInfo> list(@Param("query") UserInfoQuery query);

    int count(@Param("query") UserInfoQuery query);

    UserInfo findPublicById(@Param("userId") String userId);

    UserInfo findCredentialsById(@Param("userId") String userId);

    UserInfo findCredentialsByEmail(@Param("email") String email);

    String findSecurityQuestionByEmail(@Param("email") String email);

    int updateSecurityQuestion(@Param("userId") String userId, @Param("question") String question, @Param("answerHash") String answerHash);

    int resetPasswordByEmail(@Param("email") String email, @Param("password") String password);

    String findSearchHistory(@Param("userId") String userId);

    int updateSearchHistory(@Param("userId") String userId, @Param("historyJson") String historyJson);

    int countByEmail(@Param("email") String email);

    int countByNickname(@Param("nickName") String nickName);

    int insert(UserInfo user);

    int updateById(@Param("changes") UserInfo changes, @Param("userId") String userId);
}
