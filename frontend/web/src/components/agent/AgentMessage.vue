<template>
  <article class="agent-message" :class="role">
    <div v-if="role === 'assistant'" class="agent-avatar" aria-hidden="true">潮</div>
    <div class="message-bubble">
      <span v-if="role === 'user'" class="plain-message">{{ content }}</span>
      <template v-else>
        <template v-for="(block, index) in safeBlocks" :key="`${index}-${block.entityId || ''}`">
          <span v-if="block.type === 'text'" class="answer-text">{{ cleanText(block.text) }}</span>
          <router-link
            v-else-if="block.type === 'reference' && block.entityType === 'POST'"
            class="post-reference"
            :to="{ name: 'community-post', params: { postId: block.entityId } }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 4.5h10a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2Z" />
              <path d="M8.5 9h7M8.5 13h5" />
            </svg>
            {{ block.label || '查看相关帖子' }}
          </router-link>
        </template>
      </template>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, default: '' },
  blocks: { type: Array, default: () => [] },
})

const safeBlocks = computed(() =>
  props.blocks.length
    ? props.blocks.map((block) => ({
        type: block.type,
        text: String(block.text || ''),
        entityType: block.entity_type || block.entityType,
        entityId: Number(block.entity_id || block.entityId || 0),
        label: String(block.label || ''),
      }))
    : [{ type: 'text', text: props.content }],
)

const cleanText = (value) =>
  String(value)
    .replace(/(?:file:\/\/)?[^\s`]*knowledge-snapshots\/[^\s`]*/gi, '')
    .replace(/\b(?:scene|post|user)[ _-]?(?:id|编号)\s*[=:：]?\s*[A-Za-z0-9_-]+/gi, '')
    .replace(/\b(?:scene|post|comment|user)[ _-]?id\b/gi, '')
    .replace(/(?:现场|帖子|评论|用户)\s*(?:编号|ID|id)/g, '相关内容')
    .replace(/\[帖子\s*#?\d+(?:\s*[·.]\s*\d+\s*楼)?\]/g, '相关帖子')
    .replace(/(?<!\[)帖子\s*#\s*\d+/g, '当前帖子')
    .replace(/(?<!\[)帖子\s*\d+/g, '相关帖子')
    .replace(/第\s*\d+\s*号帖子/g, '相关帖子')
    .replace(/(^|\n)#{1,6}\s+/g, '$1')
    .replace(/\*\*([^*\n]+)\*\*/g, '$1')
    .replace(/`([^`\n]+)`/g, '$1')
    .replace(/(^|\n)[*-]\s+/g, '$1• ')
</script>

<style scoped>
.agent-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: min(780px, 92%);
}
.agent-message.user {
  align-self: flex-end;
  justify-content: flex-end;
}
.agent-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border: 1px solid #c9d8d3;
  border-radius: 10px 10px 10px 3px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-size: 11px;
  font-weight: 800;
}
.message-bubble {
  padding: 12px 15px;
  border: 1px solid var(--zt-border-soft);
  border-radius: 4px 13px 13px 13px;
  color: var(--zt-text);
  background: var(--zt-surface);
  font-size: 14px;
  line-height: 1.75;
  overflow-wrap: anywhere;
  box-shadow: var(--zt-shadow-sm);
}
.user .message-bubble {
  border-color: var(--zt-primary);
  border-radius: 13px 4px 13px 13px;
  color: #fff;
  background: var(--zt-primary);
}
.plain-message,
.answer-text {
  white-space: pre-wrap;
}
.post-reference {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 1px 3px;
  padding: 2px 6px;
  border-radius: 5px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-weight: 720;
  text-decoration: none;
  vertical-align: baseline;
}
.post-reference:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}
.post-reference svg {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}
</style>
