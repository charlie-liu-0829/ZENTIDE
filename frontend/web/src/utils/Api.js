import Request from './Request'
//单服务版本
const Api = {
  sourcePath: '/api/file/getResource?sourceName=',
  uploadImage: '/file/uploadImage',
  uploadPostMedia: '/file/uploadPostMedia',
  checkCode: '/account/checkCode', //验证码
  login: '/account/login', //登录
  logout: '/account/logout', //退出登录
  register: '/account/register', //注册
  autoLogin: '/account/autoLogin', //自动登录
  updatePassword: '/account/updatePassword', //修改密码
  securityQuestion: '/account/security-question',
  forgotPasswordQuestion: '/account/forgot-password/question',
  forgotPasswordReset: '/account/forgot-password/reset',
  getUserInfo: '/account/getUserInfo', //获取个人信息
  updateUserInfo: '/account/updateUserInfo', //修改个人信息
  zentideRadarList: '/zentide/v1/radars/list',
  zentideRadarCreate: '/zentide/v1/radars',
  zentideRadarPlan: '/zentide/v1/radars/',
  zentideChangeList: '/zentide/v1/changes/list',
  zentideChangeDetail: '/zentide/v1/changes/',
  zentideChangeWatching: '/zentide/v1/changes/watching/list',
  zentideChangeDiscussionList: '/zentide/v1/changes/',
  zentideEvidenceSubmission: '/zentide/v1/changes/',
  zentideResearchSearch: '/zentide/v1/research/search',
  zentideInsightList: '/zentide/v1/insights/list',
  zentideInsightFeedback: '/zentide/v1/insights/feedback',
  zentideDailyBriefing: '/zentide/v1/briefings/daily',
  zentideTopicDiscover: '/zentide/v1/topics/discover',
  zentideTopicFollowing: '/zentide/v1/topics/following',
  zentideTopicCreate: '/zentide/v1/topics',
  zentideTopicFollow: '/zentide/v1/topics/',
  zentideTopicSourceApplications: '/zentide/v1/topics/',
  zentideSignalList: '/zentide/v1/signals/list',
  zentideObservationActivities: '/zentide/v1/observations/activities/list',
  zentideChangeStance: '/zentide/v1/changes/',
  zentideCommunityHubs: '/zentide/v1/community/hubs',
  zentideCommunityHubDiscover: '/zentide/v1/community/hubs/discover',
  zentideCommunityHubDirections: '/zentide/v1/community/hub-directions',
  zentideCommunityHubCreate: '/zentide/v1/community/hubs/create',
  zentideCommunityHubCategories: '/zentide/v1/community/hub-categories',
  zentideCommunityInvitations: '/zentide/v1/community/invitations',
  zentideCommunityHubMembership: '/zentide/v1/community/hubs',
  zentideCommunityEntities: '/zentide/v1/community/entities',
  zentideCommunityAttentionSummary: '/zentide/v1/community/hubs',
  zentideCommunityTopics: '/zentide/v1/community/topics',
  zentideCommunityPostTypes: '/zentide/v1/community/post-types',
  zentideCommunityChangeContext: '/zentide/v1/community/changes/',
  zentideCommunityFeed: '/zentide/v1/community/feed',
  zentideCommunityEvents: '/zentide/v1/community/events',
  zentideCommunityTodayHighlights: '/zentide/v1/community/highlights/today',
  zentideCommunityManagedEvents: '/zentide/v1/community/hubs',
  zentideCommunityPosts: '/zentide/v1/community/posts',
  zentideCommunityComments: '/zentide/v1/community/comments',
  zentideCommunityUsers: '/zentide/v1/community/users',
  zentideCommunityBookmarks: '/zentide/v1/community/users/me/bookmarks',
  zentideCommunityProfileUpdate: '/zentide/v1/community/users/me/profile',
  zentideCommunityEventsAction: '/zentide/v1/community/events',
  zentideCommunitySearch: '/zentide/v1/community/search',
  zentideCommunitySearchHistory: '/zentide/v1/community/search/history',
  zentideCommunitySearchHistoryClear: '/zentide/v1/community/search/history/clear',
  zentideMessages: '/zentide/v1/messages',
  zentideAgentChat: '/zentide/v1/agent/chat',
  zentidePostReview: '/posts/review',
  zentidePostReviewEvents: '/posts/review/events',
  zentideInterestInsights: '/zentide/v1/interest-insights/generate',
  zentideInterestKeywords: '/zentide/v1/interest-insights/keywords',
  zentideInterestFeedback: '/zentide/v1/interest-insights/feedback',
  zentideInterestSaved: '/zentide/v1/interest-insights/saved',
}
const uploadImage = async (file, createThumbnail = false) => {
  let result = await Request({
    url: Api.uploadImage,
    params: {
      file,
      createThumbnail,
    },
  })
  if (!result) {
    return
  }
  return result.data
}
const uploadPostMedia = async (file, mediaType) => {
  const result = await Request({
    url: Api.uploadPostMedia,
    params: { file, mediaType },
    showLoading: false,
  })
  return result?.data
}
export { Api, uploadImage, uploadPostMedia }
