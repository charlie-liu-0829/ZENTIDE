<template>
  <main class="assistant-page">
    <aside class="context-panel">
      <router-link class="back-link" :to="backTarget"
        >← {{ isPostMode ? '返回帖子' : '返回现场' }}</router-link
      >
      <div class="assistant-identity">
        <span>潮</span>
        <div>
          <small>社区小助手</small><strong>{{ isPostMode ? '问问这篇帖子' : '问问整个现场' }}</strong>
        </div>
      </div>
      <section class="scene-context">
        <small>当前兴趣现场</small>
        <h1>{{ hub?.name || '正在载入…' }}</h1>
        <p>{{ hub?.description || '从现场帖子和评论中整理信息。' }}</p>
      </section>
      <section class="scope-section">
        <small>回答范围</small>
        <div class="scope-badge">
          <span>{{ isPostMode ? '当前帖子' : '整个现场' }}</span>
          <em>{{ isPostMode ? '帖子及其评论' : '帖子与评论' }}</em>
        </div>
      </section>
      <p class="source-note">回答仅基于当前现场中你有权查看的内容，并附上可点击的相关依据。</p>
    </aside>

    <section class="chat-panel">
      <header class="chat-header">
        <div>
          <span class="online-dot"></span>
          <div><strong>社区小助手</strong><small>基于现场内容回答</small></div>
        </div>
        <div class="chat-actions">
          <button type="button" @click="historyVisible = !historyVisible">历史会话</button>
          <button type="button" @click="newConversation">新对话</button>
        </div>
      </header>

      <aside v-if="historyVisible" class="history-panel">
        <header>
          <strong>历史会话</strong><button type="button" @click="historyVisible = false">×</button>
        </header>
        <button
          v-for="item in historyItems"
          :key="item.id"
          type="button"
          class="history-item"
          @click="restoreHistory(item)"
        >
          <strong>{{ item.title }}</strong
          ><small>{{ formatHistoryTime(item.updatedAt) }}</small>
          <i @click.stop="removeHistory(item.id)">删除</i>
        </button>
        <p v-if="!historyItems.length" class="history-empty">还没有历史会话</p>
      </aside>

      <div ref="messageList" class="message-list">
        <div v-if="contextError" class="context-error">
          <strong>{{ isPostMode ? '暂时无法进入这篇帖子' : '暂时无法进入这个现场' }}</strong>
          <p>{{ contextError }}</p>
          <router-link :to="backTarget">返回社区处理</router-link>
        </div>
        <template v-else>
          <AgentMessage
            v-for="message in messages"
            :key="message.id"
            :role="message.role"
            :content="message.content"
            :blocks="message.blocks || []"
          />
          <article v-if="sending" class="thinking-row">
            <span class="thinking-avatar">潮</span>
            <div>
              <i></i><i></i><i></i
              ><em>{{ slowResponse ? '模型响应较慢，仍在整理…' : '正在查阅现场内容' }}</em>
            </div>
            <button v-if="slowResponse" type="button" class="cancel-request" @click="cancelRequest">
              取消
            </button>
          </article>
          <div v-if="messages.length === 1 && !sending" class="suggestions">
            <span>你可以这样问</span>
            <button
              v-for="suggestion in suggestions"
              :key="suggestion"
              type="button"
              @click="send(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>
        </template>
      </div>

      <form class="composer" @submit.prevent="send()">
        <textarea
          v-model="draft"
          :disabled="Boolean(contextError) || sending"
          maxlength="2000"
          rows="2"
          aria-label="向社区小助手提问"
          :placeholder="isPostMode ? '问问这篇帖子里的观点和评论…' : '问问这个现场正在讨论什么…'"
          @input="handleMentionInput"
          @keydown.esc.prevent="mentionVisible = false"
          @keydown.enter.exact.prevent="send()"
        ></textarea>
        <div v-if="mentionVisible" class="mention-menu">
          <div class="mention-menu-header">
            <div>
              <span class="mention-symbol">@</span>
              <div>
                <strong>关联一篇帖子</strong>
                <small>从当前现场中选择，助手会优先参考它</small>
              </div>
            </div>
            <kbd>Esc</kbd>
          </div>
          <div v-if="mentionPosts.length" class="mention-results">
            <button
              v-for="post in mentionPosts"
              :key="post.postId"
              type="button"
              @click="chooseMention(post)"
            >
              <span class="mention-post-mark">↗</span>
              <span class="mention-post-copy">
                <strong>{{ post.title || '未命名帖子' }}</strong>
                <span> {{ post.authorLabel || '社区成员' }} · {{ post.commentCount || 0 }} 条评论 </span>
              </span>
              <span class="mention-arrow" aria-hidden="true">→</span>
            </button>
          </div>
          <div v-else class="mention-empty">
            <span class="mention-empty-icon">⌕</span>
            <div><strong>没有找到匹配的帖子</strong><small>试试更短的关键词，或直接输入问题</small></div>
          </div>
          <small class="mention-hint">输入 <b>@</b> 可随时重新选择</small>
        </div>
        <div v-if="selectedPosts.length" class="mention-chips">
          <div v-for="post in selectedPosts" :key="post.postId" class="mention-chip">
            <span class="chip-prefix"><i>@</i></span>
            <strong>{{ post.title || '当前帖子' }}</strong>
            <button type="button" aria-label="取消帖子提问" @click="removeMention(post.postId)">×</button>
          </div>
        </div>
        <div class="composer-footer">
          <span>{{ isPostMode ? '只查看当前帖子及其评论' : '查看整个现场' }}</span>
          <button
            type="submit"
            class="send-button"
            :disabled="!draft.trim() || sending || Boolean(contextError)"
            aria-label="发送问题"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m4 12 16-8-5.5 16-3-6.5L4 12Z" />
              <path d="m11.5 13.5 4-4" />
            </svg>
          </button>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AgentMessage from '@/components/agent/AgentMessage.vue'

