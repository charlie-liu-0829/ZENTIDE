<template>
  <main class="messages-page" :class="{ 'has-active': activePeer }">
    <aside class="conversation-panel">
      <header>
        <div>
          <span>DIRECT MESSAGES</span>
          <h1>私信</h1>
        </div>
        <button type="button" aria-label="发起私聊" @click="newChatVisible = !newChatVisible">＋</button>
      </header>

      <div v-if="newChatVisible" class="new-chat">
        <input v-model="userQuery" maxlength="40" placeholder="搜索昵称或用户名" @keyup.enter="searchUsers" />
        <button type="button" @click="searchUsers">搜索</button>
        <div v-if="searching" class="search-note">正在查找社区成员…</div>
        <button
          v-for="user in userResults"
          :key="user.peerId"
          type="button"
          class="user-result"
          @click="selectPeer(user)"
        >
          <span class="avatar"
            ><img v-if="user.peerAvatar" :src="resourceUrl(user.peerAvatar)" alt="" /><i v-else>{{
              initial(user.peerName)
            }}</i></span
          >
          <b>{{ user.peerName || '社区成员' }}</b
          ><small>开始对话</small>
        </button>
        <div v-if="searched && !searching && !userResults.length" class="search-note">没有找到匹配用户。</div>
      </div>

      <div v-if="loadingConversations" class="conversation-state">正在加载会话…</div>
      <nav v-else-if="conversations.length" aria-label="私信会话">
        <button
          v-for="conversation in conversations"
          :key="conversation.peerId"
          type="button"
          :class="{ active: String(conversation.peerId) === String(activePeer?.peerId) }"
          @click="selectPeer(conversation)"
        >
          <span class="avatar"
            ><img v-if="conversation.peerAvatar" :src="resourceUrl(conversation.peerAvatar)" alt="" /><i
              v-else
              >{{ initial(conversation.peerName) }}</i
            ></span
          >
          <span class="conversation-copy"
            ><b>{{ conversation.peerName || '社区成员' }}</b
            ><small>{{ conversation.body || '开始对话' }}</small></span
          >
          <span class="conversation-meta"
            ><time>{{ compactTime(conversation.createdAt) }}</time
            ><em v-if="conversation.unreadCount">{{
              conversation.unreadCount > 99 ? '99+' : conversation.unreadCount
            }}</em></span
          >
        </button>
      </nav>
      <div v-else class="conversation-state empty">
        <b>还没有私信</b>
        <p>可以从用户主页发起对话，或点击右上角搜索成员。</p>
      </div>
    </aside>

    <section v-if="activePeer" class="thread-panel">
      <header>
        <button type="button" class="mobile-back" @click="closeThread">←</button>
        <router-link :to="{ name: 'people', params: { userId: activePeer.peerId } }" class="thread-person">
          <span class="avatar"
            ><img v-if="activePeer.peerAvatar" :src="resourceUrl(activePeer.peerAvatar)" alt="" /><i v-else>{{
              initial(activePeer.peerName)
            }}</i></span
          >
          <span
            ><b>{{ activePeer.peerName || '社区成员' }}</b
            ><small>查看个人主页</small></span
          >
        </router-link>
      </header>

      <div ref="messageScroller" class="message-scroll">
        <div v-if="loadingMessages && !messages.length" class="thread-state">正在加载消息…</div>
        <div v-else-if="!messages.length" class="thread-state">
          <span>潮</span><b>从一句友好的问候开始</b>
          <p>私信仅对你们两人可见。</p>
        </div>
        <div v-else class="message-list">
          <article v-for="message in messages" :key="message.messageId" :class="{ mine: isMine(message) }">
            <div>{{ message.body }}</div>
            <footer>
              <time>{{ fullTime(message.createdAt) }}</time
              ><span v-if="isMine(message)">{{ message.readAt ? '已读' : '已送达' }}</span>
            </footer>
          </article>
        </div>
      </div>

      <form class="message-composer" @submit.prevent="sendMessage">
        <textarea
          v-model="draft"
          rows="2"
          maxlength="2000"
          placeholder="写一条私信…"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <div>
          <small>{{ draft.length }}/2000 · Shift + Enter 换行</small
          ><button type="submit" :disabled="sending || !draft.trim()">
            {{ sending ? '发送中' : '发送' }}
          </button>
        </div>
      </form>
    </section>

    <section v-else class="no-thread">
      <span class="tide-mark">潮</span>
      <h2>选择一段对话</h2>
      <p>和兴趣相投的人继续交流，消息只在你们之间可见。</p>
    </section>
  </main>
