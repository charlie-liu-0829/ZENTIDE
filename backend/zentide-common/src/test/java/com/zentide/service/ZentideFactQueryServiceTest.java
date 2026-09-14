package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideClaim;
import com.zentide.entity.po.ZentideEvidence;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideFactMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ZentideFactQueryServiceTest {
    @Test
    void returnsAChangeWithClaimsAndTraceableEvidence() {
        ZentideFactMapper mapper = mock(ZentideFactMapper.class);
        ZentideChange change = new ZentideChange();
        change.setChangeId(7L);
        when(mapper.findPublishedChange(7L)).thenReturn(change);
        when(mapper.listClaims(7L)).thenReturn(List.of(new ZentideClaim()));
        when(mapper.listEvidence(7L)).thenReturn(List.of(new ZentideEvidence()));

        ZentideChange result = new ZentideFactQueryService(mapper).getChange(7L);

        assertEquals(1, result.getClaims().size());
        assertEquals(1, result.getEvidence().size());
    }

    @Test
    void boundsListSizeForPublicVerifiedChanges() {
        ZentideFactMapper mapper = mock(ZentideFactMapper.class);
        new ZentideFactQueryService(mapper).listChanges(500);

        verify(mapper).listPublishedChanges(100);
    }

    @Test
    void rejectsEvidenceLookupForUnknownChange() {
        ZentideFactMapper mapper = mock(ZentideFactMapper.class);
        when(mapper.findPublishedChange(9L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> new ZentideFactQueryService(mapper).listEvidence(9L));
    }
}