const { proxy } = getCurrentInstance()
const route = useRoute()
const hubId = computed(() => Number(route.params.hubId))
const isPostMode = computed(() => route.name === 'community-post-assistant')
const postId = computed(() => {
  const value = Number(route.params.postId)
  return Number.isInteger(value) && value > 0 ? value : null
})
const selectedPosts = ref([])
const mode = computed(() => (isPostMode.value || selectedPosts.value.length ? 'post' : 'scene'))
const activePostId = computed(() =>
  isPostMode.value ? postId.value : selectedPosts.value[0]?.postId || null,
)
const activePostIds = computed(() =>
  isPostMode.value ? (postId.value ? [postId.value] : []) : selectedPosts.value.map((post) => post.postId),
)
const backTarget = computed(() =>
  isPostMode.value && postId.value
    ? { name: 'community-post', params: { postId: postId.value } }
    : { path: '/community', query: { hubId: hubId.value } },
)
const hub = ref(null)
const draft = ref('')
const sending = ref(false)
const slowResponse = ref(false)
const requestController = ref(null)
let slowResponseTimer = null
const contextError = ref('')
const messageList = ref(null)
const conversationId = ref('')
const messages = ref([])
const historyVisible = ref(false)
const historyItems = ref([])
const mentionVisible = ref(false)
const mentionPosts = ref([])
const suggestions = computed(() =>
  isPostMode.value || selectedPosts.value.length
    ? ['帮我总结这篇帖子的主要观点', '评论区有哪些不同意见？', '这篇帖子里有哪些值得继续追问的地方？']
    : ['这个现场最近主要在讨论什么？', '大家有哪些一致观点和不同看法？', '帮我找几篇值得继续阅读的帖子'],
)

const createConversationId = () =>
  typeof crypto?.randomUUID === 'function'
    ? crypto.randomUUID().replaceAll('-', '')
    : `${Date.now()}${Math.random().toString(36).slice(2)}`
