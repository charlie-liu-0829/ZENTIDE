package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideChangeReviewMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ZentideChangeReviewServiceTest {
    @Test
    void publishesOnlyCandidatesWithCompleteEvidenceCoverage() {
        ZentideChangeReviewMapper mapper = mock(ZentideChangeReviewMapper.class);
        ZentideChange change = new ZentideChange();
        change.setStatus("CANDIDATE");
        change.setVerificationStatus("UNVERIFIED");
        when(mapper.findChange(7L)).thenReturn(change);
        when(mapper.countFactClaims(7L)).thenReturn(2);
        when(mapper.countCoveredFactClaims(7L)).thenReturn(2);
        when(mapper.publishVerified(7L)).thenReturn(1);

        new ZentideChangeReviewService(mapper).verifyAndPublish(7L, "admin");

        verify(mapper).verifyClaims(7L);
        verify(mapper).insertVerificationAudit("admin", 7L);
        verify(mapper).promoteLinkedSignals(7L);
        verify(mapper).insertPromotionActivities(7L);
    }

    @Test
    void refusesPublicationWhenAnyFactClaimLacksEvidence() {
        ZentideChangeReviewMapper mapper = mock(ZentideChangeReviewMapper.class);
        ZentideChange change = new ZentideChange();
        change.setStatus("CANDIDATE");
        change.setVerificationStatus("UNVERIFIED");
        when(mapper.findChange(7L)).thenReturn(change);
        when(mapper.countFactClaims(7L)).thenReturn(2);
        when(mapper.countCoveredFactClaims(7L)).thenReturn(1);

        assertThrows(BusinessException.class, () -> new ZentideChangeReviewService(mapper).verifyAndPublish(7L, "admin"));
        verify(mapper, never()).publishVerified(any());
    }
}
