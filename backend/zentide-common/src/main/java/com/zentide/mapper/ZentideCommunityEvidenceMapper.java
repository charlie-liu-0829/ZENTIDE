package com.zentide.mapper;

import com.zentide.entity.po.ZentideEvidence;
import com.zentide.entity.po.ZentideEvidenceSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZentideCommunityEvidenceMapper {
    int countPublishedChange(Long changeId);

    int insertSubmission(ZentideEvidenceSubmission submission);

    List<ZentideEvidenceSubmission> listCandidates(@Param("limit") int limit);

    ZentideEvidenceSubmission findCandidate(Long submissionId);

    ZentideEvidence findActiveSourceMaterial(Long documentVersionId);

    int countExcerptInDocumentVersion(@Param("documentVersionId") Long documentVersionId, @Param("excerpt") String excerpt);

    int insertEvidence(ZentideEvidence evidence);

    int approve(@Param("submissionId") Long submissionId, @Param("actorId") String actorId, @Param("reviewNote") String reviewNote, @Param("evidenceId") Long evidenceId);

    int reject(@Param("submissionId") Long submissionId, @Param("actorId") String actorId, @Param("reviewNote") String reviewNote);

    int insertAudit(@Param("actorId") String actorId, @Param("action") String action, @Param("submissionId") Long submissionId, @Param("changeId") Long changeId, @Param("documentVersionId") Long documentVersionId);
}
