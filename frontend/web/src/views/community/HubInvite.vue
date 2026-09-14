<template>
  <main class="invite-page">
    <section class="invite-card">
      <div class="invite-mark">潮</div>
      <span>INTEREST SPACE INVITATION</span>
      <h1 v-if="invitation">邀请你加入「{{ invitation.hubName }}」</h1>
      <h1 v-else-if="loading">正在确认邀请…</h1>
      <h1 v-else>这个邀请暂时无法使用</h1>
      <p v-if="invitation">通过此邀请加入无需审核。加入后，这个兴趣现场会出现在你的社区首页。</p>
      <p v-else-if="!loading">链接可能已经过期，请联系创建者重新生成。</p>
      <el-button v-if="invitation && !loggedIn" type="primary" @click="loginStore.showLogin = true"
        >登录后接受邀请</el-button
      >
      <el-button v-else-if="invitation" type="primary" :loading="redeeming" @click="redeem">{{
        redeemed ? '已加入，正在前往…' : '接受邀请并加入'
      }}</el-button>
      <el-button v-else-if="!loading" @click="router.push('/community/discover')">浏览其他兴趣现场</el-button>
      <small v-if="invitation?.expiresAt">邀请有效期至 {{ formatTime(invitation.expiresAt) }}</small>
    </section>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'
const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const loginStore = useLoginStore()
const invitation = ref(null)
const loading = ref(true)
const redeeming = ref(false)
const redeemed = ref(false)
const loggedIn = computed(() => Boolean(loginStore.userInfo?.userId))
const load = async () => {
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityInvitations}/${route.params.token}`,
    params: {},
    showLoading: false,
    showError: false,
  })
  invitation.value = result?.data || null
  loading.value = false
}
const redeem = async () => {
  if (!loggedIn.value || redeeming.value || redeemed.value) return
  redeeming.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityInvitations}/${route.params.token}/redeem`,
    params: {},
    showLoading: false,
  })
  redeeming.value = false
  if (!result) return
  redeemed.value = true
  proxy.Message.success(`已加入「${result.data?.name || invitation.value.hubName}」`)
  setTimeout(
    () =>
      router.replace({ path: '/community', query: { hubId: result.data?.hubId || invitation.value.hubId } }),
    450,
  )
}
const formatTime = (value) =>
  new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
watch(loggedIn, (value) => {
  if (value && invitation.value && !redeemed.value) redeem()
})
onMounted(load)
</script>

<style scoped>
.invite-page {
  display: grid;
  min-height: calc(100vh - 150px);
  padding: 55px 0 80px;
  place-items: center;
}
.invite-card {
  display: grid;
  justify-items: center;
  width: min(520px, 100%);
  padding: 48px 44px;
  border: 1px solid var(--zt-border);
  border-radius: var(--zt-radius-lg);
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-sm);
  text-align: center;
}
.invite-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin-bottom: 22px;
  border-radius: var(--zt-radius-md);
  color: #fff;
  background: var(--zt-primary);
  font-size: 18px;
  font-weight: 800;
}
.invite-card > span {
  color: var(--zt-accent);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1.4px;
}
.invite-card h1 {
  margin: 11px 0 10px;
  color: var(--zt-text);
  font-size: 25px;
  line-height: 1.4;
}
.invite-card p {
  max-width: 390px;
  margin: 0 0 24px;
  color: var(--zt-text-2);
  line-height: 1.8;
}
.invite-card small {
  margin-top: 16px;
  color: var(--zt-text-3);
  font-size: 10px;
}
@media (max-width: 600px) {
  .invite-page {
    padding-top: 25px;
  }
  .invite-card {
    padding: 38px 22px;
  }
  .invite-card h1 {
    font-size: 21px;
  }
}
</style>
