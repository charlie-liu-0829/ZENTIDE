<template>
  <main class="governance-page">
    <header class="heading">
      <div>
        <span>AI GOVERNANCE</span>
        <h1>智能治理</h1>
        <p>系统自动识别风险并提供证据，管理员确认最终处理动作。</p>
      </div>
      <el-tag type="info">不可逆操作需人工确认</el-tag>
    </header>
    <section class="workspace">
      <div class="editor panel">
        <div class="scan-hero">
          <span class="pulse"></span>
          <div>
            <h2>自动扫描全站</h2>
            <p>读取所有已发布快照，检查全站帖子并筛出风险内容。</p>
          </div>
        </div>
        <el-button type="primary" :loading="loading" @click="scan">扫描全部已发布帖子</el-button>
        <div v-if="scanSummary" class="scan-summary">
          <strong>本次扫描 {{ scanSummary.scanned_count }} 条</strong
          ><span>发现 {{ scanSummary.risk_count }} 条风险内容</span>
        </div>
        <div v-if="risks.length" class="risk-list">
          <button v-for="item in risks" :key="item.result_id" class="risk-item" @click="selectRisk(item)">
            <span :class="['risk-dot', item.risk_level]"></span
            ><span class="risk-title">{{ item.title }}</span
            ><el-tag size="small" :type="riskType(item.risk_level)">{{ riskLabel(item.risk_level) }}</el-tag>
          </button>
        </div>
        <div v-else class="scan-hint">点击扫描后，这里会显示需要管理员处理的帖子。</div>
      </div>
      <div class="result panel">
        <div class="result-head">
          <h2>治理结果</h2>
          <el-tag v-if="result" :type="riskType(result.risk_level)">{{
            riskLabel(result.risk_level)
          }}</el-tag>
        </div>
        <div v-if="!result" class="empty">运行扫描后，风险证据会显示在这里。</div>
        <template v-else
          ><div class="action-banner" :class="result.risk_level">
            <strong>{{ actionLabel(result.suggested_action) }}</strong
            ><span>置信度 {{ Math.round(result.confidence * 100) }}%</span>
          </div>
          <div class="chips">
            <el-tag v-for="type in result.violation_types" :key="type" type="warning">{{ type }}</el-tag
            ><span v-if="!result.violation_types.length" class="safe">未发现明显违规类型</span>
          </div>
          <dl>
            <div>
              <dt>匹配规则</dt>
              <dd>{{ result.matched_rule_ids.join('、') || '无' }}</dd>
            </div>
            <div>
              <dt>匹配案例</dt>
              <dd>{{ result.matched_case_ids.join('、') || '无' }}</dd>
            </div>
          </dl>
          <section class="evidence">
            <h3>证据片段</h3>
            <p v-for="fragment in result.evidence_fragments" :key="fragment">{{ fragment }}</p>
          </section>
          <div class="feedback">
            <h3>管理员确认</h3>
            <el-select v-model="feedbackForm.finalAction" placeholder="最终动作"
              ><el-option
                v-for="action in actions"
                :key="action.value"
                :label="action.label"
                :value="action.value" /></el-select
            ><el-checkbox v-model="feedbackForm.acceptedAgentAdvice">采纳 Agent 建议</el-checkbox
            ><el-input
              v-model="feedbackForm.reason"
              type="textarea"
              :rows="3"
              placeholder="处理原因（必填）"
            /><el-button type="primary" :disabled="!feedbackForm.reason" @click="submitFeedback"
              >保存处理结果</el-button
            >
          </div></template
        >
      </div>
    </section>
  </main>
