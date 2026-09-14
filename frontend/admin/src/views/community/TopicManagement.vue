<template>
  <main class="page">
    <header class="heading"><div><span>COMMUNITY CONFIGURATION</span><h1>话题管理</h1><p>话题连接兴趣现场与帖子。停用话题不会删除历史关联，只会停止新的发现与发布。</p></div><el-button @click="load" :loading="loading">刷新</el-button></header>
    <section class="filters"><el-input v-model="query" clearable placeholder="搜索话题名称" @keyup.enter="load" /><el-select v-model="status" clearable placeholder="全部状态" @change="load"><el-option label="启用" value="ACTIVE" /><el-option label="已隐藏" value="HIDDEN" /></el-select><el-button type="primary" @click="load">搜索</el-button></section>
    <el-table :data="topics" v-loading="loading" stripe><el-table-column prop="canonicalName" label="话题" min-width="240"><template #default="s"><strong>#{{ s.row.canonicalName }}</strong><small>{{ s.row.topicType || 'INTEREST' }}</small></template></el-table-column><el-table-column prop="hubCount" label="关联现场" width="120" /><el-table-column prop="postCount" label="关联帖子" width="120" /><el-table-column prop="status" label="状态" width="110"><template #default="s"><el-tag :type="s.row.status === 'ACTIVE' ? 'success' : 'warning'">{{ s.row.status === 'ACTIVE' ? '启用' : '已隐藏' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150" fixed="right"><template #default="s"><el-button v-if="s.row.status !== 'ACTIVE'" link type="success" @click="changeStatus(s.row, 'ACTIVE')">恢复</el-button><el-button v-else link type="warning" @click="changeStatus(s.row, 'HIDDEN')">隐藏</el-button></template></el-table-column></el-table><div v-if="!loading && !topics.length" class="empty">没有符合条件的话题。</div>
  </main>
</template>
<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
const { proxy } = getCurrentInstance(); const topics = ref([]); const loading = ref(false); const query = ref(''); const status = ref('')
const load = async () => { loading.value = true; const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityTopics}/list`, params: { query: query.value || undefined, status: status.value || undefined, limit: 300 }, showLoading: false }); loading.value = false; if (result) topics.value = result.data || [] }
const changeStatus = (topic, next) => proxy.Confirm({ message: `确定${next === 'ACTIVE' ? '恢复' : '隐藏'}话题「${topic.canonicalName}」吗？`, okfun: async () => { const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityTopics}/${topic.topicId}/status`, params: { status: next }, showLoading: false }); if (result) { topic.status = next; proxy.Message.success('话题状态已更新') } } })
onMounted(load)
</script>
<style scoped>
.page{padding:28px;color:#1f2925}.heading{display:flex;justify-content:space-between;border-bottom:1px solid #e3e6e4;padding-bottom:22px}.heading span{color:#b05c49;font-size:10px;font-weight:800;letter-spacing:1.2px}.heading h1{margin:7px 0 5px;font-size:28px}.heading p{margin:0;color:#78807c;font-size:13px}.filters{display:flex;gap:12px;margin:20px 0}.filters .el-input{width:300px}.filters .el-select{width:150px}.el-table small{display:block;margin-top:4px;color:#8a938e;font-weight:400}.empty{text-align:center;padding:48px;color:#8a938e}@media(max-width:700px){.page{padding:18px}.filters{flex-wrap:wrap}.filters .el-input{width:100%}}
</style>
