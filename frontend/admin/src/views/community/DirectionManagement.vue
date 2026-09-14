<template>
  <main class="direction-management">
    <header class="page-heading">
      <div>
        <span>COMMUNITY CONFIGURATION</span>
        <h1>兴趣现场方向</h1>
        <p>维护创建兴趣现场时可选择的所属方向。停用后不会影响已有现场。</p>
      </div>
      <el-button @click="load" :loading="loading">刷新</el-button>
    </header>

    <section class="create-panel">
      <el-input v-model="draft.code" maxlength="40" placeholder="方向标识，如 TRAVEL" />
      <el-input v-model="draft.displayName" maxlength="40" placeholder="方向名称，如旅行" />
      <el-input v-model="draft.description" maxlength="160" placeholder="方向说明（可选）" />
      <el-input-number v-model="draft.sortOrder" :min="0" :max="9999" controls-position="right" />
      <el-button type="primary" :loading="creating" @click="create">增加方向</el-button>
    </section>

    <el-table :data="directions" v-loading="loading" stripe>
      <el-table-column label="标识" width="180"><template #default="s"><code>{{ s.row.code }}</code></template></el-table-column>
      <el-table-column label="名称" width="180"><template #default="s"><el-input v-model="s.row.displayName" maxlength="40" /></template></el-table-column>
      <el-table-column label="说明" min-width="260"><template #default="s"><el-input v-model="s.row.description" maxlength="160" /></template></el-table-column>
      <el-table-column label="排序" width="120"><template #default="s"><el-input-number v-model="s.row.sortOrder" :min="0" :max="9999" controls-position="right" /></template></el-table-column>
      <el-table-column label="启用" width="90"><template #default="s"><el-switch v-model="s.row.active" /></template></el-table-column>
      <el-table-column label="操作" width="170" fixed="right"><template #default="s"><el-button link type="primary" :loading="savingId === s.row.directionId" @click="save(s.row)">保存</el-button><el-button link type="danger" @click="disable(s.row)">停用</el-button></template></el-table-column>
    </el-table>
    <div v-if="!loading && !directions.length" class="empty">还没有配置兴趣现场方向。</div>
  </main>
</template>

<script setup>
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const directions = ref([])
const loading = ref(false)
const creating = ref(false)
const savingId = ref(null)
const draft = reactive({ code: '', displayName: '', description: '', sortOrder: 0 })
const normalize = (item) => ({ ...item, active: item.status === 'ACTIVE' })
const load = async () => {
  loading.value = true
  const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityDirections}/list`, params: {}, showLoading: false })
  loading.value = false
  if (result) directions.value = (result.data || []).map(normalize)
}
const create = async () => {
  if (!draft.code.trim() || !draft.displayName.trim()) return proxy.Message.warning('请输入方向标识和名称')
  creating.value = true
  const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityDirections}/create`, params: { code: draft.code.trim(), displayName: draft.displayName.trim(), description: draft.description.trim(), sortOrder: draft.sortOrder }, showLoading: false })
  creating.value = false
  if (!result) return
  directions.value.push(normalize(result.data))
  Object.assign(draft, { code: '', displayName: '', description: '', sortOrder: 0 })
  proxy.Message.success('兴趣现场方向已增加')
}
const save = async (direction) => {
  if (!direction.displayName?.trim()) return proxy.Message.warning('方向名称不能为空')
  savingId.value = direction.directionId
  const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityDirections}/${direction.directionId}/update`, params: { displayName: direction.displayName.trim(), description: direction.description?.trim() || '', active: direction.active, sortOrder: direction.sortOrder }, showLoading: false })
  savingId.value = null
  if (!result) return
  proxy.Message.success('方向配置已保存')
  await load()
}
const disable = (direction) => proxy.Confirm({ message: `停用方向「${direction.displayName}」吗？已有现场不受影响。`, okfun: async () => { const result = await proxy.Request({ url: `${proxy.Api.zentideAdminCommunityDirections}/${direction.directionId}/delete`, params: {}, showLoading: false }); if (result) { direction.active = false; direction.status = 'DISABLED'; proxy.Message.success('方向已停用') } } })
onMounted(load)
</script>

<style scoped>
.direction-management{padding:12px 14px 50px;color:#1f2925}.page-heading{display:flex;align-items:flex-end;justify-content:space-between;padding:16px 2px 24px;border-bottom:1px solid #e3e6e4}.page-heading span{color:#b05c49;font-size:9px;font-weight:800;letter-spacing:1.3px}.page-heading h1{margin:7px 0 5px;font-size:28px;letter-spacing:-.03em}.page-heading p{margin:0;color:#5f6d66;font-size:12px;line-height:1.65}.create-panel{display:grid;grid-template-columns:170px 180px minmax(220px,1fr) 130px auto;align-items:center;gap:10px;margin:20px 0;padding:16px;border:1px solid #dde2df;border-radius:8px;background:#f8faf8}.empty{padding:50px;color:#697871;text-align:center}code{padding:3px 6px;border-radius:3px;color:#3d5e53;background:#edf3ef;font-size:11px}@media(max-width:900px){.create-panel{grid-template-columns:1fr 1fr}.create-panel :deep(.el-input-number),.create-panel .el-button{width:100%}}@media(max-width:560px){.page-heading{display:block}.page-heading .el-button{margin-top:14px}.create-panel{grid-template-columns:1fr}}
</style>
