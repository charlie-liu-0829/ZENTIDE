package com.zentide.mapper;

import com.zentide.entity.po.ZentideDocument;
import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.entity.po.ZentideFetchAttempt;
import com.zentide.entity.po.ZentideRawSnapshot;
import com.zentide.entity.po.ZentideSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideSourceMapper {
    List<ZentideSource> listSources();

    ZentideSource findSource(Long sourceId);

    ZentideSource findSourceByUrlHash(String urlHash);

    int insertSource(ZentideSource source);

    int insertAttempt(ZentideFetchAttempt attempt);

    int finishAttempt(ZentideFetchAttempt attempt);

    int insertSnapshot(ZentideRawSnapshot snapshot);

    ZentideDocument findDocument(@Param("sourceId") Long sourceId, @Param("canonicalKey") String canonicalKey);

    int insertDocument(ZentideDocument document);

    ZentideDocumentVersion findVersionByHash(@Param("documentId") Long documentId, @Param("contentHash") String contentHash);

    int insertDocumentVersion(ZentideDocumentVersion version);

    int updateCurrentVersion(@Param("documentId") Long documentId, @Param("versionNo") Integer versionNo);
}
