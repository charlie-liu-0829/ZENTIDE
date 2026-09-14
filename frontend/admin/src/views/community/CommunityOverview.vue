<template>
  <main class="page">
    <header class="heading">
      <div>
        <span>COMMUNITY CONTROL PLANE</span>
        <h1>社区总览</h1>
        <p>用一张面板掌握兴趣现场、内容和成员申请，发现需要优先处理的社区信号。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新数据</el-button>
    </header>

    <section class="metrics" v-loading="loading">
      <article v-for="item in metrics" :key="item.key" class="metric-card">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ overview[item.key] ?? 0 }}</div>
        <div class="metric-hint">{{ item.hint }}</div>
      </article>
    </section>

    <section class="workbench">
      <div class="workbench-copy">
        <span>DAILY WORKBENCH</span>
        <h2>今天先处理什么？</h2>
        <p>从待审成员和隐藏内容开始，保持每个兴趣现场都有清晰、可参与的讨论空间。</p>
      </div>
      <div class="actions">
        <button @click="go('/community/members')"><strong>{{ overview.pendingMembers ?? 0 }}</strong><span>待审加入申请</span><i>→</i></button>
        <button @click="go('/community/posts?status=HIDDEN')"><strong>{{ overview.pendingPosts ?? 0 }}</strong><span>待处理帖子</span><i>→</i></button>
        <button @click="go('/community/comments?status=HIDDEN')"><strong>{{ overview.hiddenComments ?? 0 }}</strong><span>待处理评论</span><i>→</i></button>
      </div>
    </section>

    <section class="quick-grid">
      <button @click="go('/community/hubs')"><span class="eyebrow">SPACE</span><b>兴趣现场</b><small>管理状态、规模与创建者</small></button>
      <button @click="go('/community/posts')"><span class="eyebrow">CONTENT</span><b>帖子审核</b><small>搜索、隐藏、移除或恢复帖子</small></button>
      <button @click="go('/community/comments')"><span class="eyebrow">CONVERSATION</span><b>评论审核</b><small>维护讨论区的秩序与质量</small></button>
      <button @click="go('/community/post-types')"><span class="eyebrow">CONFIG</span><b>帖子类型</b><small>设置全站发帖结构</small></button>
    </section>
  </main>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const loading = ref(false)
const overview = ref({})
const metrics = [
  { key: 'hubs', label: '活跃兴趣现场', hint: '正在对外开放' },
  { key: 'posts', label: '已发布帖子', hint: '可被社区浏览' },
  { key: 'comments', label: '公开评论', hint: '保持讨论流动' },
  { key: 'pendingMembers', label: '待审申请', hint: '需要管理员处理' },
]

const load = async () => {
  loading.value = true
  const result = await proxy.Request({ url: proxy.Api.zentideAdminCommunityOverview, params: {}, showLoading: false })
  loading.value = false
  if (result) overview.value = result.data || {}
}
const go = (path) => router.push(path)
onMounted(load)
</script>

<style scoped>
.page{padding:28px;color:#202923}.heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #e3e6e4;padding-bottom:22px}.heading span,.workbench-copy span,.eyebrow{color:#b05c49;font-size:10px;font-weight:800;letter-spacing:1.3px}.heading h1{margin:7px 0 5px;font-size:28px;letter-spacing:-.03em}.heading p{margin:0;color:#78807c;font-size:13px}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:22px 0}.metric-card{padding:18px 20px;border:1px solid #dfe5e1;border-radius:8px;background:#fbfcfb}.metric-label{color:#5f6963;font-size:12px}.metric-value{margin:10px 0 3px;color:#1f2925;font-size:30px;font-weight:700;letter-spacing:-.04em}.metric-hint{color:#929a95;font-size:11px}.workbench{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:22px 24px;margin-bottom:18px;border:1px solid #dfe5e1;border-radius:8px;background:#f5f8f5}.workbench-copy{max-width:320px}.workbench-copy h2{margin:8px 0 6px;font-size:19px}.workbench-copy p{margin:0;color:#6f7973;font-size:12px;line-height:1.7}.actions{display:grid;grid-template-columns:repeat(3,minmax(150px,1fr));gap:10px;flex:1}.actions button,.quick-grid button{border:1px solid #dfe5e1;background:#fff;text-align:left;cursor:pointer;transition:.18s}.actions button{position:relative;padding:14px 34px 14px 16px;border-radius:6px}.actions button:hover,.quick-grid button:hover{border-color:#b05c49;transform:translateY(-1px)}.actions strong{display:block;font-size:22px;color:#1f2925}.actions span{display:block;margin-top:4px;color:#68736c;font-size:11px}.actions i{position:absolute;right:14px;top:18px;color:#b05c49;font-style:normal}.quick-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.quick-grid button{padding:18px;border-radius:8px}.quick-grid b{display:block;margin:10px 0 5px;font-size:15px}.quick-grid small{color:#7d8780;font-size:11px}@media(max-width:900px){.metrics,.quick-grid{grid-template-columns:repeat(2,1fr)}.workbench{display:block}.actions{margin-top:18px}}@media(max-width:600px){.page{padding:18px}.metrics,.quick-grid,.actions{grid-template-columns:1fr}.heading{gap:15px}.heading .el-button{flex:none}}
</style>
