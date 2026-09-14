<template>
  <main class="page"><header class="heading"><div><span>COMMUNITY GOVERNANCE</span><h1>加入申请</h1><p>集中处理开启审核的兴趣现场成员申请。</p></div><el-button @click="load" :loading="loading">刷新</el-button></header><el-table :data="members" v-loading="loading" stripe><el-table-column prop="hubName" label="兴趣现场" min-width="200" /><el-table-column prop="nickName" label="申请人" width="180" /><el-table-column prop="userId" label="用户 ID" width="150" /><el-table-column prop="createdAt" label="申请时间" width="190" /><el-table-column label="操作" width="180" fixed="right"><template #default="s"><el-button link type="success" @click="review(s.row, 'ACTIVE')">通过</el-button><el-button link type="danger" @click="review(s.row, 'REJECTED')">拒绝</el-button></template></el-table-column></el-table><div v-if="!loading && !members.length" class="empty">当前没有待处理的加入申请。</div></main>
</template>
<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
const { proxy } = getCurrentInstance(); const members = ref([]); const loading = ref(false)
const load = async () => { loading.value = true; const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityMembers}/pending`, params: { limit: 300 }, showLoading: false }); loading.value = false; if (result) members.value = result.data || [] }
const review = (row, status) => proxy.Confirm({ message: `确定${status === 'ACTIVE' ? '通过' : '拒绝'}该加入申请吗？`, okfun: async () => { const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityHubs}/${row.hubId}/members/${row.userId}/status`, params: { status }, showLoading: false }); if (result) { members.value = members.value.filter((item) => item !== row); proxy.Message.success('申请已处理') } } })
onMounted(load)
</script>
<style scoped>.page{padding:28px;color:#1f2925}.heading{display:flex;justify-content:space-between;border-bottom:1px solid #e3e6e4;padding-bottom:22px;margin-bottom:20px}.heading span{color:#b05c49;font-size:10px;font-weight:800;letter-spacing:1.2px}.heading h1{margin:7px 0 5px;font-size:28px}.heading p{margin:0;color:#78807c;font-size:13px}.empty{text-align:center;padding:48px;color:#8a938e}</style>
