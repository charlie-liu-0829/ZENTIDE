package com.zentide.service;

import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideChangeWatchMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZentideChangeWatchServiceTest {
    @Test
    void startsFollowingUpOnAPublishedChange() {
        ZentideChangeWatchMapper mapper = mock(ZentideChangeWatchMapper.class);
        when(mapper.countPublishedChange(8L)).thenReturn(1);

        new ZentideChangeWatchService(mapper).watch("u1", 8L, "IMMEDIATE");

        verify(mapper).watch("u1", 8L, "IMMEDIATE");
    }

    @Test
    void doesNotWatchAChangeThatIsStillForming() {
        ZentideChangeWatchMapper mapper = mock(ZentideChangeWatchMapper.class);
        when(mapper.countPublishedChange(8L)).thenReturn(0);

        assertThrows(BusinessException.class, () -> new ZentideChangeWatchService(mapper).watch("u1", 8L, null));

        verify(mapper, never()).watch("u1", 8L, "FOLLOW_UP");
    }
}
