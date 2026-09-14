<template>
  <IndexHeader></IndexHeader>
  <div class="content-body">
    <router-view v-if="route.meta.checkLogin !== true || (haveInit && showRouterView)" v-slot="{ Component }">
      <transition name="tide-page" mode="out-in"><component :is="Component" /></transition>
    </router-view>
  </div>
  <Footer v-if="route.meta.showFooter == null || route.meta.showFooter"></Footer>
</template>

<script setup>
import Footer from '@/views/footer/Footer.vue'
import IndexHeader from '@/views/header/IndexHeader.vue'
import { ref, getCurrentInstance, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const route = useRoute()

import { useLoginStore } from '@/stores/loginStore.js'
const loginStore = useLoginStore()
const haveInit = ref(false)

const needLogin = computed(() => {
  const needLogin =
    haveInit.value &&
    route.meta.checkLogin != null &&
    route.meta.checkLogin &&
    loginStore.userInfo &&
    Object.keys(loginStore.userInfo).length == 0

  return needLogin
})

watch(
  needLogin,
  (value) => {
    if (value) loginStore.showLogin = true
  },
  { immediate: true },
)

const showRouterView = computed(() => {
  return route.meta.checkLogin == null || !needLogin.value
})

const autoLogin = async () => {
  const tokenAtStart = localStorage.getItem('token')
  if (!tokenAtStart) {
    haveInit.value = true
    return
  }
  let result = await proxy.Request({
    url: proxy.Api.autoLogin,
    showLoading: false,
    showError: false,
    suppressLogin: true,
  })

  // 页面快速刷新或登录/退出期间，旧页面的请求可能晚于新页面返回。
  // 旧请求不得覆盖或清除当前已经更新的会话。
  if (localStorage.getItem('token') !== tokenAtStart) {
    haveInit.value = true
    return
  }
  if (!result) {
    localStorage.removeItem('token')
    haveInit.value = true
    return
  }
  haveInit.value = true
  if (result.data == null) {
    // 携带 token 但服务端查不到会话，说明 token 已失效；清理本地
    // 残留，避免下次刷新重复携带同一个无效 token。
    localStorage.removeItem('token')
    loginStore.saveUserInfo({})
    return
  }
  if (result.data?.token) localStorage.setItem('token', result.data.token)
  loginStore.saveUserInfo(result.data)
}

onMounted(() => {
  autoLogin()
})
</script>

<style lang="scss" scoped>
.content-body {
  width: min(var(--zt-page-width), calc(100% - 40px));
  margin: 54px auto 0;
  min-height: calc(100vh - 210px);
}
.tide-page-enter-active,
.tide-page-leave-active {
  transition: opacity 0.16s ease;
}
.tide-page-enter-from,
.tide-page-leave-to {
  opacity: 0;
}

@media (max-width: 680px) {
  .content-body {
    width: calc(100% - 20px);
    margin-top: 52px;
  }
}
</style>
