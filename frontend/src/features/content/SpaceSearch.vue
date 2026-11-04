<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ContentState } from './types'

/** Finds anything in the space's shared content and opens where it lives. */
const props = defineProps<{ data: ContentState }>()
const emit = defineEmits<{ open: [view: string, eventId: string | null, record?: string] }>()
const query = ref('')
type Hit = { key: string; kind: string; title: string; detail: string; view: string; eventId: string | null; record?: string }

const activityTitle = (id: string | null) => props.data.activities.find((a) => a.id === id)?.title
const hits = computed<Hit[]>(() => {
  const q = query.value.trim().toLowerCase()
  if (q.length < 1) return []
  const has = (...texts: (string | undefined | null)[]) => texts.some((t) => t?.toLowerCase().includes(q))
  const scope = (eventId: string | null) => activityTitle(eventId) ?? '공간'
  const d = props.data
  const out: Hit[] = []
  for (const a of d.activities) if (has(a.title)) out.push({ key: 'a' + a.id, kind: '활동', title: a.title, detail: a.status === 'completed' ? '완료' : '진행 중', view: 'home', eventId: a.id })
  for (const r of d.records) if (has(r.title, r.body)) out.push({ key: 'r' + r.id, kind: '기록', title: r.title, detail: scope(r.eventId), view: 'records', eventId: r.eventId, record: r.id })
  for (const t of d.tasks) if (has(t.title)) out.push({ key: 't' + t.id, kind: '할 일', title: t.title, detail: scope(t.eventId), view: 'tasks', eventId: t.eventId })
  for (const a of d.appointments) if (has(a.title, a.memo)) out.push({ key: 'c' + a.id, kind: '일정', title: a.title, detail: `${a.date} · ${scope(a.eventId)}`, view: a.eventId ? 'itinerary' : 'calendar', eventId: a.eventId })
  for (const p of d.places) if (has(p.title, p.address)) out.push({ key: 'p' + p.id, kind: '장소', title: p.title, detail: p.address || scope(p.eventId), view: 'places', eventId: p.eventId })
  for (const p of d.polls) if (has(p.title, ...p.options.map((o) => o.title))) out.push({ key: 'v' + p.id, kind: '투표', title: p.title, detail: scope(p.eventId), view: 'polls', eventId: p.eventId })
  for (const e of d.expenses) if (has(e.title)) out.push({ key: 'e' + e.id, kind: '지출', title: e.title, detail: `${e.amount.toLocaleString('ko-KR')}원 · ${scope(e.eventId)}`, view: 'expenses', eventId: e.eventId })
  for (const p of d.packing) if (has(p.title)) out.push({ key: 'k' + p.id, kind: '준비물', title: p.title, detail: scope(p.eventId), view: 'packing', eventId: p.eventId })
  for (const f of d.footprints ?? []) if (has(f.title, ...f.notes.map((n) => n.body))) out.push({ key: 'f' + f.id, kind: 'Foot Print', title: f.title, detail: f.status === 'published' ? '게시됨' : '초안', view: 'home', eventId: f.eventId })
  for (const w of d.wishlist ?? []) if (has(w.title, w.note)) out.push({ key: 'w' + w.id, kind: '위시리스트', title: w.title, detail: w.done ? '이룸' : '아직', view: 'wishlist', eventId: null })
  for (const a of d.anniversaries ?? []) if (has(a.title)) out.push({ key: 'n' + a.id, kind: '기념일', title: a.title, detail: a.date, view: 'anniversaries', eventId: null })
  return out.slice(0, 60)
})
</script>

<template>
  <section class="ws-section" aria-label="찾기">
    <div class="ws-head"><h2>찾기</h2></div>
    <form class="ws-form" role="search" @submit.prevent>
      <label>이 공간에서 찾기<input v-model="query" type="search" maxlength="60" placeholder="예: 숙소, 회비, 광안리" autofocus /></label>
    </form>
    <p v-if="!query.trim()" class="ws-hint">활동, 기록, 할 일, 일정, 장소, 투표, 지출, 준비물, Foot Print, 위시리스트, 기념일을 함께 찾습니다. 공지와 댓글은 아직 검색하지 않습니다.</p>
    <p v-else-if="!hits.length" class="ws-empty">"{{ query.trim() }}"과(와) 맞는 내용이 없습니다.</p>
    <div v-else class="ws-list">
      <button v-for="h in hits" :key="h.key" type="button" class="ws-row hit" @click="emit('open', h.view, h.eventId, h.record)">
        <span class="ws-badge">{{ h.kind }}</span>
        <span class="ws-grow"><strong>{{ h.title }}</strong><small>{{ h.detail }}</small></span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.hit { width: 100%; border: 0; border-bottom: 1px solid #e5d6cc; background: none; text-align: left; font: inherit; color: inherit; cursor: pointer; }
.hit:hover { background: #fbf4ef; }
</style>
