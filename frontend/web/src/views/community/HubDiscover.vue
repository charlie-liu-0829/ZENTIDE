<template>
  <main class="hub-discover">
    <header class="discover-heading">
      <div>
        <span>潮汐图谱 · TIDE MAP</span>
        <h1>发现兴趣现场</h1>
        <p>从正在发生的兴趣中，找到值得长期停留、持续交换见解的现场。</p>
      </div>
      <el-button type="primary" @click="createHub">创建兴趣现场</el-button>
    </header>

    <section class="discover-controls">
      <el-input v-model="query" clearable placeholder="搜索兴趣现场" @keyup.enter="load"
        ><template #prefix>⌕</template></el-input
      >
      <div class="category-tabs">
        <button
          v-for="item in categoryOptions"
          :key="item.value || 'all'"
          type="button"
          :class="{ active: category === item.value }"
          @click="selectCategory(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section v-if="hubs.length" class="hub-grid">
      <article v-for="hub in hubs" :key="hub.hubId" class="hub-card">
        <button
          type="button"
          class="hub-visual"
          :class="`tone-${(hub.category || 'GENERAL').toLowerCase()}`"
          @click="openHub(hub)"
        >
          <img v-if="hub.coverUrl" :src="resourceUrl(hub.coverUrl)" alt="" /><span v-else>{{
            hub.name.slice(0, 1)
          }}</span>
        </button>
        <div class="hub-card-body">
          <div class="hub-meta">
            <span>{{ categoryLabel(hub.category) }}</span
            ><small>{{ hub.joinPolicy === 'APPROVAL' ? '加入需审核' : '开放加入' }}</small>
          </div>
          <button type="button" class="hub-title" @click="openHub(hub)">{{ hub.name }}</button>
          <p>{{ hub.description || '创建者还没有填写介绍。' }}</p>
          <footer>
            <span
              ><b>{{ hub.memberCount || 0 }}</b> 成员 · {{ hub.postCount || 0 }} 分享</span
            ><button
              type="button"
              :class="{ joined: hub.joined }"
              :disabled="hub.membershipStatus === 'PENDING'"
              @click="joinHub(hub)"
            >
              {{ membershipLabel(hub) }}
            </button>
          </footer>
        </div>
      </article>
    </section>
    <section v-else class="discover-empty">
      <strong>没有找到匹配的兴趣现场</strong>
      <p>换个关键词，或者创建一个新的现场。</p>
    </section>

    <el-dialog
      v-model="previewVisible"
      class="hub-preview-dialog"
      width="min(92vw, 560px)"
      :show-close="true"
    >
      <article v-if="previewHub" class="hub-preview">
        <div class="preview-cover" :class="`tone-${(previewHub.category || 'GENERAL').toLowerCase()}`">
          <img v-if="previewHub.coverUrl" :src="resourceUrl(previewHub.coverUrl)" alt="" /><span v-else>{{
            previewHub.name.slice(0, 1)
          }}</span>
        </div>
        <div class="preview-meta">
          <span>{{ categoryLabel(previewHub.category) }}</span
          ><small>{{ previewHub.joinPolicy === 'APPROVAL' ? '加入需要创建者审核' : '开放加入' }}</small>
        </div>
        <h2>{{ previewHub.name }}</h2>
        <p>{{ previewHub.description || '创建者还没有填写介绍。' }}</p>
        <div class="preview-stats">
          <span
            ><b>{{ previewHub.memberCount || 0 }}</b> 成员</span
          ><span
            ><b>{{ previewHub.postCount || 0 }}</b> 分享</span
          >
        </div>
        <footer>
          <el-button @click="previewVisible = false">继续浏览</el-button
          ><el-button
            type="primary"
            :loading="joining"
            :disabled="previewHub.membershipStatus === 'PENDING'"
            @click="joinHub(previewHub)"
            >{{ membershipLabel(previewHub) }}</el-button
          >
        </footer>
      </article>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'

