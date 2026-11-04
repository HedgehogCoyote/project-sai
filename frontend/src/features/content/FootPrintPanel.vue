<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ApiError } from '@/services/api'
import { fileUrl } from '@/services/workspace'
import PhotoAlbum from '@/components/workspace/PhotoAlbum.vue'
import { souvenirIsPhoto, souvenirLabel, souvenirsFor, withSubject } from '@/features/room/souvenirs'
import type { Activity, ContentState, FootPrint, Member } from './types'

const props = defineProps<{
  spaceId: number
  activity: Activity
  footprint: FootPrint | undefined
  members: Member[]
  canModerate: boolean
  busy: boolean
  readOnly: boolean
  commit: (mutate: (next: ContentState) => void) => Promise<boolean>
}>()
const emit = defineEmits<{ sessionExpired: [error: ApiError] }>()
const picked = ref<number[]>([]),
  note = ref(''),
  souvenir = ref(''),
  souvenirPhoto = ref<number | null>(null),
  error = ref('')

watch(
  () => props.footprint,
  (fp) => {
    picked.value = [...(fp?.photoIds ?? [])]
    souvenir.value = fp?.souvenir?.kind ?? ''
    souvenirPhoto.value = fp?.souvenir?.photoId ?? fp?.photoIds[0] ?? null
  },
  { immediate: true },
)
const photoSrc = (id: number, full = false) => fileUrl(`/api/spaces/${props.spaceId}/photos/${id}/file${full ? '' : '?size=thumb'}`)
const name = (id: string) => props.members.find((m) => m.id === id)?.name ?? '멤버'
const options = computed(() => souvenirsFor(props.activity.type))
const dirtyPhotos = computed(() => JSON.stringify(picked.value) !== JSON.stringify(props.footprint?.photoIds ?? []))

function edit(mutate: (fp: FootPrint) => void, after?: (next: ContentState) => void) {
  error.value = ''
  return props.commit((next) => {
    next.footprints ??= []
    const fp = next.footprints.find((f) => f.eventId === props.activity.id)
    if (!fp) throw new Error('Foot Print를 찾을 수 없습니다. 새로고침해 주세요.')
    mutate(fp)
    after?.(next)
  })
}
function start() {
  return props.commit((next) => {
    next.footprints ??= []
    if (next.footprints.some((f) => f.eventId === props.activity.id)) return
    next.footprints.push({
      id: crypto.randomUUID(),
      eventId: props.activity.id,
      title: props.activity.title,
      start: props.activity.start,
      end: props.activity.end,
      status: 'draft',
      photoIds: [],
      coverPhotoId: null,
      notes: [],
      souvenir: null,
      publishedAt: '',
    })
  })
}
const savePhotos = () =>
  edit((fp) => {
    fp.photoIds = [...picked.value]
    if (fp.coverPhotoId && !fp.photoIds.includes(fp.coverPhotoId)) fp.coverPhotoId = null
    fp.coverPhotoId ??= fp.photoIds[0] ?? null
  })
async function addNote() {
  const body = note.value.trim()
  if (!body) return
  if (await edit((fp) => fp.notes.push({ id: crypto.randomUUID(), memberId: 'self', body, createdAt: new Date().toISOString() })))
    note.value = ''
}
const removeNote = (id: string) => edit((fp) => (fp.notes = fp.notes.filter((n) => n.id !== id)))
async function publish() {
  if (!souvenir.value) {
    error.value = '방에 놓을 기념품을 골라 주세요.'
    return
  }
  if (souvenirIsPhoto(souvenir.value) && !souvenirPhoto.value) {
    error.value = '사진 기념품에 넣을 사진을 먼저 골라 주세요.'
    return
  }
  await edit((fp) => {
    if (!fp.photoIds.length && !fp.notes.length) throw new Error('사진이나 후기를 하나 이상 남긴 뒤 게시해 주세요.')
    fp.souvenir = { kind: souvenir.value, photoId: souvenirIsPhoto(souvenir.value) ? souvenirPhoto.value : null }
    fp.status = 'published'
    fp.publishedAt = new Date().toISOString()
  }, (next) => {
    // A new souvenir goes on the shelf while it has room; otherwise it waits in the archive.
    const shown = (next.footprints ?? []).filter((f) => f.status === 'published' && f.displayed !== false && f.eventId !== props.activity.id)
    const fp = next.footprints?.find((f) => f.eventId === props.activity.id)
    if (fp) fp.displayed = shown.length < 8
  })
}
const reopen = () => edit((fp) => (fp.status = 'draft'))
</script>

