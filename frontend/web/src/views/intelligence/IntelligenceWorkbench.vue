<template>
  <main class="workbench">
    <section class="heading">
      <div class="heading-current" aria-hidden="true"><i></i><i></i><i></i><span></span></div>
      <div>
        <span class="eyebrow">{{ sectionLabel }}</span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <el-button v-if="actionLabel" type="primary" plain @click="openAction">{{ actionLabel }}</el-button>
    </section>

    <section class="tide-summary" aria-label="知潮守望状态">
      <p class="tide-message">{{ tideMessage }}</p>
      <div class="tide-markers">
        <span v-for="item in model" :key="item.label"
          ><b>{{ item.value }}</b
          >{{ item.label }}</span
        >
      </div>
    </section>

    <section v-if="route.name === 'today' && insights.length" class="insight-list">
      <article v-for="insight in insights" :key="insight.insightId" class="insight-item">
        <div class="insight-topline">
          <span class="relevance">{{ tideFeeling(insight.relevanceScore) }}</span
          ><span>{{ formatTime(insight.updatedAt) }}</span>
        </div>
        <h2>{{ insight.title }}</h2>
        <p>{{ insight.body }}</p>
        <div class="why">
          <strong>与你有关</strong
          ><span v-for="topic in whyTopics(insight.whyJson)" :key="topic">{{ topic }}</span>
        </div>
        <footer>
          <span>基于你的关注范围</span>
          <div class="feedback">
            <el-button
              link
              :type="insight.feedbackType === 'USEFUL' ? 'primary' : ''"
              @click="sendInsightFeedback(insight, 'USEFUL')"
              >有用</el-button
            ><el-button link type="danger" @click="sendInsightFeedback(insight, 'IRRELEVANT')"
              >与我无关</el-button
            >
          </div>
        </footer>
      </article>
    </section>

    <section v-else-if="route.name === 'following'" class="radar-home">
      <div class="radar-main">
        <div class="radar-section-heading">
          <div>
            <span>今日与你有关</span>
            <h2>
              {{ insights.length ? `有 ${insights.length} 个变化值得看看` : '今天还没有需要打扰你的变化' }}
            </h2>
          </div>
        </div>
        <div v-if="insights.length" class="radar-insight-list">
          <article
            v-for="insight in insights.slice(0, 3)"
            :key="insight.insightId"
            class="radar-insight"
            @click="router.push('/today')"
          >
            <span class="relevance">{{ tideFeeling(insight.relevanceScore) }}</span>
            <div>
              <h3>{{ insight.title }}</h3>
              <p>{{ insight.body }}</p>
            </div>
          </article>
        </div>
        <div v-else class="radar-quiet">
          <strong>还没有与你相关的新变化。</strong
          ><span>来源完成核验后，命中你的关注话题的变化会出现在这里。</span>
        </div>
      </div>
      <aside class="radar-side">
        <div class="radar-section-heading">
          <div>
            <span>我的续看</span>
            <h2>
              {{ watchedChanges.length ? `${watchedChanges.length} 件事已加入续看` : '暂时没有续看的事' }}
            </h2>
          </div>
        </div>
        <div v-if="watchedChanges.length" class="watch-list">
          <button
            v-for="change in watchedChanges.slice(0, 3)"
            :key="change.changeId"
            type="button"
            @click="openChange(change.changeId)"
          >
            {{ change.title }}
          </button>
        </div>
        <p v-else>在变化详情中点“加入续看”，以后可以从这里回到这条变化。</p>
        <el-button link type="primary" @click="router.push('/changes')">去看看潮变</el-button>
      </aside>
      <section class="radar-list" aria-label="我的关注">
        <div class="radar-section-heading">
          <div>
            <span>MY FOLLOWING</span>
            <h2>{{ radars.length ? '我的关注' : '从一个关注开始' }}</h2>
          </div>
          <el-button type="primary" plain @click="openAction">{{
            radars.length ? '编辑关注' : '添加关注'
          }}</el-button>
        </div>
        <article v-for="radar in radars" :key="radar.radarId" class="radar-item">
          <div class="radar-identity">
            <span class="radar-status">{{ radar.status === 'ACTIVE' ? '正在关注' : radar.status }}</span>
            <h3>{{ radar.name }}</h3>
            <p v-if="radar.userContext">{{ radar.userContext }}</p>
            <div class="topic-list">
              <button
                v-for="topic in radar.topics"
                :key="topic.topicId"
                type="button"
                @click="router.push(`/topics/${topic.topicId}`)"
              >
                {{ topic.canonicalName }}
              </button>
            </div>
          </div>
          <div class="radar-plan">
            <div>
              <span>话题</span><strong>{{ (radar.topics || []).length }} 个</strong>
            </div>
            <div>
              <span>来源</span><strong>{{ radar.sourceCount || 0 }} 个已接入</strong>
            </div>
            <div>
              <span>已确认变化</span><strong>{{ radar.changeCount || 0 }} 个</strong>
            </div>
          </div>
          <div class="radar-plan-footer">
            <span>系统只会从已接入来源中筛选变化</span>
            <button type="button" @click="editRadar(radar)">编辑话题</button>
          </div>
        </article>
        <p v-if="!radars.length" class="radar-empty">
          从 Spring Boot、OpenAI 或你正在使用的技术开始。之后相关的已确认变化会进入“今日”。
        </p>
      </section>
    </section>

    <section v-else-if="route.name === 'changes' && changes.length" class="change-list">
      <article v-for="change in changes" :key="change.changeId" class="change-item">
        <div class="change-topline">
          <div class="change-context">
            <span class="change-status" :class="change.verificationStatus?.toLowerCase()">{{
              verificationLabel(change.verificationStatus)
            }}</span>
            <span v-if="change.entityType">{{ change.entityType }}</span>
          </div>
          <span>{{ formatTime(change.occurredAt || change.publishedAt || change.createdAt) }}</span>
        </div>
        <h2>{{ change.title }}</h2>
        <p>{{ change.whatHappened || change.summary }}</p>
        <footer>
          <span>{{ change.changeType || '新的动向' }}</span>
          <el-button link type="primary" @click="openChange(change.changeId)">查看这股潮</el-button>
        </footer>
      </article>
    </section>

    <section v-else class="empty-state">
      <div class="empty-mark">Z</div>
      <div>
        <h2>{{ emptyTitle }}</h2>
        <p>把话题加入你的关注范围后，相关的已确认变化会慢慢出现在这里。</p>
        <el-button v-if="route.name === 'following'" type="primary" plain @click="openAction"
          >添加关注</el-button
        >
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="我的关注" width="560px" class="radar-plan-dialog">
      <p class="plan-intro">先选择你关心的话题。系统只会从已接入并完成核验的来源中找到相关变化。</p>
      <el-form label-position="top" class="plan-form">
        <el-form-item label="关注话题"
          ><el-input v-model="form.topics" placeholder="例如：Spring Boot, Java, LangChain" /><small
            >多个话题用逗号分开。</small
          ></el-form-item
        >
      </el-form>
      <template #footer
        ><el-button @click="dialogVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveRadar">保存关注</el-button></template
      >
    </el-dialog>

    <el-dialog v-model="changeDialogVisible" title="潮变详情" width="680px">
      <div v-if="selectedChange" class="change-detail">
        <div class="detail-heading">
          <span class="change-status" :class="selectedChange.verificationStatus?.toLowerCase()">{{
            verificationLabel(selectedChange.verificationStatus)
          }}</span>
          <div class="detail-actions">
            <span>{{
              formatTime(selectedChange.occurredAt || selectedChange.publishedAt || selectedChange.createdAt)
            }}</span
            ><el-button link type="primary" :loading="watchingChange" @click="watchChange">{{
              watchingSelectedChange ? '已加入续看' : '加入续看'
            }}</el-button>
          </div>
        </div>
        <h2>{{ selectedChange.title }}</h2>
        <section class="change-pulse">
          <div>
            <span>重要性</span><strong>{{ importanceLabel(selectedChange.importance) }}</strong>
          </div>
          <div>
            <span>大家怎么做</span
            ><strong>{{
              stanceSummary.totalCount ? `${stanceSummary.totalCount} 人留下真实状态` : '等待第一条真实反馈'
            }}</strong>
          </div>
          <div>
            <span>续看</span
            ><strong>{{ watchingSelectedChange ? '已加入你的续看列表' : '可加入续看列表' }}</strong>
          </div>
        </section>
        <section class="stance-section">
          <h3>大家怎么做 <span>选择你此刻的真实状态</span></h3>
          <div class="stance-options">
            <button
              v-for="stance in stanceOptions"
              :key="stance.value"
              type="button"
              :class="{ active: myStance === stance.value }"
              @click="chooseStance(stance.value)"
            >
              <strong>{{ stance.label }}</strong
              ><span
                >{{ stanceCount(stance.countField)
                }}<small v-if="stanceSummary.totalCount">
                  · {{ stancePercent(stance.countField) }}%</small
                ></span
              >
            </button>
          </div>
          <p v-if="!stanceSummary.totalCount" class="muted">
            暂时没有群体判断。你的选择会成为第一条真实状态。
          </p>
        </section>
        <section class="observation-section">
          <h3>知潮正在怎么判断 <span>Agent 的观察过程</span></h3>
          <div v-if="observationActivities.length" class="observation-timeline">
            <article v-for="activity in observationActivities" :key="activity.activityId">
              <i></i>
              <div>
                <strong>{{ activityStage(activity.stage) }}</strong>
                <p>{{ activity.message }}</p>
                <small
                  >{{ activity.sourceName || '知潮观察系统' }} · {{ formatTime(activity.createdAt) }}</small
                >
              </div>
            </article>
          </div>
          <p v-else class="muted">这条变化暂无可展示的观察记录。</p>
        </section>
        <section>
          <h3>发生了什么</h3>
          <p>{{ selectedChange.whatHappened || selectedChange.summary }}</p>
        </section>
        <section>
          <h3>这一股潮说明了什么</h3>
          <p v-for="claim in selectedChange.claims || []" :key="claim.claimId">{{ claim.claimText }}</p>
          <p v-if="!(selectedChange.claims || []).length" class="muted">知潮还在等更多线索汇成清晰的潮变。</p>
        </section>
        <section
          v-if="selectedChange.supersedesChangeId || selectedChange.correctedByChangeId"
          class="tide-history"
        >
          <h3>更正与后续</h3>
          <p v-if="selectedChange.supersedesChangeId">
            这股潮接续了之前的变化，新的版本正在重新定义它的走向。
          </p>
          <p v-if="selectedChange.correctedByChangeId">
            这股潮已经出现更正或后续修订，知潮会把新的进展继续带给你。
          </p>
          <el-button
            v-if="selectedChange.correctedByChangeId"
            link
            type="primary"
            @click="openChange(selectedChange.correctedByChangeId)"
            >查看最新更正</el-button
          >
        </section>
        <section>
          <h3>来源</h3>
          <article
            v-for="evidence in selectedChange.evidence || []"
            :key="evidence.evidenceId"
            class="evidence-item"
          >
            <a :href="evidence.sourceUrl" target="_blank" rel="noreferrer">{{ evidence.sourceName }}</a>
            <p>{{ evidence.excerpt || evidence.locator }}</p>
          </article>
          <p v-if="!(selectedChange.evidence || []).length" class="muted">来源还在汇集，知潮会继续看。</p>
        </section>
        <section class="discussion-section">
          <h3>讨论与经验 <span>分享实践，也可以补充新的来源</span></h3>
          <el-input
            v-model="discussionBody"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="分享你的实践观察，或说说这次潮变对你意味着什么。"
          />
          <div class="discussion-actions">
            <el-select v-model="discussionEvidenceId" clearable placeholder="带上一处来源（可选）">
              <el-option
                v-for="evidence in selectedChange.evidence || []"
                :key="evidence.evidenceId"
                :label="`${evidence.sourceName} #${evidence.evidenceId}`"
                :value="evidence.evidenceId"
              />
            </el-select>
            <el-button type="primary" :loading="publishingDiscussion" @click="publishDiscussion"
              >发布讨论</el-button
            >
          </div>
          <div v-if="discussions.length" class="discussion-list">
            <article v-for="discussion in discussions" :key="discussion.discussionId">
              <div>
                <strong>{{ discussion.authorLabel }}</strong
                ><span>{{ formatTime(discussion.createdAt) }}</span>
              </div>
              <p>{{ discussion.body }}</p>
              <small v-if="discussion.evidenceId">带来一处来源</small>
            </article>
          </div>
          <p v-else class="muted">还没有讨论。第一条经验可以从这里开始。</p>
        </section>
        <section class="submission-section">
          <h3>补充来源 <span>知潮会比对原文，再决定是否带给更多人</span></h3>
          <el-input v-model="submissionSourceUrl" maxlength="2048" placeholder="原文链接" />
          <el-input
            v-model="submissionExcerpt"
            class="submission-excerpt"
            type="textarea"
            :rows="3"
            maxlength="4000"
            show-word-limit
            placeholder="摘录原文中最能说明这次潮变的内容（至少 20 个字符）"
          />
          <el-button type="primary" plain :loading="submittingEvidence" @click="submitEvidence"
            >补充来源</el-button
          >
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="briefingDialogVisible" title="今日潮报" width="680px">
      <div v-if="briefing" class="briefing-detail">
        <h2>{{ briefing.headline }}</h2>
        <article v-for="item in briefingItems" :key="item.insightId">
          <div>
            <strong>{{ item.title }}</strong
            ><span>{{ tideFeeling(item.relevanceScore) }}</span>
          </div>
          <p>{{ item.body }}</p>
        </article>
      </div>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const loginStore = useLoginStore()
