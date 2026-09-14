<template>
  <main class="today-page">
    <header class="today-heading">
      <div>
        <span class="eyebrow">TODAY · 社区精选</span>
        <h1>今天，值得停留的讨论</h1>
        <p>从你已经加入的兴趣现场里，挑出今天互动最充分的分享与回复。</p>
      </div>
      <div class="today-mark" aria-hidden="true"><i></i><i></i><i></i><span>潮</span></div>
    </header>

    <section class="selection-bar" aria-label="精选筛选条件">
      <div class="selection-copy">
        <span>筛选标准</span>
        <strong>{{ metricLabel }}优先</strong>
        <small>可以随时切换，结果会即时更新</small>
      </div>
      <div class="selection-controls">
        <label>
          <span>排序</span>
          <select v-model="metric" @change="load">
            <option value="BOOKMARKS">收藏量</option>
            <option value="COMMENTS">评论数</option>
            <option value="VIEWS">浏览量</option>
          </select>
        </label>
        <label>
          <span>至少</span>
          <input v-model.number="minCount" type="number" min="0" max="1000000" inputmode="numeric" @keyup.enter="load" />
          <em>{{ metricUnit }}</em>
        </label>
        <button type="button" class="refresh-button" :disabled="loading" @click="load">{{ loading ? '更新中…' : '更新精选' }}</button>
      </div>
    </section>

    <section v-if="loading" class="today-state">正在整理今天的高互动内容…</section>
    <section v-else-if="!items.length" class="today-state empty-state">
      <div class="empty-mark"><i></i><i></i><i></i></div>
      <strong>今天还没有符合条件的精选</strong>
      <p>可以降低最低{{ metricUnit }}，或者回到社区参与一场讨论。</p>
      <button type="button" @click="router.push('/community')">回到社区</button>
    </section>
    <section v-else class="today-list">
      <article v-for="(item, index) in items" :key="`${item.kind}-${item.postId}-${item.commentId || ''}`" class="today-item" @click="openItem(item)">
        <div class="rank"><span>{{ String(index + 1).padStart(2, '0') }}</span><i></i></div>
        <div class="item-copy">
          <div class="item-meta"><span class="kind">{{ item.kind === 'COMMENT' ? '回复' : '分享' }}</span><span>{{ item.hubName || '兴趣现场' }}</span><time>{{ formatTime(item.createdAt) }}</time></div>
          <h2>{{ item.kind === 'COMMENT' ? (item.title || '社区讨论') : (item.title || '无标题分享') }}</h2>
          <p>{{ excerpt(item.body) }}</p>
          <footer><span>由 {{ item.authorLabel || '社区成员' }}</span><span>♥ {{ item.likeCount || 0 }}</span><span>评论 {{ item.commentCount || 0 }}</span><span>收藏 {{ item.bookmarkCount || 0 }}</span><span>浏览 {{ item.viewCount || 0 }}</span></footer>
        </div>
        <span class="score"><b>{{ score(item) }}</b><small>{{ metricUnit }}</small></span>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()
const metric = ref('BOOKMARKS')
const minCount = ref(0)
const items = ref([])
const loading = ref(false)
const metricLabel = computed(() => ({ BOOKMARKS: '收藏量', COMMENTS: '评论数', VIEWS: '浏览量' }[metric.value] || '收藏量'))
const metricUnit = computed(() => ({ BOOKMARKS: '收藏', COMMENTS: '评论', VIEWS: '浏览' }[metric.value] || '收藏'))
const readFilter = () => {
  try {
    const saved = JSON.parse(localStorage.getItem('zentide.today-filter') || '{}')
    if (['BOOKMARKS', 'COMMENTS', 'VIEWS'].includes(saved.metric)) metric.value = saved.metric
    if (Number.isFinite(Number(saved.minCount))) minCount.value = Math.max(0, Number(saved.minCount))
  } catch {
    // Ignore malformed local preferences and use the calm default.
  }
}
watch([metric, minCount], () => {
  localStorage.setItem('zentide.today-filter', JSON.stringify({ metric: metric.value, minCount: minCount.value }))
})
const load = async () => {
  loading.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.zentideCommunityTodayHighlights,
      params: { metric: metric.value, minCount: Math.max(0, Number(minCount.value || 0)), limit: 30 },
      showLoading: false,
      showError: false,
    })
    if (result) items.value = result.data || []
  } finally {
    loading.value = false
  }
}
const score = (item) => Number({ BOOKMARKS: item.bookmarkCount, COMMENTS: item.commentCount, VIEWS: item.viewCount }[metric.value] || 0)
const excerpt = (body) => String(body || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim().slice(0, 180) || '这条内容还没有文字摘要。'
const formatTime = (value) => value ? new Date(value).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) : '今天'
const openItem = (item) => item.postId && router.push({ name: 'community-post', params: { postId: item.postId } })
onMounted(() => {
  readFilter()
  load()
})
</script>

