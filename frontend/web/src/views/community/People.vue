<template>
  <main class="profile-layout">
    <section class="profile-main">
      <button type="button" class="back" @click="router.back()">← 返回</button>
      <section v-if="loading" class="state-card" aria-live="polite">
        <span class="state-spinner"></span>
        <p>正在加载个人主页…</p>
      </section>
      <section v-else-if="!profile" class="state-card state-error">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="9" />
          <path d="M9 10h.01M15 10h.01M9 16c1.8-1.5 4.2-1.5 6 0" />
        </svg>
        <h1>暂时无法显示这个主页</h1>
        <p>用户可能不存在，或者社区服务尚未启动。</p>
        <button type="button" @click="load">重新加载</button>
      </section>
      <section v-else class="profile-card">
        <div class="profile-cover" :style="coverStyle">
          <span v-if="!profile.coverUrl" class="cover-pattern"></span>
        </div>
        <div class="identity-row">
          <div class="avatar-wrap">
            <img v-if="profile.avatar" :src="resourceUrl(profile.avatar)" alt="用户头像" /><span v-else>{{
              (profile.nickName || '社').slice(0, 1)
            }}</span>
          </div>
          <div class="profile-actions">
            <button v-if="isSelf" type="button" class="edit" @click="openEditor">编辑个人资料</button
            ><button v-else-if="isLoggedIn" type="button" class="message-user" @click="startMessage">
              发私信</button
            ><button
              v-if="!isSelf && isLoggedIn"
              type="button"
              :class="{ following: profile.followedByMe }"
              @click="toggleFollow"
            >
              {{ profile.followedByMe ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
        <div class="identity-copy">
          <h1>{{ profile.nickName || '社区成员' }}</h1>
          <span class="handle">@{{ profile.handle || `user${profile.userId}` }}</span>
          <p class="bio">
            {{ profile.bio || (isSelf ? '写一段个人简介，让同好更了解你。' : '这个人还没有填写个人简介。') }}
          </p>
          <div class="profile-meta">
            <span v-if="profile.location"
              ><svg viewBox="0 0 24 24">
                <path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" />
                <circle cx="12" cy="10" r="2.2" /></svg
              >{{ profile.location }}</span
            >
            <a v-if="profile.websiteUrl" :href="profile.websiteUrl" target="_blank" rel="noreferrer"
              ><svg viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="8.5" />
                <path
                  d="M3.8 12h16.4M12 3.5c2.2 2.3 3.3 5.1 3.3 8.5S14.2 18.2 12 20.5M12 3.5C9.8 5.8 8.7 8.6 8.7 12s1.1 6.2 3.3 8.5"
                /></svg
              >{{ websiteHost }}</a
            >
            <span
              ><svg viewBox="0 0 24 24"><path d="M5 5.5h14v15H5zM8 3v5M16 3v5M5 9h14" /></svg
              >{{ joinDate }} 加入</span
            >
          </div>
          <div class="stats">
            <span
              ><b>{{ profile.followingCount || 0 }}</b> 正在关注</span
            ><span
              ><b>{{ profile.followersCount || 0 }}</b> 关注者</span
            ><span
              ><b>{{ profile.postsCount || 0 }}</b> 篇分享</span
            >
          </div>
        </div>
      </section>

      <section v-if="profile" class="activity-card">
        <nav class="tabs">
          <button type="button" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'">
            分享 <span>{{ posts.length }}</span></button
          ><button
            type="button"
            :class="{ active: activeTab === 'comments' }"
            @click="activeTab = 'comments'"
          >
            回复 <span>{{ comments.length }}</span></button
          ><button
            v-if="isSelf"
            type="button"
            :class="{ active: activeTab === 'bookmarks' }"
            @click="activeTab = 'bookmarks'"
          >
            收藏 <span>{{ bookmarks.length }}</span>
          </button>
        </nav>
        <div v-if="activeTab === 'posts'">
          <article v-for="post in posts" :key="post.postId" class="post-row" @click="openPost(post.postId)">
            <div class="post-copy">
              <div class="post-line">
                <span>{{ post.hubName }}</span
                ><time>{{ formatTime(post.createdAt) }}</time>
              </div>
              <h2>{{ postTitle(post) }}</h2>
              <p v-if="post.title">{{ postExcerpt(post.body) }}</p>
              <footer>
                <span>{{ post.likeCount || 0 }} 赞同</span><span>{{ post.commentCount || 0 }} 评论</span
                ><span>浏览 {{ post.viewCount || 0 }}</span>
              </footer>
            </div>
            <img v-if="postCover(post)" :src="resourceUrl(postCover(post))" alt="" />
          </article>
          <div v-if="!posts.length" class="empty">还没有公开分享。</div>
        </div>
        <div v-else-if="activeTab === 'comments'">
          <article
            v-for="comment in comments"
            :key="comment.commentId"
            class="comment-row"
            @click="openPost(comment.postId)"
          >
            <div>
              <span>回复于 {{ comment.hubName }}</span
              ><time>{{ formatTime(comment.createdAt) }}</time>
            </div>
            <h3>{{ comment.postTitle || '社区讨论' }}</h3>
            <p>{{ comment.body }}</p>
            <footer>{{ comment.likeCount || 0 }} 人赞同 · 查看对话 →</footer>
          </article>
          <div v-if="!comments.length" class="empty">还没有公开回复。</div>
        </div>
        <div v-else class="bookmark-list">
          <article
            v-for="post in bookmarks"
            :key="post.postId"
            class="post-row"
            @click="openPost(post.postId)"
          >
            <div class="post-copy">
              <div class="post-line">
                <span>{{ post.hubName }}</span
                ><time>{{ formatTime(post.createdAt) }}</time>
              </div>
              <h2>{{ postTitle(post) }}</h2>
              <p>{{ bookmarkExcerpt(post.body) }}</p>
              <footer>
                <span>{{ post.likeCount || 0 }} 赞同</span><span>{{ post.commentCount || 0 }} 评论</span
                ><span>浏览 {{ post.viewCount || 0 }}</span>
              </footer>
            </div>
            <img v-if="postCover(post)" :src="resourceUrl(postCover(post))" alt="" />
          </article>
          <div v-if="!bookmarks.length" class="empty">
            还没有收藏内容。回到社区，收藏一篇你想以后再看的分享吧。
          </div>
        </div>
      </section>
    </section>

    <aside v-if="profile" class="profile-side">
      <section>
        <h3>关于这个主页</h3>
        <p>这里集中展示用户在不同兴趣现场发布的分享与参与过的讨论。</p>
        <div>
          <span>加入时间</span><b>{{ joinDate }}</b>
        </div>
        <div>
          <span>公开分享</span><b>{{ profile.postsCount || 0 }}</b>
        </div>
      </section>
    </aside>

    <el-dialog v-model="editorVisible" class="profile-editor-dialog" width="min(94vw, 620px)">
      <template #header>
        <div class="dialog-heading">
          <span>PROFILE SETTINGS</span>
          <strong>编辑个人资料</strong>
          <small>完善你的社区名片，让同好更快认识你</small>
        </div>
      </template>
      <div class="edit-cover" :style="draftCoverStyle">
        <div class="cover-caption">
          <span>主页封面</span>
          <small>建议使用 3:1 横向图片</small>
        </div>
        <div class="media-actions">
          <button v-if="draft.coverUrl" type="button" @click="draft.coverUrl = ''">移除封面</button
          ><el-upload :show-file-list="false" accept="image/*" :http-request="uploadProfileCover"
            ><button type="button">{{ uploadingCover ? '上传中…' : '更换主页封面' }}</button></el-upload
          >
        </div>
      </div>
      <div class="edit-avatar">
        <div>
          <img v-if="draft.avatar" :src="resourceUrl(draft.avatar)" alt="" /><span v-else>{{
            (draft.nickName || '我').slice(0, 1)
          }}</span>
        </div>
        <div class="avatar-actions">
          <div class="avatar-caption">
            <strong>头像</strong>
            <small>展示你的个性</small>
          </div>
          <el-upload :show-file-list="false" accept="image/*" :http-request="uploadAvatar"
            ><button type="button">{{ uploadingAvatar ? '上传中…' : '更换头像' }}</button></el-upload
          ><button v-if="draft.avatar" type="button" class="remove-avatar" @click="draft.avatar = ''">
            移除
          </button>
        </div>
      </div>
      <el-form label-position="top" class="profile-form"
        ><div class="form-grid">
          <el-form-item label="显示名"
            ><el-input v-model="draft.nickName" maxlength="20" show-word-limit /></el-form-item
          ><el-form-item label="用户名"
            ><el-input v-model="draft.handle" maxlength="30"><template #prefix>@</template></el-input>
            <p class="field-tip">你的唯一社区标识，可使用小写字母、数字和下划线。</p></el-form-item
          >
        </div>
        <el-form-item label="个人简介"
          ><el-input
            v-model="draft.bio"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="介绍你的兴趣、正在做的事或想遇见的人"
        /></el-form-item>
        <div class="form-grid">
          <el-form-item label="所在地"
            ><el-input v-model="draft.location" maxlength="80" placeholder="例如：上海" /></el-form-item
          ><el-form-item label="个人链接"
            ><el-input v-model="draft.websiteUrl" placeholder="https://"
          /></el-form-item></div
      ></el-form>
      <template #footer
        ><el-button @click="editorVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button></template
      >
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginStore } from '@/stores/loginStore.js'
import { uploadImage } from '@/utils/Api.js'
import { richTextExcerpt } from '@/utils/richText.js'
const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const loginStore = useLoginStore()
const profile = ref(null)
const posts = ref([])
const comments = ref([])
const bookmarks = ref([])
const activeTab = ref('posts')
const editorVisible = ref(false)
const loading = ref(true)
const saving = ref(false)
const uploadingAvatar = ref(false)
const uploadingCover = ref(false)
const draft = reactive({
  nickName: '',
  handle: '',
  avatar: '',
  coverUrl: '',
  bio: '',
  location: '',
  websiteUrl: '',
})
const isLoggedIn = computed(() => Boolean(loginStore.userInfo?.userId))
const isSelf = computed(() => String(loginStore.userInfo?.userId || '') === String(route.params.userId || ''))
const resourceUrl = (source) => (source ? `${proxy.Api.sourcePath}${encodeURIComponent(source)}` : '')
const coverStyle = computed(() =>
  profile.value?.coverUrl ? { backgroundImage: `url(${resourceUrl(profile.value.coverUrl)})` } : {},
)
const draftCoverStyle = computed(() =>
  draft.coverUrl ? { backgroundImage: `url(${resourceUrl(draft.coverUrl)})` } : {},
)
const websiteHost = computed(() => {
  try {
    return new URL(profile.value?.websiteUrl).host
  } catch {
    return profile.value?.websiteUrl
  }
})
const load = async () => {
  const userId = route.params.userId
  loading.value = true
  profile.value = null
  posts.value = []
  comments.value = []
  bookmarks.value = []
  const [a, b, c] = await Promise.all([
    proxy.Request({
      url: `${proxy.Api.zentideCommunityUsers}/${userId}`,
      params: {},
      showLoading: false,
      showError: false,
    }),
    proxy.Request({
      url: `${proxy.Api.zentideCommunityUsers}/${userId}/posts`,
      params: { limit: 50 },
      showLoading: false,
      showError: false,
    }),
    proxy.Request({
      url: `${proxy.Api.zentideCommunityUsers}/${userId}/comments`,
      params: { limit: 50 },
      showLoading: false,
      showError: false,
    }),
  ])
  if (a) profile.value = a.data
  if (b) posts.value = b.data || []
  if (c) comments.value = c.data || []
  if (!isSelf.value) activeTab.value = 'posts'
  if (isSelf.value) {
    const saved = await proxy.Request({
      url: proxy.Api.zentideCommunityBookmarks,
      params: { limit: 200 },
      showLoading: false,
      showError: false,
    })
    if (saved) bookmarks.value = saved.data || []
  }
  loading.value = false
}
const openEditor = () => {
  Object.assign(draft, {
    nickName: profile.value.nickName || '',
    handle: profile.value.handle || `user${profile.value.userId}`,
    avatar: profile.value.avatar || '',
    coverUrl: profile.value.coverUrl || '',
    bio: profile.value.bio || '',
    location: profile.value.location || '',
    websiteUrl: profile.value.websiteUrl || '',
  })
  editorVisible.value = true
}
const uploadAvatar = async (o) => {
  uploadingAvatar.value = true
  try {
    const v = await uploadImage(o.file, false)
    if (v) draft.avatar = v
  } finally {
    uploadingAvatar.value = false
  }
}
const uploadProfileCover = async (o) => {
  uploadingCover.value = true
  try {
    const v = await uploadImage(o.file, false)
    if (v) draft.coverUrl = v
  } finally {
    uploadingCover.value = false
  }
}
const saveProfile = async () => {
  if (draft.nickName.trim().length < 2) return proxy.Message.warning('显示名至少需要两个字符')
  if (!/^[a-z0-9_]{3,30}$/.test(draft.handle.trim().toLowerCase()))
    return proxy.Message.warning('用户名需要是 3 到 30 位小写字母、数字或下划线')
  if (draft.websiteUrl.trim() && !/^https?:\/\/[^\s]+$/i.test(draft.websiteUrl.trim()))
    return proxy.Message.warning('个人链接需要以 http:// 或 https:// 开头')
  saving.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.zentideCommunityProfileUpdate,
      params: {
        ...draft,
        nickName: draft.nickName.trim(),
        handle: draft.handle.trim().toLowerCase(),
        websiteUrl: draft.websiteUrl.trim(),
      },
      showLoading: false,
    })
    if (!result) return
    profile.value = result.data
    loginStore.saveUserInfo({
      ...loginStore.userInfo,
      nickName: profile.value.nickName,
      avatar: profile.value.avatar,
    })
    editorVisible.value = false
    proxy.Message.success('个人资料已更新')
  } finally {
    saving.value = false
  }
}
const toggleFollow = async () => {
  const active = !profile.value.followedByMe
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityUsers}/${route.params.userId}/follow`,
    params: { active },
    showLoading: false,
  })
  if (result) {
    profile.value.followedByMe = active
    profile.value.followersCount = Math.max(0, Number(profile.value.followersCount || 0) + (active ? 1 : -1))
  }
}
const startMessage = () => router.push({ name: 'messages', params: { userId: route.params.userId } })
const postImages = (p) => {
  try {
    const v = JSON.parse(p.mediaJson || '[]')
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}
const postCover = (p) => p.coverUrl || postImages(p)[0] || ''
const openPost = (postId) => router.push({ name: 'community-post', params: { postId } })
const postTitle = (post) => post.title || richTextExcerpt(post.body, 100) || '社区分享'
const postExcerpt = (body) => richTextExcerpt(body, 220)
const bookmarkExcerpt = (body) => richTextExcerpt(body, 220)
const joinDate = computed(() =>
  profile.value?.joinTime
    ? new Date(profile.value.joinTime).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
    : '最近',
)
const formatTime = (v) =>
  v ? new Date(v).toLocaleDateString('zh-CN', { year: 'numeric', month: 'numeric', day: 'numeric' }) : '刚刚'
watch(() => route.params.userId, load)
onMounted(load)
</script>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 820px) 280px;
  gap: 24px;
  align-items: start;
  padding: 25px 0 80px;
  color: #25262a;
}
.back {
  margin-bottom: 12px;
  border: 0;
  color: #777a80;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}
.profile-card,
.activity-card,
.profile-side section,
.state-card {
  overflow: hidden;
  border: 1px solid #dfe0e3;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(24, 25, 29, 0.035);
}
.state-card {
  display: grid;
  min-height: 330px;
  padding: 40px;
  place-items: center;
  align-content: center;
  color: #81848a;
  text-align: center;
}
.state-card p {
  margin: 10px 0;
  font-size: 13px;
}
.state-spinner {
  width: 26px;
  height: 26px;
  border: 2px solid #e2e3e6;
  border-top-color: #4c5264;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.state-error svg {
  width: 42px;
  height: 42px;
  fill: none;
  stroke: #878b95;
  stroke-linecap: round;
  stroke-width: 1.5;
}
.state-error h1 {
  margin: 16px 0 0;
  color: #37393e;
  font-size: 18px;
}
.state-error button {
  margin-top: 12px;
  padding: 8px 13px;
  border: 1px solid #d7d9dd;
  border-radius: 7px;
  color: #41444a;
  background: #fff;
  cursor: pointer;
}
.profile-cover {
  position: relative;
  height: 220px;
  background: linear-gradient(125deg, #2f343a, #606872);
  background-position: center;
  background-size: cover;
}
.cover-pattern {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 22% 40%, rgba(255, 255, 255, 0.15), transparent 28%),
    linear-gradient(
      135deg,
      transparent 45%,
      rgba(255, 255, 255, 0.08) 45%,
      rgba(255, 255, 255, 0.08) 55%,
      transparent 55%
    );
}
.identity-row {
  display: flex;
  justify-content: space-between;
  height: 56px;
  padding: 0 25px;
}
.avatar-wrap {
  position: relative;
  transform: translateY(-56px);
  width: 122px;
  height: 122px;
  padding: 4px;
  border-radius: 50%;
  background: #fff;
}
.avatar-wrap img,
.avatar-wrap span {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-wrap span {
  display: grid;
  place-items: center;
  color: #fff;
  background: #4d5361;
  font-size: 40px;
  font-weight: 750;
}
.profile-actions {
  padding-top: 13px;
}
.profile-actions button {
  padding: 8px 17px;
  border: 1px solid #292b30;
  border-radius: 8px;
  color: #fff;
  background: #292b30;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.profile-actions .following,
.profile-actions .edit {
  color: #404248;
  background: #fff;
  border-color: #d7d8dc;
}
.identity-copy {
  padding: 0 29px 27px;
}
.identity-copy h1 {
  margin: 3px 0 2px;
  font-size: 25px;
}
.handle {
  color: #8b8e94;
  font-size: 12px;
}
.bio {
  max-width: 620px;
  margin: 16px 0 13px;
  color: #50535a;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.profile-meta,
.stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
}
.profile-meta span,
.profile-meta a {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #84878d;
  font-size: 11px;
  text-decoration: none;
}
.profile-meta svg {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.6;
}
.stats {
  margin-top: 16px;
  gap: 19px;
}
.stats span {
  color: #777a80;
  font-size: 11px;
}
.stats b {
  color: #2f3136;
  font-size: 13px;
}
.activity-card {
  margin-top: 15px;
}
.tabs {
  display: flex;
  gap: 2px;
  padding: 5px 13px;
  border-bottom: 1px solid #e5e6e9;
}
.tabs button {
  padding: 10px 13px;
  border: 0;
  border-radius: 7px;
  color: #777a80;
  background: transparent;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}
.tabs button.active {
  color: #33375f;
  background: #eff0f6;
}
.tabs span {
  margin-left: 4px;
  color: #999;
}
.post-row {
  display: flex;
  gap: 17px;
  padding: 19px 22px;
  border-bottom: 1px solid #ececef;
  cursor: pointer;
}
.post-row:hover,
.comment-row:hover {
  background: #fafafb;
}
.post-copy {
  min-width: 0;
  flex: 1;
}
.post-line {
  display: flex;
  gap: 8px;
  color: #92949a;
  font-size: 10px;
}
.post-line span {
  color: #555a79;
  font-weight: 700;
}
.post-line time {
  margin-left: auto;
}
.post-row h2 {
  margin: 9px 0 5px;
  font-size: 16px;
}
.post-row p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #666970;
  font-size: 12px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.post-row footer {
  display: flex;
  gap: 13px;
  margin-top: 11px;
  color: #92949a;
  font-size: 10px;
}
.post-row > img {
  width: 130px;
  height: 92px;
  border-radius: 8px;
  object-fit: cover;
}
.comment-row {
  padding: 18px 22px;
  border-bottom: 1px solid #ececef;
  cursor: pointer;
}
.comment-row > div {
  display: flex;
  justify-content: space-between;
  color: #92949a;
  font-size: 10px;
}
.comment-row h3 {
  margin: 9px 0 6px;
  font-size: 13px;
}
.comment-row p {
  margin: 0;
  color: #55585e;
  font-size: 13px;
  line-height: 1.65;
}
.comment-row footer {
  margin-top: 10px;
  color: #777c9b;
  font-size: 10px;
}
.empty {
  padding: 55px;
  color: #92949a;
  text-align: center;
}
.profile-side {
  position: sticky;
  top: 80px;
}
.profile-side section {
  padding: 19px;
}
.profile-side h3 {
  margin: 0 0 9px;
  font-size: 14px;
}
.profile-side p {
  color: #777a80;
  font-size: 12px;
  line-height: 1.7;
}
.profile-side section > div {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid #ececef;
  color: #898c92;
  font-size: 11px;
}
.edit-cover {
  display: flex;
  align-items: end;
  justify-content: flex-end;
  height: 150px;
  margin: -20px -20px 0;
  padding: 12px;
  background: linear-gradient(125deg, #353a42, #69717b);
  background-position: center;
  background-size: cover;
}
.media-actions,
.avatar-actions {
  display: flex;
  align-items: center;
  gap: 7px;
}
.edit-cover button,
.edit-avatar button {
  padding: 7px 10px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 7px;
  color: #fff;
  background: rgba(22, 24, 28, 0.55);
  font-size: 11px;
  cursor: pointer;
}
.edit-avatar {
  display: flex;
  align-items: end;
  gap: 12px;
  height: 62px;
  margin-bottom: 25px;
  padding: 0 16px;
}
.edit-avatar > div:first-child {
  transform: translateY(-30px);
  width: 84px;
  height: 84px;
  padding: 3px;
  border-radius: 50%;
  background: #fff;
}
.edit-avatar img,
.edit-avatar span {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.edit-avatar span {
  display: grid;
  place-items: center;
  color: #fff;
  background: #555b66;
  font-size: 25px;
}
.edit-avatar button {
  margin-bottom: 9px;
  color: #4e5158;
  background: #fff;
  border-color: #d9dade;
}
.edit-avatar .remove-avatar {
  border-color: transparent;
  color: #898b90;
}
.field-tip {
  margin: 5px 0 0;
  color: #96989d;
  font-size: 10px;
  line-height: 1.5;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
  .profile-side {
    display: none;
  }
}
@media (max-width: 640px) {
  .profile-cover {
    height: 150px;
  }
  .identity-row {
    padding-inline: 17px;
  }
  .avatar-wrap {
    width: 96px;
    height: 96px;
    transform: translateY(-43px);
  }
  .identity-copy {
    padding: 0 20px 23px;
  }
  .post-row {
    padding-inline: 16px;
  }
  .post-row > img {
    width: 92px;
    height: 72px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
/* Unified editorial community skin */
.profile-layout {
  color: var(--zt-text);
}
.back {
  color: var(--zt-text-2);
}
.back:hover {
  color: var(--zt-primary);
}
.profile-card,
.activity-card,
.profile-side section,
.state-card {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-md);
  background: var(--zt-surface);
  box-shadow: var(--zt-shadow-sm);
}
.profile-cover {
  background: linear-gradient(120deg, #173f34, #5e7169);
}
.cover-pattern {
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.05), transparent 55%),
    radial-gradient(circle at 22% 35%, rgba(255, 255, 255, 0.13), transparent 26%);
}
.avatar-wrap,
.edit-avatar > div:first-child {
  background: var(--zt-surface);
}
.avatar-wrap span,
.edit-avatar span {
  background: var(--zt-primary);
}
.profile-actions button {
  border-color: var(--zt-primary);
  border-radius: var(--zt-radius-sm);
  background: var(--zt-primary);
}
.profile-actions button:hover {
  background: var(--zt-primary-hover);
}
.profile-actions .following,
.profile-actions .edit {
  color: var(--zt-primary);
  background: var(--zt-surface);
  border-color: var(--zt-border);
}
.identity-copy h1,
.stats b,
.post-row h2,
.comment-row h3,
.profile-side h3 {
  color: var(--zt-text);
}
.handle,
.profile-meta span,
.profile-meta a,
.stats span,
.post-line,
.post-row footer,
.comment-row > div,
.empty,
.field-tip {
  color: var(--zt-text-3);
}
.bio,
.post-row p,
.comment-row p,
.profile-side p {
  color: var(--zt-text-2);
}
.tabs,
.post-row,
.comment-row,
.profile-side section > div {
  border-color: var(--zt-border-soft);
}
.tabs button {
  border-radius: var(--zt-radius-sm);
  color: var(--zt-text-2);
}
.tabs button.active {
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.post-row:hover,
.comment-row:hover {
  background: var(--zt-surface-hover);
}
.post-line span,
.comment-row footer {
  color: var(--zt-primary);
}
.edit-cover {
  background: linear-gradient(120deg, #173f34, #5e7169);
}
.edit-cover button,
.edit-avatar button {
  border-radius: var(--zt-radius-sm);
}
.edit-avatar button {
  color: var(--zt-text-2);
  background: var(--zt-surface);
  border-color: var(--zt-border);
}

/* 知潮主页：克制的潮汐感，封面只做身份背景，不与用户内容争夺注意力。 */
.profile-layout {
  grid-template-columns: minmax(0, 800px) 260px;
  gap: 28px;
  padding: 30px 0 88px;
}
.profile-card,
.activity-card,
.state-card {
  border-radius: 8px;
  box-shadow: none;
}
.profile-cover {
  height: 176px;
  isolation: isolate;
  background: linear-gradient(125deg, var(--zt-primary), var(--zt-tide));
}
.profile-cover::after {
  position: absolute;
  z-index: 1;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(8, 35, 31, 0.05), rgba(8, 35, 31, 0.34)),
    repeating-linear-gradient(168deg, transparent 0 34px, rgba(255, 255, 255, 0.035) 35px 36px);
  content: '';
}
.cover-pattern {
  z-index: 2;
}
.identity-row {
  height: 50px;
  padding-inline: 26px;
}
.avatar-wrap {
  z-index: 2;
  width: 106px;
  height: 106px;
  transform: translateY(-46px);
  box-shadow: 0 8px 22px rgba(17, 46, 39, 0.12);
}
.avatar-wrap span {
  font-size: 34px;
}
.profile-actions {
  padding-top: 12px;
}
.profile-actions button {
  min-height: 34px;
  padding: 7px 15px;
}
.identity-copy {
  padding: 0 28px 27px;
}
.identity-copy h1 {
  margin: 4px 0 1px;
  font-size: 26px;
  font-weight: 770;
  letter-spacing: -0.035em;
}
.bio {
  margin: 15px 0 13px;
}
.activity-card {
  margin-top: 14px;
}
.tabs {
  gap: 20px;
  padding: 0 21px;
}
.tabs button {
  position: relative;
  padding: 15px 1px 13px;
  border-radius: 0;
}
.tabs button.active {
  color: var(--zt-primary);
  background: transparent;
}
.tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--zt-tide);
  content: '';
}
.post-row {
  padding: 20px 22px;
}
.post-row > img {
  width: 124px;
  height: 88px;
  border-radius: 6px;
}
.profile-side section {
  padding: 16px 2px;
  border: 0;
  border-top: 2px solid var(--zt-tide);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.profile-side h3 {
  margin-bottom: 10px;
}

/* Profile editor: a calmer, editorial sheet instead of a stock admin modal. */
:global(.profile-editor-dialog) {
  overflow: hidden;
  border: 1px solid var(--zt-border) !important;
  border-radius: 16px !important;
  background: var(--zt-surface) !important;
  box-shadow: 0 24px 70px rgba(24, 37, 31, 0.18) !important;
}
:global(.profile-editor-dialog .el-dialog__header) {
  padding: 18px 24px 16px !important;
  margin: 0 !important;
  border-bottom: 1px solid var(--zt-border-soft);
  background: color-mix(in srgb, var(--zt-surface) 92%, var(--zt-primary-soft));
}
:global(.profile-editor-dialog .el-dialog__headerbtn) {
  top: 18px !important;
  right: 18px !important;
  width: 28px !important;
  height: 28px !important;
  border-radius: 50%;
  transition: background-color 0.15s ease;
}
:global(.profile-editor-dialog .el-dialog__headerbtn:hover) {
  background: var(--zt-primary-soft);
}
:global(.profile-editor-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: var(--zt-text-3) !important;
  font-size: 16px;
}
:global(.profile-editor-dialog .el-dialog__body) {
  max-height: min(72vh, 650px);
  overflow-y: auto;
  padding: 0 24px 22px !important;
}
:global(.profile-editor-dialog .el-dialog__footer) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 24px 18px !important;
  border-top: 1px solid var(--zt-border-soft);
  background: var(--zt-surface);
}
:global(.profile-editor-dialog .el-dialog__footer .el-button + .el-button) {
  margin-left: 0;
}
.dialog-heading {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-right: 35px;
}
.dialog-heading span {
  color: var(--zt-tide);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.16em;
}
.dialog-heading strong {
  color: var(--zt-text);
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.01em;
}
.dialog-heading small {
  color: var(--zt-text-3);
  font-size: 11px;
}
.edit-cover {
  position: relative;
  height: 168px;
  margin: 0 -24px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
  background:
    linear-gradient(180deg, rgba(12, 38, 30, 0.1), rgba(12, 38, 30, 0.56)),
    linear-gradient(120deg, #173f34, #5e7169);
  background-position: center;
  background-size: cover;
}
.edit-cover::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.09), transparent 45%);
}
.cover-caption {
  position: absolute;
  z-index: 1;
  top: 17px;
  left: 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #fff;
}
.cover-caption span {
  font-size: 12px;
  font-weight: 700;
}
.cover-caption small {
  color: rgba(255, 255, 255, 0.7);
  font-size: 10px;
}
.media-actions {
  position: relative;
  z-index: 1;
  gap: 8px;
}
.edit-cover .media-actions {
  position: absolute;
  right: 20px;
  bottom: 16px;
}
.edit-cover button,
.edit-avatar button {
  min-height: 31px;
  padding: 6px 11px;
  border: 1px solid rgba(255, 255, 255, 0.52);
  border-radius: 6px;
  color: #fff;
  background: rgba(20, 34, 29, 0.48);
  font-size: 11px;
  font-weight: 650;
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}
.edit-cover button:hover {
  border-color: rgba(255, 255, 255, 0.8);
  background: rgba(20, 34, 29, 0.68);
}
.edit-avatar {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 84px;
  margin: 0 0 22px;
  padding: 0 0 0 4px;
}
.edit-avatar > div:first-child {
  flex: 0 0 86px;
  width: 86px;
  height: 86px;
  margin-top: -26px;
  transform: none;
  padding: 4px;
  border: 1px solid var(--zt-border);
  border-radius: 50%;
  background: var(--zt-surface);
  box-shadow: 0 7px 16px rgba(24, 37, 31, 0.14);
}
.edit-avatar img,
.edit-avatar span {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.edit-avatar span {
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--zt-primary);
  font-size: 25px;
  font-weight: 700;
}
.avatar-actions {
  align-items: center;
  gap: 9px;
  padding-top: 4px;
}
.avatar-caption {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 72px;
  margin-right: 3px;
}
.avatar-caption strong {
  color: var(--zt-text);
  font-size: 12px;
}
.avatar-caption small {
  color: var(--zt-text-3);
  font-size: 10px;
}
.edit-avatar button {
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
  border-color: transparent;
  backdrop-filter: none;
}
.edit-avatar button:hover {
  background: #d6e2da;
}
.edit-avatar .remove-avatar {
  color: var(--zt-text-3);
  background: transparent;
  border-color: transparent;
}
.edit-avatar .remove-avatar:hover {
  color: var(--zt-danger);
  background: var(--zt-accent-soft);
}
.profile-form {
  padding-top: 1px;
}
:global(.profile-editor-dialog .el-form-item) {
  margin-bottom: 18px;
}
:global(.profile-editor-dialog .el-form-item__label) {
  height: auto;
  padding-bottom: 6px !important;
  color: var(--zt-text-2) !important;
  font-size: 11px !important;
  font-weight: 700 !important;
  letter-spacing: 0.01em;
  line-height: 1.2 !important;
}
:global(.profile-editor-dialog .el-input__wrapper),
:global(.profile-editor-dialog .el-textarea__inner) {
  min-height: 38px;
  border-radius: 7px !important;
  background: #fffefa !important;
}
:global(.profile-editor-dialog .el-textarea__inner) {
  min-height: 102px;
  padding: 10px 12px;
  line-height: 1.6;
}
:global(.profile-editor-dialog .el-input__inner),
:global(.profile-editor-dialog .el-textarea__inner) {
  color: var(--zt-text) !important;
  font-size: 13px;
}
:global(.profile-editor-dialog .el-input__inner::placeholder),
:global(.profile-editor-dialog .el-textarea__inner::placeholder) {
  color: #aab0ab;
}
:global(.profile-editor-dialog .el-input__count),
:global(.profile-editor-dialog .el-textarea .el-input__count) {
  color: var(--zt-text-3);
  font-size: 10px;
}
:global(.profile-editor-dialog .el-button) {
  min-height: 34px;
  padding-inline: 15px;
  font-size: 12px;
  font-weight: 650;
}
:global(.profile-editor-dialog .el-button--primary) {
  --el-button-bg-color: var(--zt-primary);
  --el-button-border-color: var(--zt-primary);
  --el-button-hover-bg-color: var(--zt-primary-hover);
  --el-button-hover-border-color: var(--zt-primary-hover);
}
@media (max-width: 640px) {
  :global(.profile-editor-dialog) {
    width: calc(100vw - 24px) !important;
    margin: 12px auto !important;
    border-radius: 12px !important;
  }
  :global(.profile-editor-dialog .el-dialog__header) {
    padding: 15px 17px 13px !important;
  }
  :global(.profile-editor-dialog .el-dialog__body) {
    max-height: calc(100vh - 160px);
    padding: 0 17px 15px !important;
  }
  :global(.profile-editor-dialog .el-dialog__footer) {
    padding: 12px 17px 15px !important;
  }
  .edit-cover {
    height: 136px;
    margin-inline: -17px;
    padding-inline: 14px;
  }
  .edit-cover .media-actions {
    right: 14px;
    bottom: 12px;
  }
  .cover-caption {
    top: 13px;
    left: 14px;
  }
  .edit-avatar {
    margin-bottom: 17px;
    padding-left: 0;
  }
  .edit-avatar > div:first-child {
    flex-basis: 72px;
    width: 72px;
    height: 72px;
    margin-top: -20px;
  }
  .avatar-caption {
    display: none;
  }
  .avatar-actions {
    flex-wrap: wrap;
  }
  .profile-layout {
    padding: 18px 0 76px;
  }
  .profile-cover {
    height: 132px;
  }
  .identity-row {
    padding-inline: 18px;
  }
  .avatar-wrap {
    width: 88px;
    height: 88px;
    transform: translateY(-37px);
  }
  .identity-copy {
    padding: 0 20px 23px;
  }
  .identity-copy h1 {
    font-size: 23px;
  }
  .tabs {
    padding-inline: 17px;
  }
  .post-row {
    padding: 17px 16px;
  }
  .post-row > img {
    width: 92px;
    height: 72px;
  }
}
</style>
