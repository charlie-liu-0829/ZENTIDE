package com.zentide.service;

import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideInterestMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZentideAdminCommunityServiceTest {
    @Test
    void commentModerationRefreshesTheParentPostCounter() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.adminFindCommentPostId(12L)).thenReturn(7L);
        when(mapper.adminUpdateCommentStatus(12L, "HIDDEN")).thenReturn(1);

        new ZentideAdminCommunityService(mapper).updateCommentStatus(12L, "hidden");

        verify(mapper).refreshCommentCount(7L);
    }

    @Test
    void moderationRejectsUnknownStatuses() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideAdminCommunityService service = new ZentideAdminCommunityService(mapper);

        assertThrows(BusinessException.class, () -> service.updatePostStatus(1L, "DELETED"));
        assertThrows(BusinessException.class, () -> service.updateTopicStatus(1L, "DELETED"));
    }

    @Test
    void overviewCombinesTheCommunityWorkQueue() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.countAdminHubs("ACTIVE")).thenReturn(8);
        when(mapper.countAdminHubs("HIDDEN")).thenReturn(1);
        when(mapper.countAdminPosts("PUBLISHED")).thenReturn(40);
        when(mapper.countAdminPosts("HIDDEN")).thenReturn(3);
        when(mapper.countAdminComments("PUBLISHED")).thenReturn(75);
        when(mapper.countAdminComments("HIDDEN")).thenReturn(2);
        when(mapper.countAdminPendingMembers()).thenReturn(4);

        Map<String, Integer> result = new ZentideAdminCommunityService(mapper).overview();

        assertEquals(8, result.get("hubs"));
        assertEquals(3, result.get("pendingPosts"));
        assertEquals(2, result.get("hiddenComments"));
        assertEquals(4, result.get("pendingMembers"));
    }
}