</template>
<script setup>
import { getCurrentInstance, ref } from 'vue'
const { proxy } = getCurrentInstance()
const loading = ref(false)
const result = ref(null)
const risks = ref([])
const scanSummary = ref(null)
const feedbackForm = ref({ finalAction: '', acceptedAgentAdvice: false, reason: '' })
const actions = [
  { value: 'allow', label: '允许发布' },
  { value: 'flag', label: '标记关注' },
  { value: 'manual_review', label: '进入人工审核' },
  { value: 'temporary_hide', label: '暂时隐藏' },
]
const scan = async () => {
  loading.value = true
  const response = await proxy.Request({
    url: proxy.Api.zentideAdminGovernanceScan,
    params: { max_posts: 500 },
    dataType: 'json',
    showLoading: false,
  })
  loading.value = false
  if (response) {
    scanSummary.value = response.data
    risks.value = uniqueRisks(response.data.results || [])
    if (risks.value[0]) selectRisk(risks.value[0])
  }
}
const selectRisk = (item) => {
  result.value = item
  feedbackForm.value = { finalAction: item.suggested_action, acceptedAgentAdvice: true, reason: '' }
}
const submitFeedback = async () => {
  if (!result.value?.content_id || !feedbackForm.value.finalAction || !feedbackForm.value.reason.trim())
    return
  const response = await proxy.Request({
    url: proxy.Api.zentideAdminGovernanceFeedback,
    params: {
      content_id: Number(result.value.content_id),
      agent_result_id: result.value.result_id,
      final_action: feedbackForm.value.finalAction,
      accepted_agent_advice: feedbackForm.value.acceptedAgentAdvice,
      corrected_violation_types: result.value.violation_types || [],
      reviewer_reason: feedbackForm.value.reason.trim(),
    },
    dataType: 'json',
    showLoading: false,
  })
  if (response) {
    const hidden = response.data?.content_hidden === true
    proxy.Message.success(hidden ? '帖子已暂时隐藏，处理结果已记录' : '治理处理已记录')
    if (hidden) {
      risks.value = risks.value.filter((item) => Number(item.content_id) !== Number(result.value.content_id))
      result.value = null
      feedbackForm.value = { finalAction: '', acceptedAgentAdvice: false, reason: '' }
    }
  }
}
const loadPending = async () => {
  const response = await proxy.Request({ url: proxy.Api.zentideAdminGovernancePending, params: {}, dataType: 'json', showLoading: false })
  if (response) {
    risks.value = uniqueRisks(response.data?.results || [])
    if (risks.value[0]) selectRisk(risks.value[0])
  }
}
const riskLabel = (risk) => ({ low: '低风险', medium: '中风险', high: '高风险' })[risk] || risk
const riskType = (risk) => ({ low: 'success', medium: 'warning', high: 'danger' })[risk] || 'info'
const actionLabel = (action) => actions.find((item) => item.value === action)?.label || action
const uniqueRisks = (items) => {
  const seen = new Set()
  return items.filter((item) => {
    const key = String(item.content_id || item.result_id)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}
loadPending()
</script>
<style scoped>
.governance-page {
  padding: 28px;
  color: #20243a;
}
.heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #e1e4ef;
  padding-bottom: 23px;
}
.heading span {
  color: #6866e8;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.5px;
}
.heading h1 {
  margin: 7px 0 5px;
  font-size: 30px;
}
.heading p {
  margin: 0;
  color: #858da2;
  font-size: 13px;
}
.workspace {
  display: grid;
  grid-template-columns: minmax(300px, 0.85fr) minmax(380px, 1.15fr);
  gap: 18px;
  margin-top: 22px;
}
.panel {
  background: #fff;
  border: 1px solid #e1e4ef;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 8px 24px rgba(35, 40, 76, 0.06);
}
.panel h2 {
  margin: 0 0 18px;
  font-size: 18px;
}
.editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.editor .el-button {
  height: 42px;
}
.result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty {
  text-align: center;
  color: #9299aa;
  padding: 80px 15px;
  font-size: 13px;
}
.action-banner {
  display: flex;
  justify-content: space-between;
  padding: 16px;
  border-radius: 10px;
  margin: 5px 0 17px;
}
.action-banner.low {
  background: #ecf8f1;
  color: #347c5e;
}
.action-banner.medium {
  background: #fff5e7;
  color: #ad7029;
}
.action-banner.high {
  background: #fff0ef;
  color: #bc554f;
}
.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.safe {
  color: #4c9875;
  font-size: 12px;
}
.result dl {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 19px 0;
}
.result dl div {
  background: #f7f8fb;
  padding: 12px;
  border-radius: 8px;
}
.result dt {
  color: #9299aa;
  font-size: 11px;
}
.result dd {
  margin: 5px 0 0;
  font-weight: 700;
}
.evidence {
  border-top: 1px solid #edf0f5;
  padding-top: 16px;
}
.evidence h3,
.feedback h3 {
  font-size: 13px;
  margin: 0 0 10px;
}
.evidence p {
  font-size: 12px;
  line-height: 1.6;
  background: #f8f8fc;
  border-left: 3px solid #7673e7;
  padding: 8px 10px;
  margin: 7px 0;
}
.feedback {
  border-top: 1px solid #edf0f5;
  margin-top: 18px;
  padding-top: 17px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.feedback .el-button {
  align-self: flex-end;
}
.scan-hero {
  display: flex;
  gap: 12px;
}
.scan-hero h2 {
  margin: 0 0 5px;
}
.scan-hero p {
  margin: 0;
  color: #858da2;
  font-size: 12px;
  line-height: 1.6;
}
.pulse {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #6866e8;
  box-shadow: 0 0 0 5px #eeedff;
}
.scan-summary {
  display: flex;
  justify-content: space-between;
  padding: 13px;
  background: #f6f7ff;
  border-radius: 10px;
  font-size: 12px;
}
.risk-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  max-height: 330px;
  overflow: auto;
}
.risk-item {
  display: flex;
  align-items: center;
  gap: 9px;
  border: 1px solid #edf0f5;
  background: #fff;
  border-radius: 9px;
  padding: 11px;
  text-align: left;
  cursor: pointer;
}
.risk-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.risk-dot.high {
  background: #e35d67;
}
.risk-dot.medium {
  background: #e9a849;
}
.scan-hint {
  padding: 22px 8px;
  color: #9aa1b3;
  font-size: 12px;
  text-align: center;
  background: #fafbfe;
  border-radius: 9px;
}
.hub-status {
  float: right;
  color: #9299aa;
  margin-left: 14px;
}
@media (max-width: 850px) {
  .workspace {
    grid-template-columns: 1fr;
  }
  .governance-page {
    padding: 18px;
  }
  .heading {
    display: block;
  }
}
.evidence p {
  line-height: 1.7;
  white-space: pre-line;
  padding: 11px 13px;
}
</style>