const radars = ref([])
const changes = ref([])
const dialogVisible = ref(false)
const changeDialogVisible = ref(false)
const selectedChange = ref(null)
const watchedChanges = ref([])
const watchingChange = ref(false)
const watchingSelectedChange = ref(false)
const insights = ref([])
const briefing = ref(null)
const briefingDialogVisible = ref(false)
const saving = ref(false)
const editingRadarId = ref(null)
const discussions = ref([])
const stanceSummary = ref({
  totalCount: 0,
  actionedCount: 0,
  planningCount: 0,
  watchingCount: 0,
  blockedCount: 0,
  notRelevantCount: 0,
})
const myStance = ref(null)
const observationActivities = ref([])
const discussionBody = ref('')
const discussionEvidenceId = ref(null)
const publishingDiscussion = ref(false)
const submissionSourceUrl = ref('')
const submissionExcerpt = ref('')
const submittingEvidence = ref(false)
const form = reactive({
  name: '',
  topics: '',
  attentionLevel: 'NORMAL',
  notificationStrategy: 'DAILY',
  changeTypes: ['RELEASE', 'BREAKING_CHANGE', 'SECURITY'],
  sourcePreferences: ['ALL'],
  ignoreRules: '',
  userContext: '',
})
const stanceOptions = [
  { value: 'ACTIONED', label: '已行动', countField: 'actionedCount' },
  { value: 'PLANNING', label: '准备行动', countField: 'planningCount' },
  { value: 'WATCHING', label: '继续观望', countField: 'watchingCount' },
  { value: 'BLOCKED', label: '遇到问题', countField: 'blockedCount' },
  { value: 'NOT_RELEVANT', label: '与我无关', countField: 'notRelevantCount' },
]
const config = {
  following: {
    sectionLabel: 'MY FOLLOWING',
    title: '我的关注',
    description: '管理你关心的话题，并查看已加入续看的变化。',
    emptyTitle: '还没有关注的话题',
  },
  changes: {
    sectionLabel: 'TIDE CHANGES',
    title: '潮变',
    description: '经过观察和来源比对，值得你停下来看的重要变化。',
    emptyTitle: '新的潮变正在形成',
  },
  today: {
    sectionLabel: 'TODAY',
    title: '今日',
    description: '只展示命中你的关注范围、且已确认的变化。',
    emptyTitle: '今天还没有相关变化',
  },
}
const current = computed(() => config[route.name] || config.following)
const sectionLabel = computed(() => current.value.sectionLabel)
const title = computed(() => current.value.title)
const description = computed(() => current.value.description)
const emptyTitle = computed(() => current.value.emptyTitle)
const actionLabel = computed(() => (route.name === 'today' ? '生成今日摘要' : ''))
const tideMessage = computed(() => {
  if (route.name === 'changes')
    return changes.value.length
      ? `知潮确认了 ${changes.value.length} 个值得留意的潮变。`
      : '知潮正在观察，新的潮变出现时会留在这里。'
  if (route.name === 'following')
    return radars.value.length ? '这里汇总你的关注话题和已加入续看的变化。' : '从一个你真正关心的话题开始。'
  if (route.name === 'today')
    return insights.value.length
      ? '这些变化命中了你的关注范围，并且已有来源证据。'
      : '当已确认变化命中你的关注话题时，会出现在这里。'
  return '只展示已确认、且能回到来源证据的变化。'
})
const briefingItems = computed(() => {
  try {
    return JSON.parse(briefing.value?.bodyJson || '[]')
  } catch {
    return []
  }
})
const model = computed(() => [
  {
    label: '关注',
    value: route.name === 'following' ? radars.value.length : '--',
    note: route.name === 'following' ? '你关心的话题集合' : '等待你的关注',
  },
  {
    label: '潮变',
    value: changes.value.length || '--',
    note: route.name === 'changes' ? '值得你停下来看的新动向' : '等待新的潮变',
  },
  {
    label: '续看',
    value: route.name === 'following' ? watchedChanges.value.length || '--' : '--',
    note: '已加入续看的变化',
  },
])
const loadRadars = async () => {
  if (!['following'].includes(route.name)) return
  const result = await proxy.Request({ url: proxy.Api.zentideRadarList, params: {}, showLoading: false })
  if (result) radars.value = result.data || []
}
const loadChanges = async () => {
  if (!['changes', 'following'].includes(route.name)) return
  const result = await proxy.Request({
    url: proxy.Api.zentideChangeList,
    params: { limit: 50 },
    showLoading: false,
  })
  if (result) changes.value = result.data || []
}
const loadWatchedChanges = async () => {
  if (!['following'].includes(route.name)) return
  const result = await proxy.Request({
    url: proxy.Api.zentideChangeWatching,
    params: { limit: 20 },
    showLoading: false,
    showError: false,
  })
  if (result) watchedChanges.value = result.data || []
}
const loadInsights = async () => {
  if (!['today', 'following'].includes(route.name)) return
  const result = await proxy.Request({
    url: proxy.Api.zentideInsightList,
    params: { limit: 50 },
    showLoading: false,
  })
  if (result) insights.value = result.data || []
}
const openChange = async (changeId) => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideChangeDetail}${changeId}`,
    params: {},
    showLoading: false,
  })
  if (!result) return
  selectedChange.value = result.data
  watchingSelectedChange.value = watchedChanges.value.some((change) => change.changeId === changeId)
  discussionBody.value = ''
  discussionEvidenceId.value = null
  submissionSourceUrl.value = ''
  submissionExcerpt.value = ''
  myStance.value = null
  await Promise.all([loadDiscussions(changeId), loadStances(changeId), loadObservationActivities(changeId)])
  changeDialogVisible.value = true
}
const loadStances = async (changeId) => {
  const summaryResult = await proxy.Request({
    url: `${proxy.Api.zentideChangeStance}${changeId}/stances/summary`,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (summaryResult) stanceSummary.value = summaryResult.data || { totalCount: 0 }
  if (!Object.keys(loginStore.userInfo || {}).length) return
  const mineResult = await proxy.Request({
    url: `${proxy.Api.zentideChangeStance}${changeId}/stances/mine`,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (mineResult) myStance.value = mineResult.data
}
const chooseStance = async (stanceType) => {
  if (!Object.keys(loginStore.userInfo || {}).length) {
    loginStore.showLogin = true
    return
  }
  if (!selectedChange.value) return
  const result = await proxy.Request({
    url: `${proxy.Api.zentideChangeStance}${selectedChange.value.changeId}/stances`,
    params: { stanceType },
    showLoading: false,
  })
  if (!result) return
  myStance.value = stanceType
  stanceSummary.value = result.data || stanceSummary.value
}
const loadObservationActivities = async (changeId) => {
  const result = await proxy.Request({
    url: proxy.Api.zentideObservationActivities,
    params: { changeId, limit: 20 },
    showLoading: false,
    showError: false,
  })
  if (result) observationActivities.value = (result.data || []).slice().reverse()
}
const watchChange = async () => {
  if (!selectedChange.value || watchingSelectedChange.value) return
  watchingChange.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideChangeDetail}${selectedChange.value.changeId}/watch`,
    params: { watchMode: 'FOLLOW_UP' },
    showLoading: false,
  })
  watchingChange.value = false
  if (!result) return
  watchingSelectedChange.value = true
  proxy.Message.success('已加入续看')
  await loadWatchedChanges()
}
const loadDiscussions = async (changeId) => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideChangeDiscussionList}${changeId}/discussions/list`,
    params: { limit: 100 },
    showLoading: false,
  })
  if (result) discussions.value = result.data || []
}
const publishDiscussion = async () => {
  const body = discussionBody.value.trim()
  if (body.length < 2) return proxy.Message.warning('分享至少需要两个字符')
  if (!selectedChange.value) return
  publishingDiscussion.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideChangeDiscussionList}${selectedChange.value.changeId}/discussions`,
    params: { body, evidenceId: discussionEvidenceId.value },
    showLoading: false,
  })
  publishingDiscussion.value = false
  if (!result) return
  discussions.value.push(result.data)
  discussionBody.value = ''
  discussionEvidenceId.value = null
}
const submitEvidence = async () => {
  const sourceUrl = submissionSourceUrl.value.trim()
  const excerpt = submissionExcerpt.value.trim()
  if (!/^https?:\/\//i.test(sourceUrl)) return proxy.Message.warning('请填写 HTTP 或 HTTPS 原文链接')
  if (excerpt.length < 20) return proxy.Message.warning('请提供至少 20 个字符的原文摘录')
  if (!selectedChange.value) return
  submittingEvidence.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideEvidenceSubmission}${selectedChange.value.changeId}/evidence-submissions`,
    params: { sourceUrl, excerpt },
    showLoading: false,
  })
  submittingEvidence.value = false
  if (!result) return
  submissionSourceUrl.value = ''
  submissionExcerpt.value = ''
  proxy.Message.success('来源已补充；知潮比对后会带给更多人')
}
const verificationLabel = (value) =>
  ({ VERIFIED: '潮变已确认', CANDIDATE: '正在形成', UNVERIFIED: '刚刚发现', CONFLICTED: '来源仍有分歧' })[
    value
  ] || '正在形成'
const tideFeeling = (value) =>
  Number(value) >= 80 ? '高度相关' : Number(value) >= 50 ? '值得知道' : '顺手看看'
const importanceLabel = (value) =>
  ({ HIGH: '重要', MEDIUM: '值得知道', LOW: '顺手看看' })[value] || '值得知道'
const activityStage = (value) =>
  ({ DISCOVERED: '发现线索', SOURCE_CHECK: '找到来源', CHANGE_ASSESSMENT: '判断变化', VERIFIED: '确认完成' })[
    value
  ] || '持续观察'
const stanceCount = (field) => Number(stanceSummary.value[field] || 0)
const stancePercent = (field) =>
  stanceSummary.value.totalCount
    ? Math.round((stanceCount(field) * 100) / Number(stanceSummary.value.totalCount))
    : 0
const formatTime = (value) =>
  value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '时间待确认'
const jsonArray = (value) => {
  try {
    return JSON.parse(value || '[]')
  } catch {
    return []
  }
}
const resetRadarForm = () =>
  Object.assign(form, {
    name: '我的关注',
    topics: '',
    attentionLevel: 'NORMAL',
    notificationStrategy: 'DAILY',
    changeTypes: ['RELEASE', 'BREAKING_CHANGE', 'SECURITY'],
    sourcePreferences: ['ALL'],
    ignoreRules: '',
    userContext: '',
  })
const openAction = () => {
  if (route.name === 'following') {
    const firstRadar = radars.value[0]
    if (firstRadar) return editRadar(firstRadar)
    editingRadarId.value = null
    resetRadarForm()
    dialogVisible.value = true
  }
  if (route.name === 'today') loadDailyBriefing()
}
const editRadar = (radar) => {
  editingRadarId.value = radar.radarId
  Object.assign(form, {
    name: radar.name || '',
    topics: (radar.topics || []).map((topic) => topic.canonicalName).join(', '),
    attentionLevel: radar.attentionLevel || 'NORMAL',
    notificationStrategy: radar.notificationStrategy || 'DAILY',
    changeTypes: jsonArray(radar.changeTypesJson).length
      ? jsonArray(radar.changeTypesJson)
      : ['RELEASE', 'BREAKING_CHANGE', 'SECURITY'],
    sourcePreferences: jsonArray(radar.sourcePreferencesJson).length
      ? jsonArray(radar.sourcePreferencesJson)
      : ['ALL'],
    ignoreRules: jsonArray(radar.ignoreRulesJson).join(', '),
    userContext: radar.userContext || '',
  })
  dialogVisible.value = true
}
const whyTopics = (value) => {
  try {
    return JSON.parse(value || '{}').matchedTopics || []
  } catch {
    return []
  }
}
const sendInsightFeedback = async (insight, feedbackType) => {
  const result = await proxy.Request({
    url: proxy.Api.zentideInsightFeedback,
    params: { insightId: insight.insightId, feedbackType },
    showLoading: false,
  })
  if (!result) return
  if (feedbackType === 'USEFUL') insight.feedbackType = feedbackType
  else insights.value = insights.value.filter((item) => item.insightId !== insight.insightId)
}
const loadDailyBriefing = async () => {
  const result = await proxy.Request({ url: proxy.Api.zentideDailyBriefing, params: {}, showLoading: false })
  if (!result) return
  briefing.value = result.data
  briefingDialogVisible.value = true
}
const saveRadar = async () => {
  const topics = form.topics
    .split(/[,，]/)
    .map((value) => value.trim())
    .filter(Boolean)
  if (!topics.length) return proxy.Message.warning('请至少填写一个关注话题')
  const ignoreRules = form.ignoreRules
    .split(/[,，]/)
    .map((value) => value.trim())
    .filter(Boolean)
  saving.value = true
  const url = editingRadarId.value
    ? `${proxy.Api.zentideRadarPlan}${editingRadarId.value}/plan`
    : proxy.Api.zentideRadarCreate
  const result = await proxy.Request({
    url,
    params: {
      name: form.name.trim(),
      topics: JSON.stringify(topics),
      attentionLevel: form.attentionLevel,
      notificationStrategy: form.notificationStrategy,
      changeTypes: JSON.stringify(form.changeTypes),
      sourcePreferences: JSON.stringify(form.sourcePreferences),
      ignoreRules: JSON.stringify(ignoreRules),
      userContext: form.userContext.trim(),
    },
  })
  saving.value = false
  if (!result) return
  dialogVisible.value = false
  proxy.Message.success(editingRadarId.value ? '关注已更新' : '已开始关注')
  editingRadarId.value = null
  resetRadarForm()
  await loadRadars()
}
const loadPage = async () => {
  await Promise.all([loadRadars(), loadChanges(), loadInsights(), loadWatchedChanges()])
  if (route.name === 'changes' && route.query.changeId) await openChange(route.query.changeId)
}
onMounted(loadPage)
watch(() => [route.name, route.query.changeId], loadPage)
</script>

<style scoped>
.workbench {
  padding: 56px 0 96px;
}
.heading {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  min-height: 170px;
  padding: 22px 0 30px;
  overflow: hidden;
  border-bottom: 1px solid rgba(21, 116, 113, 0.18);
}
.heading > div:not(.heading-current),
.heading > .el-button {
  position: relative;
  z-index: 1;
}
.heading-current {
  position: absolute;
  top: -115px;
  right: -94px;
  width: 485px;
  height: 285px;
}
.heading-current i {
  position: absolute;
  border: 1px solid rgba(20, 127, 124, 0.25);
  border-radius: 50%;
  animation: current-flow 12s ease-in-out infinite alternate;
}
.heading-current i:nth-child(1) {
  inset: 0 10px 44px 50px;
}
.heading-current i:nth-child(2) {
  inset: 46px 64px 0 0;
  border-color: rgba(111, 91, 196, 0.18);
  animation-delay: -5s;
}
.heading-current i:nth-child(3) {
  inset: 82px 105px -36px 56px;
  border-color: rgba(237, 121, 103, 0.22);
  animation-delay: -8s;
}
.heading-current span {
  position: absolute;
  top: 109px;
  right: 139px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f07967;
  box-shadow:
    0 0 0 7px rgba(240, 121, 103, 0.13),
    0 0 24px rgba(240, 121, 103, 0.4);
  animation: radar-ping 3.2s ease-out infinite;
}
.eyebrow {
  color: #147f7c;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1.4px;
}
h1 {
  margin: 12px 0 10px;
  color: #16313a;
  font-size: 42px;
  line-height: 1.2;
}
.heading p {
  max-width: 630px;
  margin: 0;
  color: #627a80;
  line-height: 1.7;
}
.tide-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin: 26px 0 18px;
  padding: 16px 17px;
  border: 1px solid rgba(20, 127, 124, 0.14);
  border-radius: 14px 14px 14px 4px;
  background: rgba(242, 250, 248, 0.7);
  box-shadow: 0 12px 28px rgba(29, 99, 97, 0.04);
}
.tide-message {
  max-width: 660px;
  margin: 0;
  color: #4f6e74;
  font-size: 14px;
  line-height: 1.7;
}
.tide-markers {
  display: flex;
  flex-shrink: 0;
  align-items: baseline;
  gap: 18px;
  color: #829a9b;
  font-size: 12px;
}
.tide-markers span {
  display: flex;
  align-items: baseline;
  gap: 5px;
  white-space: nowrap;
}
.tide-markers b {
  color: #147f7c;
  font-size: 18px;
  font-weight: 700;
}
.architecture-loop {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0 0 34px;
  border-top: 1px solid rgba(21, 116, 113, 0.13);
  border-bottom: 1px solid rgba(21, 116, 113, 0.13);
}
.architecture-loop button {
  position: relative;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 10px;
  min-height: 67px;
  padding: 10px 14px;
  border: 0;
  border-right: 1px solid rgba(21, 116, 113, 0.12);
  color: #657b80;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.25s ease,
    color 0.25s ease;
}
.architecture-loop button:last-child {
  border-right: 0;
}
.architecture-loop button:hover,
.architecture-loop button.active {
  color: #176b65;
  background: rgba(226, 246, 241, 0.48);
}
.architecture-loop button.active::after {
  position: absolute;
  right: 14px;
  bottom: -1px;
  left: 14px;
  height: 2px;
  background: #ef7967;
  content: '';
}
.loop-index {
  color: #9bb0af;
  font-size: 9px;
  font-weight: 800;
}
.architecture-loop button > span:nth-child(2) {
  display: grid;
  gap: 4px;
}
.architecture-loop strong {
  color: #2f4d53;
  font-size: 13px;
}
.architecture-loop small {
  font-size: 10px;
}
.architecture-loop i {
  color: #93aaa8;
  font-size: 15px;
  font-style: normal;
}
.empty-state {
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 240px;
  padding: 30px 0;
  border-bottom: 1px solid rgba(21, 116, 113, 0.16);
}
.empty-mark {
  display: grid;
  place-items: center;
  flex: 0 0 52px;
  width: 52px;
  height: 52px;
  border: 1px solid #82c0b7;
  border-radius: 50% 50% 50% 10%;
  color: #147f7c;
  font-size: 21px;
  font-weight: 800;
  animation: topic-float 5s ease-in-out infinite;
}
.empty-state h2 {
  margin: 0 0 10px;
  color: #243047;
  font-size: 20px;
}
.empty-state p {
  max-width: 680px;
  margin: 0 0 18px;
  color: #718096;
  line-height: 1.8;
}
.radar-home {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 46px;
  align-items: start;
}
.radar-main {
  min-height: 236px;
}
.radar-section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 13px;
  border-bottom: 1px solid #dce6e2;
}
.radar-section-heading span {
  color: #176b65;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
}
.radar-section-heading h2 {
  margin: 6px 0 0;
  color: #243047;
  font-size: 20px;
  line-height: 1.4;
}
.radar-insight-list {
  border-top: 0;
}
.radar-insight {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 18px 2px;
  border-bottom: 1px solid #e6ece9;
  cursor: pointer;
}
.radar-insight:hover h3 {
  color: #176b65;
}
.radar-insight h3 {
  margin: 0 0 5px;
  color: #2a3947;
  font-size: 16px;
  transition: color 0.2s ease;
}
.radar-insight p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #718096;
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.radar-quiet {
  display: grid;
  gap: 7px;
  padding: 25px 0;
  color: #718096;
  font-size: 14px;
  line-height: 1.7;
}
.radar-quiet strong {
  color: #2a3947;
  font-size: 16px;
}
.radar-side {
  align-self: stretch;
  padding: 2px 0 18px 24px;
  border-left: 1px solid #dce6e2;
}
.radar-side .radar-section-heading {
  display: block;
}
.radar-side p {
  margin: 16px 0 8px;
  color: #718096;
  font-size: 13px;
  line-height: 1.75;
}
.watch-list {
  display: grid;
  gap: 1px;
  margin: 13px 0 5px;
}
.watch-list button {
  overflow: hidden;
  width: 100%;
  padding: 8px 0;
  border: 0;
  border-bottom: 1px solid #e6ece9;
  color: #526078;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.watch-list button:hover {
  color: #176b65;
}
.radar-list {
  grid-column: 1 / -1;
  margin-top: 10px;
  border-top: 1px solid #dce6e2;
}
.radar-list > .radar-section-heading {
  padding: 22px 0 14px;
}
.radar-item {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(360px, 0.9fr);
  gap: 18px 34px;
  padding: 24px 4px;
  border-bottom: 1px solid #e6ece9;
}
.radar-identity h3 {
  margin: 7px 0 0;
  color: #243047;
  font-size: 18px;
}
.radar-identity > p {
  max-width: 650px;
  margin: 8px 0 0;
  color: #74898c;
  font-size: 12px;
  line-height: 1.65;
}
.radar-status {
  color: #176b65;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
}
.topic-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 11px;
}
.topic-list button {
  padding: 4px 7px;
  border: 1px solid #dce9e5;
  border-radius: 5px;
  color: #526f72;
  background: #f7fbfa;
  cursor: pointer;
  font-size: 11px;
}
.topic-list button:hover {
  border-color: #91c3bb;
  color: #176b65;
}
.radar-plan {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  background: #deebe7;
  border: 1px solid #deebe7;
}
.radar-plan div {
  display: grid;
  gap: 5px;
  min-height: 62px;
  padding: 10px 12px;
  background: #f8fbfa;
}
.radar-plan span {
  color: #8ba09f;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.6px;
}
.radar-plan strong {
  color: #315158;
  font-size: 12px;
}
.radar-plan-footer {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  color: #829795;
  font-size: 10px;
}
.radar-plan-footer > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.radar-plan-footer button {
  padding: 0;
  border: 0;
  color: #287f78;
  background: transparent;
  cursor: pointer;
  font-size: 11px;
  font-weight: 700;
}
.radar-empty {
  max-width: 630px;
  margin: 0;
  padding: 15px 0 8px;
  color: #718096;
  font-size: 13px;
  line-height: 1.8;
}
.change-list {
  position: relative;
  display: grid;
  gap: 0;
  border-top: 1px solid #dce6e2;
}
.change-item {
  position: relative;
  padding: 28px 4px 28px 28px;
  border-bottom: 1px solid #e6ece9;
  transition: background 0.2s ease;
}
.change-item::before {
  position: absolute;
  top: 35px;
  left: 0;
  width: 9px;
  height: 9px;
  border: 2px solid #5b9c92;
  border-radius: 50%;
  background: #fff;
  content: '';
}
.change-item:hover {
  background: #fbfcfa;
}
.change-topline,
.detail-heading,
.change-item footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #7d899c;
  font-size: 12px;
}
.change-context,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}
.change-item h2,
.change-detail h2 {
  margin: 14px 0 9px;
  color: #243047;
  font-size: 21px;
  line-height: 1.45;
}
.change-item p,
.change-detail p {
  margin: 0;
  color: #617086;
  line-height: 1.75;
}
.change-item footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #edf0f0;
}
.change-status {
  color: #8a6717;
  font-size: 11px;
  font-weight: 700;
}
.change-status.verified {
  color: #176b65;
}
.change-status.conflicted {
  color: #aa3c3c;
}
.change-detail section {
  margin-top: 24px;
}
.change-detail h3 {
  margin: 0 0 8px;
  color: #243047;
  font-size: 14px;
}
.change-pulse {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 15px 0;
  border-top: 1px solid #e5ece9;
  border-bottom: 1px solid #e5ece9;
}
.change-pulse div {
  display: grid;
  gap: 5px;
}
.change-pulse span {
  color: #829092;
  font-size: 11px;
}
.change-pulse strong {
  color: #27434b;
  font-size: 13px;
  line-height: 1.45;
}
.tide-history {
  padding-left: 14px;
  border-left: 2px solid #9ccbc4;
}
.tide-history .el-button {
  margin-top: 7px;
  padding: 0;
}
.evidence-item {
  margin-top: 10px;
  padding: 12px;
  border-left: 3px solid #176b65;
  background: #f7faf9;
}
.evidence-item a {
  color: #176b65;
  font-weight: 700;
  text-decoration: none;
}
.evidence-item p {
  margin-top: 6px;
  font-size: 13px;
}
.muted {
  color: #9aa4b2 !important;
}
.stance-section h3,
.observation-section h3 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.stance-section h3 span,
.observation-section h3 span {
  color: #7d899c;
  font-size: 12px;
  font-weight: 400;
}
.stance-options {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
  margin-top: 12px;
}
.stance-options button {
  display: grid;
  gap: 6px;
  min-height: 65px;
  padding: 10px 8px;
  border: 1px solid #dfe9e6;
  border-radius: 7px;
  color: #617086;
  background: #fbfcfb;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}
.stance-options button:hover {
  border-color: #8bbeb6;
  transform: translateY(-2px);
}
.stance-options button.active {
  border-color: #398f85;
  color: #176b65;
  background: #eaf6f2;
  box-shadow: 0 7px 18px rgba(23, 107, 101, 0.08);
}
.stance-options strong {
  font-size: 11px;
}
.stance-options span {
  font-size: 13px;
  font-weight: 700;
}
.stance-options small {
  font-size: 9px;
  font-weight: 400;
}
.observation-timeline {
  position: relative;
  display: grid;
  gap: 0;
  margin-top: 13px;
  padding-left: 10px;
}
.observation-timeline::before {
  position: absolute;
  top: 5px;
  bottom: 12px;
  left: 13px;
  width: 1px;
  background: #b8d8d2;
  content: '';
}
.observation-timeline article {
  position: relative;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  gap: 12px;
  padding: 0 0 15px;
}
.observation-timeline article > i {
  z-index: 1;
  width: 8px;
  height: 8px;
  margin-top: 4px;
  border: 2px solid #f8fbfa;
  border-radius: 50%;
  background: #4a9e94;
  box-shadow: 0 0 0 3px rgba(74, 158, 148, 0.14);
}
.observation-timeline article > div {
  display: grid;
  gap: 4px;
}
.observation-timeline strong {
  color: #347d75;
  font-size: 10px;
}
.observation-timeline p {
  color: #536c72;
  font-size: 12px;
}
.observation-timeline small {
  color: #93a3a4;
  font-size: 9px;
}
.discussion-section h3 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.discussion-section h3 span {
  color: #7d899c;
  font-size: 12px;
  font-weight: 400;
}
.discussion-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}
.discussion-actions .el-select {
  flex: 1;
}
.discussion-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}
.discussion-list article {
  padding: 13px;
  border-left: 3px solid #9ccbc4;
  background: #f7faf9;
}
.discussion-list article > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #7d899c;
  font-size: 12px;
}
.discussion-list strong {
  color: #243047;
}
.discussion-list p {
  margin-top: 7px;
}
.discussion-list small {
  display: block;
  margin-top: 7px;
  color: #176b65;
  font-size: 11px;
}
.submission-section h3 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.submission-section h3 span {
  color: #7d899c;
  font-size: 12px;
  font-weight: 400;
}
.submission-excerpt {
  margin: 10px 0;
}
.research-panel {
  padding: 22px;
  border: 1px solid #e5eaf1;
  background: #fff;
}
.research-form {
  display: flex;
  gap: 10px;
}
.research-form .el-input {
  flex: 1;
}
.research-boundary {
  margin: 12px 0 0;
  color: #7d899c;
  font-size: 12px;
  line-height: 1.6;
}
.research-results {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}
.research-results article {
  padding: 18px;
  border: 1px solid #e5eaf1;
  background: #fbfcfe;
}
.result-meta,
.research-results footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #7d899c;
  font-size: 12px;
}
.research-results h2 {
  margin: 12px 0 8px;
  color: #243047;
  font-size: 18px;
}
.research-results p {
  margin: 0;
  color: #617086;
  line-height: 1.75;
  white-space: pre-line;
}
.research-results footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #edf0f4;
}
.research-results a {
  color: #176b65;
  font-weight: 700;
  text-decoration: none;
}
.research-empty {
  margin-top: 22px;
  padding: 18px;
  color: #718096;
  background: #f7faf9;
  line-height: 1.7;
}
.insight-list {
  display: grid;
  gap: 12px;
}
.insight-item {
  padding: 22px;
  border: 1px solid #e5eaf1;
  background: #fff;
}
.insight-topline,
.insight-item footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #7d899c;
  font-size: 12px;
}
.relevance {
  padding: 3px 7px;
  color: #176b65;
  background: #e8f5f1;
  font-size: 11px;
  font-weight: 700;
}
.insight-item h2 {
  margin: 14px 0 9px;
  color: #243047;
  font-size: 20px;
}
.insight-item > p {
  margin: 0;
  color: #617086;
  line-height: 1.75;
}
.why {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  border-left: 3px solid #176b65;
  background: #f7faf9;
  color: #526078;
  font-size: 13px;
}
.why strong {
  color: #243047;
}
.why span {
  padding: 3px 7px;
  background: #e8f5f1;
  color: #176b65;
  font-size: 12px;
}
.insight-item footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #edf0f4;
}
.feedback {
  display: flex;
  gap: 3px;
}
.feedback .el-button {
  margin: 0;
}
.briefing-detail h2 {
  margin: 0 0 18px;
  color: #243047;
  font-size: 20px;
}
.briefing-detail article {
  padding: 16px 0;
  border-top: 1px solid #edf0f4;
}
.briefing-detail article > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #243047;
}
.briefing-detail article span {
  color: #176b65;
  font-size: 12px;
}
.briefing-detail p {
  margin: 8px 0 0;
  color: #617086;
  line-height: 1.7;
}
.me-center {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.8fr);
  gap: 36px;
}
.me-profile {
  display: flex;
  align-items: center;
  gap: 17px;
  min-height: 128px;
  padding: 20px 0;
  border-top: 1px solid #dce6e2;
  border-bottom: 1px solid #dce6e2;
}
.me-mark {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  flex: none;
  border-radius: 18px 18px 18px 5px;
  color: #fff;
  background: linear-gradient(145deg, #24948b, #285e75);
  box-shadow: 0 12px 24px rgba(30, 105, 107, 0.18);
  font-size: 22px;
  font-weight: 800;
}
.me-profile > div {
  min-width: 0;
}
.me-profile > div > span {
  color: #277f78;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
}
.me-profile h2 {
  margin: 5px 0 4px;
  color: #29464d;
  font-size: 22px;
}
.me-profile p {
  overflow: hidden;
  margin: 0;
  color: #7a9092;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.me-stats {
  grid-column: 2;
  grid-row: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  min-height: 128px;
  border-top: 1px solid #dce6e2;
  border-bottom: 1px solid #dce6e2;
}
.me-stats button {
  display: grid;
  align-content: center;
  gap: 6px;
  padding: 12px;
  border: 0;
  border-right: 1px solid #e2ebe8;
  color: #7d9293;
  background: transparent;
  cursor: pointer;
}
.me-stats button:last-child {
  border-right: 0;
}
.me-stats button:hover {
  background: #f5faf8;
}
.me-stats strong {
  color: #257c75;
  font-size: 25px;
}
.me-stats span {
  font-size: 11px;
}
.me-actions {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid #dce6e2;
  border-bottom: 1px solid #dce6e2;
}
.me-actions button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 18px;
  gap: 7px 12px;
  min-height: 110px;
  padding: 20px;
  border: 0;
  border-right: 1px solid #e2ebe8;
  color: #74898c;
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.me-actions button:last-child {
  border-right: 0;
}
.me-actions button:hover {
  background: #f7fbfa;
}
.me-actions strong {
  color: #2e4b51;
  font-size: 14px;
}
.me-actions span {
  grid-column: 1;
  font-size: 11px;
  line-height: 1.6;
}
.me-actions i {
  grid-column: 2;
  grid-row: 1 / 3;
  align-self: center;
  color: #4a938c;
  font-size: 17px;
  font-style: normal;
}
.plan-intro {
  margin: -4px 0 20px;
  padding: 11px 13px;
  border-left: 3px solid #47a296;
  color: #58757a;
  background: #f2faf7;
  font-size: 12px;
  line-height: 1.7;
}
.plan-form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
}
.plan-form :deep(.el-form-item__label) {
  color: #2d4c52;
  font-weight: 700;
}
.plan-form :deep(.el-form-item__content > small) {
  display: block;
  width: 100%;
  margin-top: 6px;
  color: #8a9d9d;
  font-size: 10px;
  line-height: 1.5;
}
.choice-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  width: 100%;
}
.source-choice {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.choice-grid :deep(.el-checkbox) {
  margin-right: 10px;
}
.choice-grid :deep(.el-checkbox__label) {
  padding-left: 5px;
  font-size: 12px;
}
@keyframes current-flow {
  from {
    transform: translate3d(-8px, 5px, 0) scale(0.96);
  }
  to {
    transform: translate3d(14px, -7px, 0) scale(1.04);
  }
}
@keyframes radar-ping {
  0% {
    box-shadow:
      0 0 0 0 rgba(240, 121, 103, 0.38),
      0 0 16px rgba(240, 121, 103, 0.3);
  }
  80%,
  100% {
    box-shadow:
      0 0 0 19px rgba(240, 121, 103, 0),
      0 0 2px rgba(240, 121, 103, 0);
  }
}
@media (max-width: 900px) {
  .radar-item {
    grid-template-columns: 1fr;
  }
  .radar-plan-footer {
    grid-column: auto;
  }
  .choice-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .me-center {
    grid-template-columns: 1fr;
  }
  .me-stats {
    grid-column: 1;
    grid-row: auto;
  }
  .me-actions {
    grid-template-columns: 1fr;
  }
  .me-actions button {
    border-right: 0;
    border-bottom: 1px solid #e2ebe8;
  }
  .me-actions button:last-child {
    border-bottom: 0;
  }
}
@media (max-width: 760px) {
  .workbench {
    padding: 30px 0 108px;
  }
  .heading {
    align-items: flex-start;
    flex-direction: column;
    min-height: 168px;
  }
  .heading-current {
    right: -220px;
    opacity: 0.7;
  }
  .tide-summary {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    margin: 20px 0 26px;
  }
  .tide-markers {
    gap: 13px;
  }
  .empty-state {
    align-items: flex-start;
    flex-direction: column;
  }
  .radar-home {
    display: block;
  }
  .radar-side {
    margin-top: 28px;
    padding: 22px 0;
    border-top: 1px solid #dce6e2;
    border-left: 0;
  }
  .radar-list {
    margin-top: 8px;
  }
  .radar-list > .radar-section-heading {
    align-items: flex-start;
  }
  .radar-plan-footer {
    grid-template-columns: 1fr;
  }
  .plan-form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .choice-grid,
  .source-choice {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .research-form,
  .discussion-actions {
    flex-direction: column;
  }
  .result-meta,
  .research-results footer,
  .insight-item footer,
  .change-item footer {
    align-items: flex-start;
    flex-direction: column;
  }
  .change-item {
    padding-left: 24px;
  }
  .change-topline {
    align-items: flex-start;
  }
  .change-context {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .change-pulse {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .stance-options {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .stance-options button:last-child {
    grid-column: 1 / -1;
  }
  .topic-list {
    justify-content: flex-start;
  }
  .me-stats strong {
    font-size: 21px;
  }
  h1 {
    font-size: 32px;
  }
}
/* Unified editorial community skin */
.workbench {
  padding: 42px 0 88px;
  color: var(--zt-text);
}
.heading {
  min-height: 142px;
  padding: 24px 0 28px;
  border-color: var(--zt-border);
}
.heading-current {
  display: none;
}
.eyebrow {
  color: var(--zt-accent);
  font-size: 10px;
  letter-spacing: 1.35px;
}
h1 {
  margin: 10px 0 8px;
  color: var(--zt-text);
  font-size: 38px;
  font-weight: 780;
  letter-spacing: -0.03em;
}
.heading p {
  color: var(--zt-text-2);
}
.tide-summary {
  margin: 22px 0 24px;
  padding: 14px 16px;
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-sm);
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-sm);
}
.tide-message {
  color: var(--zt-text-2);
}
.tide-markers {
  color: var(--zt-text-3);
}
.tide-markers b {
  color: var(--zt-primary);
}
.empty-state {
  border-color: var(--zt-border);
}
.empty-mark {
  border-color: var(--zt-primary);
  border-radius: var(--zt-radius-md);
  color: var(--zt-primary);
  animation: none;
}
.empty-state h2 {
  color: var(--zt-text);
}
.empty-state p {
  color: var(--zt-text-2);
}
.radar-home {
  gap: 40px;
}
.radar-section-heading,
.radar-side,
.radar-list,
.radar-insight,
.radar-item,
.watch-list button {
  border-color: var(--zt-border-soft);
}
.radar-section-heading span,
.radar-status {
  color: var(--zt-accent);
}
.radar-section-heading h2,
.radar-insight h3,
.radar-quiet strong,
.radar-identity h3 {
  color: var(--zt-text);
}
.radar-insight p,
.radar-quiet,
.radar-side p,
.radar-identity > p,
.radar-empty {
  color: var(--zt-text-2);
}
.radar-insight:hover h3,
.watch-list button:hover {
  color: var(--zt-primary);
}
.watch-list button {
  color: var(--zt-text-2);
}
.topic-list button {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-sm);
  color: var(--zt-primary);
  background: var(--zt-surface);
}
.topic-list button:hover {
  border-color: #9caaa4;
  color: var(--zt-primary-hover);
  background: var(--zt-primary-soft);
}
.radar-plan {
  border-color: var(--zt-border);
  background: var(--zt-border);
}
.radar-plan div {
  background: var(--zt-surface);
}
.radar-plan span,
.radar-plan-footer {
  color: var(--zt-text-3);
}
.radar-plan strong {
  color: var(--zt-text);
}
.radar-plan-footer button {
  color: var(--zt-primary);
}
.change-list {
  border-color: var(--zt-border);
}
.change-item {
  border-color: var(--zt-border-soft);
}
.change-item::before {
  border-color: var(--zt-primary);
  background: var(--zt-bg);
}
.change-item:hover {
  background: var(--zt-surface-hover);
}
.change-topline,
.detail-heading,
.change-item footer {
  color: var(--zt-text-3);
}
.change-item h2,
.change-detail h2,
.change-detail h3 {
  color: var(--zt-text);
}
.change-item p,
.change-detail p {
  color: var(--zt-text-2);
}
.change-item footer {
  border-color: var(--zt-border-soft);
}
.change-status.verified {
  color: var(--zt-primary);
}
.change-pulse {
  border-color: var(--zt-border-soft);
}
.change-pulse span {
  color: var(--zt-text-3);
}
.change-pulse strong {
  color: var(--zt-text);
}
.tide-history {
  border-color: #9eafa8;
}
.evidence-item,
.discussion-list article {
  border-color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.evidence-item a,
.discussion-list small {
  color: var(--zt-primary);
}
.stance-section h3 span,
.observation-section h3 span,
.discussion-section h3 span,
.submission-section h3 span {
  color: var(--zt-text-3);
}
.stance-options button {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-sm);
  color: var(--zt-text-2);
  background: var(--zt-surface);
}
.stance-options button:hover {
  border-color: #93a59d;
  transform: none;
}
.stance-options button.active {
  border-color: var(--zt-primary);
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
  box-shadow: none;
}
.observation-timeline::before {
  background: #b7c3be;
}
.observation-timeline article > i {
  background: var(--zt-primary);
  box-shadow: 0 0 0 3px var(--zt-primary-soft);
}
.observation-timeline strong {
  color: var(--zt-primary);
}
.observation-timeline p {
  color: var(--zt-text-2);
}
.observation-timeline small {
  color: var(--zt-text-3);
}
.insight-item,
.research-panel {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-sm);
  background: var(--zt-surface);
}
.insight-topline,
.insight-item footer {
  color: var(--zt-text-3);
}
.relevance {
  border-radius: var(--zt-radius-xs);
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.insight-item h2,
.briefing-detail h2 {
  color: var(--zt-text);
}
.insight-item > p,
.briefing-detail p {
  color: var(--zt-text-2);
}
.why {
  border-color: var(--zt-primary);
  background: var(--zt-primary-soft);
  color: var(--zt-text-2);
}
.why strong {
  color: var(--zt-text);
}
.why span {
  color: var(--zt-primary);
  background: var(--zt-surface);
}
.insight-item footer,
.briefing-detail article {
  border-color: var(--zt-border-soft);
}
.plan-intro {
  border-color: var(--zt-primary);
  color: var(--zt-text-2);
  background: var(--zt-primary-soft);
}
.plan-form :deep(.el-form-item__label) {
  color: var(--zt-text);
}
.plan-form :deep(.el-form-item__content > small) {
  color: var(--zt-text-3);
}
@media (max-width: 760px) {
  .workbench {
    padding: 24px 0 78px;
  }
  .heading {
    min-height: 136px;
  }
  h1 {
    font-size: 31px;
  }
  .tide-summary {
    margin-top: 16px;
  }
}
</style>
