import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/account/Account.vue') },
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      redirect: '/community/overview',
      children: [
        {
          path: '/community/overview',
          name: 'community-overview',
          component: () => import('@/views/community/CommunityOverview.vue'),
          meta: { itemList: ['社区运营', '社区总览'], title: '社区总览' },
        },
        {
          path: '/community/hubs',
          name: 'community-hubs',
          component: () => import('@/views/community/HubManagement.vue'),
          meta: { itemList: ['社区运营', '兴趣现场'], title: '兴趣现场管理' },
        },
        {
          path: '/community/posts',
          name: 'community-posts',
          component: () => import('@/views/community/PostModeration.vue'),
          meta: { itemList: ['社区运营', '帖子审核'], title: '帖子审核' },
        },
        {
          path: '/community/members',
          name: 'community-members',
          component: () => import('@/views/community/MemberReview.vue'),
          meta: { itemList: ['社区运营', '加入申请'], title: '加入申请' },
        },
        {
          path: '/community/comments',
          name: 'community-comments',
          component: () => import('@/views/community/CommentModeration.vue'),
          meta: { itemList: ['社区运营', '评论审核'], title: '评论审核' },
        },
        {
          path: '/community/governance',
          name: 'community-governance',
          component: () => import('@/views/community/GovernanceWorkbench.vue'),
          meta: { itemList: ['社区运营', '智能治理'], title: '智能治理' },
        },
        {
          path: '/community/governance-rules',
          name: 'community-governance-rules',
          component: () => import('@/views/community/GovernanceRuleManagement.vue'),
          meta: { itemList: ['社区配置', '治理规则'], title: '治理规则' },
        },
        {
          path: '/community/topics',
          name: 'community-topics',
          component: () => import('@/views/community/TopicManagement.vue'),
          meta: { itemList: ['社区配置', '话题管理'], title: '话题管理' },
        },
        {
          path: '/community/post-types',
          name: 'community-post-types',
          component: () => import('@/views/community/PostTypeManagement.vue'),
          meta: {
            itemList: ['社区运营', '固定帖子类型'],
            title: '固定帖子类型',
            description: '维护全站兴趣现场都能使用的基础帖子类型。',
          },
        },
        {
          path: '/community/directions',
          name: 'community-directions',
          component: () => import('@/views/community/DirectionManagement.vue'),
          meta: { itemList: ['社区配置', '兴趣现场方向'], title: '兴趣现场方向' },
        },
        {
          path: '/user/userList',
          name: 'users',
          component: () => import('@/views/user/UserList.vue'),
          meta: { itemList: ['用户管理', '用户列表'] },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/community/hubs' },
  ],
})

export default router
