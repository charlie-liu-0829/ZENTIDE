<template>
  <main class="type-management">
    <header class="page-heading">
      <div>
        <span>COMMUNITY CONFIGURATION</span>
        <h1>固定帖子类型</h1>
        <p>这里定义所有兴趣现场默认拥有的帖子类型。现场创建者只能管理自己的扩展类型。</p>
      </div>
    </header>

    <section class="create-panel">
      <div>
        <h2>增加固定类型</h2>
        <p>新增后会立即出现在所有兴趣现场的发帖器和筛选栏。</p>
      </div>
      <el-input v-model="draft.displayName" maxlength="20" placeholder="类型名称，例如：攻略" />
      <el-input v-model="draft.description" maxlength="160" placeholder="一句话说明适合发布什么内容" />
      <el-button type="primary" :loading="creating" @click="createType">增加类型</el-button>
    </section>

    <section class="type-table">
      <header>
        <div>
          <h2>当前固定类型</h2>
          <p>停用后不会再出现在新帖中，历史帖子不受影响。</p>
        </div>
        <span>{{ types.length }} 个</span>
      </header>
      <el-table :data="types" v-loading="loading" style="width: 100%">
        <el-table-column label="名称" width="180"
          ><template #default="scope"><el-input v-model="scope.row.displayName" maxlength="20" /></template
        ></el-table-column>
        <el-table-column label="发布说明" min-width="280"
          ><template #default="scope"><el-input v-model="scope.row.description" maxlength="160" /></template
        ></el-table-column>
        <el-table-column label="排序" width="110"
          ><template #default="scope"
            ><el-input-number
              v-model="scope.row.sortOrder"
              :min="0"
              :max="9999"
              controls-position="right" /></template
        ></el-table-column>
        <el-table-column label="启用" width="90"
          ><template #default="scope"><el-switch v-model="scope.row.active" /></template
        ></el-table-column>
        <el-table-column label="标识" width="190"
          ><template #default="scope"
            ><code>{{ scope.row.typeCode }}</code></template
          ></el-table-column
        >
        <el-table-column label="操作" width="150" fixed="right"
          ><template #default="scope"
            ><el-button
              link
              type="primary"
              :loading="savingId === scope.row.postTypeId"
              @click="saveType(scope.row)"
              >保存</el-button
            ><el-button link type="danger" @click="deleteType(scope.row)">删除</el-button></template
          ></el-table-column
        >
      </el-table>
      <div v-if="!loading && !types.length" class="empty">还没有固定帖子类型，请先增加一个。</div>
    </section>
  </main>
</template>

<script setup>
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'

const { proxy } = getCurrentInstance()
const types = ref([])
const loading = ref(false)
const creating = ref(false)
const savingId = ref(null)
const draft = reactive({ displayName: '', description: '' })

const normalize = (item) => ({ ...item, active: item.status === 'ACTIVE' })
const load = async () => {
  loading.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPostTypes}/list`,
    params: {},
    showLoading: false,
  })
  loading.value = false
  if (result) types.value = (result.data || []).map(normalize)
}
const createType = async () => {
  const displayName = draft.displayName.trim()
  if (!displayName) return proxy.Message.warning('请输入类型名称')
  creating.value = true
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPostTypes}/create`,
    params: { displayName, description: draft.description.trim() },
    showLoading: false,
  })
  creating.value = false
  if (!result) return
  types.value.push(normalize(result.data))
  draft.displayName = ''
  draft.description = ''
  proxy.Message.success('固定帖子类型已增加')
}
const saveType = async (type) => {
  if (!type.displayName?.trim()) return proxy.Message.warning('类型名称不能为空')
  savingId.value = type.postTypeId
  const result = await proxy.Request({
    url: `${proxy.Api.zentideCommunityPostTypes}/${type.postTypeId}/update`,
    params: {
      displayName: type.displayName.trim(),
      description: type.description?.trim() || '',
      active: type.active,
      sortOrder: type.sortOrder,
    },
    showLoading: false,
  })
  savingId.value = null
  if (!result) return
  type.status = type.active ? 'ACTIVE' : 'DISABLED'
  proxy.Message.success('类型配置已保存')
  await load()
}
const deleteType = (type) =>
  proxy.Confirm({
    message: `删除固定类型「${type.displayName}」吗？历史帖子会保留名称，但所有兴趣现场将不能再发布此类型。`,
    okfun: async () => {
      const result = await proxy.Request({
        url: `${proxy.Api.zentideCommunityPostTypes}/${type.postTypeId}/delete`,
        params: {},
        showLoading: false,
      })
      if (!result) return
      types.value = types.value.filter((item) => item.postTypeId !== type.postTypeId)
      proxy.Message.success('固定帖子类型已删除')
    },
  })

onMounted(load)
</script>

<style scoped>
.type-management {
  padding: 12px 14px 50px;
  color: #1f2925;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 16px 2px 24px;
  border-bottom: 1px solid #e3e6e4;
}
.page-heading span {
  color: #b05c49;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1.3px;
}
.page-heading h1 {
  margin: 7px 0 5px;
  font-size: 28px;
  letter-spacing: -0.03em;
}
.page-heading p,
.create-panel p,
.type-table header p {
  margin: 0;
  color: #78807c;
  font-size: 12px;
  line-height: 1.65;
}
.create-panel {
  display: grid;
  grid-template-columns: minmax(210px, 0.8fr) 180px minmax(260px, 1.2fr) auto;
  align-items: end;
  gap: 12px;
  margin: 20px 0;
  padding: 18px;
  border: 1px solid #dde2df;
  border-radius: 8px;
  background: #f8faf8;
}
.create-panel h2,
.type-table h2 {
  margin: 0 0 4px;
  font-size: 15px;
}
.type-table {
  overflow: hidden;
  border: 1px solid #dfe3e1;
  border-radius: 8px;
  background: #fff;
}
.type-table > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #e7e9e8;
}
.type-table > header > span {
  color: #818985;
  font-size: 11px;
}
.type-table code {
  padding: 3px 6px;
  border-radius: 3px;
  color: #57625d;
  background: #f0f2f1;
  font-size: 10px;
}
.empty {
  padding: 50px;
  color: #8c938f;
  text-align: center;
}
@media (max-width: 1050px) {
  .create-panel {
    grid-template-columns: 1fr 1fr;
  }
  .create-panel > div {
    grid-column: 1/-1;
  }
}
@media (max-width: 700px) {
  .create-panel {
    grid-template-columns: 1fr;
  }
  .create-panel > div {
    grid-column: auto;
  }
}
</style>
