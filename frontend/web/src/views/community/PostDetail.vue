<template>
  <main class="detail-layout">
    <section class="detail-main">
      <button type="button" class="back" @click="router.back()">← 返回社区</button>
      <article v-if="post" class="post-detail">
        <header class="post-meta">
          <router-link to="/community">{{ post.hubName }}</router-link
          ><span>由</span
          ><router-link :to="{ name: 'people', params: { userId: post.authorId } }"
            >@{{ post.authorLabel }}</router-link
          ><span>发布 · {{ formatTime(post.createdAt) }}</span
          ><em class="detail-type">{{ typeLabel(post.postType, post.postTypeLabel) }}</em
          ><span v-if="isOwner" class="owner-actions"
            ><button type="button" @click="editPost">编辑</button
            ><button type="button" class="danger" @click="deletePost">删除</button></span
          >
        </header>
        <h1>{{ post.title || typeLabel(post.postType, post.postTypeLabel) }}</h1>
        <img v-if="post.coverUrl" class="cover-image" :src="resourceUrl(post.coverUrl)" alt="帖子封面" />
        <div class="post-body rich-content" v-html="safePostBody"></div>
        <div v-if="images.length" class="image-gallery" :class="`count-${Math.min(images.length, 3)}`">
          <button v-for="(image, index) in images" :key="image" type="button" @click="previewIndex = index">
            <img :src="resourceUrl(image)" :alt="`帖子图片 ${index + 1}`" />
          </button>
        </div>
        <div v-if="post.changeId" class="verified">
          <span>已确认变化</span
          ><button
            type="button"
            @click="router.push({ path: '/changes', query: { changeId: post.changeId } })"
          >
            {{ post.changeTitle || '查看变化与原始证据' }} →
          </button>
        </div>
        <div class="tags">
          <span v-if="post.topicNames"># {{ post.topicNames }}</span
          ><span v-if="post.eventTitle">活动 · {{ post.eventTitle }}</span>
        </div>
        <footer>
          <button type="button" :class="{ active: post.liked }" @click="togglePostLike">
            <svg viewBox="0 0 24 24"><path d="m6 14 6-6 6 6" /></svg>{{ post.likeCount || 0 }} 赞同</button
          ><button type="button">
            <svg viewBox="0 0 24 24">
              <path
                d="M20 15a3 3 0 0 1-3 3H9l-5 3v-6a3 3 0 0 1-1-2.2V7a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3Z"
              /></svg
            >{{ post.commentCount || 0 }} 评论</button
          ><button type="button" :class="{ active: post.bookmarked }" @click="toggleBookmark">
            <svg viewBox="0 0 24 24"><path d="M6 4.5h12v16l-6-3.8-6 3.8Z" /></svg
            >{{ post.bookmarked ? '已收藏' : '收藏' }}
          </button>
          <span class="post-view-count">浏览 {{ post.viewCount || 0 }}</span>
        </footer>
      </article>

      <section class="discussion">
        <header>
          <div>
            <h2>讨论</h2>
            <p>{{ comments.length }} 条评论 · 可以回复任意一层</p>
          </div>
        </header>
        <div class="root-compose">
          <textarea v-model="commentDraft" rows="4" placeholder="加入讨论，分享你的观点或经验…"></textarea
          ><button type="button" @click="submitRootComment">发表评论</button>
        </div>
        <div v-if="commentTree.length" class="comment-tree">
          <CommentThread
            v-for="comment in commentTree"
            :key="comment.commentId"
            :comment="comment"
            :resource-url="resourceUrl"
            @like="toggleCommentLike"
            @share="shareComment"
            @reply="submitReply"
          />
        </div>
        <div v-else class="no-comments">还没有评论，来开启第一段对话。</div>
      </section>
    </section>
    <aside v-if="post" class="detail-side">
      <section>
        <span>来自兴趣现场</span>
        <h3>{{ post.hubName }}</h3>
        <p>围绕同一兴趣继续阅读相关分享，也可以回到现场参与正在发生的讨论。</p>
        <router-link
          class="assistant-link"
          :to="{
            name: 'community-post-assistant',
            params: { hubId: post.hubId, postId: post.postId },
          }"
          >问问现场小助手</router-link
        >
        <router-link to="/community">回到兴趣现场 →</router-link>
      </section>
    </aside>

    <el-image-viewer
      v-if="previewIndex !== null"
      :url-list="images.map(resourceUrl)"
      :initial-index="previewIndex"
      @close="previewIndex = null"
    />
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'
import CommentThread from '@/components/community/CommentThread.vue'
import { sanitizeRichText } from '@/utils/richText.js'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const loginStore = useLoginStore()
const post = ref(null)
const comments = ref([])
const commentDraft = ref('')
const previewIndex = ref(null)
const loggedIn = () => Object.keys(loginStore.userInfo || {}).length > 0
const isOwner = computed(
  () => Boolean(post.value) && String(post.value.authorId) === String(loginStore.userInfo?.userId || ''),
)
const safePostBody = computed(() => sanitizeRichText(post.value?.body || ''))
const images = computed(() => {
  try {
    const value = JSON.parse(post.value?.mediaJson || '[]')
    return Array.isArray(value) ? value.filter(Boolean) : []
  } catch {
    return []
  }
})
const commentTree = computed(() => {
  const nodes = new Map(comments.value.map((item) => [item.commentId, { ...item, children: [] }]))
  const roots = []
  nodes.forEach((node) => {
    const parent = nodes.get(node.parentCommentId)
    if (parent) parent.children.push(node)
    else roots.push(node)
  })
  return roots
})
const resourceUrl = (source) => (source ? `${proxy.Api.sourcePath}${encodeURIComponent(source)}` : '')
const load = async () => {
  const postId = route.params.postId
  const [postResult, commentResult] = await Promise.all([
    proxy.Request({ url: `${proxy.Api.zentideCommunityPosts}/${postId}`, params: {}, showLoading: false }),
    proxy.Request({
      url: `${proxy.Api.zentideCommunityPosts}/${postId}/comments/list`,
      params: { limit: 200 },
      showLoading: false,
    }),
  ])
  if (postResult) post.value = postResult.data
  if (commentResult) comments.value = commentResult.data || []
}
const submitComment = async (body, parentCommentId = null) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.value.postId}/comments`,
    params: { body, parentCommentId },
    showLoading: false,
  })
  if (!result) return false
  comments.value.push(result.data)
  post.value.commentCount = Number(post.value.commentCount || 0) + 1
  return true
}
const submitRootComment = async () => {
  const body = commentDraft.value.trim()
  if (body.length < 2) return proxy.Message.warning('评论至少需要两个字符')
  if (await submitComment(body)) commentDraft.value = ''
}
const submitReply = async ({ parent, body }) => {
  await submitComment(body, parent.commentId)
}
const toggleCommentLike = async (comment) => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !comment.liked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityComments}/${comment.commentId}/like`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    const stored = comments.value.find((item) => item.commentId === comment.commentId)
    if (stored) {
      stored.liked = active
      stored.likeCount = Math.max(0, Number(stored.likeCount || 0) + (active ? 1 : -1))
    }
  }
}
const togglePostLike = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !post.value.liked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.value.postId}/like`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    post.value.liked = active
    post.value.likeCount = Math.max(0, Number(post.value.likeCount || 0) + (active ? 1 : -1))
  }
}
const toggleBookmark = async () => {
  if (!loggedIn()) return (loginStore.showLogin = true)
  const active = !post.value.bookmarked
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPosts}/${post.value.postId}/bookmark`,
    params: { active },
    showLoading: false,
  })
  if (result) post.value.bookmarked = active
}
const editPost = () => router.push({ path: '/community', query: { editPostId: post.value.postId } })
const deletePost = () =>
  proxy.Confirm({
    message: '删除后帖子、评论和互动记录将无法恢复，确定删除吗？',
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityPosts}/${post.value.postId}/delete`,
        params: {},
        showLoading: false,
      })
      if (!result) return
      proxy.Message.success('帖子已删除')
      router.push('/community')
    },
  })
const shareComment = async (comment) => {
  const url = `${location.origin}${location.pathname}#comment-${comment.commentId}`
  await navigator.clipboard.writeText(url)
  proxy.Message.success('评论链接已复制')
}
const formatTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
    : '刚刚'
