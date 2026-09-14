<template>
  <aside class="right-rail">
    <section class="welcome-card">
      <h2>{{ loggedIn ? `你好，${userName || '社区成员'}` : '加入知潮社区' }}</h2>
      <p>围绕真实兴趣交换信息、经验与观点。重要变化都能回到原始证据。</p>
      <button v-if="loggedIn" type="button" @click="$emit('open-composer')">发布分享</button>
    </section>

    <section v-if="activeHub" class="stats-card">
      <div>
        <strong>{{ activeHub.memberCount || 0 }}</strong
        ><span>成员</span>
      </div>
      <div>
        <strong>{{ activeHub.postCount || 0 }}</strong
        ><span>动态</span>
      </div>
    </section>

    <section v-if="topics.length" class="side-card">
      <header>
        <h3>热门话题</h3>
        <span>按讨论数</span>
      </header>
      <button
        v-for="topic in topics.slice(0, 6)"
        :key="topic.topicId"
        type="button"
        class="topic-row"
        @click="$emit('choose-topic', topic)"
      >
        <span># {{ topic.canonicalName }}</span
        ><small>{{ topic.postCount || 0 }}</small>
      </button>
    </section>

    <section class="side-card assistant-card">
      <header>
        <h3>社区小助手</h3>
        <span>Agent</span>
      </header>
      <div class="assistant-intro">
        <span class="assistant-orb" aria-hidden="true">潮</span>
        <p>我会从本现场的帖子与评论中，帮你整理大家正在关注什么。</p>
      </div>
      <button
        type="button"
        class="assistant-cta"
        :disabled="!canUseAssistant"
        @click="$emit('open-assistant')"
      >
        {{ canUseAssistant ? '问问这个现场' : activeHub ? '加入现场后可使用' : '先选择一个兴趣现场' }}
        <span aria-hidden="true">→</span>
      </button>
    </section>

    <section v-if="events.length" class="side-card">
      <header>
        <h3>近期活动</h3>
        <button v-if="canManageEvents" type="button" class="manage-link" @click="$emit('manage-events')">
          管理活动
        </button>
        <span v-else>时间顺序</span>
      </header>
      <div v-for="event in events.slice(0, 3)" :key="event.eventId" class="event-row">
        <button type="button" class="event-info" @click="$emit('open-event-stats', event)">
          <time
            ><b>{{ eventDay(event.startsAt) }}</b
            ><span>{{ eventMonth(event.startsAt) }}</span></time
          >
          <span class="event-copy">
            <b>{{ event.title }}</b
            ><small>{{ event.venue || '地点待确认' }}</small>
          </span>
        </button>
        <el-dropdown
          trigger="click"
          @command="(status) => $emit('set-attendance', event, status)"
          @click.stop
        >
          <button type="button">{{ attendanceLabel(event.attendanceStatus) }}</button>
          <template #dropdown
            ><el-dropdown-menu
              ><el-dropdown-item
                v-for="status in attendanceStatuses"
                :key="status.value"
                :command="status.value"
                >{{ status.label }}</el-dropdown-item
              ><el-dropdown-item command="" divided>取消标记</el-dropdown-item></el-dropdown-menu
            ></template
          >
        </el-dropdown>
      </div>
    </section>
    <section v-else-if="canManageEvents" class="side-card empty-event-card">
      <header>
        <h3>近期活动</h3>
        <button type="button" class="manage-link" @click="$emit('manage-events')">创建活动</button>
      </header>
      <p class="side-note">为现场添加下一场活动，成员可以标记想参加、参加中或已参加。</p>
    </section>
    <p class="legal-links">关于 · 社区规则 · 隐私 · 联系我们<br />© 2026 ZENTIDE</p>
  </aside>
</template>

<script setup>
defineProps({
  loggedIn: { type: Boolean, default: false },
  userName: { type: String, default: '' },
  activeHub: { type: Object, default: null },
  canManageHub: { type: Boolean, default: false },
  canManageEvents: { type: Boolean, default: false },
  canUseAssistant: { type: Boolean, default: false },
  topics: { type: Array, default: () => [] },
  events: { type: Array, default: () => [] },
  attendanceStatuses: { type: Array, default: () => [] },
  eventDay: { type: Function, required: true },
  eventMonth: { type: Function, required: true },
  attendanceLabel: { type: Function, required: true },
})

defineEmits([
  'open-composer',
  'open-assistant',
  'choose-topic',
  'set-attendance',
  'manage-events',
  'open-event-stats',
])
</script>
<style scoped>
.manage-link {
  border: 0;
  background: none;
  color: var(--zt-tide);
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}
.side-note {
  margin: 0;
  color: var(--zt-text-2);
  font-size: 12px;
  line-height: 1.7;
}
.empty-event-card {
  padding-bottom: 14px;
}
.assistant-intro {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.assistant-intro p {
  margin: 0;
  color: var(--zt-text-2);
  font-size: 12px;
  line-height: 1.7;
}
.assistant-orb {
  display: grid;
  place-items: center;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  border-radius: 9px 9px 9px 3px;
  color: var(--zt-primary);
  background: var(--zt-tide-soft);
  font-size: 11px;
  font-weight: 760;
}
.assistant-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 13px;
  padding: 9px 10px;
  border: 1px solid var(--zt-border);
  border-radius: 5px;
  color: var(--zt-primary);
  background: var(--zt-surface);
  font-size: 11px;
  font-weight: 680;
  text-align: left;
  cursor: pointer;
}
.assistant-cta:hover:not(:disabled) {
  border-color: var(--zt-tide);
  background: var(--zt-tide-soft);
}
.assistant-cta:disabled {
  color: var(--zt-text-3);
  background: transparent;
  cursor: not-allowed;
}
</style>
