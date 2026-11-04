<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ApiError } from '@/services/api'
import { api, fileUrl, timeAgo, type Photo } from '@/services/workspace'
import CommentThread from './CommentThread.vue'

const props = defineProps<{
  spaceId: number
  eventId?: string | null
  canModerate: boolean
  activities: { id: string; title: string }[]
  /** When set, tapping a photo toggles it in this list instead of opening it (Foot Print picker). */
  picked?: number[] | null
  readOnly?: boolean
}>()
const emit = defineEmits<{ sessionExpired: [error: ApiError]; 'update:picked': [ids: number[]]; uploaded: [photo: Photo] }>()
const photos = ref<Photo[]>([]),
  loading = ref(true),
  error = ref(''),
  progress = ref(''),
  open = ref<Photo | null>(null),
  caption = ref(''),
  eventChoice = ref(''),
  busy = ref(false),
  dialog = ref<HTMLDialogElement | null>(null),
  input = ref<HTMLInputElement | null>(null)

const title = computed(() => (props.eventId ? '활동 사진' : '사진첩'))
const eventTitle = (id: string | null) => props.activities.find((a) => a.id === id)?.title
const canEdit = (p: Photo) => p.mine || props.canModerate

function fail(e: unknown, fallback: string) {
  if (e instanceof ApiError && e.status === 401) emit('sessionExpired', e)
  error.value = e instanceof Error ? e.message : fallback
}
async function load() {
  loading.value = true
  try {
    photos.value = await api.photos(props.spaceId, props.eventId)
  } catch (e) {
    fail(e, '사진을 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}
async function upload(files: FileList | null) {
  if (!files?.length) return
  error.value = ''
  const list = Array.from(files)
  for (const [i, file] of list.entries()) {
    progress.value = `사진 올리는 중 ${i + 1}/${list.length}`
    try {
      const photo = await api.uploadPhoto(props.spaceId, file, props.eventId)
      photos.value.unshift(photo)
      emit('uploaded', photo)
    } catch (e) {
      fail(e, `${file.name}을(를) 올리지 못했습니다.`)
    }
  }
  progress.value = ''
  if (input.value) input.value.value = ''
}
async function show(p: Photo) {
  if (props.picked) {
    const set = new Set(props.picked)
    if (set.has(p.id)) set.delete(p.id)
    else set.add(p.id)
    emit('update:picked', [...set])
    return
  }
  open.value = p
  caption.value = p.caption ?? ''
  eventChoice.value = p.eventId ?? ''
  await nextTick()
  dialog.value?.showModal()
}
function close() {
  dialog.value?.close()
  open.value = null
}
async function save() {
  if (!open.value) return
  busy.value = true
  try {
    const p = await api.updatePhoto(props.spaceId, open.value.id, caption.value.trim(), eventChoice.value || null)
    photos.value = photos.value.map((x) => (x.id === p.id ? p : x)).filter((x) => !props.eventId || x.eventId === props.eventId)
    open.value = p
  } catch (e) {
    fail(e, '사진 정보를 저장하지 못했습니다.')
  } finally {
    busy.value = false
  }
}
async function remove() {
  if (!open.value) return
  busy.value = true
  try {
    await api.deletePhoto(props.spaceId, open.value.id)
    photos.value = photos.value.filter((x) => x.id !== open.value?.id)
    close()
  } catch (e) {
    fail(e, '사진을 지우지 못했습니다.')
  } finally {
    busy.value = false
  }
}
onMounted(load)
watch(() => props.eventId, load)
defineExpose({ reload: load })
</script>

<template>
  <section class="ws-section" :aria-label="title">
    <div class="ws-head">
      <h2>{{ title }} <span class="ws-meta">{{ photos.length }}장</span></h2>
      <label v-if="!readOnly" class="ws-btn primary" :class="{ disabled: !!progress }">
        사진 올리기
        <input ref="input" class="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple :disabled="!!progress" @change="upload(($event.target as HTMLInputElement).files)" />
      </label>
    </div>
    <p class="ws-hint">
      {{ picked ? '기록에 넣을 사진을 눌러 고르세요.' : '멤버 모두가 보는 사진첩입니다. 활동에 연결한 사진은 그 활동의 Foot Print에 모입니다. 한 장에 10MB까지 올릴 수 있습니다.' }}
    </p>
    <p v-if="progress" class="ws-ok" role="status">{{ progress }}</p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="loading" class="ws-meta" role="status">사진을 불러오는 중...</p>
    <p v-else-if="!photos.length" class="ws-empty">아직 사진이 없습니다. 함께한 순간을 올려 보세요.</p>
    <div v-else class="ws-photos">
      <button
        v-for="p in photos"
        :key="p.id"
        class="ws-photo"
        :class="{ picked: picked?.includes(p.id) }"
        type="button"
        :aria-label="p.caption || `${p.uploaderName}님의 사진`"
        :aria-pressed="picked ? picked.includes(p.id) : undefined"
        @click="show(p)"
      >
        <img :src="fileUrl(p.url) + '?size=thumb'" alt="" loading="lazy" />
        <span v-if="picked?.includes(p.id)" class="ws-pick">✓</span>
      </button>
    </div>
    <dialog ref="dialog" class="ws-dialog" aria-label="사진 보기" @close="open = null" @click.self="close">
      <div v-if="open" class="ws-section">
        <div class="ws-head">
          <strong>{{ open.caption || '사진' }}</strong>
          <button class="ws-btn small" type="button" aria-label="닫기" @click="close">닫기</button>
        </div>
        <img class="full" :src="fileUrl(open.url)" :alt="open.caption || '사진'" />
        <p class="ws-meta">
          {{ open.uploaderName }} · {{ timeAgo(open.createdAt) }}<template v-if="eventTitle(open.eventId)"> · {{ eventTitle(open.eventId) }}</template>
          · <a :href="fileUrl(open.url)" download>원본 받기</a>
        </p>
        <form v-if="canEdit(open) && !readOnly" class="ws-form" @submit.prevent="save">
          <label>사진 설명<input v-model="caption" maxlength="200" :disabled="busy" /></label>
          <label>연결할 활동
            <select v-model="eventChoice" :disabled="busy">
              <option value="">연결 안 함</option>
              <option v-for="a in activities" :key="a.id" :value="a.id">{{ a.title }}</option>
            </select>
          </label>
          <div class="ws-actions">
            <button class="ws-btn primary" type="submit" :disabled="busy">저장</button>
            <button class="ws-btn danger" type="button" :disabled="busy" @click="remove">사진 지우기</button>
          </div>
        </form>
        <CommentThread :space-id="spaceId" :target="`photos:${open.id}`" :can-moderate="canModerate" title="댓글" @session-expired="emit('sessionExpired', $event)" />
      </div>
    </dialog>
  </section>
</template>

<style scoped>
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
label.ws-btn { position: relative; display: inline-flex; align-items: center; }
label.ws-btn.disabled { opacity: .55; }
</style>
