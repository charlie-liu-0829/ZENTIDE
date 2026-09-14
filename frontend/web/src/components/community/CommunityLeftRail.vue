<template>
  <aside class="left-rail">
    <nav class="primary-links" aria-label="主要页面">
      <router-link to="/community"
        ><svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3.5 10.7 12 3.8l8.5 6.9v8.1a1.7 1.7 0 0 1-1.7 1.7H5.2a1.7 1.7 0 0 1-1.7-1.7Z" />
          <path d="M9.2 20.5v-6.2h5.6v6.2" /></svg
        >社区首页</router-link
      >
      <router-link to="/community/discover"
        ><svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="8.5" />
          <path d="m14.8 9.2-1.7 3.9-3.9 1.7 1.7-3.9Z" /></svg
        >发现兴趣现场</router-link
      >
      <router-link to="/today"
        ><svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3.5 14.3 9l5.9.5-4.5 3.9 1.4 5.8-5.1-3-5.1 3 1.4-5.8-4.5-3.9L9.7 9Z" /></svg
        >今日精选</router-link
      >
      <router-link to="/community/insights"
        ><svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3.5a8.5 8.5 0 1 0 8.5 8.5A8.5 8.5 0 0 0 12 3.5Z" />
          <path d="M12 7v5l3.2 2" /></svg
        >我的兴趣情报</router-link
      >
    </nav>

    <div class="rail-heading">
      <span>{{ loggedIn ? '我的兴趣现场' : '热门兴趣现场' }}</span>
      <div>
        <button v-if="loggedIn" type="button" title="整理分类" @click="$emit('manage-categories')">
          整理
        </button>
      </div>
    </div>
    <nav class="hub-links" aria-label="兴趣现场">
      <button type="button" :class="{ active: !selectedHub }" @click="$emit('select-hub', null)">
        <i class="hub-icon all"
          ><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5.5h16M4 12h16M4 18.5h10" /></svg></i
        ><span>全部动态</span>
      </button>
      <section v-for="group in groupedHubs" :key="group.key" class="hub-group">
        <div class="hub-group-label">{{ group.name }}</div>
        <button
          v-for="hub in group.hubs"
          :key="hub.hubId"
          type="button"
          :class="{ active: selectedHub === hub.hubId }"
          @click="$emit('select-hub', hub.hubId)"
        >
          <i class="hub-icon"
            ><img v-if="hub.coverUrl" :src="resourceUrl(hub.coverUrl)" alt="" /><span v-else>{{
              hub.name.slice(0, 1)
            }}</span></i
          >
          <span>{{ hub.name }}</span
          ><em v-if="hub.owned">创建</em><small>{{ hub.postCount || 0 }}</small>
        </button>
      </section>
      <p v-if="!hubs.length" class="hub-list-empty">
        {{ loggedIn ? '还没有加入兴趣现场' : '暂时没有热门现场' }}
      </p>
    </nav>
    <button type="button" class="new-hub" @click="$emit('create-hub')">＋ 创建兴趣现场</button>
    <p class="rail-note">加入的现场可以按你的方式整理成音乐、游戏或任意分类。</p>
  </aside>
</template>

<script setup>
defineProps({
  loggedIn: { type: Boolean, default: false },
  groupedHubs: { type: Array, default: () => [] },
  hubs: { type: Array, default: () => [] },
  selectedHub: { type: [Number, String], default: null },
  resourceUrl: { type: Function, required: true },
})

defineEmits(['select-hub', 'manage-categories', 'create-hub'])
</script>
