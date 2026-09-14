<template>
  <main class="rules-page">
    <header class="page-head">
      <div>
        <span>POLICY CENTER</span>
        <h1>治理规则</h1>
        <p>全站治理规则，修改后下一次扫描立即生效。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增规则</el-button>
    </header>
    <section class="toolbar panel">
      <span class="scope-badge">全站生效</span><span class="count">共 {{ rules.length }} 条规则</span>
    </section>
    <section v-if="!rules.length" class="panel empty">还没有全站治理规则，点击右上角新增第一条规则。</section>
    <section v-else class="rule-grid">
      <article v-for="item in rules" :key="item.rule_id" class="rule-card panel">
        <div class="rule-top">
          <div>
            <span class="rule-id">RULE-{{ item.rule_id }}</span>
            <h2>{{ item.violation_type }}</h2>
          </div>
          <el-switch v-model="item.enabled" :active-value="1" :inactive-value="0" @change="save(item)" />
        </div>
        <div class="description">{{ item.rule_description || '未填写规则说明' }}</div>
        <div class="rule-bottom">
          <el-tag :type="severityType(item.severity)">{{ severityLabel(item.severity) }}</el-tag
          ><span>{{ item.enabled ? '启用中' : '已停用' }}</span
          ><el-button link type="primary" @click="openEdit(item)">编辑</el-button>
          <el-button link type="danger" @click="remove(item)">删除</el-button>
        </div>
      </article>
    </section>
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑治理规则' : '新增治理规则'"
      width="min(92vw, 520px)"
    >
      <el-form label-position="top"
        ><el-form-item label="违规类型"
          ><el-input
            v-model="form.violationType"
            maxlength="80"
            placeholder="例如：站外引流、广告刷屏" /></el-form-item
        ><el-form-item label="规则说明" required
          ><el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="用自然语言描述什么情况属于违规、判断边界和例外。例如：禁止在帖子中引导用户离开社区进行交易；仅分享公开资料链接不算违规。"
          /></el-form-item
        ><el-form-item label="风险等级"
          ><el-select v-model="form.severity" style="width: 100%"
            ><el-option label="低风险" value="low" /><el-option label="中风险" value="medium" /><el-option
              label="高风险"
              value="high" /></el-select></el-form-item
        ><el-form-item v-if="editing" label="状态"
          ><el-switch v-model="form.enabled" active-text="启用规则" inactive-text="停用规则" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="dialogVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" :disabled="!valid" @click="submit"
          >保存</el-button
        ></template
      >
    </el-dialog>
  </main>
</template>
<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
const { proxy } = getCurrentInstance()
const rules = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(null)
const form = ref({ violationType: '', description: '', severity: 'medium', enabled: true })
const valid = computed(() => Boolean(form.value.violationType.trim() && form.value.description.trim()))
const loadRules = async () => {
  const response = await proxy.Request({
    url: proxy.Api.zentideAdminGovernanceRuleList,
    params: {},
    dataType: 'json',
    showLoading: false,
  })
  if (response)
    rules.value = (response.data?.rules || []).map((item) => ({
      ...item,
      enabled: item.enabled === true || item.enabled === 1 || item.enabled === '1' ? 1 : 0,
    }))
}
const openCreate = () => {
  editing.value = null
  form.value = { violationType: '', description: '', severity: 'medium', enabled: true }
  dialogVisible.value = true
}
const openEdit = (item) => {
  editing.value = item
  form.value = {
    violationType: item.violation_type,
    description: item.rule_description || '',
    severity: item.severity,
    enabled: Boolean(item.enabled),
  }
  dialogVisible.value = true
}
const payload = () => ({
  violation_type: form.value.violationType.trim(),
  rule_description: form.value.description.trim(),
  keywords: [],
  severity: form.value.severity,
  enabled: form.value.enabled,
})
const submit = async () => {
  saving.value = true
  const response = await proxy.Request({
    url: editing.value
      ? `${proxy.Api.zentideAdminGovernanceRules}/${editing.value.rule_id}`
      : proxy.Api.zentideAdminGovernanceRules,
    params: payload(),
    dataType: 'json',
    showLoading: false,
  })
  saving.value = false
  if (response) {
    dialogVisible.value = false
    proxy.Message.success(editing.value ? '规则已更新' : '规则已创建')
    await loadRules()
  }
}
const save = async (item) => {
  const enabled = Number(item.enabled) === 1
  const response = await proxy.Request({
    url: `${proxy.Api.zentideAdminGovernanceRules}/${item.rule_id}`,
    params: {
      violation_type: item.violation_type,
      rule_description: item.rule_description,
      keywords: [],
      severity: item.severity,
      enabled,
    },
    dataType: 'json',
    showLoading: false,
  })
  if (response) {
    item.enabled = enabled ? 1 : 0
    proxy.Message.success(enabled ? '规则已启用' : '规则已停用')
  } else await loadRules()
}
const remove = (item) => proxy.Confirm({ message: `确定删除规则「${item.violation_type}」吗？删除后全站扫描将不再使用这条规则。`, okfun: async () => {
  const response = await proxy.Request({ url: `${proxy.Api.zentideAdminGovernanceRules}/${item.rule_id}/delete`, params: {}, showLoading: false })
  if (response) { rules.value = rules.value.filter((row) => row.rule_id !== item.rule_id); proxy.Message.success('规则已删除') }
} })
const severityLabel = (value) => ({ low: '低风险', medium: '中风险', high: '高风险' })[value] || value
const severityType = (value) => ({ low: 'success', medium: 'warning', high: 'danger' })[value] || 'info'
onMounted(loadRules)
</script>
<style scoped>
.rules-page {
  padding: 28px;
  color: #20243a;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #e1e4ef;
  padding-bottom: 23px;
}
.page-head span {
  color: #6866e8;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.5px;
}
.page-head h1 {
  margin: 7px 0 5px;
  font-size: 30px;
  letter-spacing: -0.04em;
}
.page-head p {
  margin: 0;
  color: #858da2;
  font-size: 13px;
}
.panel {
  background: #fff;
  border: 1px solid #e1e4ef;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(35, 40, 76, 0.06);
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 22px 0;
}
.toolbar .el-select {
  width: 320px;
}
.count {
  color: #8b92a4;
  font-size: 13px;
}
.rule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.rule-card {
  min-height: 148px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.rule-top,
.rule-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.rule-id {
  font-size: 10px;
  color: #9299aa;
  letter-spacing: 1px;
}
.rule-card h2 {
  font-size: 17px;
  margin: 5px 0 0;
}
.keywords {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 18px 0;
  color: #a0a6b5;
  font-size: 12px;
}
.rule-bottom {
  border-top: 1px solid #edf0f5;
  padding-top: 13px;
  color: #9299aa;
  font-size: 12px;
}
.rule-bottom .el-button {
  margin-left: auto;
}
.empty {
  text-align: center;
  color: #9299aa;
  padding: 76px 20px;
  font-size: 13px;
}
@media (max-width: 700px) {
  .rules-page {
    padding: 18px;
  }
  .page-head {
    display: block;
  }
  .page-head .el-button {
    margin-top: 16px;
  }
  .toolbar {
    display: block;
  }
  .toolbar .el-select {
    width: 100%;
  }
  .count {
    display: block;
    margin-top: 12px;
  }
}
</style>
