package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideClaim;
import com.zentide.entity.po.ZentideDocument;
import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.entity.po.ZentideEvidence;
import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.po.ZentideSignal;
import com.zentide.mapper.ZentideChangeCandidateMapper;
import com.zentide.mapper.ZentideTopicMapper;
import com.zentide.source.NormalizedDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ZentideChangeCandidateServiceTest {
    @Test
    void createsAnUnverifiedCandidateWithClaimAndEvidence() {
        ZentideChangeCandidateMapper mapper = mock(ZentideChangeCandidateMapper.class);
        doAnswer(invocation -> { invocation.<ZentideSignal>getArgument(0).setSignalId(30L); return 1; }).when(mapper).insertSignal(any());
        doAnswer(invocation -> { invocation.<ZentideChange>getArgument(0).setChangeId(31L); return 1; }).when(mapper).insertChange(any());
        doAnswer(invocation -> { invocation.<ZentideClaim>getArgument(0).setClaimId(32L); return 1; }).when(mapper).insertClaim(any());
        ZentideSource source = new ZentideSource();
        source.setSourceId(3L);
        source.setSourceName("Demo release feed");
        source.setCanonicalUrl("https://api.github.com/repos/acme/demo/releases");
        source.setTrustTier("PRIMARY");
        ZentideDocument document = new ZentideDocument();
        document.setCanonicalKey("github-release:v1.2.0");
        ZentideDocumentVersion version = new ZentideDocumentVersion();
        version.setDocumentVersionId(11L);
        version.setVersionNo(1);
        NormalizedDocument normalized = new NormalizedDocument("github-release:v1.2.0", "Release 1.2", "Notes", "hash",
                LocalDateTime.of(2026, 8, 26, 9, 0), "https://github.com/acme/demo/releases/v1.2.0", "Fixed a security issue");

        ZentideTopicMapper topicMapper = mock(ZentideTopicMapper.class);
        Long changeId = new ZentideChangeCandidateService(mapper, topicMapper).create(source, document, version, normalized);

        assertEquals(31L, changeId);
        verify(mapper).insertSignal(org.mockito.ArgumentMatchers.argThat(signal -> "OBSERVING".equals(signal.getStatus()) && signal.getDocumentVersionId().equals(11L)));
        verify(mapper).linkSignalToChange(30L, 31L);
        verify(mapper).insertChange(org.mockito.ArgumentMatchers.argThat(change -> "CANDIDATE".equals(change.getStatus()) && "UNVERIFIED".equals(change.getVerificationStatus())));
        verify(mapper).insertEvidence(org.mockito.ArgumentMatchers.argThat(evidence -> evidence.getClaimId().equals(32L) && evidence.getDocumentVersionId().equals(11L)));
    }
}
