<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '@/services/api'
import { api, timeAgo, type Post } from '@/services/workspace'

const props = defineProps<{ spaceId: number; canManage: boolean }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError] }>()
const items = ref<Post[]>([]),
  loading = ref(true),
  body = ref(''),
  pinned = ref(true),
  busy = ref(false),
  error = ref('')

function fail(e: unknown, fallback: string) {
  if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
  error.value = e instanceof Error ? e.message : fallback
}
async function load() {
  loading.value = true
  try {
    items.value = await api.notices(props.spaceId)
  } catch (e) {
    fail(e, '공지를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}
async function post() {
  if (!body.value.trim() || busy.value) return
  busy.value = true
  error.value = ''
  try {
    await api.postNotice(props.spaceId, body.value.trim(), pinned.value)
    body.value = ''
    await load()
  } catch (e) {
    fail(e, '공지를 올리지 못했습니다.')
  } finally {
    busy.value = false
  }
}
async function remove(p: Post) {
  busy.value = true
  try {
    await api.deletePost(props.spaceId, p.id)
    items.value = items.value.filter((x) => x.id !== p.id)
  } catch (e) {
    fail(e, '공지를 지우지 못했습니다.')
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>

<template>
  <section class="ws-section" aria-label="공지">
    <div class="ws-head"><h2>공지</h2></div>
    <p class="ws-hint">소유자와 관리자가 올리는 알림입니다. 고정한 공지는 맨 위와 공간 홈에 보입니다.</p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <form v-if="canManage" class="ws-form ws-card" @submit.prevent="post">
      <label>새 공지<textarea v-model="body" maxlength="2000" placeholder="예: 이번 주 정기 모임은 토요일 오후 2시입니다." :disabled="busy" /></label>
      <label class="ws-check"><input v-model="pinned" type="checkbox" :disabled="busy" /> 맨 위에 고정</label>
      <div class="ws-actions"><button class="ws-btn primary" type="submit" :disabled="busy || !body.trim()">공지 올리기</button></div>
    </form>
    <p v-if="loading" class="ws-meta" role="status">공지를 불러오는 중...</p>
    <p v-else-if="!items.length" class="ws-empty">아직 공지가 없습니다.</p>
    <article v-for="p in items" :key="p.id" class="ws-card" :class="{ notice: p.pinned }">
      <div class="ws-head">
        <span v-if="p.pinned" class="ws-badge strong">고정</span>
        <span class="ws-meta">{{ p.authorName }} · {{ timeAgo(p.createdAt) }}</span>
        <button v-if="p.mine || canManage" class="ws-link" type="button" :disabled="busy" @click="remove(p)">지우기</button>
      </div>
      <p class="ws-pre">{{ p.body }}</p>
    </article>
  </section>
</template>
