<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '@/services/api'
import { api, timeAgo, type FeedItem } from '@/services/workspace'

const props = defineProps<{ spaceId: number }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError]; open: [module: string | null]; seen: [] }>()
const items = ref<FeedItem[]>([]),
  loading = ref(true),
  error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await api.spaceFeed(props.spaceId, 50)
    await api.seen(props.spaceId)
    emit('seen')
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
    error.value = e instanceof Error ? e.message : '소식을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <section class="ws-section" aria-label="최근 소식">
    <div class="ws-head"><h2>최근 소식</h2><button class="ws-btn small" type="button" :disabled="loading" @click="load">새로고침</button></div>
    <p class="ws-hint">멤버들이 이 공간에서 한 일입니다. 점이 있는 줄은 지난번에 들어온 뒤 생긴 소식입니다.</p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="loading" class="ws-meta" role="status">소식을 불러오는 중...</p>
    <p v-else-if="!items.length" class="ws-empty">아직 소식이 없습니다. 활동을 만들거나 사진을 올리면 여기에 쌓입니다.</p>
    <div v-else class="ws-list">
      <div v-for="item in items" :key="item.id" class="ws-row" :class="{ unread: item.unread }">
        <span v-if="item.unread" class="ws-dot" aria-label="새 소식" />
        <div class="ws-grow"><strong>{{ item.summary }}</strong><small>{{ timeAgo(item.createdAt) }}</small></div>
        <button v-if="item.module" class="ws-btn small" type="button" @click="emit('open', item.module)">열기</button>
      </div>
    </div>
  </section>
</template>
