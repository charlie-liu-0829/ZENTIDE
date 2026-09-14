package com.zentide.mapper;

import com.zentide.entity.po.ZentideRetrievalChunk;
import com.zentide.entity.vo.ZentideResearchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideRetrievalMapper {
    int deleteByDocumentVersionId(Long documentVersionId);

    int insertChunk(ZentideRetrievalChunk chunk);

    List<com.zentide.entity.po.ZentideDocumentVersion> listUnindexedDocumentVersions(@Param("limit") int limit);

    List<ZentideResearchResult> searchVerified(@Param("query") String query, @Param("limit") int limit);
}
