import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      children: [
        {
          path: '',
          redirect: { name: 'community' },
        },
        {
          path: 'today',
          name: 'today',
          component: () => import('@/views/community/CommunityToday.vue'),
          meta: { checkLogin: true },
        },
        {
          path: 'discover',
          name: 'discover',
          component: () => import('@/views/Index.vue'),
        },
        {
          path: 'community',
          name: 'community',
          component: () => import('@/views/community/CommunityDiscuit.vue'),
          meta: { showFooter: false },
        },
        {
          path: 'community/discover',
          name: 'community-discover',
          component: () => import('@/views/community/HubDiscover.vue'),
        },
        {
          path: 'community/invite/:token',
          name: 'community-invite',
          component: () => import('@/views/community/HubInvite.vue'),
        },
        {
          path: 'people/:userId',
          name: 'people',
          component: () => import('@/views/community/People.vue'),
        },
        {
          path: 'messages/:userId?',
          name: 'messages',
          component: () => import('@/views/community/Messages.vue'),
          meta: { checkLogin: true, showFooter: false },
        },
        {
          path: 'community/posts/:postId',
          name: 'community-post',
          component: () => import('@/views/community/PostDetail.vue'),
          meta: { showFooter: false },
        },
        {
          path: 'community/assistant/:hubId',
          name: 'community-assistant',
          component: () => import('@/views/community/AssistantChat.vue'),
          meta: { checkLogin: true, showFooter: false },
        },
        {
          path: 'community/assistant/:hubId/post/:postId',
          name: 'community-post-assistant',
          component: () => import('@/views/community/AssistantChat.vue'),
          meta: { checkLogin: true, showFooter: false },
        },
        {
          path: 'community/bookmarks',
          name: 'community-bookmarks',
          component: () => import('@/views/community/Bookmarks.vue'),
          meta: { checkLogin: true },
        },
        {
          path: 'community/insights',
          name: 'community-insights',
          component: () => import('@/views/community/InterestInsights.vue'),
          meta: { checkLogin: true, showFooter: false },
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/community/SearchResults.vue'),
        },
        {
          path: 'topics/:topicId',
          name: 'topic',
          component: () => import('@/views/intelligence/TopicDetail.vue'),
        },
        {
          path: 'following',
          name: 'following',
          component: () => import('@/views/intelligence/IntelligenceWorkbench.vue'),
          meta: { checkLogin: true },
        },
        {
          path: 'radar',
          redirect: { name: 'following' },
        },
        {
          path: 'changes',
          name: 'changes',
          component: () => import('@/views/intelligence/IntelligenceWorkbench.vue'),
        },
        {
          path: 'watching',
          redirect: { name: 'following' },
        },
        {
          path: 'me',
          redirect: { name: 'following' },
        },
        {
          path: 'insights',
          redirect: { name: 'today' },
        },
        {
          path: 'research',
          redirect: { name: 'discover' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router
