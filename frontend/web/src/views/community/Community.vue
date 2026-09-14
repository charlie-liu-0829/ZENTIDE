<template>
  <main class="community-page">
    <header class="community-header">
      <div>
        <span class="eyebrow">ZENTIDE COMMUNITY</span>
        <h1>发现正在发生的兴趣</h1>
        <p>聊演出、新品、游戏、电影，或任何值得和同好分享的事。</p>
      </div>
      <el-button type="primary" size="large" @click="openComposer">＋ 发布分享</el-button>
    </header>

    <section class="hub-nav" aria-label="兴趣现场">
      <button type="button" :class="{ active: !selectedHub }" @click="selectHub(null)">推荐</button>
      <button
        v-for="hub in hubs"
        :key="hub.hubId"
        type="button"
        :class="{ active: selectedHub === hub.hubId }"
        @click="selectHub(hub.hubId)"
      >
        {{ hub.name }}
      </button>
      <button type="button" class="create-hub" @click="showHubComposer">＋ 创建兴趣现场</button>
    </section>

    <section v-if="activeHub" class="hub-summary">
      <div>
        <h2>{{ activeHub.name }}</h2>
        <p>{{ activeHub.description || '和同好一起讨论、记录与分享。' }}</p>
        <span>{{ activeHub.memberCount || 0 }} 位成员 · {{ activeHub.postCount || 0 }} 条动态</span>
      </div>
      <el-button :type="activeHub.joined ? 'default' : 'primary'" plain @click="toggleHubMembership">{{
        activeHub.joined ? '已加入' : '加入兴趣现场'
      }}</el-button>
    </section>

    <section class="community-layout">
      <div class="feed-column">
        <div class="feed-toolbar">
          <div>
            <h2>{{ selectedHubName || '社区动态' }}</h2>
            <p>{{ selectedHub ? '来自这个兴趣现场的最新分享' : '来自所有兴趣现场的最新分享' }}</p>
          </div>
          <div class="post-filters">
            <button
              v-for="filter in filters"
              :key="filter.value"
              type="button"
              :class="{ active: selectedType === filter.value }"
              @click="selectType(filter.value)"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>
        <button class="quick-publish" type="button" @click="openComposer">
          <span>✦</span>
          <div>
            <b>此刻你在关注什么？</b
            ><small>{{
              selectedHubName ? `分享到「${selectedHubName}」` : '分享一个发现、问题或体验'
            }}</small>
          </div>
          <i>发布 →</i>
        </button>

        <div v-if="posts.length" class="post-feed">
          <article v-for="post in posts" :key="post.postId" class="post-card">
            <header class="post-header">
              <router-link class="avatar" :to="{ name: 'people', params: { userId: post.authorId } }">{{
                (post.authorLabel || '社').slice(0, 1)
              }}</router-link>
              <div>
                <router-link class="author" :to="{ name: 'people', params: { userId: post.authorId } }">{{
                  post.authorLabel || '社区成员'
                }}</router-link>
                <p>{{ post.hubName }} · {{ formatTime(post.createdAt, true) }}</p>
              </div>
              <span class="post-type">{{ typeLabel(post.postType) }}</span>
            </header>
            <div v-if="post.eventTitle" class="context-link">关联活动：{{ post.eventTitle }}</div>
            <div v-if="post.topicNames" class="topic-line"># {{ post.topicNames }}</div>
            <button
              v-if="post.changeId"
              type="button"
              class="change-context"
              @click="openChange(post.changeId)"
            >
              <span>基于已确认变化</span><b>{{ post.changeTitle || '查看变化与证据' }} →</b>
            </button>
            <h3 v-if="post.title">{{ post.title }}</h3>
            <p class="post-body">{{ post.body }}</p>
            <footer class="post-footer">
              <button type="button" :class="{ liked: post.liked }" @click="toggleLike(post)">
                {{ post.liked ? '♥' : '♡' }} {{ post.likeCount || 0 }}</button
              ><button type="button" @click="openComments(post)">评论 {{ post.commentCount || 0 }}</button
              ><button type="button" :class="{ saved: post.bookmarked }" @click="toggleBookmark(post)">
                {{ post.bookmarked ? '已收藏' : '收藏' }}</button
              ><el-dropdown trigger="click" @command="(action) => setAction(post, action)"
                ><button type="button" class="status-button">{{ actionLabel(post.myAction) }} ⌄</button
                ><template #dropdown
                  ><el-dropdown-menu
                    ><el-dropdown-item
                      v-for="action in actions"
                      :key="action.value"
                      :command="action.value"
                      >{{ action.label }}</el-dropdown-item
                    ><el-dropdown-item command="" divided>清除状态</el-dropdown-item></el-dropdown-menu
                  ></template
                ></el-dropdown
              >
            </footer>
            <div v-if="post.expanded" class="comments">
              <div v-if="post.comments?.length" class="comment-list">
                <div v-for="comment in post.comments" :key="comment.commentId" class="comment">
                  <b>{{ comment.authorLabel }}</b
                  ><span>{{ comment.body }}</span>
                </div>
              </div>
              <p v-else>还没有评论，来留下第一句。</p>
              <div class="comment-compose">
                <el-input
                  v-model="post.commentDraft"
                  placeholder="说点什么…"
                  @keyup.enter="submitComment(post)"
                /><el-button type="primary" @click="submitComment(post)">发送</el-button>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">
          <b>这里还没有动态</b>
          <p>第一个分享，会让这个兴趣现场真正开始。</p>
          <el-button type="primary" @click="openComposer">发布第一条</el-button>
        </div>
      </div>

      <aside class="sidebar">
        <section v-if="selectedHub && topics.length" class="side-card">
          <div class="side-title">
            <h3>热门话题</h3>
            <span>话题</span>
          </div>
          <div class="topics">
            <button v-for="topic in topics" :key="topic.topicId" type="button" @click="chooseTopic(topic)">
              # {{ topic.canonicalName }}<small>{{ topic.postCount || 0 }}</small>
            </button>
          </div>
        </section>
        <section v-if="selectedHub && entities.length" class="side-card">
          <div class="side-title">
            <h3>大家关注</h3>
            <span>对象</span>
          </div>
          <article v-for="entity in entities.slice(0, 4)" :key="entity.entityId" class="entity">
            <div>
              <b>{{ entity.name }}</b>
              <p>{{ entity.followerCount || 0 }} 人关注 · {{ entity.postCount || 0 }} 条分享</p>
            </div>
            <el-button
              size="small"
              :type="entity.followed ? 'default' : 'primary'"
              plain
              @click="toggleEntityFollow(entity)"
              >{{ entity.followed ? '已关注' : '关注' }}</el-button
            >
          </article>
        </section>
        <section v-if="events.length" class="side-card">
          <div class="side-title">
            <h3>近期活动</h3>
            <span>活动</span>
          </div>
          <article v-for="event in events.slice(0, 3)" :key="event.eventId" class="event">
            <time
              ><b>{{ eventDay(event.startsAt) }}</b
              ><span>{{ eventMonth(event.startsAt) }}</span></time
            >
            <div>
              <b>{{ event.title }}</b>
              <p>{{ event.venue || '地点待确认' }} · {{ event.attendeeCount || 0 }} 人关注</p>
            </div>
            <el-dropdown trigger="click" @command="(status) => setAttendance(event, status)"
              ><button type="button" class="event-status">
                {{ attendanceLabel(event.attendanceStatus) }} ⌄</button
              ><template #dropdown
                ><el-dropdown-menu
                  ><el-dropdown-item
                    v-for="status in attendanceStatuses"
                    :key="status.value"
                    :command="status.value"
                    >{{ status.label }}</el-dropdown-item
                  ><el-dropdown-item command="" divided>取消标记</el-dropdown-item></el-dropdown-menu
                ></template
              ></el-dropdown
            >
          </article>
        </section>
      </aside>
    </section>

    <el-dialog v-model="composerVisible" title="发布分享" width="min(92vw, 620px)"
      ><p class="dialog-tip">先选择兴趣现场；如果内容与活动或话题相关，可以补充关联信息。</p>
      <el-form label-position="top"
        ><el-form-item label="兴趣现场"
          ><el-select v-model="composer.hubId" placeholder="选择兴趣现场" style="width: 100%"
            ><el-option
              v-for="hub in hubs"
              :key="hub.hubId"
              :label="hub.name"
              :value="hub.hubId" /></el-select></el-form-item
        ><el-form-item label="内容类型"
          ><el-radio-group v-model="composer.type"
            ><el-radio-button v-for="filter in filters.slice(1)" :key="filter.value" :label="filter.value">{{
              filter.label
            }}</el-radio-button></el-radio-group
          ></el-form-item
        ><el-form-item v-if="composerTopics.length" label="关联话题（可选）"
          ><el-select
            v-model="composer.topicId"
            clearable
            placeholder="选择正在讨论的话题"
            style="width: 100%"
            ><el-option
              v-for="topic in composerTopics"
              :key="topic.topicId"
              :label="`# ${topic.canonicalName}`"
              :value="topic.topicId" /></el-select></el-form-item
        ><el-form-item v-if="composerEvents.length" label="关联活动（可选）"
          ><el-select v-model="composer.eventId" clearable placeholder="选择关联活动" style="width: 100%"
            ><el-option
              v-for="event in composerEvents"
              :key="event.eventId"
              :label="event.title"
              :value="event.eventId" /></el-select></el-form-item
        ><el-form-item label="标题（可选）"
          ><el-input
            v-model="composer.title"
            maxlength="220"
            placeholder="用一句话概括你的分享" /></el-form-item
        ><el-form-item label="正文"
          ><el-input
            v-model="composer.body"
            type="textarea"
            :rows="6"
            maxlength="8000"
            show-word-limit
            placeholder="说说你的发现、体验或问题…" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="composerVisible = false">取消</el-button
        ><el-button type="primary" :loading="publishing" @click="publishPost">发布</el-button></template
      ></el-dialog
    >

    <el-dialog v-model="hubComposerVisible" title="创建兴趣现场" width="min(92vw, 520px)"
      ><p class="dialog-tip">一个兴趣现场适合长期聚合同好、活动和经验。</p>
      <el-form label-position="top"
        ><el-form-item label="名称"
          ><el-input
            v-model="hubDraft.name"
            maxlength="40"
            placeholder="例如：独立电影放映、城市骑行、桌游夜" /></el-form-item
        ><el-form-item label="介绍"
          ><el-input
            v-model="hubDraft.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="大家会在这里讨论什么？" /></el-form-item
        ><el-form-item label="所属方向"
          ><el-select v-model="hubDraft.category" style="width: 100%"
            ><el-option label="综合兴趣" value="GENERAL" /><el-option
              label="科技"
              value="TECHNOLOGY" /><el-option label="音乐" value="MUSIC" /><el-option
              label="游戏"
              value="GAMES" /><el-option
              label="生活方式"
              value="LIFESTYLE" /></el-select></el-form-item></el-form
      ><template #footer
        ><el-button @click="hubComposerVisible = false">取消</el-button
        ><el-button type="primary" :loading="creatingHub" @click="createHub">创建并加入</el-button></template
      ></el-dialog
    >
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'

