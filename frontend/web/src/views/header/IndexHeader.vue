<template>
  <header class="header">
    <div class="header-content">
      <router-link class="brand" to="/">
        <span class="brand-mark"
          ><svg viewBox="0 0 28 28" aria-hidden="true">
            <path d="M4 15c3.4-4.2 6.8-4.2 10.2 0s6.8 4.2 9.8 0" />
            <path d="M4 10c3.4-4.2 6.8-4.2 10.2 0s6.8 4.2 9.8 0" /></svg
        ></span>
        <span>知潮</span>
      </router-link>
      <form class="header-search" @submit.prevent="submitSearch">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="10.8" cy="10.8" r="6.7" />
          <path d="m16 16 4.2 4.2" /></svg
        ><input
          v-model="searchQuery"
          type="search"
          placeholder="搜索变化、话题或兴趣现场"
          aria-label="搜索"
          @focus="openSearchHistory"
          @blur="closeSearchHistory"
        />
        <select v-model="searchType" aria-label="搜索类型" @click.stop @change="changeSearchType">
          <option value="post">帖子</option>
          <option value="hub">现场</option>
        </select>
        <section
          v-if="searchHistoryVisible && searchHistory.length"
          class="search-history"
          @mousedown.prevent
        >
          <header>
            <span>最近搜索</span><button type="button" @click="clearSearchHistory">清空</button>
          </header>
          <button
            v-for="item in searchHistory"
            :key="`${item.type}-${item.query}`"
            type="button"
            class="history-row"
            @click="useSearchHistory(item)"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="8.5" />
              <path d="M12 7.5V12l3.2 2" />
            </svg>
            <span>{{ item.query }}</span
            ><small>{{ item.type === 'hub' ? '兴趣现场' : '帖子' }}</small>
          </button>
        </section>
      </form>
      <nav class="nav" :class="{ 'community-context': isCommunityHome }">
        <router-link to="/community">社区</router-link>
        <router-link to="/community/discover">发现</router-link>
        <router-link to="/today">今日精选</router-link>
      </nav>
      <div class="actions">
        <el-button v-if="!isLoggedIn" type="primary" @click="loginStore.showLogin = true"
          >登录 / 注册</el-button
        >
        <el-dropdown v-else>
          <Avatar :avatar="loginStore.userInfo.avatar || undefined" :width="42" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                @click="router.push({ name: 'people', params: { userId: loginStore.userInfo.userId } })"
                >我的主页</el-dropdown-item
              >
              <el-dropdown-item @click="router.push({ name: 'messages' })"
                ><span class="message-menu-item"
                  >私信<em v-if="unreadMessages">{{ unreadMessages > 99 ? '99+' : unreadMessages }}</em></span
                ></el-dropdown-item
              >
              <el-dropdown-item @click="updatePassword">修改密码</el-dropdown-item>
              <el-dropdown-item @click="securityDialogVisible = true">设置密保问题</el-dropdown-item>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
  <Account />
  <UpdatePassword ref="updatePasswordRef" />
  <el-dialog v-model="securityDialogVisible" title="设置密保问题" width="min(92vw, 460px)">
    <p class="security-dialog-tip">密保答案会使用 BCrypt 加密，仅用于找回密码。</p>
    <el-form label-position="top">
      <el-form-item label="密保问题"
        ><el-input v-model="securityForm.question" maxlength="200" placeholder="例如：我最喜欢的乐队是？"
      /></el-form-item>
      <el-form-item label="密保答案"
        ><el-input v-model="securityForm.answer" maxlength="200" placeholder="请输入答案"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="securityDialogVisible = false">取消</el-button
      ><el-button type="primary" :loading="securitySaving" @click="saveSecurityQuestion"
        >保存密保</el-button
      ></template
    >
  </el-dialog>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Avatar from '@/components/Avatar.vue'
import Account from '@/views/account/Account.vue'
import UpdatePassword from '@/views/account/UpdatePassword.vue'
import { useLoginStore } from '@/stores/loginStore.js'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()
const loginStore = useLoginStore()
const updatePasswordRef = ref()
const securityDialogVisible = ref(false)
const securitySaving = ref(false)
const securityForm = reactive({ question: '', answer: '' })
const searchQuery = ref('')
const searchType = ref('post')
const searchHistory = ref([])
const searchHistoryVisible = ref(false)
const searchHistoryLoaded = ref(false)
const unreadMessages = ref(0)
let unreadTimer
const isLoggedIn = computed(() => Object.keys(loginStore.userInfo || {}).length > 0)
const isCommunityHome = computed(() => route.name === 'community')
const loadUnreadMessages = async () => {
  if (!isLoggedIn.value) {
    unreadMessages.value = 0
    return
  }
  const result = await proxy.Request({
    url: `${proxy.Api.zentideMessages}/unread-count`,
    params: {},
    showLoading: false,
    showError: false,
  })
  if (result) unreadMessages.value = Number(result.data || 0)
}

