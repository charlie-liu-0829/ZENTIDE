package com.zentide.service;

import com.zentide.entity.po.ZentideChangeDiscussion;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideDiscussionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ZentideDiscussionServiceTest {
    @Test
    void publishesDiscussionOnlyForAVerifiedPublicChange() {
        ZentideDiscussionMapper mapper = mock(ZentideDiscussionMapper.class);
        when(mapper.countPublishedChange(5L)).thenReturn(1);
        doAnswer(call -> { call.<ZentideChangeDiscussion>getArgument(0).setDiscussionId(9L); return 1; }).when(mapper).insert(any());

        ZentideChangeDiscussion result = new ZentideDiscussionService(mapper).publish("u1", 5L, null, null, "我在生产环境也观察到了这个变化。");

        assertEquals(9L, result.getDiscussionId());
        assertEquals("社区成员", result.getAuthorLabel());
        verify(mapper).insert(any(ZentideChangeDiscussion.class));
    }

    @Test
    void rejectsEvidenceFromAnotherChange() {
        ZentideDiscussionMapper mapper = mock(ZentideDiscussionMapper.class);
        when(mapper.countPublishedChange(5L)).thenReturn(1);
        when(mapper.countEvidenceForPublishedChange(5L, 12L)).thenReturn(0);

        assertThrows(BusinessException.class, () -> new ZentideDiscussionService(mapper).publish("u1", 5L, null, 12L, "这条证据并不属于当前变化。"));
        verify(mapper, never()).insert(any());
    }

    @Test
    void doesNotExposeDiscussionForCandidateChange() {
        ZentideDiscussionMapper mapper = mock(ZentideDiscussionMapper.class);
        when(mapper.countPublishedChange(5L)).thenReturn(0);

        assertThrows(BusinessException.class, () -> new ZentideDiscussionService(mapper).list(5L, null));
        verify(mapper, never()).listPublished(anyLong(), anyInt());
    }
}