const { proxy } = getCurrentInstance()
const loginStore = useLoginStore()
const route = useRoute()
const router = useRouter()
const hubs = ref([])
const topics = ref([])
const entities = ref([])
const events = ref([])
const posts = ref([])
const composerEvents = ref([])
const composerTopics = ref([])
const selectedHub = ref(null)
const selectedType = ref(null)
const composerVisible = ref(false)
const hubComposerVisible = ref(false)
const publishing = ref(false)
const creatingHub = ref(false)
const changeContext = ref(null)
const composer = reactive({
  hubId: null,
  topicId: null,
  eventId: null,
  changeId: null,
  type: 'DISCUSSION',
  title: '',
  body: '',
})
const hubDraft = reactive({ name: '', description: '', category: 'GENERAL' })
const filters = [
  { value: null, label: '全部' },
  { value: 'DISCUSSION', label: '讨论' },
  { value: 'QUESTION', label: '提问' },
  { value: 'EXPERIENCE', label: '经验' },
  { value: 'REVIEW', label: '评测' },
  { value: 'EVENT', label: '活动' },
]
const actions = [
  { value: 'WANT', label: '想去' },
  { value: 'ATTENDED', label: '已去' },
  { value: 'USING', label: '正在用' },
  { value: 'WATCHING', label: '观望' },
  { value: 'RECOMMEND', label: '推荐' },
]
const attendanceStatuses = [
  { value: 'WANT', label: '想参加' },
  { value: 'ATTENDING', label: '参加中' },
  { value: 'ATTENDED', label: '已参加' },
]
const activeHub = computed(() => hubs.value.find((hub) => hub.hubId === selectedHub.value))
const selectedHubName = computed(() => activeHub.value?.name)
const loggedIn = () => Object.keys(loginStore.userInfo || {}).length > 0
const requestEvents = async (hubId) => {
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityEvents,
    params: { hubId, limit: 12 },
    showLoading: false,
    showError: false,
  })
  return result?.data || []
}
const requestTopics = async (hubId) => {
  if (!hubId) return []
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityTopics,
    params: { hubId, limit: 20 },
    showLoading: false,
    showError: false,
  })
  return result?.data || []
}
const loadHubs = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubs,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (result) {
    hubs.value = result.data || []
    if (!composer.hubId && hubs.value[0]) composer.hubId = hubs.value[0].hubId
  }
}
const loadFeed = async () => {
  const isContextHub = !route.query.hubId || Number(route.query.hubId) === Number(selectedHub.value)
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityFeed,
    params: {
      hubId: selectedHub.value,
      type: selectedType.value,
      topicId: isContextHub ? route.query.topicId || null : null,
      changeId: isContextHub ? route.query.changeId || null : null,
      limit: 50,
    },
    showLoading: false,
    showError: false,
  })
  if (result)
    posts.value = (result.data || []).map((post) => ({
      ...post,
      expanded: false,
      comments: [],
      commentDraft: '',
    }))
}
const loadHubContext = async () => {
  if (!selectedHub.value) {
    topics.value = []
    entities.value = []
    events.value = []
    return
  }
  const [topicResult, entityResult, eventResult] = await Promise.all([
    requestTopics(selectedHub.value),
    proxy.Request({
      url: proxy.Api.zentideCommunityEntities,
      params: { hubId: selectedHub.value, limit: 12 },
      showLoading: false,
      showError: false,
    }),
    requestEvents(selectedHub.value),
  ])
  topics.value = topicResult
  entities.value = entityResult?.data || []
  events.value = eventResult
}
const selectHub = async (value) => {
  selectedHub.value = value
  await Promise.all([loadFeed(), loadHubContext()])
}
const selectType = (value) => {
  selectedType.value = value
  loadFeed()
}
const openComposer = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  composer.hubId = selectedHub.value || composer.hubId || hubs.value[0]?.hubId
  composerVisible.value = true
  const [eventList, topicList] = await Promise.all([
    requestEvents(composer.hubId),
    requestTopics(composer.hubId),
  ])
  composerEvents.value = eventList
  composerTopics.value = topicList
}
const showHubComposer = () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  hubComposerVisible.value = true
}
const chooseTopic = async (topic) => {
  composer.topicId = topic.topicId
  await openComposer()
}
const toggleHubMembership = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !activeHub.value.joined
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${activeHub.value.hubId}/membership`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    activeHub.value.joined = active
    activeHub.value.memberCount = Math.max(0, Number(activeHub.value.memberCount || 0) + (active ? 1 : -1))
  }
}
const createHub = async () => {
  if (hubDraft.name.trim().length < 2) return proxy.Message.warning('兴趣现场名称至少需要两个字符')
  creatingHub.value = true
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubCreate,
    params: {
      name: hubDraft.name.trim(),
      description: hubDraft.description.trim(),
      category: hubDraft.category,
    },
    showLoading: false,
  })
  creatingHub.value = false
  if (!result) return
  hubComposerVisible.value = false
  const created = result.data
  hubDraft.name = ''
  hubDraft.description = ''
  selectedHub.value = created.hubId
  await Promise.all([loadHubs(), loadFeed(), loadHubContext()])
  proxy.Message.success(`已创建「${created.name}」`)
}
const publishPost = async () => {
  if (!composer.hubId) return proxy.Message.warning('请先选择兴趣现场')
  if (composer.body.trim().length < 2) return proxy.Message.warning('内容至少需要两个字符')
  publishing.value = true
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityPosts,
    params: {
      hubId: composer.hubId,
      topicId: composer.topicId,
      eventId: composer.eventId,
      changeId: composer.changeId,
      type: composer.type,
      title: composer.title.trim(),
      body: composer.body.trim(),
    },
    showLoading: false,
  })
  publishing.value = false
  if (!result) return
  composerVisible.value = false
  composer.title = ''
  composer.body = ''
  composer.eventId = null
  composer.topicId = null
  composer.changeId = null
  changeContext.value = null
  proxy.Message.success('已发布')
  await Promise.all([loadFeed(), loadHubContext(), loadHubs()])
}
const toggleLike = async (post) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !post.liked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/like`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    post.liked = active
    post.likeCount = Math.max(0, Number(post.likeCount || 0) + (active ? 1 : -1))
  }
}
const toggleBookmark = async (post) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !post.bookmarked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/bookmark`,
    params: { active },
    showLoading: false,
  })
  if (result) post.bookmarked = active
}
const setAction = async (post, action) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const value = post.myAction === action ? '' : action
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/action`,
    params: { action: value },
    showLoading: false,
  })
  if (result) post.myAction = value || null
}
const setAttendance = async (event, status) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityEventsAction}/${event.eventId}/attendance`,
    params: { status },
    showLoading: false,
  })
  if (result) event.attendanceStatus = status || null
}
const toggleEntityFollow = async (entity) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !entity.followed
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityEntities}/${entity.entityId}/follow`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    entity.followed = active
    entity.followerCount = Math.max(0, Number(entity.followerCount || 0) + (active ? 1 : -1))
  }
}
const openComments = async (post) => {
  post.expanded = !post.expanded
  if (post.expanded && !post.comments.length) {
    const result = await proxy.Request({
      url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/comments/list`,
      params: { limit: 100 },
      showLoading: false,
      showError: false,
    })
    if (result) post.comments = result.data || []
  }
}
const submitComment = async (post) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  if (!post.commentDraft?.trim()) return
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.postId}/comments`,
    params: { body: post.commentDraft.trim() },
    showLoading: false,
  })
  if (result) {
    post.comments.push(result.data)
    post.commentCount = Number(post.commentCount || 0) + 1
    post.commentDraft = ''
  }
}
const openChange = (changeId) => router.push({ path: '/changes', query: { changeId } })
const typeLabel = (value) =>
  ({ DISCUSSION: '讨论', QUESTION: '提问', EXPERIENCE: '经验', REVIEW: '评测', EVENT: '活动' })[value] ||
  '分享'
const actionLabel = (value) =>
  ({ WANT: '想去', ATTENDED: '已去', USING: '正在用', WATCHING: '观望', RECOMMEND: '推荐' })[value] ||
  '标记状态'
const attendanceLabel = (value) =>
  ({ WANT: '想参加', ATTENDING: '参加中', ATTENDED: '已参加' })[value] || '标记参与'
const formatTime = (value) =>
  value ? new Date(value).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) : '刚刚'