// v3 intentionally drops conversations created with the old prompt, which
// could contain internal route identifiers in assistant text.
const storageKey = computed(() => `zentide-agent:v3:${hubId.value}:${postId.value || 'scene'}`)
const historyKey = computed(() => `zentide-agent-history:v2:${hubId.value}`)
const greeting = () => ({
  id: `greeting-${Date.now()}`,
  role: 'assistant',
  content: isPostMode.value
    ? '我会聚焦当前帖子及其评论回答。你想先了解什么？'
    : `我会从「${hub.value?.name || '这个现场'}」的帖子和评论中寻找依据。你想了解什么？`,
})
const persist = () => {
  sessionStorage.setItem(
    storageKey.value,
    JSON.stringify({ conversationId: conversationId.value, messages: messages.value.slice(-30) }),
  )
  const firstQuestion = messages.value.find((message) => message.role === 'user')?.content
  const existing = historyItems.value.find((item) => item.id === conversationId.value)
  const item = {
    id: conversationId.value,
    title: existing?.title || firstQuestion?.slice(0, 28) || '新对话',
    updatedAt: Date.now(),
    mode: mode.value,
    postId: activePostId.value,
    posts: selectedPosts.value,
    messages: messages.value.slice(-30),
  }
  historyItems.value = [item, ...historyItems.value.filter((entry) => entry.id !== item.id)].slice(0, 30)
  localStorage.setItem(historyKey.value, JSON.stringify(historyItems.value))
}
const loadHistory = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(historyKey.value) || '[]')
    historyItems.value = Array.isArray(saved) ? saved : []
  } catch {
    historyItems.value = []
  }
}
const formatHistoryTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : ''
const restoreHistory = (item) => {
  conversationId.value = item.id
  if (!isPostMode.value)
    selectedPosts.value = Array.isArray(item.posts) ? item.posts : item.post ? [item.post] : []
  messages.value = Array.isArray(item.messages) ? item.messages : [greeting()]
  historyVisible.value = false
  persist()
  scrollToBottom()
}
const removeHistory = (id) => {
  historyItems.value = historyItems.value.filter((item) => item.id !== id)
  localStorage.setItem(historyKey.value, JSON.stringify(historyItems.value))
}
const restore = () => {
  try {
    const saved = JSON.parse(sessionStorage.getItem(storageKey.value) || '{}')
    if (saved.conversationId && Array.isArray(saved.messages) && saved.messages.length) {
      conversationId.value = saved.conversationId
      messages.value = saved.messages.map((message) => ({
        ...message,
        content:
          message.role === 'assistant'
            ? String(message.content || '')
                .replace(/我会聚焦帖子\s*#?\d+\s*及其评论回答/g, '我会聚焦当前帖子及其评论回答')
                .replace(/帖子\s*#\d+/g, '当前帖子')
                .replace(/帖子\s*\d+\s*及其评论/g, '当前帖子及其评论')
            : message.content,
      }))
      return
    }
  } catch {
    sessionStorage.removeItem(storageKey.value)
  }
  newConversation(false)
}
const newConversation = (announce = true) => {
  conversationId.value = createConversationId()
  messages.value = [greeting()]
  if (!isPostMode.value) selectedPosts.value = []
  mentionVisible.value = false
  draft.value = ''
  persist()
  if (announce) proxy.Message.success('已开始新对话')
}
const handleMentionInput = async () => {
  if (isPostMode.value) return
  const match = draft.value.match(/(?:^|\s)@([^\s]*)$/)
  if (!match) {
    mentionVisible.value = false
    return
  }
  mentionVisible.value = true
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityFeed,
    params: { hubId: hubId.value, limit: 30 },
    showLoading: false,
    showError: false,
  })
  const query = match[1].toLowerCase()
  mentionPosts.value = (result?.data || [])
    .filter(
      (post) =>
        !query ||
        String(post.title || '')
          .toLowerCase()
          .includes(query),
    )
    .slice(0, 8)
}
const chooseMention = (post) => {
  if (!selectedPosts.value.some((item) => item.postId === post.postId)) selectedPosts.value.push(post)
  draft.value = draft.value.replace(/(?:^|\s)@([^\s]*)$/, ' ').trimStart()
  mentionVisible.value = false
}
const removeMention = (postIdToRemove) => {
  selectedPosts.value = selectedPosts.value.filter((post) => post.postId !== postIdToRemove)
}
const scrollToBottom = async () => {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}
const loadContext = async () => {
  if (!hubId.value) {
    contextError.value = '兴趣现场参数不正确。'
    return
  }
  if (isPostMode.value && !postId.value) {
    contextError.value = '帖子参数不正确。'
    return
  }
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubs,
    params: {},
    showLoading: false,
  })
  hub.value = (result?.data || []).find((item) => Number(item.hubId) === hubId.value) || null
  if (!hub.value || !hub.value.joined) {
    contextError.value = '请先加入这个兴趣现场，再使用社区小助手。'
    return
  }
  loadHistory()
  restore()
}
const send = async (suggestion) => {
  const question = String(suggestion || draft.value).trim()
  if (!question || sending.value || contextError.value) return
  messages.value.push({ id: `user-${Date.now()}`, role: 'user', content: question })
  draft.value = ''
  sending.value = true
  slowResponse.value = false
  requestController.value = new AbortController()
  slowResponseTimer = window.setTimeout(() => (slowResponse.value = true), 8000)
  persist()
  await scrollToBottom()
  let streamAssistant = null
  try {
    const apiOrigin = import.meta.env.PROD ? import.meta.env.VITE_DOMAIN || '' : ''
    const response = await fetch(`${apiOrigin}/api/zentide/v1/agent/chat/stream`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', token: localStorage.getItem('token') || '' },
      body: JSON.stringify({
        sceneId: hubId.value,
        currentPostId: activePostId.value,
        currentPostIds: activePostIds.value,
        mode: mode.value,
        question,
        conversationId: conversationId.value,
      }),
      signal: requestController.value.signal,
    })
    if (!response.ok) {
      let detail = ''
      try {
        const body = await response.json()
        detail = body?.info || body?.detail || ''
      } catch {
        // The proxy may return an empty body for a closed stream.
      }
      throw new Error(detail || '社区小助手暂时无法回答，请稍后再试')
    }
    const reader = response.body?.getReader()
    if (!reader) throw new Error('当前浏览器不支持流式回答')
    const decoder = new TextDecoder()
    let buffer = ''
    let assistant = null
    streamAssistant = assistant
    let completed = false
    const ensureAssistant = () => {
      if (!assistant) {
        assistant = { id: `assistant-${Date.now()}`, role: 'assistant', content: '', blocks: [] }
        streamAssistant = assistant
        messages.value.push(assistant)
      }
      return assistant
    }
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      for (const eventText of events) {
        const dataLine = eventText.split('\n').find((line) => line.startsWith('data:'))
        if (!dataLine) continue
        const event = JSON.parse(dataLine.slice(5).trim())
        if (event.event === 'start') conversationId.value = event.conversation_id || conversationId.value
        if (event.event === 'delta') {
          ensureAssistant().content += event.text || ''
          messages.value = [...messages.value]
        }
        if (event.event === 'done') {
          ensureAssistant()
          conversationId.value = event.conversation_id || conversationId.value
          assistant.content = event.answer || assistant.content
          assistant.blocks = event.blocks || []
          completed = true
          messages.value = [...messages.value]
        }
        if (event.event === 'error') throw new Error(event.message || '社区小助手暂时无法回答')
      }
    }
    if (!completed && !requestController.value.signal.aborted) {
      throw new Error(
        assistant?.content ? '回答连接已中断，已保留已生成内容' : '社区小助手暂时无法回答，请稍后再试',
      )
    }
    persist()
  } catch (error) {
    if (error?.name !== 'AbortError' && !streamAssistant?.content) {
      proxy.Message.error(error?.message || '社区小助手暂时无法回答')
    }
    persist()
  } finally {
    sending.value = false
    slowResponse.value = false
    requestController.value = null
    window.clearTimeout(slowResponseTimer)
    await scrollToBottom()
  }
}

