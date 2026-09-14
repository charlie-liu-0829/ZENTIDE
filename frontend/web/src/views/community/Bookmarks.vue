<template><main class="bookmarks"><header><span class="eyebrow">YOUR LIBRARY</span><h1>我的收藏</h1><p>把值得回看的帖子留在这里。</p></header><div v-if="posts.length" class="list"><article v-for="post in posts" :key="post.postId" @click="open(post.postId)"><div><small>{{ post.hubName }} · {{ post.authorLabel || '社区成员' }}</small><h2>{{ post.title || '无标题分享' }}</h2><p>{{ excerpt(post.body) }}</p></div><strong>♥ {{ post.likeCount || 0 }}</strong></article></div><div v-else class="empty">还没有收藏内容。回到社区，收藏一篇你想以后再看的分享吧。</div></main></template>
<script setup>
import { getCurrentInstance, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
const { proxy } = getCurrentInstance(); const router = useRouter(); const posts = ref([])
const load = async () => { const result = await proxy.Request({ url: proxy.Api.zentideCommunityBookmarks, params: { limit: 200 }, showLoading: false }); if (result) posts.value = result.data || [] }
const open = (postId) => router.push({ name: 'community-post', params: { postId } }); const excerpt = (body) => (body || '').replace(/<[^>]+>/g, '').slice(0, 150)
onMounted(load)
</script>
<style scoped>
.bookmarks{max-width:860px;margin:0 auto;padding:40px 0 88px;color:var(--zt-text)}
.eyebrow{color:var(--zt-tide);font-size:10px;font-weight:800;letter-spacing:1.45px}
.bookmarks h1{margin:9px 0 6px;font-size:clamp(30px,3.5vw,40px);font-weight:770;letter-spacing:-.045em}
.bookmarks header p{margin:0 0 25px;color:var(--zt-text-3);font-size:13px}
.list{display:grid;gap:9px}.list article{display:flex;justify-content:space-between;gap:20px;padding:19px 21px;border:1px solid var(--zt-border);border-radius:8px;background:var(--zt-surface);cursor:pointer;transition:.18s}.list article:hover{border-color:color-mix(in srgb,var(--zt-tide) 48%,var(--zt-border));box-shadow:0 8px 22px rgba(24,55,48,.06);transform:translateY(-1px)}.list small{color:var(--zt-text-3);font-size:11px}.list h2{margin:8px 0 5px;font-size:18px;letter-spacing:-.015em}.list p{margin:0;color:var(--zt-text-2);font-size:13px;line-height:1.6}.list strong{color:var(--zt-tide);white-space:nowrap}.empty{padding:70px 20px;border:1px dashed var(--zt-border);color:var(--zt-text-3);text-align:center;background:var(--zt-surface-hover)}@media(max-width:680px){.bookmarks{padding:26px 0 74px}.list article{padding:16px}}
</style>