const eventDay = (value) => (value ? new Date(value).toLocaleDateString('zh-CN', { day: 'numeric' }) : '--')
const eventMonth = (value) =>
  value ? new Date(value).toLocaleDateString('zh-CN', { month: 'short' }) : '待定'
watch(
  () => composer.hubId,
  async (hubId, oldHubId) => {
    if (!composerVisible.value || hubId === oldHubId) return
    composer.topicId = null
    composer.eventId = null
    const [eventList, topicList] = await Promise.all([requestEvents(hubId), requestTopics(hubId)])
    composerEvents.value = eventList
    composerTopics.value = topicList
  },
)
onMounted(async () => {
  await loadHubs()
  const hubId = Number(route.query.hubId)
  if (hubId && hubs.value.some((hub) => hub.hubId === hubId)) selectedHub.value = hubId
  const topicId = Number(route.query.topicId)
  const changeId = Number(route.query.changeId)
  if (changeId) {
    const result = await proxy.Request({
      url: `${proxy.Api.zentideCommunityChangeContext}${changeId}/context`,
      params: {},
      showLoading: false,
      showError: false,
    })
    changeContext.value =
      result?.data?.find(
        (item) => item.hubId === selectedHub.value && (!topicId || item.topicId === topicId),
      ) ||
      result?.data?.[0] ||
      null
  }
  await Promise.all([loadFeed(), loadHubContext()])
  if (route.query.compose === '1') {
    composer.hubId = selectedHub.value
    composer.topicId = topicId || null
    composer.changeId = changeId || null
    await openComposer()
  }
})
</script>

