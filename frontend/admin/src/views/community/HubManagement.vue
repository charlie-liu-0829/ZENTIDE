<template>
  <main class="page">
    <header class="heading">
      <div><span>COMMUNITY GOVERNANCE</span><h1>兴趣现场</h1><p>管理现场状态、查看创建者与社区规模。下架采用软状态，不删除用户内容。</p></div>
      <el-button @click="load" :loading="loading">刷新</el-button>
    </header>
    <section class="filters">
      <el-input v-model="query" clearable placeholder="搜索现场名称或 slug" @keyup.enter="load" />
      <el-select v-model="status" clearable placeholder="全部状态" @change="load">
        <el-option label="正常" value="ACTIVE" /><el-option label="隐藏" value="HIDDEN" /><el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <el-button type="primary" @click="load">搜索</el-button>
    </section>
    <el-table :data="hubs" v-loading="loading" stripe>
      <el-table-column prop="name" label="兴趣现场" min-width="220"><template #default="s"><strong>{{ s.row.name }}</strong><small>{{ s.row.slug }}</small></template></el-table-column>
      <el-table-column prop="ownerName" label="创建者" width="140" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="规模" width="150"><template #default="s">{{ s.row.memberCount || 0 }} 成员 · {{ s.row.postCount || 0 }} 帖子</template></el-table-column>
      <el-table-column prop="status" label="状态" width="110"><template #default="s"><el-tag :type="tagType(s.row.status)">{{ statusLabel(s.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180" fixed="right"><template #default="s"><el-button v-if="s.row.status !== 'ACTIVE'" link type="success" @click="changeStatus(s.row, 'ACTIVE')">恢复</el-button><el-button v-if="s.row.status === 'ACTIVE'" link type="warning" @click="changeStatus(s.row, 'HIDDEN')">隐藏</el-button><el-button v-if="s.row.status !== 'ARCHIVED'" link type="info" @click="changeStatus(s.row, 'ARCHIVED')">归档</el-button></template></el-table-column>
    </el-table>
    <div v-if="!loading && !hubs.length" class="empty">没有符合条件的兴趣现场。</div>
  </main>
</template>
<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
const { proxy } = getCurrentInstance(); const hubs = ref([]); const loading = ref(false); const query = ref(''); const status = ref('')
const load = async () => { loading.value = true; const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityHubs}/list`, params: { query: query.value || undefined, status: status.value || undefined, limit: 200 }, showLoading: false }); loading.value = false; if (result) hubs.value = result.data || [] }
const changeStatus = (hub, next) => proxy.Confirm({ message: `确定将「${hub.name}」设为${statusLabel(next)}吗？`, okfun: async () => { const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityHubs}/${hub.hubId}/status`, params: { status: next }, showLoading: false }); if (result) { hub.status = next; proxy.Message.success('现场状态已更新') } } })
const statusLabel = (value) => ({ ACTIVE: '正常', HIDDEN: '隐藏', ARCHIVED: '已归档' })[value] || value
const tagType = (value) => ({ ACTIVE: 'success', HIDDEN: 'warning', ARCHIVED: 'info' })[value] || 'info'
onMounted(load)
</script>
<style scoped>
.page{padding:28px;color:#1f2925}.heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #e3e6e4;padding-bottom:22px}.heading span{color:#b05c49;font-size:10px;font-weight:800;letter-spacing:1.2px}.heading h1{margin:7px 0 5px;font-size:28px}.heading p{margin:0;color:#78807c;font-size:13px}.filters{display:flex;gap:12px;margin:20px 0}.filters .el-input{width:300px}.filters .el-select{width:150px}.el-table small{display:block;color:#8a938e;font-weight:400;margin-top:3px}.empty{text-align:center;padding:48px;color:#8a938e}@media(max-width:700px){.page{padding:18px}.filters{flex-wrap:wrap}.filters .el-input{width:100%}}
</style>