const updatePassword = () => updatePasswordRef.value.show()
const saveSecurityQuestion = async () => {
  if (securityForm.question.trim().length < 4 || securityForm.answer.trim().length < 2) {
    proxy.Message.warning('请填写有效的密保问题和答案')
    return
  }
  securitySaving.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.securityQuestion,
      params: { securityQuestion: securityForm.question, securityAnswer: securityForm.answer },
    })
    if (result) {
      proxy.Message.success('密保问题已保存')
      securityDialogVisible.value = false
      securityForm.question = ''
      securityForm.answer = ''
    }
  } finally {
    securitySaving.value = false
  }
}
const submitSearch = () => {
  const q = searchQuery.value.trim()
  if (!q) return router.push({ path: '/search' })
  if (isLoggedIn.value) {
    searchHistory.value = [
      { query: q, type: searchType.value },
      ...searchHistory.value.filter(
        (item) => item.type !== searchType.value || item.query.toLowerCase() !== q.toLowerCase(),
      ),
    ].slice(0, 5)
  }
  searchHistoryVisible.value = false
  router.push({ path: '/search', query: q ? { q, type: searchType.value } : {} })
}
const openSearchHistory = async () => {
  if (!isLoggedIn.value) return
  searchHistoryVisible.value = true
  if (searchHistoryLoaded.value) return
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunitySearchHistory,
    params: {},
    showLoading: false,
    showError: false,
  })
  searchHistory.value = result?.data || []
  searchHistoryLoaded.value = true
}
const closeSearchHistory = () =>
  window.setTimeout(() => {
    searchHistoryVisible.value = false
  }, 120)
const useSearchHistory = (item) => {
  searchQuery.value = item.query
  searchType.value = item.type
  submitSearch()
}
const clearSearchHistory = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunitySearchHistoryClear,
    params: {},
    showLoading: false,
  })
  if (result) searchHistory.value = []
}
const changeSearchType = () => {
  if (route.name === 'search' && searchQuery.value.trim()) submitSearch()
}
watch(
  () => route.fullPath,
  () => {
    if (route.name !== 'search') return
    searchQuery.value = String(route.query.q || '')
    searchType.value = route.query.type === 'hub' ? 'hub' : 'post'
  },
  { immediate: true },
)
watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) loadUnreadMessages()
  else unreadMessages.value = 0
})
const logout = () => {
  proxy.Confirm({
    message: '确定要退出吗？',
    okfun: async () => {
      try {
        // 服务端退出失败（例如会话已过期）时也要清理本地会话，
        // 避免用户被旧 token 卡在已登录状态。
        await proxy.Request({
          url: proxy.Api.logout,
          showError: false,
          suppressLogin: true,
        })
      } finally {
        searchHistory.value = []
        searchHistoryLoaded.value = false
        loginStore.saveUserInfo({})
        localStorage.removeItem('token')

        // 退出可能发生在社区、个人主页或需要登录的页面；刷新后由
        // Layout 重新初始化匿名状态并重新拉取公开内容。
        window.location.reload()
      }
    },
  })
}
onMounted(() => {
  loadUnreadMessages()
  unreadTimer = window.setInterval(loadUnreadMessages, 30000)
})
onBeforeUnmount(() => window.clearInterval(unreadTimer))
</script>

<style lang="scss" scoped>
.header {
  position: fixed;
  inset: 0 0 auto;
  z-index: 20;
  border-bottom: 1px solid color-mix(in srgb, var(--zt-border) 82%, transparent);
  background: color-mix(in srgb, var(--zt-bg) 91%, transparent);
  backdrop-filter: blur(18px) saturate(1.08);
}
.message-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
}
.message-menu-item em {
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
.header-content {
  display: flex;
  align-items: center;
  gap: 22px;
  width: min(var(--zt-page-width), calc(100% - 40px));
  height: 54px;
  margin: auto;
}
.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  flex: none;
  color: var(--zt-primary);
  font-size: 18px;
  font-weight: 820;
  letter-spacing: -0.02em;
  text-decoration: none;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 31px;
  height: 31px;
  border: 0;
  border-radius: 8px 8px 8px 3px;
  color: var(--zt-tide);
  background: var(--zt-tide-soft);
}
.brand-mark svg {
  width: 23px;
  height: 23px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-width: 1.8;
}
.header-search {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  width: min(360px, 30vw);
  height: 36px;
  padding: 0 10px 0 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: color-mix(in srgb, var(--zt-surface-soft) 72%, var(--zt-bg));
  transition:
    border-color 0.15s,
    background-color 0.15s;
}
.search-history {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 40;
  width: 100%;
  min-width: 310px;
  overflow: hidden;
  padding: 6px;
  border: 1px solid var(--zt-border);
  border-radius: var(--zt-radius-md);
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-md);
}
.search-history header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 7px 7px;
  color: var(--zt-text-3);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}