<style scoped>
.community-page {
  max-width: 1160px;
  padding-top: 34px;
  color: #24333c;
}
.community-header {
  position: relative;
  overflow: hidden;
  padding: 38px 42px;
  border: 0;
  border-radius: 22px;
  color: #f8f3ea;
  background: #18343b;
  box-shadow: 0 18px 40px rgba(23, 51, 57, 0.16);
}
.community-header::before,
.community-header::after {
  position: absolute;
  border-radius: 50%;
  content: '';
  pointer-events: none;
}
.community-header::before {
  top: -100px;
  right: 120px;
  width: 270px;
  height: 270px;
  background: rgba(236, 180, 113, 0.16);
}
.community-header::after {
  right: -44px;
  bottom: -90px;
  width: 210px;
  height: 210px;
  border: 1px solid rgba(216, 239, 225, 0.42);
}
.eyebrow {
  position: relative;
  color: #f0bd7c;
  letter-spacing: 0.18em;
}
.community-header h1 {
  position: relative;
  margin: 10px 0 6px;
  font-family: Georgia, 'Songti SC', serif;
  font-size: 42px;
  font-weight: 600;
  letter-spacing: -0.06em;
}
.community-header p {
  position: relative;
  max-width: 490px;
  color: #c6d4d1;
  font-size: 14px;
}
.community-header :deep(.el-button) {
  position: relative;
  z-index: 1;
  border: 0;
  border-radius: 10px;
  box-shadow: 0 7px 18px rgba(0, 0, 0, 0.14);
}
.community-header :deep(.el-button--primary) {
  color: #24383d;
  background: #efbd7d;
}
.hub-nav {
  gap: 7px;
  padding: 21px 5px;
}
.hub-nav button {
  border: 0;
  border-radius: 999px;
  color: #647278;
  background: transparent;
  font-size: 13px;
}
.hub-nav button:hover,
.hub-nav button.active {
  color: #fff;
  background: #203c43;
  box-shadow: 0 4px 10px rgba(31, 60, 67, 0.15);
}
.hub-nav .create-hub {
  margin-left: auto;
  border: 1px solid #c8d7d1;
  color: #387d76;
  background: #fff;
}
.hub-summary {
  padding: 22px 25px;
  border: 0;
  border-radius: 15px;
  background: linear-gradient(100deg, #eef6f1, #fdfaf4);
  box-shadow: inset 0 0 0 1px #e4ebe5;
}
.hub-summary h2 {
  font-family: Georgia, 'Songti SC', serif;
  font-size: 25px;
}
.hub-summary :deep(.el-button) {
  border-radius: 9px;
}
.community-layout {
  gap: 38px;
}
.feed-toolbar {
  padding-bottom: 15px;
  border-color: #dce4df;
}
.feed-toolbar h2 {
  font-family: Georgia, 'Songti SC', serif;
  font-size: 26px;
  letter-spacing: -0.04em;
}
.post-filters button {
  border-radius: 999px;
}
.post-filters button.active {
  color: #fff;
  background: #245a58;
}
.quick-publish {
  padding: 15px 17px;
  border: 0;
  border-radius: 14px;
  background: #fdfbf8;
  box-shadow: inset 0 0 0 1px #e4e8e1;
}
.quick-publish:hover {
  background: #fff;
  box-shadow:
    inset 0 0 0 1px #91b8ac,
    0 8px 20px rgba(41, 72, 70, 0.07);
}
.quick-publish span {
  color: #e07f5a;
  background: #f9e8d9;
}
.quick-publish b {
  font-size: 13px;
}
.post-feed {
  gap: 17px;
}
.post-card {
  position: relative;
  padding: 23px 24px;
  border: 0;
  border-radius: 16px;
  background: #fffdf9;
  box-shadow:
    0 7px 22px rgba(42, 63, 61, 0.06),
    inset 0 0 0 1px #e7e8e0;
}
.post-card::before {
  position: absolute;
  top: 22px;
  left: 0;
  width: 3px;
  height: 34px;
  border-radius: 0 4px 4px 0;
  background: #efbd7d;
  content: '';
}
.avatar {
  background: linear-gradient(135deg, #285a5a, #4c9288);
}
.author {
  font-size: 13px;
}
.post-type {
  color: #4d786f;
  background: #e9f4ed;
}
.context-link,
.topic-line {
  border-radius: 999px;
}
.post-card h3 {
  font-family: Georgia, 'Songti SC', serif;
  font-size: 21px;
  letter-spacing: -0.025em;
}
.post-body {
  color: #4e6166;
  line-height: 1.86;
}
.post-footer {
  border-color: #e7ebe6;
}
.post-footer button {
  font-size: 12px;
}
.sidebar {
  gap: 17px;
}
.side-card {
  padding: 19px;
  border: 0;
  border-radius: 14px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #e4e8e2;
}
.side-card:first-child {
  background: #f8f1e6;
}
.side-title h3 {
  font-family: Georgia, 'Songti SC', serif;
  font-size: 17px;
}
.topics button {
  border: 0;
  border-radius: 999px;
  color: #3f6964;
  background: rgba(255, 255, 255, 0.76);
}
.topics button:hover {
  color: #fff;
  background: #2c6964;
}
.entity,
.event {
  padding: 12px 0;
}
.event time {
  border-radius: 9px;
  color: #9b563e;
  background: #f9e7d8;
}
.empty-state {
  border: 0;
  background: #f8f5ef;
}
@media (max-width: 600px) {
  .community-header {
    padding: 29px 24px;
    border-radius: 17px;
  }
  .community-header h1 {
    font-size: 33px;
  }
  .community-header :deep(.el-button) {
    width: 100%;
  }
  .hub-nav {
    padding-left: 0;
  }
  .hub-nav .create-hub {
    margin-left: 0;
  }
  .post-card {
    padding: 20px;
  }
  .community-layout {
    gap: 25px;
  }
  .sidebar {
    gap: 12px;
  }
}
</style>
