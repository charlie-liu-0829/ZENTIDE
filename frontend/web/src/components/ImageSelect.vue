<template>
  <el-upload
    ref="uploaderRef"
    :multiple="false"
    :show-file-list="false"
    :http-request="selectImage"
    :accept="proxy.imageAccept"
  >
    <div v-if="props.modelValue" class="cover">
      <Cover :source="props.modelValue" :width="width" :scale="scale"></Cover>
    </div>
    <div
      v-else
      class="iconfont icon-image image-upload"
      :style="{ width: width + 'px', height: width + 'px', 'font-size': width / 2 + 'px' }"
    ></div>
  </el-upload>
</template>
<script setup>
import { getCurrentInstance } from 'vue'
const { proxy } = getCurrentInstance()

import { uploadImage } from '@/utils/Api.js'

const props = defineProps({
  width: {
    type: Number,
    default: 100,
  },
  modelValue: {
    type: [String, File],
  },
  cutWidth: {
    type: Number,
    default: 150,
  },
  //高宽比例
  scale: {
    type: Number,
    default: 1,
  },
})
const selectImage = async (file) => {
  // imageCoverCutRef.value.show()
  const result = await uploadImage(file.file, true)
  emits('update:modelValue', result)
}
const emits = defineEmits(['update:modelValue'])
</script>

<style lang="scss" scoped>
.image-upload {
  border: 1px dashed var(--zt-border);
  border-radius: var(--zt-radius-sm);
  background: var(--zt-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--zt-text-3);
}

.cover {
  background: var(--zt-surface-soft);
  position: relative;
  overflow: hidden;
  border-radius: var(--zt-radius-sm);
}
</style>