<template>
  <section class="section ws-section" aria-label="Foot Print">
    <div class="ws-head">
      <h2>Foot Print</h2>
      <span v-if="footprint" class="ws-badge" :class="{ strong: footprint.status === 'published' }">
        {{ footprint.status === 'published' ? '게시됨' : '초안' }}
      </span>
    </div>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <template v-if="!footprint">
      <div v-if="activity.status === 'completed'" class="ws-card notice">
        <strong>활동을 끝냈습니다. 함께한 기록을 남겨 보세요.</strong>
        <p class="ws-hint">사진과 한 줄 후기를 모아 게시하면 고른 기념품이 공간 방의 선반에 놓입니다. 기념품을 누르면 이 기록이 다시 열립니다.</p>
        <div class="ws-actions"><button class="ws-btn primary" type="button" :disabled="busy || readOnly" @click="start">Foot Print 만들기</button></div>
      </div>
      <p v-else class="ws-hint">활동을 완료로 표시하면 사진과 후기를 모아 Foot Print를 남길 수 있습니다.</p>
    </template>
    <template v-else-if="footprint.status === 'draft'">
      <p class="ws-hint">여러 멤버가 함께 채우는 초안입니다. 사진을 고르고 후기를 남긴 뒤 기념품을 정해 게시하세요. 게시하기 전까지는 방에 놓이지 않습니다.</p>
      <PhotoAlbum
        v-model:picked="picked"
        :space-id="spaceId"
        :event-id="activity.id"
        :can-moderate="canModerate"
        :activities="[{ id: activity.id, title: activity.title }]"
        :read-only="readOnly"
        @session-expired="emit('sessionExpired', $event)"
      />
      <div v-if="dirtyPhotos" class="ws-actions">
        <button class="ws-btn primary" type="button" :disabled="busy" @click="savePhotos">고른 사진 {{ picked.length }}장 저장</button>
      </div>
      <div class="ws-card">
        <strong>한 줄 후기</strong>
        <div v-for="n in footprint.notes" :key="n.id" class="ws-comment">
          <header><strong>{{ name(n.memberId) }}</strong><button v-if="n.memberId === 'self' || canModerate" class="ws-link" type="button" :disabled="busy" @click="removeNote(n.id)">지우기</button></header>
          <p class="ws-pre">{{ n.body }}</p>
        </div>
        <form class="ws-comment-form" @submit.prevent="addNote">
          <input v-model="note" maxlength="300" aria-label="한 줄 후기" placeholder="예: 새벽 광안대교 보면서 먹은 컵라면" :disabled="busy || readOnly" />
          <button class="ws-btn" type="submit" :disabled="busy || !note.trim()">남기기</button>
        </form>
      </div>
      <div class="ws-card">
        <strong>방에 놓을 기념품</strong>
        <div class="ws-choice" role="radiogroup" aria-label="기념품">
          <button v-for="s in options" :key="s.id" type="button" role="radio" :aria-checked="souvenir === s.id" @click="souvenir = s.id">
            <strong>{{ s.label }}</strong><span>{{ s.photo ? '사진이 들어감' : '물건' }}</span>
          </button>
        </div>
        <label v-if="souvenir && souvenirIsPhoto(souvenir)" class="ws-label">
          기념품에 넣을 사진
          <span v-if="!footprint.photoIds.length" class="ws-hint">위에서 사진을 골라 저장하면 여기서 고를 수 있습니다.</span>
          <span class="ws-photos">
            <button v-for="id in footprint.photoIds" :key="id" class="ws-photo" :class="{ picked: souvenirPhoto === id }" type="button" :aria-pressed="souvenirPhoto === id" @click="souvenirPhoto = id">
              <img :src="photoSrc(id)" alt="" loading="lazy" />
            </button>
          </span>
        </label>
        <div class="ws-actions"><button class="ws-btn primary" type="button" :disabled="busy || readOnly" @click="publish">게시하고 방에 놓기</button></div>
      </div>
    </template>
    <template v-else>
      <div class="ws-card notice">
        <strong>{{ footprint.title }} · {{ footprint.start || '날짜 미정' }}{{ footprint.end && footprint.end !== footprint.start ? ` ~ ${footprint.end}` : '' }}</strong>
        <div v-if="footprint.photoIds.length" class="ws-photos">
          <a v-for="id in footprint.photoIds" :key="id" class="ws-photo" :href="photoSrc(id, true)" target="_blank" rel="noopener"><img :src="photoSrc(id)" alt="" loading="lazy" /></a>
        </div>
        <div v-for="n in footprint.notes" :key="n.id" class="ws-comment"><header><strong>{{ name(n.memberId) }}</strong></header><p class="ws-pre">{{ n.body }}</p></div>
        <p class="ws-hint">{{ footprint.souvenir ? withSubject(souvenirLabel(footprint.souvenir.kind)) : '기념품이' }} 공간 방 선반에 놓였습니다. 방에서 누르면 이 기록이 열립니다.</p>
        <div class="ws-actions"><button class="ws-btn" type="button" :disabled="busy || readOnly" @click="reopen">다시 고치기</button></div>
      </div>
    </template>
  </section>
</template>