const cancelRequest = () => requestController.value?.abort()

onMounted(loadContext)
</script>

<style scoped>
.assistant-page {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  height: calc(100vh - 76px);
  min-height: 620px;
  padding: 22px 0 18px;
  gap: 18px;
}
.context-panel,
.chat-panel {
  border: 1px solid var(--zt-border);
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-sm);
}
.context-panel {
  padding: 20px;
  border-radius: 10px;
}
.back-link {
  display: inline-block;
  margin-bottom: 26px;
  color: var(--zt-text-3);
  font-size: 12px;
  text-decoration: none;
}
.back-link:hover {
  color: var(--zt-primary);
}
.assistant-identity {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--zt-border-soft);
}
.assistant-identity > span {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 12px 12px 12px 4px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-size: 12px;
  font-weight: 850;
}
.assistant-identity div {
  display: flex;
  flex-direction: column;
}
.assistant-identity small,
.scene-context small,
.scope-section > small {
  color: var(--zt-text-3);
  font-size: 10px;
  letter-spacing: 0.08em;
}
.assistant-identity strong {
  font-size: 16px;
}
.scene-context {
  padding: 22px 0;
}
.scene-context h1 {
  margin: 5px 0 8px;
  font-size: 18px;
  line-height: 1.35;
}
.scene-context p,
.source-note {
  margin: 0;
  color: var(--zt-text-2);
  font-size: 12px;
  line-height: 1.7;
}
.scope-section {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding-top: 18px;
  border-top: 1px solid var(--zt-border-soft);
}
.scope-section > small {
  margin-bottom: 3px;
}
.scope-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 10px;
  border: 1px solid #c9d9d3;
  border-radius: 6px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  text-align: left;
}
.scope-section span {
  font-size: 12px;
  font-weight: 680;
}
.scope-section em {
  color: var(--zt-text-3);
  font-size: 10px;
  font-style: normal;
}
.source-note {
  margin-top: 22px;
  padding: 12px;
  border-left: 2px solid var(--zt-tide);
  background: var(--zt-surface-hover);
}
.chat-panel {
  position: relative;
  display: flex;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
  border-radius: 10px;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid var(--zt-border-soft);
}
.chat-actions {
  display: flex;
  gap: 8px;
}
.chat-actions button,
.history-panel header button {
  border: 1px solid var(--zt-border);
  border-radius: 5px;
  padding: 7px 10px;
  color: var(--zt-text-2);
  background: var(--zt-surface);
  cursor: pointer;
}
.history-panel {
  position: absolute;
  z-index: 5;
  top: 74px;
  right: 18px;
  width: min(340px, calc(100% - 36px));
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--zt-border);
  border-radius: 8px;
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-md);
}
.history-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.history-item {
  position: relative;
  display: block;
  width: 100%;
  padding: 10px 42px 10px 8px;
  border: 0;
  border-bottom: 1px solid var(--zt-border-soft);
  color: var(--zt-text);
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.history-item strong,
.history-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-item small,
.history-item i,
.history-empty {
  color: var(--zt-text-3);
  font-size: 11px;
}
.history-item i {
  position: absolute;
  top: 12px;
  right: 6px;
  font-style: normal;
}
.history-empty {
  margin: 18px 0;
  text-align: center;
}
.mention-menu {
  position: absolute;
  right: 18px;
  bottom: 102px;
  left: 18px;
  z-index: 4;
  overflow: hidden;
  border: 1px solid #d6e0da;
  border-radius: 10px;
  background: var(--zt-surface);
  box-shadow:
    0 16px 42px rgb(29 54 45 / 14%),
    0 2px 8px rgb(29 54 45 / 7%);
}
.mention-menu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #e6ece9;
  background: #f8faf9;
}
.mention-menu-header > div {
  display: flex;
  align-items: center;
  gap: 9px;
}
.mention-menu-header > div > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.mention-menu-header strong {
  color: var(--zt-text);
  font-size: 12px;
  letter-spacing: 0.01em;
}
.mention-menu-header small {
  color: var(--zt-text-3);
  font-size: 10px;
}
.mention-symbol {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 6px;
  color: var(--zt-primary);
  background: #e4f0eb;
  font-size: 12px;
  font-weight: 800;
}
.mention-menu-header kbd {
  padding: 3px 6px;
  border: 1px solid #d5dfda;
  border-bottom-width: 2px;
  border-radius: 5px;
  color: var(--zt-text-3);
  background: #fff;
  font-family: inherit;
  font-size: 10px;
}
.mention-results {
  max-height: 224px;
  overflow: auto;
  padding: 4px;
}
.mention-results button {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
  padding: 8px;
  border: 0;
  border-radius: 6px;
  color: var(--zt-text);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}