</template>

<script setup>
import { getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const loginStore = useLoginStore()
const conversations = ref([])
const messages = ref([])
const activePeer = ref(null)
const draft = ref('')
const loadingConversations = ref(true)
const loadingMessages = ref(false)
const sending = ref(false)
const messageScroller = ref(null)
const newChatVisible = ref(false)
const userQuery = ref('')
const userResults = ref([])
const searching = ref(false)
const searched = ref(false)
let pollTimer

const currentUserId = () => String(loginStore.userInfo?.userId || '')
const resourceUrl = (source) => (source ? `${proxy.Api.sourcePath}${encodeURIComponent(source)}` : '')
const initial = (name) => (name || '社').slice(0, 1)
const isMine = (message) => String(message.senderId) === currentUserId()
const compactTime = (value) =>
  value ? new Date(value).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) : ''
const fullTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
    : '刚刚'
const scrollToBottom = () =>
  nextTick(() => {
    if (messageScroller.value) messageScroller.value.scrollTop = messageScroller.value.scrollHeight
  })

const loadConversations = async () => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideMessages}/conversations`,
    params: { limit: 100 },
    showLoading: false,
    showError: false,
  })
  if (result) conversations.value = result.data || []
  loadingConversations.value = false
}
const resolvePeer = async (peerId) => {
  const existing = conversations.value.find((item) => String(item.peerId) === String(peerId))
  if (existing) return existing
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityUsers}/${encodeURIComponent(peerId)}`,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (!result) return null
  return { peerId: result.data.userId, peerName: result.data.nickName, peerAvatar: result.data.avatar }
}
const loadThread = async (scroll = false) => {
  if (!activePeer.value) return
  const previousLastId = messages.value.at(-1)?.messageId
  loadingMessages.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideMessages}/with/${encodeURIComponent(activePeer.value.peerId)}`,
    params: { limit: 300 },
    showLoading: false,
    showError: false,
  })
  loadingMessages.value = false
  if (!result) return
  messages.value = result.data || []
  const conversation = conversations.value.find(
    (item) => String(item.peerId) === String(activePeer.value.peerId),
  )
  if (conversation) conversation.unreadCount = 0
  if (scroll || previousLastId !== messages.value.at(-1)?.messageId) scrollToBottom()
}
const selectPeer = async (peer) => {
  activePeer.value = peer
  newChatVisible.value = false
  userResults.value = []
  await router.push({ name: 'messages', params: { userId: peer.peerId } })
}
const closeThread = () => router.push({ name: 'messages' })
const sendMessage = async () => {
  const body = draft.value.trim()
  if (!body || !activePeer.value || sending.value) return
  sending.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideMessages}/with/${encodeURIComponent(activePeer.value.peerId)}/send`,
    params: { body },
    showLoading: false,
  })
  sending.value = false
  if (!result) return
  messages.value.push({ ...result.data, senderId: currentUserId(), recipientId: activePeer.value.peerId })
  draft.value = ''
  await scrollToBottom()
  await loadConversations()
}
const searchUsers = async () => {
  const query = userQuery.value.trim()
  if (!query) return
  searching.value = true
  searched.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideMessages}/users/search`,
    params: { query },
    showLoading: false,
  })
  searching.value = false
  userResults.value = result?.data || []
}

watch(
  () => route.params.userId,
  async (peerId) => {
    if (!peerId) {
      activePeer.value = null
      messages.value = []
      return
    }
    activePeer.value = await resolvePeer(peerId)
    if (activePeer.value) await loadThread(true)
  },
)

onMounted(async () => {
  await loadConversations()
  const peerId = route.params.userId
  if (peerId) {
    activePeer.value = await resolvePeer(peerId)
    if (activePeer.value) await loadThread(true)
  }
  pollTimer = window.setInterval(async () => {
    await loadConversations()
    if (activePeer.value) await loadThread(false)
  }, 5000)
})
onBeforeUnmount(() => window.clearInterval(pollTimer))
</script>

<style scoped>
.messages-page {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  width: min(1120px, calc(100% - 40px));
  height: calc(100vh - 86px);
  margin: 20px auto 12px;
  overflow: hidden;
  border: 1px solid var(--zt-border);
  border-radius: 10px;
  background: var(--zt-surface);
  box-shadow: 0 18px 50px rgba(27, 38, 34, 0.06);
}
.conversation-panel {
  min-width: 0;
  border-right: 1px solid var(--zt-border-soft);
  background: #f8f7f2;
}
.conversation-panel > header,
.thread-panel > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 18px;
  border-bottom: 1px solid var(--zt-border-soft);
}
.conversation-panel > header span {
  color: var(--zt-text-3);
  font-size: 9px;
  letter-spacing: 0.14em;
}
.conversation-panel h1 {
  margin: 2px 0 0;
  color: var(--zt-text);
  font-size: 20px;
}
.conversation-panel > header button {
  width: 30px;
  height: 30px;
  border: 1px solid var(--zt-border);
  border-radius: 6px;
  color: var(--zt-primary);
  background: transparent;
  font-size: 20px;
  cursor: pointer;
}
.conversation-panel nav {
  overflow-y: auto;
  max-height: calc(100% - 68px);
}
.conversation-panel nav > button,
.user-result {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 11px;
  align-items: center;
  width: 100%;
  padding: 13px 15px;
  border: 0;
  border-bottom: 1px solid var(--zt-border-soft);
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.conversation-panel nav > button:hover,
.conversation-panel nav > button.active {
  background: #eeece4;
}
.avatar {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: var(--zt-tide-soft);
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar i {
  color: var(--zt-primary);
  font-style: normal;
  font-weight: 760;
}
.conversation-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
}
.conversation-copy b {
  font-size: 13px;
}
.conversation-copy small {
  overflow: hidden;
  color: var(--zt-text-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation-meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: var(--zt-text-3);
  font-size: 9px;
}
.conversation-meta em {
  display: grid;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: var(--zt-accent);
  font-size: 9px;
  font-style: normal;
}
.new-chat {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 7px;
  padding: 12px;
  border-bottom: 1px solid var(--zt-border);
}
.new-chat input {
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid var(--zt-border);
  border-radius: 5px;
  outline: none;
  background: var(--zt-surface);
}
.new-chat > button:not(.user-result) {
  padding: 0 11px;
  border: 0;
  border-radius: 5px;
  color: #fff;
  background: var(--zt-primary);
  cursor: pointer;
}
.new-chat .user-result,
.new-chat .search-note {
  grid-column: 1/-1;
}
.user-result {
  border: 0;
  border-radius: 6px !important;
}
.user-result b {
  font-size: 12px;
}
.user-result small {
  color: var(--zt-text-3);
}
.search-note {
  padding: 8px;
  color: var(--zt-text-3);
  font-size: 11px;
}
.conversation-state {
  padding: 30px 20px;
  color: var(--zt-text-3);
  font-size: 12px;
  text-align: center;
}
.conversation-state b {
  display: block;
  margin-bottom: 7px;
  color: var(--zt-text);
  font-size: 15px;
}
.conversation-state p {
  line-height: 1.6;
}
.thread-panel {
  display: grid;
  grid-template-rows: 68px minmax(0, 1fr) auto;
  min-width: 0;
}
.thread-person {
  display: flex;
  align-items: center;
  gap: 10px;
  color: inherit;
  text-decoration: none;
}
.thread-person .avatar {
  width: 38px;
  height: 38px;
}
.thread-person > span:last-child {
  display: grid;
  gap: 2px;
}
.thread-person b {
  font-size: 13px;
}
.thread-person small {
  color: var(--zt-text-3);
  font-size: 10px;
}
.mobile-back {
  display: none;
  border: 0;
  background: transparent;
}
.message-scroll {
  overflow-y: auto;
  padding: 28px 30px;
  background: #fcfbf7;
}
.message-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
}
.message-list article {
  align-self: flex-start;
  max-width: min(72%, 620px);
}
.message-list article > div {
  padding: 10px 13px;
  border: 1px solid var(--zt-border-soft);
  border-radius: 4px 12px 12px 12px;
  color: var(--zt-text);
  background: #fff;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-list article.mine {
  align-self: flex-end;
}
.message-list article.mine > div {
  border-color: transparent;
  border-radius: 12px 4px 12px 12px;
  color: #f8fbf9;
  background: var(--zt-primary);
}
.message-list footer {
  display: flex;
  justify-content: flex-start;
  gap: 6px;
  margin-top: 4px;
  color: var(--zt-text-3);
  font-size: 9px;
}
.message-list .mine footer {
  justify-content: flex-end;
}
.thread-state {
  display: grid;
  height: 100%;
  place-content: center;
  justify-items: center;
  color: var(--zt-text-3);
  text-align: center;
}
.thread-state > span,
.tide-mark {
  display: grid;
  width: 44px;
  height: 44px;
  margin-bottom: 12px;
  place-items: center;
  border-radius: 13px 13px 13px 4px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-weight: 800;
}
.thread-state b {
  color: var(--zt-text);
}
.thread-state p {
  font-size: 11px;
}
.message-composer {
  padding: 13px 16px 14px;
  border-top: 1px solid var(--zt-border-soft);
  background: var(--zt-surface);
}
.message-composer textarea {
  display: block;
  box-sizing: border-box;
  width: 100%;
  resize: none;
  border: 0;
  outline: none;
  color: var(--zt-text);
  background: transparent;
  font: inherit;
  font-size: 13px;
  line-height: 1.6;
}
.message-composer > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.message-composer small {
  color: var(--zt-text-3);
  font-size: 9px;
}
.message-composer button {
  padding: 7px 17px;
  border: 0;
  border-radius: 5px;
  color: #fff;
  background: var(--zt-primary);
  font-size: 11px;
  cursor: pointer;
}
.message-composer button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.no-thread {
  display: grid;
  place-content: center;
  justify-items: center;
  padding: 30px;
  color: var(--zt-text-3);
  text-align: center;
  background: #fcfbf7;
}
.no-thread h2 {
  margin: 0;
  color: var(--zt-text);
  font-size: 19px;
}
.no-thread p {
  font-size: 12px;
}
.no-thread .tide-mark {
  margin: 0 0 14px;
}
@media (max-width: 760px) {
  .messages-page {
    grid-template-columns: 1fr;
    width: 100%;
    height: calc(100vh - 55px);
    margin: 0;
    border: 0;
    border-radius: 0;
  }
  .messages-page.has-active .conversation-panel {
    display: none;
  }
  .thread-panel {
    height: 100%;
  }
  .messages-page:not(.has-active) .no-thread {
    display: none;
  }
  .mobile-back {
    display: block;
    margin-right: 8px;
  }
  .thread-panel > header {
    justify-content: flex-start;
  }
  .message-scroll {
    padding: 20px 14px;
  }
  .message-list article {
    max-width: 84%;
  }
}
</style>
