<template>
  <main class="page">
    <header class="heading">
      <div><span>COMMUNITY GOVERNANCE</span><h1>评论审核</h1><p>评论与帖子属于同一条讨论链路，隐藏评论会同步更新帖子评论数。</p></div>
      <el-button @click="load" :loading="loading">刷新</el-button>
    </header>
    <section class="filters">
      <el-input v-model="query" clearable placeholder="搜索评论内容或帖子标题" @keyup.enter="load" />
      <el-select v-model="status" clearable placeholder="全部状态" @change="load">
        <el-option label="已发布" value="PUBLISHED" /><el-option label="已隐藏" value="HIDDEN" /><el-option label="已移除" value="REMOVED" />
      </el-select>
      <el-button type="primary" @click="load">搜索</el-button>
    </section>
    <el-table :data="comments" v-loading="loading" stripe>
      <el-table-column label="评论" min-width="360"><template #default="s"><div class="body">{{ s.row.body }}</div><small>{{ s.row.postTitle || '无标题帖子' }}</small></template></el-table-column>
      <el-table-column prop="hubName" label="兴趣现场" width="160" />
      <el-table-column prop="authorLabel" label="作者" width="130" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column prop="status" label="状态" width="110"><template #default="s"><el-tag :type="tagType(s.row.status)">{{ statusLabel(s.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="190" fixed="right"><template #default="s"><el-button v-if="s.row.status !== 'PUBLISHED'" link type="success" @click="changeStatus(s.row, 'PUBLISHED')">恢复发布</el-button><el-button v-if="s.row.status === 'PUBLISHED'" link type="warning" @click="changeStatus(s.row, 'HIDDEN')">隐藏</el-button><el-button v-if="s.row.status !== 'REMOVED'" link type="danger" @click="changeStatus(s.row, 'REMOVED')">移除</el-button></template></el-table-column>
    </el-table>
    <div v-if="!loading && !comments.length" class="empty">没有符合条件的评论。</div>
  </main>
</template>
<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
const { proxy } = getCurrentInstance(); const comments = ref([]); const loading = ref(false); const query = ref(''); const status = ref(''); const route = useRoute(); status.value = route.query.status || ''
const load = async () => { loading.value = true; const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityComments}/list`, params: { query: query.value || undefined, status: status.value || undefined, limit: 300 }, showLoading: false }); loading.value = false; if (result) comments.value = result.data || [] }
const changeStatus = (comment, next) => proxy.Confirm({ message: `确定将该评论设为${statusLabel(next)}吗？`, okfun: async () => { const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityComments}/${comment.commentId}/status`, params: { status: next }, showLoading: false }); if (result) { comment.status = next; proxy.Message.success('评论状态已更新') } } })
const statusLabel = (value) => ({ PUBLISHED: '已发布', HIDDEN: '已隐藏', REMOVED: '已移除' })[value] || value
const tagType = (value) => ({ PUBLISHED: 'success', HIDDEN: 'warning', REMOVED: 'danger' })[value] || 'info'
onMounted(load)
</script>
<style scoped>
.page{padding:28px;color:#1f2925}.heading{display:flex;justify-content:space-between;border-bottom:1px solid #e3e6e4;padding-bottom:22px}.heading span{color:#b05c49;font-size:10px;font-weight:800;letter-spacing:1.2px}.heading h1{margin:7px 0 5px;font-size:28px}.heading p{margin:0;color:#78807c;font-size:13px}.filters{display:flex;gap:12px;margin:20px 0}.filters .el-input{width:320px}.filters .el-select{width:150px}.body{line-height:1.5;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.el-table small{display:block;margin-top:4px;color:#8a938e}.empty{text-align:center;padding:48px;color:#8a938e}@media(max-width:700px){.page{padding:18px}.filters{flex-wrap:wrap}.filters .el-input{width:100%}}
</style>