.mention-results button:hover,
.mention-results button:focus-visible {
  outline: 0;
  background: #edf5f1;
  transform: none;
}
.mention-post-mark {
  display: grid;
  place-items: center;
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 6px;
  color: var(--zt-primary);
  background: #f0f6f3;
  font-size: 13px;
}
.mention-post-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}
.mention-post-copy strong {
  overflow: hidden;
  color: var(--zt-text);
  font-size: 12px;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mention-post-copy span {
  overflow: hidden;
  color: var(--zt-text-3);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mention-arrow {
  padding: 0 3px;
  color: #94aaa0;
  font-size: 14px;
  transition: color 0.15s ease;
}
.mention-results button:hover .mention-arrow {
  color: var(--zt-primary);
}
.mention-empty {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 13px;
  color: var(--zt-text-2);
}
.mention-empty-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: var(--zt-text-3);
  background: #edf2ef;
  font-size: 20px;
}
.mention-empty div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.mention-empty strong {
  font-size: 11px;
}
.mention-empty small,
.mention-hint {
  color: var(--zt-text-3);
  font-size: 10px;
}
.mention-hint {
  display: block;
  padding: 7px 12px 8px;
  border-top: 1px solid #edf1ef;
}
.mention-hint b {
  color: var(--zt-primary);
  font-weight: 750;
}
.mention-chip {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
  margin: 0 18px 8px;
  padding: 7px 8px 7px 9px;
  border: 1px solid #c9ded4;
  border-radius: 6px;
  color: var(--zt-primary);
  background: #eef7f3;
  font-size: 11px;
}
.mention-chip .chip-prefix {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
  color: #5c7c6d;
  font-size: 10px;
}
.mention-chip .chip-prefix i {
  color: var(--zt-primary);
  font-size: 13px;
  font-style: normal;
  font-weight: 800;
}
.mention-chip strong {
  min-width: 0;
  overflow: hidden;
  color: var(--zt-text);
  font-size: 11px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mention-chip button {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 21px;
  height: 21px;
  margin-left: auto;
  padding: 0;
  border: 0;
  border-radius: 5px;
  color: #6b8b7d;
  background: transparent;
  cursor: pointer;
}
.mention-chip button:hover {
  color: #b34f43;
  background: #fbeceb;
}
.chat-header > div {
  display: flex;
  align-items: center;
  gap: 9px;
}
.chat-header > div > div {
  display: flex;
  flex-direction: column;
}
.chat-header strong {
  font-size: 14px;
}
.chat-header small {
  color: var(--zt-text-3);
  font-size: 10px;
}
.online-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #43836a;
  box-shadow: 0 0 0 4px #e3eee8;
}
.chat-header button {
  padding: 6px 9px;
  border: 1px solid var(--zt-border);
  border-radius: 5px;
  color: var(--zt-text-2);
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}
.chat-header button:hover {
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
}
.message-list {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  flex-direction: column;
  gap: 16px;
  padding: 26px 28px;
  scroll-behavior: smooth;
  background: color-mix(in srgb, var(--zt-surface) 91%, var(--zt-bg));
}
.suggestions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  max-width: 780px;
  margin: 3px 0 0 42px;
}
.suggestions > span {
  grid-column: 1/-1;
  color: var(--zt-text-3);
  font-size: 10px;
}
.suggestions button {
  padding: 10px 11px;
  border: 1px solid var(--zt-border);
  border-radius: 7px;
  color: var(--zt-text-2);
  background: var(--zt-surface);
  font-size: 11px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
}
.suggestions button:hover {
  border-color: var(--zt-tide);
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
}
.thinking-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.thinking-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 10px 10px 10px 3px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-size: 10px;
  font-weight: 800;
}
.thinking-row > div {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--zt-text-3);
}
.cancel-request {
  margin-left: 4px;
  padding: 5px 9px;
  border: 1px solid #d8dfdb;
  border-radius: 5px;
  color: var(--zt-text-2);
  background: var(--zt-surface);
  font-size: 10px;
  cursor: pointer;
}
.cancel-request:hover {
  border-color: #c78e86;
  color: #a64e45;
  background: #fff8f7;
}
.thinking-row i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--zt-tide);
  animation: pulse 1.1s infinite;
}
.thinking-row i:nth-child(2) {
  animation-delay: 0.15s;
}
.thinking-row i:nth-child(3) {
  animation-delay: 0.3s;
}
.thinking-row em {
  margin-left: 5px;
  font-size: 10px;
  font-style: normal;
}
.composer {
  margin: 0;
  padding: 14px 18px 16px;
  border-top: 1px solid var(--zt-border-soft);
  background: var(--zt-surface);
}
.composer textarea {
  display: block;
  width: 100%;
  max-height: 130px;
  resize: none;
  padding: 0;
  border: 0;
  outline: 0;
  color: var(--zt-text);
  background: transparent;
  font-size: 14px;
  line-height: 1.6;
}
.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 9px;
}
.composer span {
  color: var(--zt-text-3);
  font-size: 10px;
}
.send-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: var(--zt-primary);
  cursor: pointer;
}
.composer button:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}
.composer svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}
.context-error {
  max-width: 520px;
  margin: auto;
  padding: 28px;
  border: 1px solid var(--zt-border);
  border-radius: 9px;
  background: var(--zt-surface);
  text-align: center;
}
.context-error strong {
  font-size: 17px;
}
.context-error p {
  color: var(--zt-text-2);
}
.context-error a {
  color: var(--zt-primary);
  font-weight: 680;
}
@keyframes pulse {
  0%,
  70%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  35% {
    opacity: 1;
    transform: translateY(-2px);
  }
}
@media (max-width: 820px) {
  .assistant-page {
    grid-template-columns: 1fr;
    height: auto;
    min-height: calc(100vh - 60px);
    padding: 12px 0;
  }
  .context-panel {
    padding: 14px;
  }
  .back-link {
    margin-bottom: 12px;
  }
  .assistant-identity,
  .source-note {
    display: none;
  }
  .scene-context {
    padding: 12px 0;
  }
  .scope-section {
    padding-top: 10px;
  }
  .chat-panel {
    min-height: 70vh;
  }
  .message-list {
    padding: 20px 14px;
  }
  .suggestions {
    grid-template-columns: 1fr;
    margin-left: 0;
  }
  .composer {
    padding: 12px;
  }
}
</style>
