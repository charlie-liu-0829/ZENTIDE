<template>
  <el-config-provider :locale="zhCn" :message="config">
    <router-view></router-view>
  </el-config-provider>
</template>

<script setup>
import { reactive } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const config = reactive({
  max: 1,
})
</script>

<style lang="scss" scoped></style>
