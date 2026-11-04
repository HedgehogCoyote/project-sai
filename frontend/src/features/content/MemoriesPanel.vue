<script setup lang="ts">
import { computed, ref } from 'vue'
import { fileUrl } from '@/services/workspace'
import { souvenirLabel } from '@/features/room/souvenirs'
import type { ContentState, FootPrint } from './types'

/** The space's archive: every published Foot Print, newest first, and which souvenirs stand on the shelf. */
const props = defineProps<{
  spaceId: number
  data: ContentState
  busy: boolean
  commit: (mutate: (next: ContentState) => void) => Promise<boolean>
}>()
const emit = defineEmits<{ open: [eventId: string] }>()
const error = ref('')
const published = computed(() =>
  (props.data.footprints ?? []).filter((f) => f.status === 'published').sort((a, b) => b.publishedAt.localeCompare(a.publishedAt)),
)
const drafts = computed(() => (props.data.footprints ?? []).filter((f) => f.status === 'draft').length)
const shown = computed(() => published.value.filter((f) => f.souvenir && f.displayed !== false).length)
const cover = (f: FootPrint) => {
  const id = f.coverPhotoId ?? f.photoIds[0]
  return id ? fileUrl(`/api/spaces/${props.spaceId}/photos/${id}/file?size=thumb`) : null
}
const period = (f: FootPrint) => (!f.start && !f.end ? '날짜 미정' : f.end && f.end !== f.start ? `${f.start} ~ ${f.end}` : f.start || f.end)
async function toggle(f: FootPrint) {
  error.value = ''
  const on = f.displayed === false
  if (on && shown.value >= 8) {
    error.value = '선반에는 8개까지 놓을 수 있습니다. 다른 기념품을 먼저 내려 주세요.'
    return
  }
  await props.commit((next) => {
    const target = next.footprints?.find((x) => x.id === f.id)
    if (target) target.displayed = on
  })
}
</script>

<template>
  <section class="ws-section" aria-label="추억 보관함">
    <div class="ws-head"><h2>추억 {{ published.length }}개</h2><span class="ws-meta">선반 {{ shown }}/8</span></div>
    <p class="ws-hint">게시한 Foot Print가 쌓이는 곳입니다. 방 선반에는 기념품을 8개까지 놓을 수 있고, 내린 기념품도 여기에는 남습니다.<template v-if="drafts"> 아직 게시하지 않은 초안 {{ drafts }}개가 있습니다.</template></p>
    <p v-if="error" class="ws-error" role="alert">{{ error }}</p>
    <p v-if="!published.length" class="ws-empty">아직 남긴 추억이 없습니다. 활동을 완료하고 Foot Print를 게시하면 여기에 쌓입니다.</p>
    <article v-for="f in published" :key="f.id" class="memory ws-card">
      <img v-if="cover(f)" class="cover" :src="cover(f)!" alt="" loading="lazy" />
      <div v-else class="cover empty" aria-hidden="true">{{ f.title.slice(0, 1) }}</div>
      <div class="body">
        <strong>{{ f.title }}</strong>
        <span class="ws-meta">{{ period(f) }} · 사진 {{ f.photoIds.length }}장 · 후기 {{ f.notes.length }}개</span>
        <p v-if="f.notes[0]" class="ws-pre quote">“{{ f.notes[0].body }}”</p>
        <div class="ws-actions">
          <button class="ws-btn small" type="button" @click="emit('open', f.eventId)">기록 열기</button>
          <button v-if="f.souvenir" class="ws-btn small" type="button" :disabled="busy" :aria-pressed="f.displayed !== false" @click="toggle(f)">
            {{ souvenirLabel(f.souvenir.kind) }} · {{ f.displayed === false ? '선반에 놓기' : '선반에서 내리기' }}
          </button>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.memory { grid-template-columns: 120px minmax(0, 1fr); align-items: start; gap: 16px; }
.cover { width: 120px; height: 120px; border-radius: 8px; object-fit: cover; background: #efe3dc; }
.cover.empty { display: grid; place-items: center; font-size: 36px; color: #a58aa0; font-weight: 700; }
.body { display: grid; gap: 6px; min-width: 0; }
.quote { color: #6b5a64; font-size: 13px; }
@media (max-width: 560px) { .memory { grid-template-columns: 84px minmax(0, 1fr); } .cover { width: 84px; height: 84px; } }
</style>
