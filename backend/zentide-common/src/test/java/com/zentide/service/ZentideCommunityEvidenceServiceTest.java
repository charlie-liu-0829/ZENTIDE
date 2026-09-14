package com.zentide.service;

import com.zentide.entity.po.ZentideEvidence;
import com.zentide.entity.po.ZentideEvidenceSubmission;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideCommunityEvidenceMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ZentideCommunityEvidenceServiceTest {
    @Test
    void submitsCommunityEvidenceAsCandidateInsteadOfPublicEvidence() {
        ZentideCommunityEvidenceMapper mapper = mock(ZentideCommunityEvidenceMapper.class);
        when(mapper.countPublishedChange(7L)).thenReturn(1);

        new ZentideCommunityEvidenceService(mapper).submit("u1", 7L, "https://example.com/notice", "这是可以被管理员后续核对的原始摘录内容。", "官方公告线索");

        verify(mapper).insertSubmission(any(ZentideEvidenceSubmission.class));
        verify(mapper, never()).insertEvidence(any());
    }

    @Test
    void approvalRequiresExcerptToExistInCollectedDocumentVersion() {
        ZentideCommunityEvidenceMapper mapper = mock(ZentideCommunityEvidenceMapper.class);
        ZentideEvidenceSubmission submission = new ZentideEvidenceSubmission();
        submission.setSubmissionId(3L);
        submission.setChangeId(7L);
        submission.setExcerpt("这是可以被管理员后续核对的原始摘录内容。");
        ZentideEvidence material = new ZentideEvidence();
        material.setSourceId(5L);
        material.setSourceName("Official Source");
        material.setSourceUrl("https://example.com/feed");
        material.setTrustTier("PRIMARY");
        when(mapper.findCandidate(3L)).thenReturn(submission);
        when(mapper.findActiveSourceMaterial(12L)).thenReturn(material);
        when(mapper.countExcerptInDocumentVersion(12L, submission.getExcerpt())).thenReturn(0);

        assertThrows(BusinessException.class, () -> new ZentideCommunityEvidenceService(mapper).approve("admin", 3L, 12L, "未通过"));
        verify(mapper, never()).insertEvidence(any());
    }

    @Test
    void rejectsCandidateWithAnAuditableReason() {
        ZentideCommunityEvidenceMapper mapper = mock(ZentideCommunityEvidenceMapper.class);
        ZentideEvidenceSubmission submission = new ZentideEvidenceSubmission();
        submission.setSubmissionId(3L);
        submission.setChangeId(7L);
        when(mapper.findCandidate(3L)).thenReturn(submission);
        when(mapper.reject(3L, "admin", "无法在已采集版本中核对摘录。")).thenReturn(1);

        new ZentideCommunityEvidenceService(mapper).reject("admin", 3L, "无法在已采集版本中核对摘录。 ");

        verify(mapper).insertAudit("admin", "REJECT_COMMUNITY_EVIDENCE", 3L, 7L, null);
    }
}
