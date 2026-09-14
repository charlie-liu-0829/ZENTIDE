<template>
  <section class="page">
    <span>知潮 ZENTIDE · Control Plane</span>
    <h1>{{ route.meta.title }}</h1>
    <p>{{ route.meta.description }}</p>
    <el-alert
      title="该领域入口已建立边界，真实数据链路将在对应阶段接入。"
      type="info"
      :closable="false"
      show-icon
    />
  </section>
</template>
<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
<style scoped>
.page {
  max-width: 760px;
  padding: 54px 24px;
}
.page span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}
.page h1 {
  color: #17233c;
  font-size: 32px;
}
.page p {
  margin-bottom: 28px;
  color: #6d788c;
  font-size: 16px;
}
</style>
