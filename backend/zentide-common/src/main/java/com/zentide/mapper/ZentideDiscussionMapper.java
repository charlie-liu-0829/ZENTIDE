package com.zentide.mapper;

import com.zentide.entity.po.ZentideChangeDiscussion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideDiscussionMapper {
    int countPublishedChange(Long changeId);

    List<ZentideChangeDiscussion> listPublished(@Param("changeId") Long changeId, @Param("limit") int limit);

    ZentideChangeDiscussion findPublishedInChange(@Param("changeId") Long changeId, @Param("discussionId") Long discussionId);

    int countEvidenceForPublishedChange(@Param("changeId") Long changeId, @Param("evidenceId") Long evidenceId);

    int insert(ZentideChangeDiscussion discussion);

    int removeOwned(@Param("authorId") String authorId, @Param("discussionId") Long discussionId);
}
