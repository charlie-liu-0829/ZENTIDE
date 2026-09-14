<template>
  <div
    class="image-panel"
    ref="coverRef"
    :style="{
      'border-radius': borderRadius,
      width: width ? width + 'px' : '100%',
      'aspect-ratio': scale,
    }"
  >
    <el-image :lazy="lazy" :src="coverFile" :fit="fit" @click="showViewerHandler">
      <template #placeholder>
        <div class="loading" :style="{ height: loadingHeight + 'px' }">
          <img :src="proxy.Utils.getLocalResource('loading.gif')" />
        </div>
      </template>
    </el-image>
    <el-image-viewer
      :hide-on-click-modal="true"
      @close="
        () => {
          showViewer = false
        }
      "
      v-if="showViewer"
      :url-list="imageList"
      :initial-index="initialIndex"
      :teleported="true"
    />
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, computed, onMounted, watch } from 'vue'
const { proxy } = getCurrentInstance()

const props = defineProps({
  source: {
    type: [String, File],
  },
  width: {
    type: Number,
  },
  scale: {
    type: Number,
    default: 1,
  },
  fit: {
    type: String,
    default: 'cover',
  },
  preview: {
    type: Boolean,
    default: false,
  },
  borderRadius: {
    type: String,
    default: '5px',
  },
  lazy: {
    type: Boolean,
    default: true,
  },
  preImageList: {
    type: Array,
    default: () => [],
  },
})

const coverFile = ref()
const getCover = async () => {
  if (props.source == 'avatar.png') {
    coverFile.value = proxy.Utils.getLocalResource('avatar.png')
    return
  }
  if (typeof props.source == 'string') {
    coverFile.value = proxy.Api.sourcePath + props.source
  } else if (props.source instanceof File) {
    let img = new FileReader()
    img.readAsDataURL(props.source)
    img.onload = ({ target }) => {
      coverFile.value = target.result
    }
  }
}

watch(
  () => props.source,
  async (newSource) => {
    getCover(newSource)
  },
  { immediate: true },
)

const imageList = computed(() => {
  return props.preImageList.map((item) => {
    return proxy.Api.sourcePath + item.replace(proxy.imageThumbnailSuffix, '')
  })
})

const initialIndex = ref(0)
const showViewer = ref(false)
const showViewerHandler = () => {
  if (props.preImageList.length == 0) {
    return
  }
  initialIndex.value = props.preImageList.findIndex((item) => {
    return item === props.source
  })
  showViewer.value = true
}

const coverRef = ref()
const loadingHeight = ref()
onMounted(() => {
  loadingHeight.value = coverRef.value.clientWidth * props.scale
})
</script>

<style lang="scss" scoped>
.image-panel {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  background: #f8f8f8;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;

  :deep(.el-image) {
    width: 100%;
    height: 100%;
  }

  :deep(.is-loading) {
    display: none;
  }

  :deep(.el-image__wrapper) {
    position: relative;
    vertical-align: top;
    width: 100%;
    height: 100%;
    display: flex;
  }

  .icon-image-error {
    margin: 0px auto;
    font-size: 20px;
    color: #838383;
    height: 100%;
  }

  .loading {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      width: 20px;
    }
  }
}
</style>