const { proxy } = getCurrentInstance()
const router = useRouter()
const loginStore = useLoginStore()
const hubs = ref([])
const query = ref('')
const category = ref(null)
const directions = ref([])
const previewHub = ref(null)
const previewVisible = ref(false)
const joining = ref(false)
const categoryOptions = computed(() => [
  { value: null, label: '全部' },
  ...directions.value.map((item) => ({ value: item.code, label: item.displayName })),
])
const loggedIn = () => Boolean(loginStore.userInfo?.userId)
const load = async () => {
  const result = await proxy.Request({
    url: proxy.Api.zentideCommunityHubDiscover,
    params: { query: query.value.trim(), category: category.value, limit: 100 },
    showLoading: false,
    showError: false,
  })
  if (result) hubs.value = result.data || []
}
const loadDirections = async () => {
  const result = await proxy.Request({ url: proxy.Api.zentideCommunityHubDirections, params: {}, showLoading: false, showError: false })
  if (result) directions.value = result.data || []
}
const selectCategory = (value) => {
  category.value = value
  load()
}
const resourceUrl = (source) => (source ? `${proxy.Api.sourcePath}${encodeURIComponent(source)}` : '')
const categoryLabel = (value) => directions.value.find((item) => item.code === value)?.displayName || value || '综合兴趣'
const membershipLabel = (hub) =>
  hub.joined ? '进入现场' : hub.membershipStatus === 'PENDING' ? '等待审核' : hub.owned ? '管理现场' : '加入'
