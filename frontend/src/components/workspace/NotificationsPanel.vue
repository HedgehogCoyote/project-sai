<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '@/services/api'
import { api, timeAgo, type FeedItem } from '@/services/workspace'

const emit = defineEmits<{ sessionExpired: [error: ApiError]; navigate: [spaceId: number, module: string | null] }>()
const items = ref<FeedItem[]>([]),
  loading = ref(true),
  error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await api.feed(60)
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
    error.value = e instanceof Error ? e.message : '알림을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <section class="ws-section" aria-label="알림">
    <div class="ws-head"><h1 class="title">알림</h1><button class="ws-btn small" type="button" :disabled="loading" @click="load">새로고침</button></div>
    <p class="ws-hint">내가 속한 공간에서 다른 멤버가 한 일입니다. 공간에 들어가면 그 공간의 새 알림이 읽음으로 바뀝니다.</p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="loading" class="ws-meta" role="status">알림을 불러오는 중...</p>
    <p v-else-if="!items.length" class="ws-empty">아직 알림이 없습니다.</p>
    <div v-else class="ws-list">
      <button v-for="item in items" :key="item.id" type="button" class="ws-row as-button" :class="{ unread: item.unread }" @click="emit('navigate', item.spaceId, item.module)">
        <span v-if="item.unread" class="ws-dot" aria-label="새 알림" />
        <span class="ws-grow"><strong>{{ item.summary }}</strong><small>{{ item.spaceTitle }} · {{ timeAgo(item.createdAt) }}</small></span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.title { margin: 0 auto 0 0; font-size: 26px; }
.as-button { width: 100%; border: 0; border-bottom: 1px solid var(--line); background: none; text-align: left; font: inherit; cursor: pointer; }
.as-button:hover { background: #faf6f8; }
</style>
