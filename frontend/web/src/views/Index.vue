<template>
  <main class="discover-page">
    <section class="heading">
      <div class="heading-copy">
        <span>DISCOVER / PUBLIC FACTS</span>
        <h1>先看变化，<em>再决定行动。</em></h1>
        <p>只看能回到原始来源的变化。把你关心的话题加入“我的关注”，以后相关更新会出现在今日页。</p>
        <el-input v-model="query" clearable size="large" placeholder="搜索变化或话题" @input="loadTopics">
          <template #prefix><span class="search-glyph">⌕</span></template>
        </el-input>
      </div>
      <div class="pulse-card" aria-label="知潮公共事实概览">
        <div class="pulse-orbit"><i></i><i></i><b>LIVE</b></div>
        <div class="pulse-copy">
          <span>PUBLIC PULSE</span><strong>知潮正在观察</strong><small>只把有证据的变化带到你面前</small>
        </div>
        <div class="pulse-stats">
          <span
            ><b>{{ changes.length }}</b
            >条变化</span
          ><span
            ><b>{{ topics.length }}</b
            >个话题</span
          >
        </div>
      </div>
    </section>

    <section class="changes">
      <div class="section-heading">
        <div>
          <span>VERIFIED CHANGES</span>
          <h2>最近变化</h2>
        </div>
        <small>{{ filteredChanges.length }} 条 · 按时间更新</small>
      </div>
      <div v-if="filteredChanges.length" class="change-list">
        <button
          v-for="change in filteredChanges"
          :key="change.changeId"
          type="button"
          @click="openChange(change.changeId)"
        >
          <div>
            <span class="importance">{{ importanceLabel(change.importance) }}</span
            ><time>{{ formatTime(change.occurredAt || change.publishedAt || change.createdAt) }}</time>
          </div>
          <strong>{{ change.title }}</strong>
          <p>{{ change.whatHappened || change.summary }}</p>
          <footer>
            <span>已确认 · 可查看证据</span
            ><b
              class="community-action"
              role="button"
              tabindex="0"
              @click.stop="openCommunity(change)"
              @keydown.enter.stop="openCommunity(change)"
              >去社区讨论 →</b
            >
          </footer>
        </button>
      </div>
      <p v-else class="empty">还没有匹配的已确认变化。</p>
    </section>

    <section class="topics">
      <div class="section-heading">
        <div>
          <span>TOPICS</span>
          <h2>按话题浏览</h2>
        </div>
        <small>话题用于分类，不是另一套订阅</small>
      </div>
      <div v-if="topics.length" class="topic-list">
        <article v-for="topic in topics" :key="topic.topicId">
          <button type="button" class="topic-name" @click="openTopic(topic)">
            <strong>{{ topic.canonicalName }}</strong
            ><small>{{ topic.sourceCount || 0 }} 个已接入来源</small>
          </button>
          <el-button
            link
            type="primary"
            :loading="addingTopicId === topic.topicId"
            @click="addToFollowing(topic)"
            >加入我的关注</el-button
          >
        </article>
      </div>
      <p v-else class="empty">没有找到话题。</p>
    </section>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const loginStore = useLoginStore()
const query = ref(String(route.query.q || ''))
const topics = ref([])
const changes = ref([])
const addingTopicId = ref(null)

const filteredChanges = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase()
  if (!keyword) return changes.value
  return changes.value.filter((change) =>
    `${change.title || ''} ${change.summary || ''} ${change.whatHappened || ''}`
      .toLocaleLowerCase()
      .includes(keyword),
  )
})