.search-history header button {
  padding: 2px 0;
  border: 0;
  color: var(--zt-text-3);
  background: transparent;
  font-size: 10px;
  letter-spacing: 0;
  cursor: pointer;
}
.search-history header button:hover {
  color: var(--zt-danger);
}
.history-row {
  display: grid;
  align-items: center;
  width: 100%;
  min-height: 35px;
  padding: 6px 8px;
  border: 0;
  border-radius: 6px;
  grid-template-columns: 17px minmax(0, 1fr) auto;
  gap: 8px;
  color: var(--zt-text-2);
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.history-row:hover {
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.history-row svg {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: var(--zt-text-3);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}
.history-row span {
  overflow: hidden;
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.history-row small {
  color: var(--zt-text-3);
  font-size: 9px;
}
.header-search:focus-within {
  border-color: color-mix(in srgb, var(--zt-tide) 42%, var(--zt-border));
  background: var(--zt-surface);
  box-shadow: 0 0 0 3px rgba(53, 119, 111, 0.055);
}
.header-search svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: var(--zt-text-3);
  stroke-linecap: round;
  stroke-width: 1.7;
}
.header-search input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  color: var(--zt-text);
  background: transparent;
  font: inherit;
  font-size: 12px;
}
.header-search input::placeholder {
  color: var(--zt-text-3);
  opacity: 1;
}
.header-search input::-webkit-search-cancel-button {
  display: none;
}
.header-search select {
  flex: none;
  padding: 3px 0 3px 5px;
  border: 0;
  border-left: 1px solid var(--zt-border);
  outline: 0;
  color: var(--zt-text-2);
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}
.nav {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
}
.nav a {
  position: relative;
  padding: 7px 9px;
  border-radius: 4px;
  color: var(--zt-text-2);
  text-decoration: none;
  font-size: 12px;
  font-weight: 650;
}
.nav a:hover {
  color: var(--zt-primary);
  background: transparent;
}
.nav a.router-link-active {
  color: var(--zt-primary);
  background: transparent;
}
.nav a.router-link-active::after {
  position: absolute;
  right: 9px;
  bottom: 2px;
  left: 9px;
  height: 2px;
  border-radius: 2px;
  background: var(--zt-tide);
  content: '';
}
.nav.community-context {
  display: none;
}
.nav.community-context + .actions {
  margin-left: auto;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.actions :deep(.el-button--primary) {
  --el-button-bg-color: var(--zt-primary);
  --el-button-border-color: var(--zt-primary);
  --el-button-hover-bg-color: var(--zt-primary-hover);
  --el-button-hover-border-color: var(--zt-primary-hover);
  font-weight: 700;
}
.security-dialog-tip {
  margin: 0 0 16px;
  color: var(--zt-text-3);
  font-size: 12px;
}
@media (max-width: 850px) {
  .header-content {
    gap: 10px;
  }
  .header-search {
    width: min(260px, 33vw);
  }
  .nav a {
    padding-inline: 6px;
  }
  .nav a:last-child {
    display: none;
  }
}
@media (max-width: 680px) {
  .header {
    backdrop-filter: none;
  }
  .header-content {
    width: calc(100% - 20px);
    height: 52px;
  }
  .brand > span:last-child {
    display: none;
  }
  .header-search {
    flex: 1;
    width: auto;
  }
  .search-history {
    min-width: min(340px, calc(100vw - 88px));
  }
  .actions :deep(.el-button) {
    padding-inline: 10px;
  }
  .nav {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 30;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 0;
    padding: 6px 8px calc(6px + env(safe-area-inset-bottom));
    border-top: 1px solid var(--zt-border);
    background: var(--zt-surface);
  }
  .nav.community-context {
    display: grid;
  }
  .nav.community-context + .actions {
    margin-left: 0;
  }
  .nav a,
  .nav a:last-child {
    display: block;
    padding: 8px 2px;
    text-align: center;
    font-size: 11px;
  }
}
</style>
