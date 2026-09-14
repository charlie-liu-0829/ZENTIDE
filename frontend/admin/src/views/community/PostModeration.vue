<template>
  <main class="page">
    <header class="heading">
      <div>
        <span>COMMUNITY GOVERNANCE</span>
        <h1>帖子审核</h1>
        <p>处理违规内容时优先隐藏，保留记录并支持恢复。</p>
      </div>
      <el-button @click="load" :loading="loading">刷新</el-button>
    </header>
    <section class="filters">
      <el-input v-model="query" clearable placeholder="搜索标题或正文" @keyup.enter="load" /><el-select
        v-model="status"
        clearable
        placeholder="全部状态"
        @change="load"
        ><el-option label="已发布" value="PUBLISHED" /><el-option label="已隐藏" value="HIDDEN" /><el-option
          label="已移除"
          value="REMOVED" /></el-select
      ><el-button type="primary" @click="load">搜索</el-button>
    </section>
    <el-table :data="posts" v-loading="loading" stripe @row-click="openDetail">
      <el-table-column label="帖子" min-width="280"
        ><template #default="s"
          ><strong>{{ s.row.title || '无标题' }}</strong>
          <p>{{ excerpt(s.row.body) }}</p></template
        ></el-table-column
      ><el-table-column prop="hubName" label="兴趣现场" width="160" /><el-table-column
        prop="authorLabel"
        label="作者"
        width="130"
      /><el-table-column prop="postTypeLabel" label="类型" width="110" /><el-table-column
        prop="status"
        label="状态"
        width="110"
        ><template #default="s"
          ><el-tag :type="tagType(s.row.status)">{{ statusLabel(s.row.status) }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" width="180" fixed="right"
        ><template #default="s"
          ><el-button
            v-if="s.row.status !== 'PUBLISHED'"
            link
            type="success"
            @click.stop="changeStatus(s.row, 'PUBLISHED')"
            >恢复发布</el-button
          ><el-button
            v-if="s.row.status === 'PUBLISHED'"
            link
            type="warning"
            @click.stop="changeStatus(s.row, 'HIDDEN')"
            >隐藏</el-button
          ><el-button
            v-if="s.row.status !== 'REMOVED'"
            link
            type="danger"
            @click.stop="changeStatus(s.row, 'REMOVED')"
            >移除</el-button
          ></template
        ></el-table-column
      >
    </el-table>
    <div v-if="!loading && !posts.length" class="empty">没有符合条件的帖子。</div>
    <el-dialog v-model="detailVisible" title="帖子详情" width="min(92vw, 760px)" top="7vh" destroy-on-close>
      <template v-if="selectedPost">
        <div class="detail-meta">
          <el-tag :type="tagType(selectedPost.status)">{{ statusLabel(selectedPost.status) }}</el-tag
          ><span>{{ selectedPost.hubName }}</span
          ><span>{{ selectedPost.authorLabel }}</span
          ><span>{{ selectedPost.createdAt }}</span>
        </div>
        <h2 class="detail-title">{{ selectedPost.title || '无标题' }}</h2>
        <div class="detail-body" v-html="selectedPost.body"></div>
      </template>
      <template #footer>
        <div class="detail-actions" v-if="selectedPost">
          <el-button
            v-if="selectedPost.status !== 'PUBLISHED'"
            type="success"
            plain
            @click="changeStatusAndClose(selectedPost, 'PUBLISHED')"
            >恢复发布</el-button
          >
          <el-button
            v-if="selectedPost.status === 'PUBLISHED'"
            type="warning"
            plain
            @click="changeStatusAndClose(selectedPost, 'HIDDEN')"
            >隐藏帖子</el-button
          >
          <el-button
            v-if="selectedPost.status !== 'REMOVED'"
            type="danger"
            plain
            @click="changeStatusAndClose(selectedPost, 'REMOVED')"
            >移除帖子</el-button
          >
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </main>
</template>
<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const posts = ref([])
const loading = ref(false)
const query = ref('')
const status = ref('')
const detailVisible = ref(false)
const selectedPost = ref(null)
const route = useRoute()
status.value = route.query.status || ''
const load = async () => {
  loading.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideAdminCommunityPosts}/list`,
    params: { query: query.value || undefined, status: status.value || undefined, limit: 300 },
    showLoading: false,
  })
  loading.value = false
  if (result) posts.value = result.data || []
}
const changeStatus = (post, next) =>
  proxy.Confirm({
    message: `确定将该帖子设为${statusLabel(next)}吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideAdminCommunityPosts}/${post.postId}/status`,
        params: { status: next },
        showLoading: false,
      })
      if (result) {
        post.status = next
        proxy.Message.success('帖子状态已更新')
      }
    },
  })
const changeStatusAndClose = (post, next) =>
  proxy.Confirm({
    message: `确定将该帖子设为${statusLabel(next)}吗？`,
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideAdminCommunityPosts}/${post.postId}/status`,
        params: { status: next },
        showLoading: false,
      })
      if (result) {
        post.status = next
        detailVisible.value = false
        proxy.Message.success('帖子状态已更新')
      }
    },
  })
const openDetail = (post) => {
  selectedPost.value = post
  detailVisible.value = true
}
const excerpt = (value) => (value || '').replace(/<[^>]+>/g, '').slice(0, 90)
const statusLabel = (value) => ({ PUBLISHED: '已发布', HIDDEN: '已隐藏', REMOVED: '已移除' })[value] || value
const tagType = (value) => ({ PUBLISHED: 'success', HIDDEN: 'warning', REMOVED: 'danger' })[value] || 'info'
onMounted(load)
</script>
<style scoped>
.page {
  padding: 28px;
  color: #1f2925;
}
.heading {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #e3e6e4;
  padding-bottom: 22px;
}
.heading span {
  color: #b05c49;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.2px;
}
.heading h1 {
  margin: 7px 0 5px;
  font-size: 28px;
}
.heading p {
  margin: 0;
  color: #78807c;
  font-size: 13px;
}
.filters {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}
.filters .el-input {
  width: 300px;
}
.filters .el-select {
  width: 150px;
}
.el-table strong {
  display: block;
}
.el-table p {
  margin: 4px 0 0;
  color: #7e8781;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.empty {
  text-align: center;
  padding: 48px;
  color: #8a938e;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #7a847e;
  font-size: 12px;
}
.detail-title {
  margin: 18px 0 14px;
  font-size: 24px;
  line-height: 1.35;
}
.detail-body {
  color: #39443e;
  line-height: 1.8;
  word-break: break-word;
}
.detail-body :deep(img),
.detail-body :deep(video) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}
.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
@media (max-width: 700px) {
  .page {
    padding: 18px;
  }
  .filters {
    flex-wrap: wrap;
  }
  .filters .el-input {
    width: 100%;
  }
}
</style>