const typeLabel = (value, provided) => provided || value || '分享'
onMounted(load)
</script>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 840px) 280px;
  gap: 24px;
  align-items: start;
  padding: 28px 0 80px;
}
.back {
  margin-bottom: 14px;
  border: 0;
  color: #676a70;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}
.post-detail,
.discussion,
.detail-side section {
  border: 1px solid #dfe0e3;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(24, 25, 29, 0.035);
}
.post-detail {
  padding: 24px 28px;
}
.post-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #96989e;
  font-size: 11px;
}
.post-meta a {
  color: #555a79;
  font-weight: 650;
  text-decoration: none;
}
.owner-actions {
  display: flex;
  gap: 4px;
  margin-left: auto;
}
.owner-actions button {
  padding: 4px 7px;
  border: 0;
  border-radius: 4px;
  color: #59605d;
  background: #f0f1ef;
  font-size: 10px;
  cursor: pointer;
}
.owner-actions button:hover {
  color: #173c33;
  background: #e5ebe5;
}
.owner-actions button.danger:hover {
  color: #a54e3b;
  background: #f7e9e4;
}
.post-detail h1 {
  margin: 15px 0 17px;
  color: #202125;
  font-size: 28px;
  line-height: 1.32;
  letter-spacing: -0.025em;
}
.cover-image {
  display: block;
  width: 100%;
  max-height: 460px;
  margin-bottom: 20px;
  border-radius: 10px;
  object-fit: cover;
}
.post-body {
  color: #3f4147;
  font-size: 16px;
  line-height: 1.85;
  overflow-wrap: anywhere;
}
.rich-content :deep(p) {
  margin: 0 0 14px;
}
.rich-content :deep(p:last-child) {
  margin-bottom: 0;
}
.rich-content :deep(h2) {
  margin: 28px 0 10px;
  color: #24262b;
  font-size: 23px;
  line-height: 1.42;
}
.rich-content :deep(h3) {
  margin: 23px 0 9px;
  color: #292b31;
  font-size: 19px;
  line-height: 1.48;
}
.rich-content :deep(ul),
.rich-content :deep(ol) {
  margin: 12px 0 17px;
  padding-left: 29px;
}
.rich-content :deep(li) {
  margin: 4px 0;
}
.rich-content :deep(blockquote) {
  margin: 20px 0;
  padding: 6px 0 6px 17px;
  border-left: 3px solid #bfc3cd;
  color: #62666e;
}
.rich-content :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  background: #f0f1f3;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 0.88em;
}
.rich-content :deep(pre) {
  overflow: auto;
  margin: 18px 0;
  padding: 16px 18px;
  border-radius: 9px;
  color: #e8e9ec;
  background: #25282e;
  font-size: 14px;
  line-height: 1.65;
}
.rich-content :deep(pre code) {
  padding: 0;
  background: transparent;
}
.rich-content :deep(a) {
  color: #4557a5;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.rich-content :deep(img),
.rich-content :deep(video) {
  display: block;
  width: auto;
  max-width: 100%;
  max-height: 680px;
  margin: 22px auto;
  border-radius: 8px;
  background: #eef0ef;
}
.rich-content :deep(video) {
  width: 100%;
  background: #151817;
}
.image-gallery {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 19px 0;
}
.image-gallery.count-1 {
  grid-template-columns: 1fr;
}
.image-gallery.count-3 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.image-gallery.count-3 button:first-child {
  grid-row: span 2;
}
.image-gallery button {
  overflow: hidden;
  min-height: 180px;
  padding: 0;
  border: 0;
  border-radius: 9px;
  background: #eee;
  cursor: zoom-in;
}
.image-gallery img {
  display: block;
  width: 100%;
  height: 100%;
  max-height: 430px;
  object-fit: cover;
}
.verified {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
  padding: 12px 14px;
  border: 1px solid #d8e9e0;
  border-left: 3px solid #3b8d68;
  border-radius: 8px;
  background: #f5faf7;
}
.verified span {
  color: #387459;
  font-size: 10px;
  font-weight: 800;
}
.verified button {
  border: 0;
  color: #435f52;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}
.tags {
  display: flex;
  gap: 6px;
}
.tags span {
  padding: 4px 8px;
  border: 1px solid #e3e4e8;
  border-radius: 6px;
  color: #6c7077;
  background: #f8f8f9;
  font-size: 10px;
}
.post-detail > footer {
  display: flex;
  gap: 5px;
  margin-top: 22px;
  padding-top: 13px;
  border-top: 1px solid #ececef;
}
.post-detail > footer button {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 10px;
  border: 0;
  border-radius: 7px;
  color: #777a80;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}
.post-detail > footer button:hover,
.post-detail > footer button.active {
  color: #474d83;
  background: #eff0f6;
}
.post-detail > footer svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}
.post-detail > footer .post-view-count {
  display: inline-flex;
  align-items: center;
  margin-left: auto;
  padding: 7px 4px;
  color: #949792;
  font-size: 11px;
}
.discussion {
  margin-top: 16px;
  padding: 24px 28px;
}
.discussion > header h2 {
  margin: 0;
  font-size: 20px;
}
.discussion > header p {
  margin: 5px 0 0;
  color: #92949a;
  font-size: 11px;
}
.root-compose {
  margin: 20px 0 8px;
  padding: 12px;
  border: 1px solid #dfe0e4;
  border-radius: 10px;
  background: #fafafb;
}
.root-compose textarea {
  display: block;
  width: 100%;
  resize: vertical;
  border: 0;
  outline: 0;
  color: #333;
  background: transparent;
  font: inherit;
  line-height: 1.6;
}
.root-compose button {
  display: block;
  margin: 8px 0 0 auto;
  padding: 7px 13px;
  border: 0;
  border-radius: 7px;
  color: #fff;
  background: #292b30;
  cursor: pointer;
}
.no-comments {
  padding: 45px 0;
  color: #92949a;
  text-align: center;
}
.detail-side {
  position: sticky;
  top: 80px;
}
.detail-side section {
  padding: 18px;
}
.detail-side .assistant-link {
  display: block;
  margin: 15px 0 8px;
  padding: 9px 11px;
  border: 1px solid var(--zt-border);
  border-radius: 6px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-weight: 680;
  text-decoration: none;
}
.detail-side span {
  color: #96989d;
  font-size: 10px;
}
.detail-side h3 {
  margin: 7px 0;
  font-size: 16px;
}
.detail-side p {
  color: #777a80;
  font-size: 12px;
  line-height: 1.65;
}
.detail-side a {
  color: #4d557f;
  font-size: 11px;
  text-decoration: none;
}
@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .detail-side {
    display: none;
  }
}
@media (max-width: 640px) {
  .detail-layout {
    padding-top: 14px;
  }
  .post-detail,
  .discussion {
    padding: 19px 17px;
  }
  .post-detail h1 {
    font-size: 23px;
  }
  .post-body {
    font-size: 15px;
  }
  .image-gallery {
    grid-template-columns: 1fr;
  }
  .image-gallery button {
    min-height: 150px;
  }
}
/* Unified editorial community skin */
.back {
  color: var(--zt-text-2);
}
.back:hover {
  color: var(--zt-primary);
}
.post-detail,
.discussion,
.detail-side section {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-md);
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-sm);
}
.post-meta,
.discussion > header p,
.detail-side span,
.no-comments {
  color: var(--zt-text-3);
}
.post-meta a,
.detail-side a {
  color: var(--zt-primary);
}
.detail-type {
  margin-left: auto;
  padding: 3px 7px;
  border: 1px solid var(--zt-border);
  border-radius: 4px;
  color: var(--zt-text-2);
  background: var(--zt-surface-hover);
  font-size: 9px;
  font-style: normal;
}
.detail-type + .owner-actions {
  margin-left: 2px;
}
.post-detail h1,
.discussion > header h2,
.detail-side h3,
.rich-content :deep(h2),
.rich-content :deep(h3) {
  color: var(--zt-text);
}
.post-body,
.detail-side p {
  color: var(--zt-text-2);
}
.rich-content :deep(a) {
  color: var(--zt-primary);
}
.rich-content :deep(blockquote) {
  border-color: #9eaba5;
  color: var(--zt-text-2);
}
.rich-content :deep(code) {
  background: var(--zt-surface-soft);
}
.verified {
  border-color: #cbd7d1;
  border-left-color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.verified span,
.verified button {
  color: var(--zt-primary);
}
.tags span {
  border-color: var(--zt-border-soft);
  color: var(--zt-text-2);
  background: var(--zt-surface-hover);
}
.post-detail > footer {
  border-color: var(--zt-border-soft);
}
.post-detail > footer button {
  color: var(--zt-text-2);
}
.post-detail > footer button:hover,
.post-detail > footer button.active {
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.root-compose {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-sm);
  background: var(--zt-surface-hover);
}
.root-compose textarea {
  color: var(--zt-text);
}
.root-compose button {
  border-radius: var(--zt-radius-sm);
  background: var(--zt-primary);
}
.root-compose button:hover {
  background: var(--zt-primary-hover);
}

/* 知潮详情页：像一张可阅读、可继续对话的内容纸张，而不是后台详情面板。 */
.detail-layout {
  grid-template-columns: minmax(0, 820px) 260px;
  gap: 28px;
  padding: 30px 0 88px;
}
.post-detail,
.discussion {
  border-radius: 8px;
  box-shadow: none;
}
.post-detail {
  position: relative;
  padding: 30px 34px 26px;
}
.post-detail::before {
  position: absolute;
  top: -1px;
  left: 34px;
  width: 56px;
  height: 2px;
  border-radius: 2px;
  background: var(--zt-tide);
  content: '';
}
.post-meta {
  gap: 7px;
  font-size: 10px;
}
.post-detail h1 {
  margin: 18px 0 19px;
  font-size: clamp(26px, 3vw, 34px);
  font-weight: 760;
  line-height: 1.28;
  letter-spacing: -0.04em;
}
.cover-image {
  max-height: 420px;
  border-radius: 6px;
  filter: saturate(0.9);
}
.post-body {
  max-width: 730px;
  color: var(--zt-text-2);
  font-size: 16px;
  line-height: 1.92;
}
.tags {
  margin-top: 18px;
}
.post-detail > footer {
  gap: 12px;
  margin-top: 24px;
  padding-top: 15px;
}
.post-detail > footer button {
  padding: 6px 2px;
  border-radius: 3px;
}
.post-detail > footer button:hover,
.post-detail > footer button.active {
  background: transparent;
}
.discussion {
  margin-top: 14px;
  padding: 26px 34px 30px;
}
.discussion > header h2 {
  font-size: 21px;
  letter-spacing: -0.025em;
}
.root-compose {
  margin-top: 19px;
  padding: 14px;
  background: color-mix(in srgb, var(--zt-surface) 84%, var(--zt-tide-soft));
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}
.root-compose:focus-within {
  border-color: color-mix(in srgb, var(--zt-tide) 52%, var(--zt-border));
  box-shadow: 0 0 0 3px rgba(53, 119, 111, 0.06);
}
.root-compose button {
  min-height: 33px;
  padding-inline: 14px;
}
.detail-side section {
  padding: 16px 2px;
  border: 0;
  border-top: 2px solid var(--zt-tide);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.detail-side span {
  color: var(--zt-tide);
  font-weight: 720;
  letter-spacing: 0.08em;
}
.detail-side h3 {
  margin: 9px 0 8px;
}
@media (max-width: 640px) {
  .detail-layout {
    padding: 17px 0 76px;
  }
  .post-detail,
  .discussion {
    padding: 21px 18px;
  }
  .post-detail::before {
    left: 18px;
  }
  .post-detail h1 {
    font-size: 25px;
  }
  .post-body {
    font-size: 15px;
    line-height: 1.82;
  }
}
</style>
