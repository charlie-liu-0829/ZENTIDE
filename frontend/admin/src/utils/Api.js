import Request from '@/utils/Request'
const Api = {
  //登录 退出
  checkCode: '/account/checkCode',
  login: '/account/login', //登录
  logout: '/account/logout', //退出
  zentideCommunityPostTypes: '/zentide/v1/community/post-types',
  zentideAdminCommunityHubs: '/zentide/v1/admin/community/hubs',
  zentideAdminCommunityPosts: '/zentide/v1/admin/community/posts',
  zentideAdminCommunityMembers: '/zentide/v1/admin/community/members',
  zentideAdminCommunityComments: '/zentide/v1/admin/community/comments',
  zentideAdminCommunityOverview: '/zentide/v1/admin/community/overview',
  zentideAdminCommunityTopics: '/zentide/v1/admin/community/topics',
  zentideAdminCommunityDirections: '/zentide/v1/admin/community/directions',
  zentideAdminGovernanceScan: '/zentide/v1/admin/governance/scan',
  zentideAdminGovernanceRules: '/zentide/v1/admin/governance/rules',
  zentideAdminGovernanceRuleList: '/zentide/v1/admin/governance/rules/all',
  zentideAdminGovernanceFeedback: '/zentide/v1/admin/governance/feedback',
  zentideAdminGovernancePending: '/zentide/v1/admin/governance/results/pending',
  //资源
  sourcePath: '/api/file/getResource?sourceName=',
  uploadImage: '/file/uploadImage',
  //用户列表
  loadUser: '/user/loadUser', //获取用户信息,
  changeStatus: '/user/changeStatus', //启用禁用用户
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
export { Api, uploadImage }
