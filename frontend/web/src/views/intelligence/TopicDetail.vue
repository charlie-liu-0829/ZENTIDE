<template>
  <main class="topic-page">
    <button type="button" class="back" @click="router.push('/discover')">返回发现</button>
    <section class="topic-heading">
      <span class="topic-mark">{{ topicName.slice(0, 1) }}</span>
      <div>
        <span>TOPIC</span>
        <h1>{{ topicName }}</h1>
        <p>查看这个话题下已接入来源与已经确认的变化。</p>
      </div>
    </section>
    <div class="source-note">
      <span>来源需经过审核与试运行，确保后续变化能回到可靠原文。</span
      ><el-button size="small" type="primary" plain @click="applicationVisible = true">提交来源</el-button>
    </div>
    <section class="topic-layout">
      <div>
        <div class="section-title">
          <div>
            <span>VERIFIED CHANGES</span>
            <h2>已确认变化</h2>
          </div>
          <small>{{ feed.length }} 条</small>
        </div>
        <div v-if="feed.length" class="feed-list">
          <article v-for="change in feed" :key="change.changeId">
            <time>{{ formatTime(change.occurredAt || change.publishedAt || change.createdAt) }}</time>
            <h3>{{ change.title }}</h3>
            <p>{{ change.whatHappened || change.summary }}</p>
            <div class="change-actions">
              <el-button
                link
                type="primary"
                @click="router.push({ path: '/changes', query: { changeId: change.changeId } })"
                >查看证据与详情</el-button
              ><el-button link type="primary" @click="openCommunity(change)">去社区讨论</el-button>
            </div>
          </article>
        </div>
        <p v-else class="empty">这个话题还没有已确认变化。</p>
      </div>
      <aside>
        <div class="section-title">
          <div>
            <span>SOURCES</span>
            <h2>已接入来源</h2>
          </div>
          <small>{{ sources.length }}</small>
        </div>
        <div v-if="sources.length" class="source-list">
          <article v-for="source in sources" :key="source.topicSourceId">
            <strong>{{ source.sourceName }}</strong
            ><small>{{ platformLabel(source.platformKey) }}</small
            ><a :href="source.canonicalUrl" target="_blank" rel="noreferrer">查看原文</a>
          </article>
        </div>
        <p v-else class="empty">暂未接入来源。</p>
      </aside>
    </section>
    <el-dialog v-model="applicationVisible" title="提交来源" width="min(92vw, 480px)">
      <p class="dialog-note">提交后会先由运营审核，再进行安全抓取和解析试运行；通过后才会正式接入。</p>
      <el-form label-position="top"
        ><el-form-item label="来源名称"
          ><el-input v-model="application.sourceName" placeholder="例如 Spring Blog" /></el-form-item
        ><el-form-item label="类型"
          ><el-select v-model="application.sourceType" style="width: 100%"
            ><el-option label="RSS / Atom" value="RSS_ATOM" /><el-option
              label="GitHub Releases"
              value="GITHUB_RELEASES" /></el-select></el-form-item
        ><el-form-item label="来源 URL"
          ><el-input v-model="application.canonicalUrl" placeholder="https://" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="applicationVisible = false">取消</el-button
        ><el-button type="primary" :loading="submitting" @click="submitApplication"
          >提交审核</el-button
        ></template
      >
    </el-dialog>
  </main>
</template>

<script setup>
import { getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const sources = ref([])
const feed = ref([])
const topicName = ref(route.query.name || '话题')
const applicationVisible = ref(false)
const submitting = ref(false)
const application = reactive({ sourceName: '', sourceType: 'RSS_ATOM', canonicalUrl: '' })
const load = async () => {
  const id = route.params.topicId
  const [sourceResult, feedResult] = await Promise.all([
    proxy.Request({
      url: `${proxy.Api.zentideTopicFollow}${id}/sources/list`,
      params: {},
      showLoading: false,
    }),
    proxy.Request({
      url: `${proxy.Api.zentideTopicFollow}${id}/feed`,
      params: { limit: 30 },
      showLoading: false,
    }),
  ])
  if (sourceResult) sources.value = sourceResult.data || []
  if (feedResult) feed.value = feedResult.data || []
}
const platformLabel = (value) => ({ RSS: 'RSS / Atom', GITHUB: 'GitHub', WEB: '网站' })[value] || value
const formatTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
    : '时间待确认'
const openCommunity = async (change) => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityChangeContext}${change.changeId}/context`,
    params: {},
    showLoading: false,
    showError: false,
  })
  const context =
    result?.data?.find((item) => Number(item.topicId) === Number(route.params.topicId)) || result?.data?.[0]
  if (!context) return proxy.Message.info('这个变化暂未关联兴趣现场。')
  router.push({
    name: 'community',
    query: { hubId: context.hubId, topicId: context.topicId, changeId: change.changeId, compose: '1' },
  })
}
const submitApplication = async () => {
  if (!application.sourceName.trim() || !application.canonicalUrl.trim())
    return proxy.Message.warning('请填写来源名称和 URL')
  submitting.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideTopicSourceApplications}${route.params.topicId}/source-applications`,
    params: {
      sourceName: application.sourceName.trim(),
      sourceType: application.sourceType,
      canonicalUrl: application.canonicalUrl.trim(),
    },
    showLoading: false,
  })
  submitting.value = false
  if (!result) return
  applicationVisible.value = false
  application.sourceName = ''
  application.canonicalUrl = ''
  proxy.Message.success('已提交，审核和试运行通过后会接入这个话题')
}
watch(() => route.params.topicId, load)
onMounted(load)
</script>

