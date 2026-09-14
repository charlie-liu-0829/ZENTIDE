package com.zentide.mapper;

import com.zentide.entity.po.ZentideInterestComment;
import com.zentide.entity.po.ZentideInterestHub;
import com.zentide.entity.po.ZentideInterestPost;
import com.zentide.entity.po.ZentideInterestEvent;
import com.zentide.entity.po.ZentideCommunityUserProfile;
import com.zentide.entity.po.ZentideInterestEntity;
import com.zentide.entity.po.ZentideInterestTopic;
import com.zentide.entity.po.ZentideInterestChangeContext;
import com.zentide.entity.po.ZentideHubCategory;
import com.zentide.entity.po.ZentideHubInvitation;
import com.zentide.entity.po.ZentideHubMemberRequest;
import com.zentide.entity.po.ZentidePostType;
import com.zentide.entity.po.ZentideCommunityHighlight;
import com.zentide.entity.po.ZentideInterestEventStats;
import com.zentide.entity.po.ZentideHubDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ZentideInterestMapper {
    List<ZentideInterestHub> listHubs(@Param("userId") String userId);

    List<ZentideInterestHub> adminListHubs(@Param("query") String query, @Param("status") String status, @Param("limit") int limit);

    int adminUpdateHubStatus(@Param("hubId") Long hubId, @Param("status") String status);

    List<ZentideInterestHub> discoverHubs(@Param("userId") String userId, @Param("query") String query, @Param("category") String category, @Param("limit") int limit);

    List<Map<String, Object>> searchPosts(@Param("query") String query, @Param("limit") int limit);

    List<Map<String, Object>> searchHubs(@Param("query") String query, @Param("limit") int limit);

    List<ZentideHubDirection> listHubDirections(@Param("includeDisabled") boolean includeDisabled);

    ZentideHubDirection findActiveHubDirection(@Param("code") String code);

    int insertHubDirection(ZentideHubDirection direction);

    int updateHubDirection(ZentideHubDirection direction);

    int deleteHubDirection(@Param("directionId") Long directionId);

    ZentideInterestHub findHub(Long hubId);

    List<ZentideInterestTopic> listHubTopics(@Param("hubId") Long hubId, @Param("limit") int limit);

    Long findHubTopic(@Param("hubId") Long hubId, @Param("topicId") Long topicId);

    Long findTopicIdByName(String name);

    int insertCommunityTopic(ZentideInterestTopic topic);

    int attachHubTopic(@Param("hubId") Long hubId, @Param("topicId") Long topicId);

    List<ZentidePostType> listAvailablePostTypes(@Param("hubId") Long hubId);

    List<ZentidePostType> listHubCustomPostTypes(Long hubId);

    List<ZentidePostType> listSystemPostTypes();

    int countActivePostTypeForHub(@Param("hubId") Long hubId, @Param("typeCode") String typeCode);

    int countActivePostTypeCode(String typeCode);

    int insertPostType(ZentidePostType postType);

    int deleteHubPostType(@Param("hubId") Long hubId, @Param("postTypeId") Long postTypeId);

    int updateHubPostType(@Param("hubId") Long hubId, @Param("postTypeId") Long postTypeId, @Param("displayName") String displayName, @Param("description") String description);

    int updateSystemPostType(ZentidePostType postType);

    int deleteSystemPostType(Long postTypeId);

    List<ZentideInterestChangeContext> listChangeContexts(Long changeId);

    int countVerifiedChangeInHubTopic(@Param("hubId") Long hubId, @Param("topicId") Long topicId, @Param("changeId") Long changeId);

    int attachPostTopic(@Param("postId") Long postId, @Param("topicId") Long topicId);

    int refreshTopicPostCount(@Param("hubId") Long hubId, @Param("topicId") Long topicId);

    int insertHub(ZentideInterestHub hub);

    int deleteOwnedHub(@Param("hubId") Long hubId, @Param("ownerId") String ownerId);

    List<Long> listPostIdsForSnapshotByHub(@Param("hubId") Long hubId);

    List<Long> listPublishedPostIdsForSnapshot();

    int countActiveHubMember(@Param("hubId") Long hubId, @Param("userId") String userId);

    int countOwnedHub(@Param("hubId") Long hubId, @Param("userId") String userId);

    List<ZentideInterestEntity> listEntities(@Param("hubId") Long hubId, @Param("userId") String userId, @Param("limit") int limit);

    ZentideInterestEntity findEntity(Long entityId);

    int followEntity(@Param("entityId") Long entityId, @Param("userId") String userId);

    int unfollowEntity(@Param("entityId") Long entityId, @Param("userId") String userId);

    int actionEntity(@Param("entityId") Long entityId, @Param("userId") String userId, @Param("action") String action);

    int clearEntityAction(@Param("entityId") Long entityId, @Param("userId") String userId);

    List<ZentideInterestPost> listPosts(@Param("hubId") Long hubId, @Param("postType") String postType, @Param("topicId") Long topicId, @Param("changeId") Long changeId, @Param("userId") String userId, @Param("limit") int limit);

    List<ZentideInterestPost> adminListPosts(@Param("query") String query, @Param("status") String status, @Param("hubId") Long hubId, @Param("limit") int limit);

    int adminUpdatePostStatus(@Param("postId") Long postId, @Param("status") String status);

    String adminFindPostAuthorId(@Param("postId") Long postId);

    Long adminFindPostHubId(@Param("postId") Long postId);

    List<ZentideInterestComment> adminListComments(@Param("query") String query, @Param("status") String status, @Param("hubId") Long hubId, @Param("limit") int limit);

    int adminUpdateCommentStatus(@Param("commentId") Long commentId, @Param("status") String status);

    Long adminFindCommentPostId(@Param("commentId") Long commentId);

    List<ZentideInterestTopic> adminListTopics(@Param("query") String query, @Param("status") String status, @Param("limit") int limit);

    int adminUpdateTopicStatus(@Param("topicId") Long topicId, @Param("status") String status);

    int countAdminHubs(@Param("status") String status);

    int countAdminPosts(@Param("status") String status);

    int countAdminComments(@Param("status") String status);

    int countAdminPendingMembers();

    List<String> listPostTopicNames(Long postId);

    List<Long> listPostTopicIds(Long postId);

    List<ZentideInterestEvent> listEvents(@Param("hubId") Long hubId,@Param("userId") String userId,@Param("limit") int limit);

    List<ZentideInterestEvent> listOwnedEvents(@Param("hubId") Long hubId, @Param("ownerId") String ownerId);

    int insertInterestEvent(ZentideInterestEvent event);

    int updateOwnedInterestEvent(@Param("event") ZentideInterestEvent event, @Param("ownerId") String ownerId);

    int cancelOwnedInterestEvent(@Param("eventId") Long eventId, @Param("ownerId") String ownerId);

    ZentideInterestEventStats eventParticipationStats(@Param("eventId") Long eventId);

    List<ZentideCommunityHighlight> listTodayHighlights(@Param("userId") String userId, @Param("metric") String metric, @Param("minCount") int minCount, @Param("limit") int limit);

    List<ZentideCommunityHighlight> listTodayHighlightsLegacy(@Param("userId") String userId, @Param("metric") String metric, @Param("minCount") int minCount, @Param("limit") int limit);

    boolean hasViewCountColumn();

    int incrementPostView(Long postId);

    ZentideInterestPost findPost(@Param("postId") Long postId, @Param("userId") String userId);

    int insertPost(ZentideInterestPost post);

    int updateOwnedPost(ZentideInterestPost post);

    int detachPostTopics(Long postId);

    int deleteOwnedPost(@Param("postId") Long postId, @Param("authorId") String authorId);

    int insertComment(ZentideInterestComment comment);

    List<ZentideInterestComment> listComments(@Param("postId") Long postId, @Param("userId") String userId, @Param("limit") int limit);

    List<ZentideInterestComment> listCommentsForSnapshot(@Param("postId") Long postId);

    Long findCommentPostId(Long commentId);

    int refreshCommentCount(Long postId);

    int reactComment(@Param("commentId") Long commentId, @Param("userId") String userId);

    int unreactComment(@Param("commentId") Long commentId, @Param("userId") String userId);

    int refreshCommentLikeCount(Long commentId);

    int react(@Param("postId") Long postId,@Param("userId") String userId,@Param("reaction") String reaction);

    int unreact(@Param("postId") Long postId,@Param("userId") String userId);

    int refreshLikeCount(Long postId);

    int refreshPostCount(Long hubId);

    int action(@Param("postId") Long postId,@Param("userId") String userId,@Param("action") String action);

    int clearAction(@Param("postId") Long postId,@Param("userId") String userId);

    int bookmark(@Param("postId") Long postId,@Param("userId") String userId);

    int unbookmark(@Param("postId") Long postId,@Param("userId") String userId);

    ZentideCommunityUserProfile findUserProfile(@Param("userId") String userId, @Param("viewerId") String viewerId);

    int countOtherNickname(@Param("userId") String userId, @Param("nickName") String nickName);

    int countOtherHandle(@Param("userId") String userId, @Param("handle") String handle);

    int updateUserIdentity(@Param("userId") String userId, @Param("nickName") String nickName, @Param("avatar") String avatar);

    int upsertUserProfile(ZentideCommunityUserProfile profile);

    List<ZentideInterestPost> listUserPosts(@Param("userId") String userId, @Param("limit") int limit);

    List<ZentideInterestPost> listBookmarkedPosts(@Param("userId") String userId, @Param("limit") int limit);

    List<ZentideInterestComment> listUserComments(@Param("userId") String userId, @Param("limit") int limit);

    int follow(@Param("followerId") String followerId, @Param("followingId") String followingId);

    int unfollow(@Param("followerId") String followerId, @Param("followingId") String followingId);

    Integer isFollowing(@Param("followerId") String followerId, @Param("followingId") String followingId);

    ZentideInterestEvent findAttendance(@Param("eventId") Long eventId, @Param("userId") String userId);

    int setAttendance(@Param("eventId") Long eventId, @Param("userId") String userId, @Param("status") String status);

    int clearAttendance(@Param("eventId") Long eventId, @Param("userId") String userId);

    int joinHubAsOwner(@Param("hubId") Long hubId, @Param("userId") String userId);

    int requestHubMembership(@Param("hubId") Long hubId, @Param("userId") String userId, @Param("status") String status);
    int requestHubMembershipWithNote(@Param("hubId") Long hubId, @Param("userId") String userId, @Param("status") String status, @Param("applicationNote") String applicationNote);

    int leaveHub(@Param("hubId") Long hubId, @Param("userId") String userId);

    int refreshMemberCount(Long hubId);

    List<ZentideHubMemberRequest> listPendingHubMembers(Long hubId);

    List<ZentideHubMemberRequest> listHubMembers(Long hubId);

    ZentideHubMemberRequest findHubMember(@Param("hubId") Long hubId, @Param("userId") String userId);

    int updateHubMemberRole(@Param("hubId") Long hubId, @Param("memberId") String memberId, @Param("role") String role, @Param("permissionsJson") String permissionsJson);

    int removeHubMember(@Param("hubId") Long hubId, @Param("memberId") String memberId);

    List<ZentideHubMemberRequest> adminListPendingHubMembers(@Param("limit") int limit);

    int updateMembershipStatus(@Param("hubId") Long hubId, @Param("memberId") String memberId, @Param("status") String status);

    int insertHubInvitation(ZentideHubInvitation invitation);

    ZentideHubInvitation findValidInvitation(String token);

    int consumeInvitation(Long invitationId);

    List<ZentideHubCategory> listHubCategories(String userId);

    int insertHubCategory(ZentideHubCategory category);

    ZentideHubCategory findUserHubCategory(@Param("categoryId") Long categoryId, @Param("userId") String userId);

    int deleteHubCategory(@Param("categoryId") Long categoryId, @Param("userId") String userId);

    int assignHubCategory(@Param("hubId") Long hubId, @Param("userId") String userId, @Param("categoryId") Long categoryId);

    Long findEvent(Long eventId);

    Long findEventHub(Long eventId);
}
