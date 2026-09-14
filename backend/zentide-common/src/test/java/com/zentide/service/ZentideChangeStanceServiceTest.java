package com.zentide.service;

import com.zentide.entity.vo.ZentideStanceSummary;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideChangeStanceMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZentideChangeStanceServiceTest {
    @Test
    void recordsOneCurrentStancePerUserAndReturnsTheRealSummary() {
        ZentideChangeStanceMapper mapper = mock(ZentideChangeStanceMapper.class);
        ZentideStanceSummary summary = new ZentideStanceSummary();
        summary.setTotalCount(2L);
        summary.setActionedCount(1L);
        summary.setWatchingCount(1L);
        when(mapper.countPublishedChange(7L)).thenReturn(1);
        when(mapper.summarize(7L)).thenReturn(summary);

        ZentideStanceSummary result = new ZentideChangeStanceService(mapper).choose("user-1", 7L, "actioned");

        assertEquals(2L, result.getTotalCount());
        verify(mapper).upsert(7L, "user-1", "ACTIONED");
    }

    @Test
    void rejectsUnsupportedStancesWithoutWritingAnything() {
        ZentideChangeStanceMapper mapper = mock(ZentideChangeStanceMapper.class);
        when(mapper.countPublishedChange(7L)).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> new ZentideChangeStanceService(mapper).choose("user-1", 7L, "POPULAR"));

        verify(mapper, never()).upsert(7L, "user-1", "POPULAR");
    }

    @Test
    void rejectsStancesForUnpublishedChanges() {
        ZentideChangeStanceMapper mapper = mock(ZentideChangeStanceMapper.class);
        when(mapper.countPublishedChange(7L)).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> new ZentideChangeStanceService(mapper).choose("user-1", 7L, "WATCHING"));

        verify(mapper, never()).upsert(7L, "user-1", "WATCHING");
    }
}