<style scoped>
.topic-page {
  padding: 38px 0 96px;
}
.back {
  padding: 0;
  border: 0;
  color: #147f7c;
  background: transparent;
  cursor: pointer;
}
.topic-heading {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 25px 0;
  border-bottom: 1px solid #dce6e2;
}
.topic-mark {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 16px 16px 16px 4px;
  color: #fff;
  background: #147f7c;
  font-size: 22px;
  font-weight: 800;
}
.topic-heading span:not(.topic-mark),
.section-title span {
  color: #147f7c;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.1px;
}
.topic-heading h1 {
  margin: 5px 0;
  color: #243f46;
  font-size: 30px;
}
.topic-heading p {
  margin: 0;
  color: #71878a;
}
.source-note {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 20px 0 0;
  padding: 12px 14px;
  border-left: 3px solid #5ba99e;
  color: #5f787b;
  background: #f2faf7;
  font-size: 13px;
}
.dialog-note {
  margin: 0 0 16px;
  color: #71878a;
  font-size: 13px;
  line-height: 1.7;
}
.topic-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 40px;
  padding-top: 30px;
}
.topic-layout aside {
  padding-left: 25px;
  border-left: 1px solid #dce6e2;
}
.section-title {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #dce6e2;
}
.section-title h2 {
  margin: 5px 0 0;
  color: #2a474d;
  font-size: 19px;
}
.section-title small {
  color: #8b9d9d;
}
.feed-list article {
  padding: 20px 0;
  border-bottom: 1px solid #e5ece9;
}
.feed-list time {
  color: #8b9d9d;
  font-size: 11px;
}
.feed-list h3 {
  margin: 9px 0;
  color: #29464d;
  font-size: 17px;
}
.feed-list p {
  margin: 0 0 10px;
  color: #708589;
  line-height: 1.7;
}
.source-list article {
  display: grid;
  gap: 6px;
  padding: 16px 0;
  border-bottom: 1px solid #e5ece9;
}
.source-list strong {
  color: #345057;
  font-size: 14px;
}
.source-list small {
  color: #839797;
}
.source-list a {
  color: #147f7c;
  font-size: 12px;
  text-decoration: none;
}
.empty {
  color: #839797;
  font-size: 13px;
  line-height: 1.7;
}
@media (max-width: 760px) {
  .topic-page {
    padding-top: 22px;
  }
  .topic-layout {
    grid-template-columns: 1fr;
  }
  .topic-layout aside {
    padding-left: 0;
    border-left: 0;
  }
}
/* Unified editorial community skin */
.topic-page {
  padding: 30px 0 88px;
  color: var(--zt-text);
}
.back {
  color: var(--zt-text-2);
}
.back:hover {
  color: var(--zt-primary);
}
.topic-heading {
  border-color: var(--zt-border);
}
.topic-mark {
  border-radius: var(--zt-radius-md);
  background: var(--zt-primary);
}
.topic-heading span:not(.topic-mark),
.section-title span {
  color: var(--zt-accent);
}
.topic-heading h1,
.section-title h2 {
  color: var(--zt-text);
}
.topic-heading p,
.dialog-note,
.feed-list p,
.empty {
  color: var(--zt-text-2);
}
.source-note {
  border-left-color: var(--zt-primary);
  border-radius: 0 var(--zt-radius-sm) var(--zt-radius-sm) 0;
  color: var(--zt-text-2);
  background: var(--zt-primary-soft);
}
.topic-layout aside {
  border-color: var(--zt-border);
}
.section-title,
.feed-list article,
.source-list article {
  border-color: var(--zt-border-soft);
}
.section-title small,
.feed-list time,
.source-list small {
  color: var(--zt-text-3);
}
.feed-list h3,
.source-list strong {
  color: var(--zt-text);
}
.source-list a {
  color: var(--zt-primary);
}
</style>
