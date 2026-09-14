<template>
  <section class="foundation-page">
    <div class="status">知潮 ZENTIDE · 阶段化迁移</div>
    <h1>{{ route.meta.title }}</h1>
    <p>{{ route.meta.description }}</p>
    <el-alert
      title="来源申请、审批和受控试运行已上线；该入口将在对应后续阶段接入真实职位数据。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-button class="back" @click="router.push('/')">返回首页</el-button>
  </section>
</template>
<script setup>
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
</script>
<style scoped>
.foundation-page {
  max-width: 760px;
  min-height: 520px;
  padding: 72px 0;
}
.status {
  color: var(--zt-accent);
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 1px;
}
.foundation-page h1 {
  margin: 12px 0;
  color: var(--zt-text);
  font-size: 38px;
  letter-spacing: -0.025em;
}
.foundation-page > p {
  margin-bottom: 30px;
  color: var(--zt-text-2);
  font-size: 16px;
  line-height: 1.7;
}
.back {
  margin-top: 24px;
}
</style>
