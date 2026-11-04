<script setup lang="ts">
import { computed, reactive } from 'vue'
import type { ContentState, Member } from './types'

/** Relationship tools: anniversaries and wishlist (couples), dues and attendance (clubs). */
const props = defineProps<{
  tool: 'anniversaries' | 'wishlist' | 'dues' | 'attendance'
  data: ContentState
  members: Member[]
  busy: boolean
  readOnly: boolean
  commit: (mutate: (next: ContentState) => void) => Promise<boolean>
}>()
const form = reactive({ title: '', date: '', yearly: true, note: '', amount: 10000 })
const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date())
const dayNumber = (iso: string) => Date.parse(iso + 'T00:00:00Z') / 86_400_000
const won = (n: number) => n.toLocaleString('ko-KR') + '원'
const name = (id: string) => props.members.find((m) => m.id === id)?.name ?? '멤버'
const real = computed(() => props.members.filter((m) => !m.example))

/** D-day text: yearly dates count to the next occurrence; others count to or from the date. */
function dday(a: { date: string; yearly: boolean }) {
  const t = dayNumber(today())
  let target = dayNumber(a.date)
  if (a.yearly) {
    const [, m, d] = a.date.split('-')
    const year = Number(today().slice(0, 4))
    target = dayNumber(`${year}-${m}-${d}`)
    if (target < t) target = dayNumber(`${year + 1}-${m}-${d}`)
  }
  const diff = Math.round(target - t)
  return diff === 0 ? '오늘' : diff > 0 ? `D-${diff}` : `D+${-diff + 1}`
}
const anniversaries = computed(() =>
  [...(props.data.anniversaries ?? [])].sort((a, b) => dday(a).localeCompare(dday(b), 'ko', { numeric: true })),
)
const attendanceRate = (id: string) => {
  const sessions = props.data.attendance ?? []
  if (!sessions.length) return '-'
  return Math.round((sessions.filter((s) => s.present.includes(id)).length / sessions.length) * 100) + '%'
}
function reset() {
  Object.assign(form, { title: '', date: '', yearly: true, note: '', amount: 10000 })
}
async function add() {
  const title = form.title.trim()
  if (!title) return
  const id = crypto.randomUUID()
  const ok = await props.commit((next) => {
    if (props.tool === 'anniversaries') {
      if (!form.date) throw new Error('날짜를 골라 주세요.')
      ;(next.anniversaries ??= []).push({ id, title, date: form.date, yearly: form.yearly })
    } else if (props.tool === 'wishlist') {
      ;(next.wishlist ??= []).push({ id, title, note: form.note.trim(), done: false, addedBy: 'self' })
    } else if (props.tool === 'dues') {
      if (!(form.amount > 0)) throw new Error('금액은 0보다 커야 합니다.')
      ;(next.dues ??= []).unshift({ id, title, amount: Math.round(form.amount), due: form.date, paid: [] })
    } else {
      ;(next.attendance ??= []).unshift({ id, title, date: form.date || today(), present: [] })
    }
  })
  if (ok) reset()
}
function toggle(list: 'dues' | 'attendance', id: string, member: string) {
  return props.commit((next) => {
    const item = (next[list] ?? []).find((x) => x.id === id) as { paid?: string[]; present?: string[] } | undefined
    if (!item) return
    const key = list === 'dues' ? 'paid' : 'present'
    const set = new Set(item[key] ?? [])
    if (set.has(member)) set.delete(member)
    else set.add(member)
    item[key] = [...set]
  })
}
const toggleWish = (id: string) =>
  props.commit((next) => {
    const w = next.wishlist?.find((x) => x.id === id)
    if (w) w.done = !w.done
  })
const remove = (list: 'anniversaries' | 'wishlist' | 'dues' | 'attendance', id: string) =>
  props.commit((next) => {
    const items = (next[list] ?? []) as { id: string }[]
    ;(next as Record<string, unknown>)[list] = items.filter((x) => x.id !== id)
  })
const titles = {
  anniversaries: ['기념일', '처음 만난 날, 생일처럼 함께 기억할 날을 두면 D-day를 셉니다.', '예: 처음 만난 날'],
  wishlist: ['위시리스트', '같이 하고 싶은 일, 가고 싶은 곳을 모아 두고 이룬 것은 체크합니다.', '예: 제주 오름 일출 보기'],
  dues: ['회비', '기간마다 금액을 정하고 낸 사람을 체크합니다. 실제 송금은 각자 합니다.', '예: 11월 회비'],
  attendance: ['출석', '모임마다 온 사람을 체크하면 멤버별 출석률을 볼 수 있습니다.', '예: 11월 2주차 정기 모임'],
} as const
</script>

