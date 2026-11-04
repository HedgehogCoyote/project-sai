<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@/services/api'
import { api, type InviteLink } from '@/services/workspace'

const props = defineProps<{ spaceId: number }>()
const emit = defineEmits<{ sessionExpired: [error: ApiError] }>()
const router = useRouter()
const links = ref<InviteLink[]>([]),
  busy = ref(false),
  error = ref(''),
  copied = ref<number | null>(null)

const url = (l: InviteLink) => location.origin + router.resolve({ name: 'join', params: { token: l.token } }).href
const day = (iso: string) => new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date(iso))

function fail(e: unknown, fallback: string) {
  if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
  error.value = e instanceof Error ? e.message : fallback
}
async function load() {
  try {
    links.value = await api.links(props.spaceId)
  } catch (e) {
    fail(e, '초대 링크를 불러오지 못했습니다.')
  }
}
async function create() {
  busy.value = true
  error.value = ''
  try {
    const link = await api.createLink(props.spaceId)
    links.value.unshift(link)
    await copy(link)
  } catch (e) {
    fail(e, '초대 링크를 만들지 못했습니다.')
  } finally {
    busy.value = false
  }
}
async function copy(l: InviteLink) {
  try {
    await navigator.clipboard.writeText(url(l))
    copied.value = l.id
    setTimeout(() => (copied.value = null), 2500)
  } catch {
    copied.value = null
  }
}
async function revoke(l: InviteLink) {
  busy.value = true
  try {
    await api.revokeLink(props.spaceId, l.id)
    links.value = links.value.filter((x) => x.id !== l.id)
  } catch (e) {
    fail(e, '링크를 폐기하지 못했습니다.')
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>

<template>
  <section class="ws-section" aria-label="초대 링크">
    <div class="ws-head">
      <h2>초대 링크</h2>
      <button class="ws-btn primary" type="button" :disabled="busy" @click="create">링크 만들기</button>
    </div>
    <p class="ws-hint">단톡방에 링크를 올리면 계정이 있는 사람은 바로 들어오고, 없는 사람은 가입 후 들어옵니다. 링크는 7일 동안 쓸 수 있고 언제든 폐기할 수 있습니다.</p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="!links.length" class="ws-meta">사용 중인 링크가 없습니다.</p>
    <div v-else class="ws-list">
      <div v-for="l in links" :key="l.id" class="ws-row">
        <div class="ws-grow">
          <strong class="ws-link-url">{{ url(l) }}</strong>
          <small>{{ l.createdByName }} · {{ day(l.expiresAt) }}까지</small>
        </div>
        <button class="ws-btn small" type="button" @click="copy(l)">{{ copied === l.id ? '복사됨' : '복사' }}</button>
        <button class="ws-btn small danger" type="button" :disabled="busy" @click="revoke(l)">폐기</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ws-link-url { font-size: 13px !important; font-weight: 500; user-select: all; }
</style>
