<template>
  <article :id="`comment-${comment.commentId}`" class="comment-node">
    <router-link
      v-if="comment.authorId"
      class="thread-avatar"
      :to="{ name: 'people', params: { userId: comment.authorId } }"
      aria-label="查看作者主页"
    >
      <img v-if="comment.authorAvatar" :src="resourceUrl(comment.authorAvatar)" alt="" />
      <span v-else>{{ (comment.authorLabel || '社').slice(0, 1) }}</span>
    </router-link>
    <div v-else class="thread-avatar">
      <span>{{ (comment.authorLabel || '社').slice(0, 1) }}</span>
    </div>
    <div class="comment-main">
      <header>
        <router-link v-if="comment.authorId" :to="{ name: 'people', params: { userId: comment.authorId } }">{{
          comment.authorLabel || '社区成员'
        }}</router-link
        ><b v-else>{{ comment.authorLabel || '社区成员' }}</b
        ><time>{{ formatTime(comment.createdAt) }}</time>
      </header>
      <p>{{ comment.body }}</p>
      <div class="comment-actions">
        <button type="button" :class="{ active: comment.liked }" @click="$emit('like', comment)">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 14 6-6 6 6" /></svg
          ><strong>{{ comment.likeCount || 0 }}</strong>
        </button>
        <button type="button" class="down" aria-label="不赞同">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 10 6 6 6-6" /></svg>
        </button>
        <button type="button" @click="replying = !replying">回复</button>
        <button type="button" @click="$emit('share', comment)">分享</button>
      </div>
      <div v-if="replying" class="reply-box">
        <textarea
          v-model="draft"
          rows="3"
          :placeholder="`回复 ${comment.authorLabel || '这位成员'}…`"
          @keydown.meta.enter="submitReply"
          @keydown.ctrl.enter="submitReply"
        ></textarea>
        <div>
          <button type="button" @click="replying = false">取消</button
          ><button type="button" class="submit" @click="submitReply">发送回复</button>
        </div>
      </div>
      <div v-if="comment.children?.length" class="child-comments">
        <CommentThread
          v-for="child in comment.children"
          :key="child.commentId"
          :comment="child"
          :resource-url="resourceUrl"
          @like="$emit('like', $event)"
          @share="$emit('share', $event)"
          @reply="forwardReply"
        />
      </div>
    </div>
  </article>
</template>

<script setup>
import { ref } from 'vue'

defineOptions({ name: 'CommentThread' })
const props = defineProps({
  comment: { type: Object, required: true },
  resourceUrl: { type: Function, required: true },
})
const emit = defineEmits(['like', 'share', 'reply'])
const replying = ref(false)
const draft = ref('')
const submitReply = () => {
  const body = draft.value.trim()
  if (body.length < 2) return
  emit('reply', { parent: props.comment, body })
  draft.value = ''
  replying.value = false
}
const forwardReply = (payload) => emit('reply', payload)
const formatTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
    : '刚刚'
</script>

<style scoped>
.comment-node {
  position: relative;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  padding-top: 22px;
}
.comment-node::before {
  position: absolute;
  top: 68px;
  bottom: -22px;
  left: 23px;
  width: 1px;
  background: #d9dade;
  content: '';
}
.thread-avatar {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  text-decoration: none;
}
.thread-avatar img,
.thread-avatar span {
  width: 42px;
  height: 42px;
  border-radius: 50%;
}
.thread-avatar img {
  display: block;
  object-fit: cover;
}
.thread-avatar span {
  display: grid;
  place-items: center;
  color: #fff;
  background: #555b75;
  font-size: 15px;
  font-weight: 700;
}
.comment-main {
  min-width: 0;
}
.comment-main > header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 39px;
}
.comment-main > header b,
.comment-main > header a {
  color: #44486d;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}
.comment-main > header a:hover {
  text-decoration: underline;
}
.comment-main > header time {
  color: #8c8e94;
  font-size: 12px;
}
.comment-main > p {
  margin: 7px 0 12px;
  color: #33353a;
  font-size: 16px;
  line-height: 1.65;
  white-space: pre-wrap;
}
.comment-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.comment-actions button {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 7px;
  border: 0;
  border-radius: 6px;
  color: #85878c;
  background: transparent;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}
.comment-actions button:hover,
.comment-actions button.active {
  color: #464c85;
  background: #f0f1f6;
}
.comment-actions svg {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}
.comment-actions .down {
  margin-left: -4px;
}
.reply-box {
  margin-top: 12px;
  padding: 11px;
  border: 1px solid #dfe0e4;
  border-radius: 10px;
  background: #fafafb;
}
.reply-box textarea {
  display: block;
  width: 100%;
  resize: vertical;
  border: 0;
  outline: 0;
  color: #333;
  background: transparent;
  font: inherit;
  line-height: 1.55;
}
.reply-box > div {
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  margin-top: 7px;
}
.reply-box button {
  padding: 6px 10px;
  border: 0;
  border-radius: 6px;
  color: #666;
  background: transparent;
  cursor: pointer;
}
.reply-box .submit {
  color: #fff;
  background: #292b30;
}
.child-comments {
  position: relative;
  margin-left: 18px;
}
.child-comments > .comment-node::before {
  left: 23px;
}
@media (max-width: 640px) {
  .comment-node {
    grid-template-columns: 38px minmax(0, 1fr);
    gap: 8px;
  }
  .comment-node::before {
    left: 18px;
  }
  .thread-avatar {
    width: 38px;
    height: 38px;
  }
  .thread-avatar img,
  .thread-avatar span {
    width: 34px;
    height: 34px;
  }
  .comment-main > p {
    font-size: 14px;
  }
  .child-comments {
    margin-left: 5px;
  }
}
/* Unified editorial community skin */
.comment-node::before {
  background: var(--zt-border);
}
.thread-avatar span {
  background: var(--zt-primary);
}
.comment-main > header b,
.comment-main > header a {
  color: var(--zt-primary);
}
.comment-main > header time {
  color: var(--zt-text-3);
}
.comment-main > p {
  color: var(--zt-text);
}
.comment-actions button {
  color: var(--zt-text-3);
}
.comment-actions button:hover,
.comment-actions button.active {
  color: var(--zt-primary);
  background: var(--zt-primary-soft);
}
.reply-box {
  border-color: var(--zt-border);
  border-radius: var(--zt-radius-sm);
  background: var(--zt-surface-hover);
}
.reply-box textarea {
  color: var(--zt-text);
}
.reply-box button {
  color: var(--zt-text-2);
}
.reply-box .submit {
  color: #fff;
  background: var(--zt-primary);
}
</style>
