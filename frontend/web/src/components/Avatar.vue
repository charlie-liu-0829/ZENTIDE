<template>
  <Cover :source="source" :width="width" :lazy="lazy" borderRadius="50%"></Cover>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  width: {
    type: Number,
    default: 40,
  },
  avatar: {
    type: String,
    default: 'avatar.png',
  },
  lazy: {
    type: Boolean,
    default: true,
  },
})

const source = computed(() => {
  if (!props.avatar) {
    return 'avatar.png'
  } else {
    return props.avatar
  }
})
</script>
<style lang="scss" scoped></style>