<style scoped>
.today-page{max-width:940px;margin:0 auto;padding:38px 0 92px;color:var(--zt-text)}
.today-heading{display:flex;align-items:center;justify-content:space-between;gap:28px;padding:30px 2px 30px;border-bottom:1px solid var(--zt-border)}
.eyebrow{color:var(--zt-tide);font-size:10px;font-weight:800;letter-spacing:.16em}.today-heading h1{margin:10px 0 7px;font-size:clamp(30px,4vw,44px);font-weight:770;letter-spacing:-.05em}.today-heading p{margin:0;color:var(--zt-text-2);font-size:13px;line-height:1.7}
.today-mark{position:relative;display:grid;place-items:center;width:92px;height:92px;flex:0 0 92px;border:1px solid color-mix(in srgb,var(--zt-tide) 45%,var(--zt-border));border-radius:50%;color:var(--zt-primary);background:var(--zt-tide-soft)}.today-mark span{position:relative;z-index:1;font-size:18px;font-weight:780}.today-mark i{position:absolute;border:1px solid color-mix(in srgb,var(--zt-tide) 55%,transparent);border-radius:50%}.today-mark i:nth-child(1){inset:12px}.today-mark i:nth-child(2){inset:23px}.today-mark i:nth-child(3){inset:34px;border-color:var(--zt-accent)}
.selection-bar{display:flex;align-items:center;justify-content:space-between;gap:22px;padding:17px 2px;border-bottom:1px solid var(--zt-border-soft)}.selection-copy{display:flex;align-items:baseline;gap:9px;flex-wrap:wrap}.selection-copy span{color:var(--zt-text-3);font-size:10px;font-weight:700;letter-spacing:.08em}.selection-copy strong{color:var(--zt-primary);font-size:13px}.selection-copy small{color:var(--zt-text-3);font-size:11px}.selection-controls{display:flex;align-items:center;gap:8px}.selection-controls label{display:flex;align-items:center;gap:6px;color:var(--zt-text-3);font-size:11px}.selection-controls select,.selection-controls input{height:32px;padding:0 8px;border:1px solid var(--zt-border);border-radius:5px;outline:0;color:var(--zt-text);background:var(--zt-surface);font-size:12px}.selection-controls select:focus,.selection-controls input:focus{border-color:var(--zt-tide);box-shadow:0 0 0 3px rgba(53,119,111,.08)}.selection-controls input{width:64px}.selection-controls em{font-style:normal}.refresh-button{height:32px;padding:0 12px;border:1px solid var(--zt-primary);border-radius:5px;color:#fff;background:var(--zt-primary);font-size:11px;font-weight:680;cursor:pointer}.refresh-button:disabled{opacity:.6;cursor:default}.today-state{margin-top:20px;padding:72px 20px;border:1px dashed var(--zt-border);border-radius:8px;color:var(--zt-text-3);text-align:center;background:var(--zt-surface-hover)}.empty-state strong{display:block;color:var(--zt-text);font-size:15px}.empty-state p{margin:7px 0 17px;font-size:12px}.empty-state button{padding:7px 12px;border:1px solid var(--zt-primary);border-radius:5px;color:var(--zt-primary);background:transparent;font-size:11px;cursor:pointer}.empty-mark{display:flex;align-items:end;justify-content:center;gap:4px;height:28px;margin-bottom:12px}.empty-mark i{display:block;width:3px;border-radius:2px;background:var(--zt-tide)}.empty-mark i:nth-child(1){height:12px}.empty-mark i:nth-child(2){height:24px}.empty-mark i:nth-child(3){height:17px}
.today-list{display:grid;gap:9px;margin-top:20px}.today-item{display:grid;grid-template-columns:54px minmax(0,1fr) 64px;gap:14px;align-items:start;padding:19px 18px;border:1px solid var(--zt-border);border-radius:8px;background:var(--zt-surface);cursor:pointer;transition:border-color .16s,box-shadow .16s,transform .16s}.today-item:hover{border-color:color-mix(in srgb,var(--zt-tide) 45%,var(--zt-border));box-shadow:0 8px 22px rgba(24,55,48,.06);transform:translateY(-1px)}.rank{display:flex;align-items:center;gap:8px;color:var(--zt-tide);font-size:12px;font-weight:760}.rank i{display:block;width:2px;height:28px;border-radius:2px;background:var(--zt-tide-soft)}.item-meta{display:flex;align-items:center;gap:7px;color:var(--zt-text-3);font-size:10px}.item-meta .kind{padding:3px 6px;border-radius:3px;color:var(--zt-primary);background:var(--zt-tide-soft);font-weight:700}.item-meta time{margin-left:auto}.item-copy h2{margin:9px 0 5px;color:var(--zt-text);font-size:17px;letter-spacing:-.015em}.item-copy p{display:-webkit-box;overflow:hidden;margin:0;color:var(--zt-text-2);font-size:12px;line-height:1.65;-webkit-box-orient:vertical;-webkit-line-clamp:2}.item-copy footer{display:flex;gap:12px;margin-top:11px;color:var(--zt-text-3);font-size:10px}.score{display:flex;align-items:end;flex-direction:column;color:var(--zt-tide)}.score b{font-size:21px;font-weight:760;letter-spacing:-.04em}.score small{font-size:10px;color:var(--zt-text-3)}
@media(max-width:680px){.today-page{padding:24px 0 76px}.today-heading{align-items:flex-start;padding:22px 0 24px}.today-mark{width:64px;height:64px;flex-basis:64px}.today-mark i:nth-child(1){inset:8px}.today-mark i:nth-child(2){inset:16px}.today-mark i:nth-child(3){inset:24px}.today-mark span{font-size:14px}.selection-bar{align-items:flex-start;flex-direction:column;gap:13px;padding:15px 0}.selection-controls{width:100%;flex-wrap:wrap}.selection-controls label:first-child{flex:1}.selection-controls select{width:100%}.today-item{grid-template-columns:34px minmax(0,1fr) 45px;gap:8px;padding:16px 13px}.item-copy h2{font-size:16px}.item-copy footer{gap:8px;flex-wrap:wrap}.rank i{display:none}.score b{font-size:18px}}
</style>