let topicTimer
const loadTopics = async () => {
  clearTimeout(topicTimer)
  topicTimer = setTimeout(loadTopicsNow, 180)
}
const loadTopicsNow = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideTopicDiscover,
    params: { query: query.value.trim(), limit: 24 },
    showLoading: false,
    showError: false,
  })
  if (result) topics.value = result.data || []
}
const loadChanges = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideChangeList,
    params: { limit: 50 },
    showLoading: false,
    showError: false,
  })
  if (result) changes.value = result.data || []
}
const openChange = (changeId) => router.push({ path: '/changes', query: { changeId } })
const openCommunity = async (change) => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityChangeContext}${change.changeId}/context`,
    params: {},
    showLoading: false,
    showError: false,
  })
  const context = result?.data?.[0]
  if (!context) return proxy.Message.info('这条变化暂未归入兴趣现场；你仍可在详情页提交你的观点。')
  router.push({
    name: 'community',
    query: { hubId: context.hubId, topicId: context.topicId, changeId: change.changeId, compose: '1' },
  })
}
const openTopic = (topic) =>
  router.push({ name: 'topic', params: { topicId: topic.topicId }, query: { name: topic.canonicalName } })
const addToFollowing = async (topic) => {
  if (!Object.keys(loginStore.userInfo || {}).length) {
    loginStore.showLogin = true
    return
  }
  addingTopicId.value = topic.topicId
  const radarResult = await proxy.Request({ url: proxy.Api.zentideRadarList, params: {}, showLoading: false })
  const current = radarResult?.data?.[0]
  const result = current
    ? await proxy.Request({
        url: `${proxy.Api.zentideRadarPlan}${current.radarId}/topics/${topic.topicId}`,
        params: {},
        showLoading: false,
      })
    : await proxy.Request({
        url: proxy.Api.zentideRadarCreate,
        params: {
          name: '我的关注',
          topics: JSON.stringify([topic.canonicalName]),
          attentionLevel: 'NORMAL',
          notificationStrategy: 'DAILY',
          changeTypes: JSON.stringify(['RELEASE', 'BREAKING_CHANGE', 'SECURITY']),
          sourcePreferences: JSON.stringify(['ALL']),
          ignoreRules: JSON.stringify([]),
          userContext: '',
        },
        showLoading: false,
      })
  addingTopicId.value = null
  if (!result) return
  proxy.Message.success(`已将“${topic.canonicalName}”加入我的关注`)
}
const importanceLabel = (value) =>
  ({ CRITICAL: '紧急', HIGH: '重要', MEDIUM: '值得知道', LOW: '顺手看看' })[value] || '已确认'
const formatTime = (value) =>
  value ? new Date(value).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) : '时间待确认'
onMounted(() => Promise.all([loadTopicsNow(), loadChanges()]))
</script>

<style scoped>
.discover-page {
  padding: 56px 0 96px;
}
.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 56px;
  padding: 28px 0 42px;
  border-bottom: 1px solid #dce6e2;
}
.heading-copy {
  display: grid;
  gap: 11px;
  max-width: 700px;
}
.heading > div > span,
.section-heading > div > span {
  color: #147f7c;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.2px;
}
.heading h1 {
  margin: 0;
  color: #183940;
  font-size: clamp(34px, 4vw, 50px);
  letter-spacing: -1.5px;
}
.heading h1 em {
  color: #147f7c;
  font-style: normal;
}
.heading p {
  max-width: 650px;
  margin: 0;
  color: #627a80;
  line-height: 1.7;
}
.heading .el-input {
  max-width: 520px;
  margin-top: 8px;
}
.search-glyph {
  color: #147f7c;
  font-size: 20px;
  line-height: 1;
}
.pulse-card {
  display: grid;
  grid-template-columns: 62px 1fr;
  gap: 14px;
  min-width: 300px;
  padding: 18px;
  border: 1px solid rgba(20, 127, 124, 0.18);
  border-radius: 18px 18px 18px 5px;
  background: linear-gradient(145deg, rgba(239, 250, 247, 0.95), rgba(247, 250, 252, 0.88));
  box-shadow: 0 18px 38px rgba(31, 105, 104, 0.1);
}
.pulse-orbit {
  position: relative;
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border: 1px solid rgba(20, 127, 124, 0.3);
  border-radius: 50%;
  color: #ef7967;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1px;
}
.pulse-orbit::before,
.pulse-orbit::after,
.pulse-orbit i {
  position: absolute;
  inset: 8px;
  border: 1px solid rgba(20, 127, 124, 0.2);
  border-radius: 50%;
  content: '';
}
.pulse-orbit::after {
  inset: 18px;
  border-color: rgba(239, 121, 103, 0.32);
}
.pulse-orbit i:first-child {
  inset: 28px;
  border: 0;
  background: #ef7967;
  box-shadow:
    0 0 0 5px rgba(239, 121, 103, 0.14),
    0 0 18px rgba(239, 121, 103, 0.38);
}
.pulse-orbit i:nth-child(2) {
  inset: -5px 25px;
  border-color: transparent rgba(20, 127, 124, 0.28);
  transform: rotate(28deg);
}
.pulse-copy {
  display: grid;
  align-content: center;
  gap: 4px;
}
.pulse-copy span {
  color: #147f7c;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1.1px;
}
.pulse-copy strong {
  color: #24464c;
  font-size: 15px;
}
.pulse-copy small {
  color: #71878a;
  font-size: 11px;
  line-height: 1.5;
}
.pulse-stats {
  grid-column: 1 / -1;
  display: flex;
  gap: 16px;
  padding-top: 13px;
  border-top: 1px solid rgba(20, 127, 124, 0.12);
  color: #7c9293;
  font-size: 11px;
}
.pulse-stats span {
  display: flex;
  align-items: baseline;
  gap: 5px;
}
.pulse-stats b {
  color: #147f7c;
  font-size: 18px;
}
.changes,
.topics {
  padding-top: 32px;
}
.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 13px;
  border-bottom: 1px solid #dce6e2;
}
.section-heading h2 {
  margin: 6px 0 0;
  color: #29464d;
  font-size: 20px;
}
.section-heading small {
  color: #829a9b;
  font-size: 11px;
}
.change-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 30px;
}
.change-list button {
  display: grid;
  gap: 9px;
  min-height: 180px;
  padding: 21px 2px;
  border: 0;
  border-bottom: 1px solid #e5ece9;
  color: #38545a;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.change-list button:hover strong,
.change-list button:hover b {
  color: #147f7c;
}
.change-list button > div,
.change-list footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #879899;
  font-size: 11px;
}
.importance {
  color: #a36f2d;
  font-weight: 700;
}
.change-list strong {
  color: #28474d;
  font-size: 16px;
  line-height: 1.45;
}
.change-list p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #71878a;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.change-list footer {
  margin-top: auto;
  padding-top: 11px;
  border-top: 1px solid #edf2f0;
}
.change-list b {
  color: #367f78;
  font-size: 11px;
}
.topic-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid #e5ece9;
}
.topic-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 78px;
  padding: 0 14px;
  border-right: 1px solid #e5ece9;
  border-bottom: 1px solid #e5ece9;
}
.topic-list article:nth-child(3n) {
  border-right: 0;
}
.topic-name {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 0;
  border: 0;
  color: #345057;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.topic-name strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.topic-name small {
  color: #8b9f9f;
  font-size: 10px;
}
.empty {
  padding: 28px 0;
  color: #829a9b;
  font-size: 13px;
}
@media (max-width: 900px) {
  .heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 28px;
  }
  .pulse-card {
    width: min(100%, 460px);
  }
}
@media (max-width: 760px) {
  .discover-page {
    padding-top: 30px;
  }
  .heading h1 {
    font-size: 32px;
  }
  .change-list,
  .topic-list {
    grid-template-columns: 1fr;
  }
  .topic-list article {
    border-right: 0;
  }
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
  .pulse-card {
    min-width: 0;
    width: 100%;
  }
}
/* Unified editorial community skin */
.discover-page {
  padding: 42px 0 88px;
  color: var(--zt-text);
}
.heading {
  gap: 44px;
  padding: 34px 0 38px;
  border-color: var(--zt-border);
}
.heading-copy {
  gap: 10px;
}
.heading > div > span,
.section-heading > div > span {
  color: var(--zt-tide);
  font-size: 10px;
  letter-spacing: 1.35px;
}
.heading h1 {
  color: var(--zt-text);
  font-size: clamp(34px, 4vw, 46px);
  font-weight: 780;
  letter-spacing: -0.035em;
}
.heading h1 em {
  color: var(--zt-tide);
}
.heading p {
  color: var(--zt-text-2);
}
.search-glyph {
  color: var(--zt-primary);
}
.pulse-card {
  min-width: 292px;
  padding: 17px;
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-md);
  background: var(--zt-surface);
  box-shadow: none;
}
.pulse-orbit {
  border-color: #9bb4ad;
  color: var(--zt-tide);
}
.pulse-orbit::before,
.pulse-orbit::after,
.pulse-orbit i {
  border-color: #cad2ce;
}
.pulse-orbit::after {
  border-color: #e2b4a9;
}
.pulse-orbit i:first-child {
  background: var(--zt-tide);
  box-shadow: 0 0 0 4px var(--zt-tide-soft);
}
.pulse-orbit i:nth-child(2) {
  display: none;
}
.pulse-copy span,
.pulse-stats b {
  color: var(--zt-primary);
}
.pulse-copy strong {
  color: var(--zt-text);
}
.pulse-copy small,
.pulse-stats {
  color: var(--zt-text-3);
}
.pulse-stats {
  border-color: var(--zt-border-soft);
}
.changes,
.topics {
  padding-top: 30px;
}
.section-heading {
  border-color: var(--zt-border);
}
.section-heading h2 {
  color: var(--zt-text);
  font-size: 19px;
}
.section-heading small {
  color: var(--zt-text-3);
}
.change-list {
  gap: 0 32px;
}
.change-list button {
  border-color: var(--zt-border-soft);
  color: var(--zt-text-2);
}
.change-list button:hover {
  background: var(--zt-surface-hover);
}
.change-list button:hover strong,
.change-list button:hover b {
  color: var(--zt-primary);
}
.change-list button > div,
.change-list footer {
  color: var(--zt-text-3);
}
.importance {
  color: #99683a;
}
.change-list strong {
  color: var(--zt-text);
}
.change-list p {
  color: var(--zt-text-2);
}
.change-list footer {
  border-color: var(--zt-border-soft);
}
.change-list b {
  color: var(--zt-primary);
}
.topic-list,
.topic-list article {
  border-color: var(--zt-border-soft);
}
.topic-list article:hover {
  background: var(--zt-surface-hover);
}
.topic-name {
  color: var(--zt-text);
}
.topic-name small,
.empty {
  color: var(--zt-text-3);
}
@media (max-width: 760px) {
  .discover-page {
    padding-top: 24px;
  }
  .heading {
    padding-top: 22px;
  }
  .heading h1 {
    font-size: 32px;
  }
}
</style>