const openHub = (hub) => {
  if (hub.joined || hub.owned) router.push({ path: '/community', query: { hubId: hub.hubId } })
  else {
    previewHub.value = hub
    previewVisible.value = true
  }
}
const joinHub = async (hub) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  if (hub.joined || hub.owned) return openHub(hub)
  if (hub.membershipStatus === 'PENDING') return proxy.Message.info('加入申请正在等待创建者审核')
  joining.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityHubMembership}/${hub.hubId}/membership`,
    params: { active: true },
    showLoading: false,
  })
  joining.value = false
  if (!result) return
  hub.membershipStatus = result.data
  hub.joined = result.data === 'ACTIVE'
  if (hub.joined) {
    hub.memberCount = Number(hub.memberCount || 0) + 1
    previewVisible.value = false
    proxy.Message.success(`已加入「${hub.name}」`)
    router.push({ path: '/community', query: { hubId: hub.hubId } })
  } else proxy.Message.success('加入申请已提交，等待创建者审核')
}
const createHub = () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  router.push({ path: '/community', query: { createHub: '1' } })
}
onMounted(async () => {
  await loadDirections()
  await load()
})
</script>

<style scoped>
.hub-discover {
  padding: 34px 0 96px;
}
.discover-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  padding: 34px 2px 30px;
  border-bottom: 1px solid var(--zt-border);
}
.discover-heading span {
  color: var(--zt-tide);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.55px;
}
.discover-heading h1 {
  margin: 9px 0 8px;
  color: var(--zt-text);
  font-size: clamp(32px, 3.2vw, 42px);
  font-weight: 770;
  letter-spacing: -0.045em;
}
.discover-heading p {
  margin: 0;
  color: var(--zt-text-2);
  font-size: 13px;
  line-height: 1.7;
}
.discover-controls {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 2px 22px;
}
.discover-controls > .el-input {
  width: 260px;
}
.category-tabs {
  display: flex;
  gap: 3px;
  overflow: auto;
}
.category-tabs button {
  padding: 7px 11px;
  border: 0;
  border-radius: 4px;
  color: var(--zt-text-2);
  background: transparent;
  font-size: 12px;
  white-space: nowrap;
  cursor: pointer;
}
.category-tabs button:hover,
.category-tabs button.active {
  color: var(--zt-primary);
  background: transparent;
}
.category-tabs button.active {
  box-shadow: inset 0 -2px var(--zt-tide);
}
.hub-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.hub-card {
  display: grid;
  grid-template-columns: 154px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--zt-border);
  border-radius: 8px;
  background: var(--zt-surface);
  box-shadow: none;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}
.hub-card:hover {
  border-color: color-mix(in srgb, var(--zt-tide) 42%, var(--zt-border));
  box-shadow: 0 10px 28px rgba(24, 55, 48, 0.065);
  transform: translateY(-2px);
}
.hub-visual {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  min-height: 174px;
  padding: 0;
  border: 0;
  color: #fff;
  background: #2f625b;
  cursor: pointer;
}
.hub-visual::after,
.preview-cover::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, transparent 52%, rgba(8, 31, 28, 0.28)),
    repeating-linear-gradient(165deg, transparent 0 24px, rgba(255, 255, 255, 0.045) 25px 26px);
  content: '';
}
.hub-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.82) contrast(0.98);
}
.hub-visual span {
  position: relative;
  z-index: 1;
  font-size: 34px;
  font-weight: 760;
}
.tone-music {
  background: #795c54;
}
.tone-games {
  background: #435f66;
}
.tone-technology,
.tone-ai {
  background: #2f625b;
}
.tone-lifestyle {
  background: #777055;
}
.tone-culture {
  background: #655f70;
}
.tone-sports {
  background: #536d59;
}
.hub-card-body {
  display: flex;
  min-width: 0;
  padding: 17px 18px 14px;
  flex-direction: column;
}
.hub-meta,
.hub-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.hub-meta {
  color: var(--zt-tide);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.9px;
}
.hub-meta small {
  color: var(--zt-text-3);
  font-weight: 500;
  letter-spacing: 0;
}
.hub-title {
  display: block;
  margin: 9px 0 7px;
  padding: 0;
  border: 0;
  color: var(--zt-text);
  background: transparent;
  font-size: 19px;
  font-weight: 750;
  letter-spacing: -0.018em;
  cursor: pointer;
}
.hub-card p {
  display: -webkit-box;
  overflow: hidden;
  min-height: 42px;
  margin: 0;
  color: var(--zt-text-2);
  font-size: 12px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.hub-card footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--zt-border-soft);
}
.hub-card footer > span {
  color: var(--zt-text-3);
  font-size: 10px;
}
.hub-card footer b {
  color: var(--zt-text-2);
}
.hub-card footer button {
  padding: 5px 10px;
  border: 1px solid var(--zt-primary);
  border-radius: var(--zt-radius-sm);
  color: var(--zt-primary);
  background: transparent;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
}
.hub-card footer button.joined {
  border-color: transparent;
  color: var(--zt-tide);
  background: var(--zt-tide-soft);
}
.hub-card footer button:disabled {
  border-color: var(--zt-border);
  color: var(--zt-text-3);
  background: var(--zt-surface-soft);
  cursor: default;
}
.discover-empty {
  padding: 80px 20px;
  border: 1px dashed var(--zt-border);
  border-radius: var(--zt-radius-md);
  text-align: center;
}
.discover-empty strong {
  font-size: 17px;
}
.discover-empty p {
  color: var(--zt-text-3);
}
:global(.hub-preview-dialog) {
  overflow: hidden;
  border: 1px solid var(--zt-border) !important;
  border-radius: var(--zt-radius-lg) !important;
  background: var(--zt-surface) !important;
  box-shadow: var(--zt-shadow-md) !important;
}
.hub-preview {
  display: grid;
}
.preview-cover {
  position: relative;
  display: grid;
  overflow: hidden;
  height: 176px;
  margin: -10px -20px 18px;
  color: #fff;
  place-items: center;
}
.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.82);
}
.preview-cover span {
  font-size: 44px;
  font-weight: 800;
}
.preview-meta,
.preview-stats,
.hub-preview footer {
  display: flex;
  align-items: center;
}
.preview-meta {
  justify-content: space-between;
  color: var(--zt-accent);
  font-size: 10px;
  font-weight: 750;
}
.preview-meta small {
  color: var(--zt-text-3);
  font-weight: 500;
}
.hub-preview h2 {
  margin: 10px 0 7px;
  color: var(--zt-text);
  font-size: 25px;
}
.hub-preview > p {
  margin: 0;
  color: var(--zt-text-2);
  line-height: 1.75;
}
.preview-stats {
  gap: 22px;
  margin-top: 18px;
  padding: 13px 0;
  border-block: 1px solid var(--zt-border-soft);
  color: var(--zt-text-3);
  font-size: 11px;
}
.preview-stats b {
  color: var(--zt-text);
  font-size: 15px;
}
.hub-preview footer {
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}
@media (max-width: 900px) {
  .hub-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 680px) {
  .hub-discover {
    padding: 22px 0 76px;
  }
  .discover-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .discover-heading h1 {
    font-size: 30px;
  }
  .discover-controls {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }
  .discover-controls > .el-input {
    width: 100%;
  }
  .hub-grid {
    grid-template-columns: 1fr;
  }
  .hub-visual {
    min-height: 154px;
  }
  .preview-cover {
    height: 145px;
  }
}
@media (max-width: 480px) {
  .hub-card {
    grid-template-columns: 112px minmax(0, 1fr);
  }
  .hub-card-body {
    padding: 14px;
  }
  .hub-card p {
    -webkit-line-clamp: 2;
  }
  .hub-meta small {
    display: none;
  }
}
</style>
