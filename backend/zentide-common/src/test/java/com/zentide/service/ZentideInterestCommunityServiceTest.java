package com.zentide.service;

import com.zentide.entity.po.ZentideHubInvitation;
import com.zentide.entity.po.ZentideHubMemberRequest;
import com.zentide.entity.po.ZentideInterestHub;
import com.zentide.entity.po.ZentideInterestEvent;
import com.zentide.entity.po.ZentideInterestPost;
import com.zentide.entity.po.ZentidePostType;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideInterestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ZentideInterestCommunityServiceTest {
    @Test
    void agentContextRequiresMembershipAndMatchingPostScene() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestHub hub = hub(5L, "owner", "OPEN");
        hub.setVisibility("PUBLIC");
        when(mapper.findHub(5L)).thenReturn(hub);
        when(mapper.countActiveHubMember(5L, "u1")).thenReturn(1);
        when(mapper.findPost(12L, "u1")).thenReturn(post(12L, 5L, "u1"));
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertEquals(java.util.Set.of("PUBLIC"), service.agentVisibilities("u1", 5L, 12L));
        assertThrows(BusinessException.class, () -> service.agentVisibilities("outsider", 5L, null));
        assertThrows(BusinessException.class, () -> service.agentVisibilities("u1", 5L, 99L));
    }

    @Test
    void authorCanEditPostAndTopicCountsStaySynchronized() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestPost existing = post(12L, 5L, "u1");
        when(mapper.findPost(12L, "u1")).thenReturn(existing);
        when(mapper.findHubTopic(5L, 8L)).thenReturn(5L);
        when(mapper.countActivePostTypeForHub(5L, "EXPERIENCE")).thenReturn(1);
        when(mapper.listPostTopicIds(12L)).thenReturn(List.of(7L), List.of(8L));
        when(mapper.listPostTopicNames(12L)).thenReturn(List.of("新话题"));
        when(mapper.updateOwnedPost(any())).thenReturn(1);

        ZentideInterestPost result = new ZentideInterestCommunityService(mapper).updatePost("u1", 12L, "EXPERIENCE", "更新后的标题", "<p>更新后的<strong>正文</strong><script>alert(1)</script></p>", null, 8L, "[]", null);

        ArgumentCaptor<ZentideInterestPost> update = ArgumentCaptor.forClass(ZentideInterestPost.class);
        verify(mapper).updateOwnedPost(update.capture());
        assertTrue(update.getValue().getBody().contains("<strong>正文</strong>"));
        assertFalse(update.getValue().getBody().contains("script"));
        verify(mapper).detachPostTopics(12L);
        verify(mapper).attachPostTopic(12L, 8L);
        verify(mapper).refreshTopicPostCount(5L, 7L);
        verify(mapper).refreshTopicPostCount(5L, 8L);
        assertTrue(result.getTopicNames().contains("新话题"));
    }

    @Test
    void authorCanDeletePostAndAggregateCountsAreRefreshed() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findPost(12L, "u1")).thenReturn(post(12L, 5L, "u1"));
        when(mapper.listPostTopicIds(12L)).thenReturn(List.of(7L, 8L));
        when(mapper.deleteOwnedPost(12L, "u1")).thenReturn(1);

        assertTrue(new ZentideInterestCommunityService(mapper).deletePost("u1", 12L));
        verify(mapper).refreshPostCount(5L);
        verify(mapper).refreshTopicPostCount(5L, 7L);
        verify(mapper).refreshTopicPostCount(5L, 8L);
    }

    @Test
    void openingPostReturnsTheIncrementedViewCount() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestPost post = post(12L, 5L, "u1");
        post.setViewCount(4);
        when(mapper.findPost(12L, "viewer")).thenReturn(post);
        when(mapper.listPostTopicIds(12L)).thenReturn(List.of());
        when(mapper.listPostTopicNames(12L)).thenReturn(List.of());
        when(mapper.incrementPostView(12L)).thenReturn(1);

        ZentideInterestPost result = new ZentideInterestCommunityService(mapper).post(12L, "viewer");

        assertEquals(5, result.getViewCount());
        verify(mapper, times(1)).incrementPostView(12L);
    }

    @Test
    void userCannotModifyOrDeleteSomeoneElsesPost() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findPost(12L, "u2")).thenReturn(post(12L, 5L, "u1"));
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertThrows(BusinessException.class, () -> service.updatePost("u2", 12L, "DISCUSSION", null, "不能修改别人的帖子", null, null, "[]", null));
        assertThrows(BusinessException.class, () -> service.deletePost("u2", 12L));
        verify(mapper, never()).updateOwnedPost(any());
        verify(mapper, never()).deleteOwnedPost(anyLong(), anyString());
    }

    @Test
    void creatingHubPersistsCoverJoinPolicyAndOwnerMembership() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        doAnswer(invocation -> { invocation.<ZentideInterestHub>getArgument(0).setHubId(31L); return 1; }).when(mapper).insertHub(any());
        com.zentide.entity.po.ZentideHubDirection music = new com.zentide.entity.po.ZentideHubDirection();
        music.setCode("MUSIC"); music.setDisplayName("音乐"); music.setStatus("ACTIVE");
        when(mapper.findActiveHubDirection("MUSIC")).thenReturn(music);

        ZentideInterestHub hub = new ZentideInterestCommunityService(mapper).createHub("u1", "独立音乐现场", "交换演出信息", "music", "260828/cover_abcd1234.jpg", "approval");

        assertEquals(31L, hub.getHubId());
        assertEquals("MUSIC", hub.getCategory());
        assertEquals("APPROVAL", hub.getJoinPolicy());
        assertEquals("260828/cover_abcd1234.jpg", hub.getCoverUrl());
        assertTrue(hub.getOwned());
        verify(mapper).joinHubAsOwner(31L, "u1");
        verify(mapper).refreshMemberCount(31L);
    }

    @Test
    void approvalHubCreatesPendingMembershipWhileOpenHubJoinsImmediately() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestHub approvalHub = hub(7L, "owner", "APPROVAL");
        ZentideInterestHub openHub = hub(8L, "owner", "OPEN");
        when(mapper.findHub(7L)).thenReturn(approvalHub);
        when(mapper.findHub(8L)).thenReturn(openHub);
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertEquals("PENDING", service.toggleHubMembership("member", 7L, true));
        assertEquals("ACTIVE", service.toggleHubMembership("member", 8L, true));
        verify(mapper).requestHubMembership(7L, "member", "PENDING");
        verify(mapper).requestHubMembership(8L, "member", "ACTIVE");
    }

    @Test
    void ownerCannotLeaveAndNonOwnerCannotDeleteHub() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findHub(7L)).thenReturn(hub(7L, "owner", "OPEN"));
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertThrows(BusinessException.class, () -> service.toggleHubMembership("owner", 7L, false));
        assertThrows(BusinessException.class, () -> service.deleteHub("member", 7L));
        verify(mapper, never()).leaveHub(anyLong(), anyString());
        verify(mapper, never()).deleteOwnedHub(anyLong(), anyString());
    }

    @Test
    void invitationAlwaysCreatesActiveMembershipWithoutApproval() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideHubInvitation invitation = new ZentideHubInvitation();
        invitation.setInvitationId(9L);
        invitation.setHubId(7L);
        invitation.setInviteToken("12345678901234567890123456789012");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        ZentideInterestHub joined = hub(7L, "owner", "APPROVAL");
        joined.setJoined(true);
        when(mapper.findValidInvitation(invitation.getInviteToken())).thenReturn(invitation);
        when(mapper.consumeInvitation(9L)).thenReturn(1);
        when(mapper.discoverHubs("member", null, null, 100)).thenReturn(List.of(joined));

        ZentideInterestHub result = new ZentideInterestCommunityService(mapper).redeemHubInvitation("member", invitation.getInviteToken());

        assertEquals(7L, result.getHubId());
        verify(mapper).requestHubMembership(7L, "member", "ACTIVE");
        verify(mapper).refreshMemberCount(7L);
    }

    @Test
    void onlyActiveMembersCanCreateTopicsOrAssignTheirOwnCategories() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findHub(7L)).thenReturn(hub(7L, "owner", "OPEN"));
        when(mapper.countActiveHubMember(7L, "member")).thenReturn(0);
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertThrows(BusinessException.class, () -> service.createHubTopic("member", 7L, "自定义话题"));
        assertThrows(BusinessException.class, () -> service.assignHubCategory("member", 7L, 3L));
        verify(mapper, never()).insertCommunityTopic(any());
        verify(mapper, never()).assignHubCategory(anyLong(), anyString(), anyLong());
    }

    @Test
    void categoryAssignmentRejectsAnotherUsersCategory() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findHub(7L)).thenReturn(hub(7L, "owner", "OPEN"));
        when(mapper.countActiveHubMember(7L, "member")).thenReturn(1);
        when(mapper.findUserHubCategory(3L, "member")).thenReturn(null);

        assertThrows(BusinessException.class, () -> new ZentideInterestCommunityService(mapper).assignHubCategory("member", 7L, 3L));
        verify(mapper, never()).assignHubCategory(anyLong(), anyString(), anyLong());
    }

    @Test
    void creatorCanGenerateExpiringInvitation() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.countOwnedHub(7L, "owner")).thenReturn(1);
        doAnswer(invocation -> { invocation.<ZentideHubInvitation>getArgument(0).setInvitationId(12L); return 1; }).when(mapper).insertHubInvitation(any());

        ZentideHubInvitation invitation = new ZentideInterestCommunityService(mapper).createHubInvitation("owner", 7L);

        assertEquals(12L, invitation.getInvitationId());
        assertEquals(32, invitation.getInviteToken().length());
        assertNotNull(invitation.getExpiresAt());
        assertTrue(invitation.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
    }

    @Test
    void ownerCanPromoteMemberWithSelectedPermissions() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideHubMemberRequest member = new ZentideHubMemberRequest();
        member.setUserId("member");
        member.setRole("MEMBER");
        member.setMembershipStatus("ACTIVE");
        when(mapper.countOwnedHub(7L, "owner")).thenReturn(1);
        when(mapper.findHubMember(7L, "member")).thenReturn(member);
        when(mapper.updateHubMemberRole(eq(7L), eq("member"), eq("ADMIN"), anyString())).thenReturn(1);

        ZentideHubMemberRequest updated = new ZentideInterestCommunityService(mapper)
            .updateHubMemberAdmin("owner", 7L, "member", "ADMIN", "[\"REVIEW_MEMBERS\",\"INVITE_MEMBERS\",\"UNKNOWN\"]");

        assertEquals("ADMIN", updated.getRole());
        assertTrue(updated.getPermissionsJson().contains("REVIEW_MEMBERS"));
        assertTrue(updated.getPermissionsJson().contains("INVITE_MEMBERS"));
        assertFalse(updated.getPermissionsJson().contains("UNKNOWN"));
    }

    @Test
    void updatingEventAcceptsMissingStatusAndKeepsUpcomingDefault() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findEventHub(4L)).thenReturn(7L);
        when(mapper.countOwnedHub(7L, "owner")).thenReturn(1);
        when(mapper.updateOwnedInterestEvent(any(), eq("owner"))).thenReturn(1);

        ZentideInterestEvent updated = new ZentideInterestCommunityService(mapper).updateEvent(
            "owner", 4L, "秋日音乐会", "现场交流", "2026-09-10T19:30", "2026-09-10T21:30", "上海", null, null
        );

        assertEquals("UPCOMING", updated.getStatus());
        assertEquals(19, updated.getStartsAt().getHour());
        assertEquals(30, updated.getStartsAt().getMinute());
    }

    @Test
    void delegatedAdminCanUseOnlyAssignedCapability() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideHubMemberRequest admin = new ZentideHubMemberRequest();
        admin.setUserId("admin");
        admin.setRole("ADMIN");
        admin.setMembershipStatus("ACTIVE");
        admin.setPermissionsJson("[\"INVITE_MEMBERS\"]");
        when(mapper.findHubMember(7L, "admin")).thenReturn(admin);
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertNotNull(service.createHubInvitation("admin", 7L));
        assertThrows(BusinessException.class, () -> service.pendingHubMembers("admin", 7L));
    }

    @Test
    void ownerCanCreateAndDeleteHubSpecificPostType() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.countOwnedHub(7L, "owner")).thenReturn(1);
        when(mapper.listHubCustomPostTypes(7L)).thenReturn(List.of());
        doAnswer(invocation -> { invocation.<ZentidePostType>getArgument(0).setPostTypeId(91L); return 1; }).when(mapper).insertPostType(any());
        when(mapper.deleteHubPostType(7L, 91L)).thenReturn(1);
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        ZentidePostType type = service.createHubPostType("owner", 7L, "票务情报", "开票、余票和转票提醒");

        assertEquals(91L, type.getPostTypeId());
        assertEquals(7L, type.getHubId());
        assertFalse(type.getSystemFixed());
        assertTrue(type.getTypeCode().startsWith("HUB_"));
        assertTrue(service.deleteHubPostType("owner", 7L, 91L));
    }

    @Test
    void nonOwnerCannotManageHubSpecificPostTypes() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        assertThrows(BusinessException.class, () -> service.createHubPostType("member", 7L, "攻略", null));
        assertThrows(BusinessException.class, () -> service.deleteHubPostType("member", 7L, 91L));
        verify(mapper, never()).insertPostType(any());
        verify(mapper, never()).deleteHubPostType(anyLong(), anyLong());
    }

    @Test
    void publishingRejectsPostTypeFromAnotherHub() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findHub(7L)).thenReturn(hub(7L, "owner", "OPEN"));
        when(mapper.countActiveHubMember(7L, "member")).thenReturn(1);
        when(mapper.countActivePostTypeForHub(7L, "HUB_OTHER")).thenReturn(0);

        assertThrows(BusinessException.class, () -> new ZentideInterestCommunityService(mapper).publish("member", 7L, "HUB_OTHER", "标题", "这是帖子正文", null, null, null, null, "[]", null));
        verify(mapper, never()).insertPost(any());
    }

    @Test
    void publishingRejectsBlankTitleAtServiceBoundary() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.findHub(7L)).thenReturn(hub(7L, "owner", "OPEN"));
        when(mapper.countActiveHubMember(7L, "member")).thenReturn(1);
        when(mapper.countActivePostTypeForHub(7L, "DISCUSSION")).thenReturn(1);

        assertThrows(BusinessException.class, () -> new ZentideInterestCommunityService(mapper)
                .publish("member", 7L, "DISCUSSION", "   ", "这是帖子正文", null, null, null, null, "[]", null));
        verify(mapper, never()).insertPost(any());
    }

    @Test
    void adminCanCreateUpdateAndDeleteFixedPostType() {
        ZentideInterestMapper mapper = mock(ZentideInterestMapper.class);
        when(mapper.listSystemPostTypes()).thenReturn(List.of());
        doAnswer(invocation -> { invocation.<ZentidePostType>getArgument(0).setPostTypeId(51L); return 1; }).when(mapper).insertPostType(any());
        when(mapper.updateSystemPostType(any())).thenReturn(1);
        when(mapper.deleteSystemPostType(51L)).thenReturn(1);
        ZentideInterestCommunityService service = new ZentideInterestCommunityService(mapper);

        ZentidePostType created = service.createSystemPostType("admin", "攻略", "适合教程和方法总结");
        ZentidePostType updated = service.updateSystemPostType(51L, "实用攻略", "整理可执行的方法", false, 25);

        assertTrue(created.getSystemFixed());
        assertTrue(created.getTypeCode().startsWith("SYS_"));
        assertEquals("DISABLED", updated.getStatus());
        assertEquals(25, updated.getSortOrder());
        assertTrue(service.deleteSystemPostType(51L));
    }

    private ZentideInterestPost post(Long postId, Long hubId, String authorId) {
        ZentideInterestPost post = new ZentideInterestPost();
        post.setPostId(postId);
        post.setHubId(hubId);
        post.setAuthorId(authorId);
        post.setPostType("DISCUSSION");
        post.setStatus("PUBLISHED");
        return post;
    }

    private ZentideInterestHub hub(Long hubId, String ownerId, String joinPolicy) {
        ZentideInterestHub hub = new ZentideInterestHub();
        hub.setHubId(hubId);
        hub.setOwnerId(ownerId);
        hub.setJoinPolicy(joinPolicy);
        hub.setStatus("ACTIVE");
        return hub;
    }
}
