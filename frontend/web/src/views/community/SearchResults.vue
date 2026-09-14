<template>
  <main class="search-page">
    <header class="search-heading">
      <span class="eyebrow">SEARCH</span>
      <h1>搜索结果</h1>
      <p v-if="query">正在查找“{{ query }}”的{{ type === 'hub' ? '兴趣现场' : '帖子' }}</p>
      <p v-else>输入关键词，搜索社区里的帖子或兴趣现场。</p>
    </header>
    <section v-if="loading" class="search-state">正在搜索…</section>
    <section v-else-if="!query" class="search-state">从顶部搜索框开始探索。</section>
    <section v-else-if="!results.length" class="search-state">没有找到匹配内容，换个关键词试试。</section>
    <section v-else class="result-list">
      <article v-for="result in results" :key="`${result.type}-${result.id}`" class="result-card" @click="openResult(result)">
        <div class="result-meta"><span v-html="highlight(type === 'hub' ? categoryLabel(result.category) : result.hubName || '社区分享')"></span><small v-if="result.memberCount != null">{{ result.memberCount }} 成员 · {{ result.postCount || 0 }} 分享</small></div>
        <h2 v-html="highlight(result.title || '未命名内容')"></h2>
        <p v-html="highlight(result.summary || '暂无简介')"></p>
      </article>
    </section>
  </main>
</template>

<script setup>
import { getCurrentInstance, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const { proxy } = getCurrentInstance()
const route = useRoute(); const router = useRouter()
const query = ref(''); const type = ref('post'); const results = ref([]); const loading = ref(false); const directions = ref([])
const categoryLabel = (value) => directions.value.find((item) => item.code === value)?.displayName || value || '兴趣现场'
const escapeHtml = (value) => String(value).replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character])
const highlight = (value) => {
  const escaped = escapeHtml(value)
  const keyword = query.value.trim()
  if (!keyword) return escaped
  const pattern = escapeHtml(keyword).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escaped.replace(new RegExp(`(${pattern})`, 'ig'), '<mark>$1</mark>')
}
const load = async () => {
  query.value = String(route.query.q || '').trim(); type.value = route.query.type === 'hub' ? 'hub' : 'post'
  if (!query.value) { results.value = []; return }
  loading.value = true
  try { const response = await proxy.Request({ url: proxy.Api.zentideCommunitySearch, params: { query: query.value, type: type.value, limit: 30 }, showLoading: false }); results.value = response?.data || [] } finally { loading.value = false }
}
const openResult = (result) => router.push(result.type === 'hub' ? { path: '/community/discover', query: { q: result.title } } : { name: 'community-post', params: { postId: result.id } })
const loadDirections = async () => {
  const result = await proxy.Request({ url: proxy.Api.zentideCommunityHubDirections, params: {}, showLoading: false, showError: false })
  if (result) directions.value = result.data || []
}
watch(() => route.fullPath, load); onMounted(async () => { await loadDirections(); await load() })
</script>

<style scoped>
.search-page { max-width: 900px; margin: 0 auto; padding: 40px 0 88px; color: var(--zt-text); }
.eyebrow { color: var(--zt-tide); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
.search-heading h1 { margin: 9px 0 6px; font-size: clamp(30px, 3.5vw, 40px); font-weight: 770; letter-spacing: -.045em; }
.search-heading p { margin: 0 0 24px; color: var(--zt-text-3); font-size: 13px; }
.result-list { display: grid; gap: 9px; }
.result-card { padding: 19px 21px; border: 1px solid var(--zt-border); border-radius: 8px; background: var(--zt-surface); cursor: pointer; transition: border-color .15s, transform .15s, box-shadow .15s; }
.result-card:hover { border-color: color-mix(in srgb, var(--zt-tide) 48%, var(--zt-border)); box-shadow: 0 8px 22px rgba(24, 55, 48, .06); transform: translateY(-1px); }
.result-meta { display: flex; justify-content: space-between; color: var(--zt-primary); font-size: 10px; font-weight: 700; }
.result-meta small { color: var(--zt-text-3); font-weight: 400; }
.result-card :deep(mark) { padding: 0 .12em; border-radius: 2px; color: var(--zt-primary); background: #dcece6; box-decoration-break: clone; -webkit-box-decoration-break: clone; }
.result-card h2 { margin: 8px 0 5px; font-size: 18px; }
.result-card p { margin: 0; color: var(--zt-text-2); font-size: 13px; line-height: 1.65; }
.search-state { padding: 60px 20px; border: 1px dashed var(--zt-border); border-radius: var(--zt-radius-md); color: var(--zt-text-3); text-align: center; }
@media (max-width: 680px) { .search-page { padding: 26px 0 70px; } .search-heading h1 { font-size: 29px; } .result-card { padding: 15px; } }
</style>