<template>
  <section class="ws-section" :aria-label="titles[tool][0]">
    <div class="ws-head"><h2>{{ titles[tool][0] }}</h2></div>
    <p class="ws-hint">{{ titles[tool][1] }}</p>
    <form v-if="!readOnly" class="ws-form ws-card" @submit.prevent="add">
      <label>{{ tool === 'dues' ? '기간 이름' : tool === 'attendance' ? '모임 이름' : '이름' }}<input v-model="form.title" maxlength="80" :placeholder="titles[tool][2]" :disabled="busy" /></label>
      <label v-if="tool !== 'wishlist'">{{ tool === 'dues' ? '마감일 (선택)' : '날짜' }}<input v-model="form.date" type="date" :disabled="busy" /></label>
      <label v-if="tool === 'anniversaries'" class="ws-check"><input v-model="form.yearly" type="checkbox" :disabled="busy" /> 매년 돌아오는 날</label>
      <label v-if="tool === 'wishlist'">메모 (선택)<input v-model="form.note" maxlength="200" :disabled="busy" /></label>
      <label v-if="tool === 'dues'">1인 금액 (원)<input v-model.number="form.amount" type="number" min="1" step="1" :disabled="busy" /></label>
      <div class="ws-actions"><button class="ws-btn primary" type="submit" :disabled="busy || !form.title.trim()">추가</button></div>
    </form>

    <div v-if="tool === 'anniversaries'" class="ws-list">
      <p v-if="!anniversaries.length" class="ws-empty">아직 기념일이 없습니다.</p>
      <div v-for="a in anniversaries" :key="a.id" class="ws-row">
        <span class="dday">{{ dday(a) }}</span>
        <div class="ws-grow"><strong>{{ a.title }}</strong><small>{{ a.date }}{{ a.yearly ? ' · 매년' : '' }}</small></div>
        <button v-if="!readOnly" class="ws-link" type="button" :disabled="busy" @click="remove('anniversaries', a.id)">지우기</button>
      </div>
    </div>

    <div v-else-if="tool === 'wishlist'" class="ws-list">
      <p v-if="!data.wishlist?.length" class="ws-empty">아직 담은 것이 없습니다.</p>
      <div v-for="w in data.wishlist" :key="w.id" class="ws-row">
        <label class="ws-check"><input type="checkbox" :checked="w.done" :disabled="busy || readOnly" @change="toggleWish(w.id)" /></label>
        <div class="ws-grow"><strong :class="{ done: w.done }">{{ w.title }}</strong><small>{{ name(w.addedBy) }}{{ w.note ? ' · ' + w.note : '' }}</small></div>
        <button v-if="!readOnly" class="ws-link" type="button" :disabled="busy" @click="remove('wishlist', w.id)">지우기</button>
      </div>
    </div>

    <template v-else-if="tool === 'dues'">
      <p v-if="!data.dues?.length" class="ws-empty">아직 회비 기간이 없습니다.</p>
      <article v-for="d in data.dues" :key="d.id" class="ws-card">
        <div class="ws-head">
          <strong>{{ d.title }}</strong>
          <span class="ws-meta">1인 {{ won(d.amount) }}{{ d.due ? ' · ' + d.due + '까지' : '' }} · {{ d.paid.length }}/{{ real.length }}명 · 모인 돈 {{ won(d.amount * d.paid.length) }}</span>
          <button v-if="!readOnly" class="ws-link" type="button" :disabled="busy" @click="remove('dues', d.id)">지우기</button>
        </div>
        <div class="chips">
          <button v-for="m in real" :key="m.id" type="button" class="ws-badge" :class="{ strong: d.paid.includes(m.id) }" :aria-pressed="d.paid.includes(m.id)" :disabled="busy || readOnly" @click="toggle('dues', d.id, m.id)">
            {{ d.paid.includes(m.id) ? '✓ ' : '' }}{{ m.name }}
          </button>
        </div>
        <p v-if="real.length > d.paid.length" class="ws-meta">아직: {{ real.filter((m) => !d.paid.includes(m.id)).map((m) => m.name).join(', ') }}</p>
      </article>
    </template>

    <template v-else>
      <div v-if="data.attendance?.length" class="ws-card">
        <strong>멤버별 출석률</strong>
        <div class="chips"><span v-for="m in real" :key="m.id" class="ws-badge">{{ m.name }} {{ attendanceRate(m.id) }}</span></div>
      </div>
      <p v-else class="ws-empty">아직 출석을 기록한 모임이 없습니다.</p>
      <article v-for="s in data.attendance" :key="s.id" class="ws-card">
        <div class="ws-head">
          <strong>{{ s.title }}</strong><span class="ws-meta">{{ s.date }} · {{ s.present.length }}명 출석</span>
          <button v-if="!readOnly" class="ws-link" type="button" :disabled="busy" @click="remove('attendance', s.id)">지우기</button>
        </div>
        <div class="chips">
          <button v-for="m in real" :key="m.id" type="button" class="ws-badge" :class="{ strong: s.present.includes(m.id) }" :aria-pressed="s.present.includes(m.id)" :disabled="busy || readOnly" @click="toggle('attendance', s.id, m.id)">
            {{ s.present.includes(m.id) ? '✓ ' : '' }}{{ m.name }}
          </button>
        </div>
      </article>
    </template>
  </section>
</template>

<style scoped>
.dday { min-width: 64px; font-weight: 700; color: #71576c; font-variant-numeric: tabular-nums; }
.done { text-decoration: line-through; color: var(--text-muted); }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chips button { cursor: pointer; background: #fff; }
</style>
