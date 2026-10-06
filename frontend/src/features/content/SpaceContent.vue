<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import type { ParticipatingSpace } from '@/stores/spaces'
import {
  MODULES,
  type Activity,
  type Appointment,
  type ContentState,
  type ModuleId,
  type ScopeId,
} from './types'
import {
  addDays,
  coordinates,
  assertEditable,
  dateRange,
  pollClosed,
  settlement,
  splitAmount,
  today,
  validDate,
  validateParticipants,
  validatePeriod,
  validatePoll,
} from './logic'
import { useContent } from './useContent'
import RoomBoard from '@/features/room/RoomBoard.vue'
const roomBoard = ref<{ canLeave: () => boolean } | null>(null)

const props = defineProps<{
  space: ParticipatingSpace
  account: { loginId: string; name: string }
  blocked: boolean
}>()
const route = useRoute(),
  router = useRouter()
const { data, loading, saving, error, notice, load, commit } = useContent(
  props.account.loginId,
  props.space.spaceId,
  props.account.name,
)
const eventId = computed(() => (typeof route.query.event === 'string' ? route.query.event : null))
const activity = computed(() => data.value?.activities.find((a) => a.id === eventId.value))
const missingActivity = computed(() => !!eventId.value && !!data.value && !activity.value)
const readOnly = computed(() => !!activity.value?.archived)
const busy = computed(() => saving.value || props.blocked)
const installed = computed(() => activity.value?.modules ?? data.value?.modules ?? [])
const tabs = computed(() => [
  { id: 'home', label: activity.value ? '활동 홈' : '공간 홈' },
  ...(!activity.value ? [{ id: 'activities', label: '활동' }] : []),
  ...MODULES.filter((m) => installed.value.includes(m.id)).map((m) => ({
    id: m.id,
    label: m.label,
  })),
  { id: 'people', label: activity.value ? '참여자' : '사람' },
  ...(!activity.value ? [{ id: 'invite', label: '사용자 초대' }] : []),
  { id: 'modules', label: '기능 추가' },
])
const tab = computed(() =>
  tabs.value.some((t) => t.id === route.query.view) ? String(route.query.view) : 'home',
)
const members = computed(
  () =>
    data.value?.members.filter(
      (m) => !activity.value || activity.value.participants.includes(m.id),
    ) ?? [],
)
const catalog = computed(() => MODULES.filter((m) => activity.value || !m.activityOnly))
const recordSearch = ref(''),
  recordScope = ref('current'),
  taskFilter = ref('unfinished'),
  activityFilter = ref('ongoing'),
  placeSearch = ref('')
const month = ref(today().slice(0, 7)),
  selectedDay = ref(today()),
  voting = reactive<Record<string, string>>({})
const currentRecords = computed(() =>
  (data.value?.records ?? [])
    .filter(
      (r) =>
        (r.eventId === eventId.value || (!eventId.value && recordScope.value === 'all')) &&
        (r.title + ' ' + r.body).toLowerCase().includes(recordSearch.value.toLowerCase()),
    )
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
)
const tasks = computed(() =>
  (data.value?.tasks ?? []).filter(
    (t) =>
      t.eventId === eventId.value &&
      (taskFilter.value === 'all' || (taskFilter.value === 'finished' ? t.done : !t.done)),
  ),
)
const activities = computed(() =>
  (data.value?.activities ?? []).filter(
    (a) =>
      activityFilter.value === 'all' ||
      (activityFilter.value === 'archived'
        ? a.archived
        : !a.archived && a.status === activityFilter.value),
  ),
)
const appointments = computed(() =>
  (data.value?.appointments ?? [])
    .filter((a) => a.eventId === eventId.value)
    .sort((a, b) => (a.date + a.time).localeCompare(b.date + b.time)),
)
const places = computed(() =>
  (data.value?.places ?? []).filter(
    (p) =>
      p.eventId === eventId.value &&
      (p.title + ' ' + p.address).toLowerCase().includes(placeSearch.value.toLowerCase()),
  ),
)
const allPlaces = computed(() =>
  (data.value?.places ?? []).filter((p) => p.eventId === eventId.value),
)
const packing = computed(() =>
  (data.value?.packing ?? []).filter((p) => p.eventId === eventId.value),
)
const expenses = computed(() =>
  (data.value?.expenses ?? []).filter((e) => e.eventId === eventId.value),
)
const polls = computed(() => (data.value?.polls ?? []).filter((p) => p.eventId === eventId.value))
const balances = computed(() => {
  try {
    return settlement(
      expenses.value,
      members.value.map((m) => m.id),
    )
  } catch {
    return null
  }
})
const days = computed(() => dateRange(activity.value?.start ?? '', activity.value?.end ?? ''))
const calendarCells = computed(() => {
  const first = month.value + '-01'
  const offset = (new Date(first + 'T00:00:00Z').getUTCDay() + 6) % 7
  return Array.from({ length: 42 }, (_, i) => addDays(first, i - offset))
})
const clock = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | undefined
const nowClosed = (poll: ContentState['polls'][number]) => pollClosed(poll, clock.value)
const recentRecords = computed(() =>
  (data.value?.records ?? [])
    .filter(
      (r) =>
        r.eventId === eventId.value ||
        (!eventId.value && !data.value?.activities.find((a) => a.id === r.eventId)?.archived),
    )
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    .slice(0, 4),
)
const nextActivities = computed(() =>
  (data.value?.activities ?? [])
    .filter((a) => !a.archived && a.status === 'ongoing')
    .sort((a, b) => (a.start || '9999').localeCompare(b.start || '9999'))
    .slice(0, 3),
)
const unfinishedTasks = computed(() =>
  (data.value?.tasks ?? []).filter((t) => t.eventId === eventId.value && !t.done).slice(0, 4),
)
const nextAppointments = computed(() =>
  appointments.value.filter((a) => a.date >= today()).slice(0, 3),
)
const markerPlaces = computed(() => {
  const list = allPlaces.value
    .map((p, index) => ({ ...p, number: index + 1 }))
    .filter((p) => p.lat !== null && p.lon !== null)
  const lats = list.map((p) => p.lat!),
    lons = list.map((p) => p.lon!)
  const minLat = Math.min(...lats),
    maxLat = Math.max(...lats),
    minLon = Math.min(...lons),
    maxLon = Math.max(...lons)
  return list.map((p) => ({
    ...p,
    x: 60 + ((p.lon! - minLon) / (maxLon - minLon || 1)) * 320,
    y: 240 - ((p.lat! - minLat) / (maxLat - minLat || 1)) * 180,
  }))
})
const roleLabels = { OWNER: '소유자', MANAGER: '관리자', MEMBER: '멤버' }
function name(id: string) {
  const person = data.value?.members.find((m) => m.id === id)
  return person ? (person.example ? '예시 · ' : '') + person.name : '미지정'
}
function scopeName(id: ScopeId) {
  return id === null
    ? props.space.title
    : (data.value?.activities.find((a) => a.id === id)?.title ?? '알 수 없는 활동')
}
function dateLabel(value: string) {
  return value ? value.slice(5).replace('-', '/') : '미정'
}
function money(value: number) {
  return value.toLocaleString('ko-KR') + '원'
}
function updated(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
function period(a: Activity) {
  return !a.start && !a.end ? '날짜 미정' : `${a.start || '미정'} ~ ${a.end || '미정'}`
}
function newId() {
  return crypto.randomUUID()
}
async function go(view = 'home', event: ScopeId = eventId.value, record?: string) {
  if (busy.value) return
  await router.push({
    name: 'home',
    query: {
      space: String(props.space.spaceId),
      ...(event ? { event } : {}),
      ...(view === 'home' ? {} : { view }),
      ...(record ? { record } : {}),
    },
  })
}
function moveMonth(amount: number) {
  const date = new Date(month.value + '-01T00:00:00Z')
  date.setUTCMonth(date.getUTCMonth() + amount)
  month.value = date.toISOString().slice(0, 7)
}
function dayAppointments(date: string) {
  return appointments.value.filter((a) => a.date === date)
}
function dayActivities(date: string) {
  return (data.value?.activities ?? []).filter(
    (a) =>
      !a.archived && (a.start || a.end) && (a.start || a.end) <= date && (a.end || a.start) >= date,
  )
}
watch(eventId, () => {
  selectedDay.value = activity.value?.start || activity.value?.end || today()
  recordSearch.value = ''
  recordScope.value = 'current'
  placeSearch.value = ''
  taskFilter.value = 'unfinished'
  notice.value = ''
  error.value = ''
  for (const id of Object.keys(voting)) delete voting[id]
})
watch(
  () => activity.value?.start,
  (start) => {
    if (start) selectedDay.value = start
  },
  { immediate: true },
)

const recordId = computed(() =>
  tab.value === 'records' && typeof route.query.record === 'string' ? route.query.record : null,
)
const selectedRecord = computed(() => data.value?.records.find((r) => r.id === recordId.value))
const recordDraft = reactive({ title: '', body: '', eventId: null as ScopeId })
const recordBaseline = ref('')
const recordSignature = () => JSON.stringify(recordDraft)
const recordDirty = computed(
  () => !!recordId.value && !!data.value && recordSignature() !== recordBaseline.value,
)
const recordReadOnly = computed(
  () => !!data.value?.activities.find((a) => a.id === recordDraft.eventId)?.archived,
)
watch(
  () => [recordId.value, !!data.value],
  () => {
    if (!data.value) return
    const item = selectedRecord.value
    Object.assign(recordDraft, {
      title: item?.title ?? '',
      body: item?.body ?? '',
      eventId: item?.eventId ?? eventId.value,
    })
    recordBaseline.value = recordSignature()
  },
  { immediate: true },
)
function canLeave() {
  if (saving.value || props.blocked) return false
  if (roomBoard.value && !roomBoard.value.canLeave()) return false
  return (
    !recordDirty.value || window.confirm('저장하지 않은 기록이 있습니다. 저장하지 않고 나갈까요?')
  )
}
onBeforeRouteUpdate((to, from) => to.fullPath === from.fullPath || canLeave())
onBeforeRouteLeave(canLeave)
defineExpose({ canLeave })
function beforeUnload(event: BeforeUnloadEvent) {
  if (recordDirty.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  clockTimer = setInterval(() => {
    clock.value = Date.now()
  }, 1000)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
  if (clockTimer) clearInterval(clockTimer)
})
async function saveRecord() {
  if (!recordDraft.title.trim()) {
    error.value = '기록 제목을 입력해 주세요.'
    return
  }
  const id = selectedRecord.value?.id ?? newId(),
    payload = {
      ...recordDraft,
      title: recordDraft.title.trim(),
      id,
      updatedAt: new Date().toISOString(),
    }
  if (
    await commit((next) => {
      assertEditable(next.activities, payload.eventId)
      const index = next.records.findIndex((r) => r.id === id)
      if (index < 0) next.records.push(payload)
      else next.records[index] = payload
    })
  ) {
    recordBaseline.value = recordSignature()
    await go('records', eventId.value, id)
  }
}

type FormKind =
  | 'activity'
  | 'task'
  | 'appointment'
  | 'place'
  | 'packing'
  | 'expense'
  | 'poll'
  | 'participants'
  | 'ai'
  | 'collaboration'
const formKind = ref<FormKind | null>(null),
  editingId = ref(''),
  dialog = ref<HTMLDialogElement | null>(null),
  step = ref(1)
const form = reactive({
  title: '',
  body: '',
  date: '',
  time: '',
  allDay: false,
  assignee: '',
  amount: '',
  payer: 'self',
  beneficiaries: [] as string[],
  options: '',
  deadline: '',
  start: '',
  end: '',
  type: 'travel' as Activity['type'],
  modules: [] as ModuleId[],
  participants: [] as string[],
  address: '',
  lat: '',
  lon: '',
  placeId: '',
})
const formTitles: Record<FormKind, string> = {
  activity: '활동 만들기',
  task: '할 일',
  appointment: '일정',
  place: '장소',
  packing: '준비물',
  expense: '지출',
  poll: '투표 만들기',
  participants: '활동 참여자',
  ai: 'AI 기능 추천 · 향후 예시',
  collaboration: '공동 편집 · 향후 예시',
}
const formTitle = computed(() => (formKind.value ? formTitles[formKind.value] : ''))
watch(formKind, async (value) => {
  if (value) {
    await nextTick()
    dialog.value?.showModal()
  }
})
function templateModules(type: Activity['type']): ModuleId[] {
  return type === 'travel'
    ? ['records', 'itinerary', 'places', 'packing', 'expenses', 'polls']
    : type === 'custom'
      ? ['records']
      : ['records', 'tasks', 'calendar', 'polls']
}
watch(
  () => form.type,
  (type) => {
    if (formKind.value === 'activity') form.modules = templateModules(type)
  },
)
function openForm(kind: FormKind, id = '', overrides: Partial<typeof form> = {}) {
  if (busy.value) return
  if (readOnly.value && !['ai', 'collaboration'].includes(kind)) {
    error.value = '보관한 활동은 읽기 전용입니다.'
    return
  }
  error.value = ''
  editingId.value = id
  step.value = 1
  Object.assign(form, {
    title: '',
    body: '',
    date: today(),
    time: '09:00',
    allDay: false,
    assignee: '',
    amount: '',
    payer: members.value[0]?.id ?? 'self',
    beneficiaries: members.value.map((m) => m.id),
    options: '',
    deadline: '',
    start: '',
    end: '',
    type: 'travel',
    modules: templateModules('travel'),
    participants: data.value?.members.map((m) => m.id) ?? [],
    address: '',
    lat: '',
    lon: '',
    placeId: '',
  })
  if (id && data.value) {
    if (kind === 'task')
      Object.assign(
        form,
        data.value.tasks.find((t) => t.id === id),
        { date: data.value.tasks.find((t) => t.id === id)?.due ?? '' },
      )
    if (kind === 'appointment') {
      const a = data.value.appointments.find((a) => a.id === id)
      if (a) Object.assign(form, a, { body: a.memo })
    }
    if (kind === 'place') {
      const p = data.value.places.find((p) => p.id === id)
      if (p)
        Object.assign(form, p, {
          lat: p.lat === null ? '' : String(p.lat),
          lon: p.lon === null ? '' : String(p.lon),
        })
    }
    if (kind === 'packing')
      Object.assign(
        form,
        data.value.packing.find((p) => p.id === id),
      )
    if (kind === 'expense') {
      const e = data.value.expenses.find((e) => e.id === id)
      if (e)
        Object.assign(form, e, { amount: String(e.amount), beneficiaries: [...e.beneficiaries] })
    }
  }
  if (kind === 'task' && !id) form.date = ''
  if (kind === 'participants') form.participants = [...(activity.value?.participants ?? [])]
  Object.assign(form, overrides)
  formKind.value = kind
}
function closeForm() {
  if (!saving.value) {
    formKind.value = null
    error.value = ''
  }
}
function backdropClick(event: MouseEvent) {
  const bounds = dialog.value?.getBoundingClientRect()
  if (
    event.target === dialog.value &&
    bounds &&
    (event.clientX < bounds.left ||
      event.clientX > bounds.right ||
      event.clientY < bounds.top ||
      event.clientY > bounds.bottom)
  )
    closeForm()
}
function previousStep() {
  step.value = 1
  error.value = ''
}
function nextActivityStep() {
  try {
    if (!form.title.trim()) throw new Error('활동 이름을 입력해 주세요.')
    validatePeriod(form.start, form.end)
    error.value = ''
    step.value = 2
  } catch (e) {
    error.value = (e as Error).message
  }
}
function upsert<T extends { id: string }>(list: T[], value: T) {
  const index = list.findIndex((v) => v.id === value.id)
  if (index < 0) list.push(value)
  else list[index] = value
}
async function submitForm() {
  const kind = formKind.value,
    id = editingId.value || newId(),
    scope = eventId.value
  if (!kind || ['ai', 'collaboration'].includes(kind)) return
  if (kind === 'activity' && step.value === 1) {
    nextActivityStep()
    return
  }
  const ok = await commit((next) => {
    assertEditable(next.activities, scope)
    const title = form.title.trim()
    if (kind !== 'participants' && !title) throw new Error('이름이나 제목을 입력해 주세요.')
    const memberIds = next.members
      .filter(
        (m) => !scope || next.activities.find((a) => a.id === scope)?.participants.includes(m.id),
      )
      .map((m) => m.id)
    if (kind === 'activity') {
      validatePeriod(form.start, form.end)
      if (!form.modules.length) throw new Error('사용할 기능을 한 개 이상 선택해 주세요.')
      next.activities.push({
        id,
        title,
        type: form.type,
        start: form.start,
        end: form.end,
        status: 'ongoing',
        archived: false,
        modules: [...form.modules],
        participants: next.members.map((m) => m.id),
      })
    }
    if (kind === 'task') {
      if (form.date && !validDate(form.date)) throw new Error('유효한 마감일을 입력해 주세요.')
      if (form.assignee && !memberIds.includes(form.assignee))
        throw new Error('현재 참여자 중 담당자를 선택해 주세요.')
      upsert(next.tasks, {
        id,
        eventId: scope,
        title,
        assignee: form.assignee,
        due: form.date,
        done: next.tasks.find((t) => t.id === id)?.done ?? false,
      })
    }
    if (kind === 'appointment') {
      if (!validDate(form.date)) throw new Error('유효한 날짜를 입력해 주세요.')
      if (!form.allDay && !/^([01]\d|2[0-3]):[0-5]\d$/.test(form.time))
        throw new Error('유효한 시간을 입력해 주세요.')
      const target = next.activities.find((a) => a.id === scope)
      if (
        target &&
        ((target.start && form.date < target.start) || (target.end && form.date > target.end))
      )
        throw new Error('활동 기간 안의 날짜를 입력해 주세요.')
      if (form.placeId && !next.places.some((p) => p.id === form.placeId && p.eventId === scope))
        throw new Error('현재 위치에 저장한 장소를 선택해 주세요.')
      upsert(next.appointments, {
        id,
        eventId: scope,
        title,
        date: form.date,
        time: form.allDay ? '' : form.time,
        allDay: form.allDay,
        memo: form.body,
        placeId: form.placeId,
      })
    }
    if (kind === 'place') {
      const { lat, lon } = coordinates(form.lat, form.lon)
      upsert(next.places, { id, eventId: scope, title, address: form.address.trim(), lat, lon })
    }
    if (kind === 'packing') {
      if (form.assignee && !memberIds.includes(form.assignee))
        throw new Error('현재 참여자 중 담당자를 선택해 주세요.')
      upsert(next.packing, {
        id,
        eventId: scope,
        title,
        assignee: form.assignee,
        done: next.packing.find((p) => p.id === id)?.done ?? false,
      })
    }
    if (kind === 'expense') {
      const amount = Number(form.amount)
      splitAmount(amount, form.beneficiaries)
      if (
        !memberIds.includes(form.payer) ||
        form.beneficiaries.some((id) => !memberIds.includes(id))
      )
        throw new Error('현재 참여자 중 결제자·분담자를 선택해 주세요.')
      if (!validDate(form.date)) throw new Error('유효한 지출 날짜를 입력해 주세요.')
      upsert(next.expenses, {
        id,
        eventId: scope,
        title,
        amount,
        payer: form.payer,
        beneficiaries: [...form.beneficiaries],
        date: form.date,
      })
      settlement(
        next.expenses.filter((e) => e.eventId === scope),
        memberIds,
      )
    }
    if (kind === 'poll') {
      const options = form.options
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean)
      validatePoll(options, form.deadline)
      next.polls.push({
        id,
        eventId: scope,
        title,
        options: options.map((title) => ({ id: newId(), title })),
        deadline: form.deadline,
        closed: false,
        votes: {},
      })
    }
    if (kind === 'participants') {
      const a = next.activities.find((a) => a.id === scope)
      if (!a) throw new Error('활동을 찾을 수 없습니다.')
      validateParticipants(a, form.participants, next.expenses)
      a.participants = [...form.participants]
      for (const item of [...next.tasks, ...next.packing])
        if (item.eventId === scope && item.assignee && !a.participants.includes(item.assignee))
          item.assignee = ''
    }
  })
  if (ok) {
    formKind.value = null
    if (kind === 'activity') await go('home', id)
  }
}
async function toggleDone(kind: 'tasks' | 'packing', id: string) {
  await commit((next) => {
    const item = next[kind].find((t) => t.id === id)
    if (!item) return
    assertEditable(next.activities, item.eventId)
    item.done = !item.done
  })
}
async function install(id: ModuleId) {
  await commit((next) => {
    assertEditable(next.activities, eventId.value)
    const list = eventId.value
      ? next.activities.find((a) => a.id === eventId.value)!.modules
      : next.modules
    if (!list.includes(id)) list.push(id)
  })
}
async function setActivityStatus(action: 'complete' | 'archive' | 'restore') {
  if (
    action === 'archive' &&
    !window.confirm('활동을 보관할까요? 기록은 유지되며 보관 후에는 읽기 전용으로 열립니다.')
  )
    return
  await commit((next) => {
    const a = next.activities.find((a) => a.id === eventId.value)
    if (!a) return
    if (action === 'restore') a.archived = false
    else if (action === 'archive') {
      if (a.status !== 'completed') throw new Error('완료로 표시한 뒤 보관해 주세요.')
      a.archived = true
    } else {
      assertEditable(next.activities, a.id)
      a.status = 'completed'
    }
  })
}
async function vote(id: string) {
  await commit((next) => {
    const poll = next.polls.find((p) => p.id === id)
    if (!poll) return
    assertEditable(next.activities, poll.eventId)
    if (pollClosed(poll)) throw new Error('마감된 투표는 변경할 수 없습니다.')
    if (!members.value.some((m) => m.id === 'self'))
      throw new Error('활동 참여자에 본인을 포함해 주세요.')
    const choice = voting[id] ?? poll.votes.self
    if (!poll.options.some((o) => o.id === choice)) throw new Error('선택지를 골라 주세요.')
    poll.votes.self = choice!
  })
}
async function closePoll(id: string) {
  await commit((next) => {
    const poll = next.polls.find((p) => p.id === id)
    if (!poll) return
    assertEditable(next.activities, poll.eventId)
    poll.closed = true
  })
}
function appointmentPlace(a: Appointment) {
  return data.value?.places.find((p) => p.id === a.placeId)?.title ?? '장소 미지정'
}
function putPlaceInSchedule(id: string) {
  const p = data.value?.places.find((p) => p.id === id)
  if (p)
    openForm('appointment', '', {
      title: p.title,
      placeId: id,
      date: activity.value ? activity.value.start || selectedDay.value : today(),
    })
}
</script>

<template>
  <div class="space-content">
    <div v-if="activity" class="activity-return">
      <button type="button" :disabled="busy" @click="go('activities', null)">
        ← {{ space.title }}의 활동 목록
      </button>
    </div>
    <h1>{{ activity?.title ?? space.title }}</h1>
    <p class="context-description">
      {{ activity ? period(activity) : '함께하는 공간 · ' + space.spaceMemberCount + '명' }}
    </p>
    <nav class="content-tabs" aria-label="현재 공간 메뉴">
      <button
        v-for="item in tabs"
        :key="item.id"
        type="button"
        :class="{ active: tab === item.id }"
        :aria-current="tab === item.id ? 'page' : undefined"
        :disabled="busy"
        @click="go(item.id)"
      >
        {{ item.label }}
      </button>
    </nav>
    <div class="demo-banner">
      체험 데이터 · 새 세부 기능은 이 브라우저에만 저장됩니다. 다른 기기나 사용자와 공유되지
      않습니다.
    </div>
    <div v-if="readOnly" class="archive-banner" role="status">
      보관한 활동입니다. 기록은 유지되며 읽기 전용으로 열립니다.
      <button
        class="secondary-button"
        type="button"
        :disabled="busy"
        @click="setActivityStatus('restore')"
      >
        보관 해제
      </button>
    </div>
    <p v-if="error && !formKind" class="content-error" role="alert">
      {{ error }} <button v-if="!data" type="button" @click="load">다시 시도</button>
    </p>
    <p v-if="notice" class="content-notice" role="status">{{ notice }}</p>
    <p v-if="loading" role="status">체험 데이터를 불러오는 중...</p>
    <div v-else-if="missingActivity" class="empty">
      <h2>활동을 찾을 수 없습니다</h2>
      <button class="secondary-button" type="button" @click="go('activities', null)">
        활동 목록으로
      </button>
    </div>
    <template v-else-if="data">
      <section v-if="tab === 'home' && !activity" aria-label="우리 방">
        <RoomBoard
          ref="roomBoard"
          :owner="account.loginId"
          :space-id="space.spaceId"
          :modules="data.modules"
          :activities="data.activities"
          :blocked="busy"
          @open="(view, scope) => go(view, scope)"
          @invite="go('invite')"
          @create-event="openForm('activity')"
          @modules="go('modules')"
        />
      </section>
      <section v-else-if="tab === 'home'" aria-label="홈 요약">
        <div class="section-heading">
          <h2>{{ activity ? '활동 정보' : '공간 정보' }}</h2>
          <div v-if="activity" class="actions">
            <button
              v-if="!readOnly && activity.status === 'ongoing'"
              class="secondary-button"
              type="button"
              :disabled="busy"
              @click="setActivityStatus('complete')"
            >
              완료로 표시</button
            ><button
              v-if="!readOnly && activity.status === 'completed'"
              class="secondary-button"
              type="button"
              :disabled="busy"
              @click="setActivityStatus('archive')"
            >
              보관하기
            </button>
          </div>
        </div>
        <dl class="summary-stats">
          <div>
            <dt>{{ activity ? '체험 참여자' : '실제 공간 참여 인원' }}</dt>
            <dd>{{ activity ? members.length : space.spaceMemberCount }}명</dd>
          </div>
          <div>
            <dt>{{ activity ? '활동 상태' : '내 역할' }}</dt>
            <dd>
              {{
                activity
                  ? activity.archived
                    ? '보관됨'
                    : activity.status === 'completed'
                      ? '완료'
                      : '진행 중'
                  : roleLabels[space.role]
              }}
            </dd>
          </div>
        </dl>
        <div class="summary-grid">
          <section v-if="installed.includes('tasks')">
            <div class="section-heading">
              <h2>해야 할 일</h2>
              <button class="text-link" type="button" @click="go('tasks')">모두 보기</button>
            </div>
            <p v-if="!unfinishedTasks.length" class="muted">미완료 할 일이 없습니다.</p>
            <div v-for="item in unfinishedTasks" :key="item.id" class="list-line">
              <label
                ><input
                  type="checkbox"
                  :checked="item.done"
                  :disabled="readOnly || busy"
                  @change="toggleDone('tasks', item.id)"
                />
                {{ item.title }}</label
              ><small>{{ dateLabel(item.due) }}</small>
            </div>
          </section>
          <section v-if="!activity">
            <div class="section-heading">
              <h2>다가오는 활동</h2>
              <button class="text-link" type="button" @click="go('activities')">모두 보기</button>
            </div>
            <p v-if="!nextActivities.length" class="muted">진행 중인 활동이 없습니다.</p>
            <button
              v-for="item in nextActivities"
              :key="item.id"
              class="list-link"
              type="button"
              @click="go('home', item.id)"
            >
              <strong>{{ item.title }}</strong
              ><small>{{ period(item) }} →</small>
            </button>
          </section>
          <section v-if="installed.includes('calendar') || installed.includes('itinerary')">
            <div class="section-heading">
              <h2>다음 일정</h2>
              <button
                class="text-link"
                type="button"
                @click="go(installed.includes('itinerary') ? 'itinerary' : 'calendar')"
              >
                모두 보기
              </button>
            </div>
            <p v-if="!nextAppointments.length" class="muted">예정된 일정이 없습니다.</p>
            <button
              v-for="item in nextAppointments"
              :key="item.id"
              class="list-link"
              type="button"
              :disabled="readOnly"
              @click="openForm('appointment', item.id)"
            >
              <strong>{{ item.title }}</strong
              ><small>{{ dateLabel(item.date) }} · {{ item.allDay ? '종일' : item.time }}</small>
            </button>
          </section>
          <section v-if="installed.includes('packing')">
            <div class="section-heading">
              <h2>준비물</h2>
              <button class="text-link" type="button" @click="go('packing')">모두 보기</button>
            </div>
            <p class="muted">
              {{ packing.filter((p) => p.done).length }} / {{ packing.length }}개 준비 완료
            </p>
          </section>
        </div>
        <section v-if="installed.includes('records')" class="section">
          <div class="section-heading">
            <h2>최근 기록</h2>
            <button class="text-link" type="button" @click="go('records')">모두 보기</button>
          </div>
          <p v-if="!recentRecords.length" class="empty">아직 기록이 없습니다.</p>
          <button
            v-for="item in recentRecords"
            :key="item.id"
            class="list-link"
            type="button"
            @click="go('records', eventId, item.id)"
          >
            <strong>{{ item.title }}</strong
            ><small>{{ scopeName(item.eventId) }} · {{ updated(item.updatedAt) }} →</small>
          </button>
        </section>
        <section v-if="!activity" class="section">
          <div class="invite-box">
            <div>
              <strong>함께할 사람 초대</strong>
              <p>기존 초대 API로 현재 공간에 사용자를 초대합니다.</p>
            </div>
            <button class="secondary-button" type="button" @click="go('invite')">
              사용자 초대 →
            </button>
          </div>
        </section>
      </section>
      <section v-else-if="tab === 'activities'" aria-label="활동 목록">
        <div class="toolbar">
          <h2>활동</h2>
          <select v-model="activityFilter" aria-label="활동 상태">
            <option value="ongoing">진행 중</option>
            <option value="completed">완료</option>
            <option value="archived">보관한 활동</option>
            <option value="all">모든 활동</option></select
          ><button
            class="primary-button fit"
            type="button"
            :disabled="busy"
            @click="openForm('activity')"
          >
            ＋ 활동 만들기
          </button>
        </div>
        <p v-if="!activities.length" class="empty">해당 상태의 활동이 없습니다.</p>
        <div v-for="item in activities" :key="item.id" class="data-row activities-row">
          <div>
            <h3>{{ item.title }}</h3>
            <small>{{ period(item) }}</small>
          </div>
          <span class="badge">{{
            item.archived ? '보관됨' : item.status === 'completed' ? '완료' : '진행 중'
          }}</span
          ><span>{{ item.participants.length }}명 · 체험 참여자</span
          ><button class="secondary-button" type="button" @click="go('home', item.id)">
            활동 열기
          </button>
        </div>
      </section>
      <section v-else-if="tab === 'records'" aria-label="기록">
        <template v-if="recordId">
          <div class="section-heading">
            <h2>{{ recordId === 'new' ? '새 기록' : '기록 작성·편집' }}</h2>
            <span v-if="recordReadOnly" class="badge">보관한 활동 · 읽기 전용</span>
          </div>
          <div v-if="recordId !== 'new' && !selectedRecord" class="empty">
            <p>기록을 찾을 수 없습니다.</p>
            <button class="secondary-button" type="button" @click="go('records')">목록으로</button>
          </div>
          <form v-else class="record-editor" @submit.prevent="saveRecord">
            <div class="field">
              <label for="record-scope">소속</label
              ><select
                v-if="recordId === 'new' && !activity"
                id="record-scope"
                v-model="recordDraft.eventId"
                :disabled="busy"
              >
                <option :value="null">{{ space.title }} · 공간</option>
                <option
                  v-for="item in data.activities.filter(
                    (a) => !a.archived && a.modules.includes('records'),
                  )"
                  :key="item.id"
                  :value="item.id"
                >
                  {{ item.title }} · 활동
                </option>
              </select>
              <div v-else id="record-scope" class="fixed-field">
                {{ scopeName(recordDraft.eventId) }}
              </div>
            </div>
            <div class="field">
              <label for="record-title">제목</label
              ><input
                id="record-title"
                v-model="recordDraft.title"
                maxlength="120"
                required
                :disabled="recordReadOnly || busy"
                placeholder="기록 제목"
              />
            </div>
            <div class="field">
              <label for="record-body">내용</label
              ><textarea
                id="record-body"
                v-model="recordDraft.body"
                maxlength="200000"
                :readonly="recordReadOnly"
                :disabled="busy"
                placeholder="내용을 작성하세요"
              ></textarea>
            </div>
            <div class="actions">
              <button
                class="secondary-button"
                type="button"
                :disabled="busy"
                @click="go('records')"
              >
                목록으로</button
              ><button
                v-if="!recordReadOnly"
                class="primary-button fit"
                type="submit"
                :disabled="busy"
              >
                {{ saving ? '저장 중...' : '저장' }}</button
              ><small>{{
                recordDirty
                  ? '저장하지 않은 변경 있음'
                  : selectedRecord
                    ? '이 브라우저에 저장됨'
                    : '일반 텍스트 기록'
              }}</small>
            </div>
          </form>
        </template>
        <template v-else>
          <div class="toolbar">
            <h2>기록</h2>
            <input
              v-model="recordSearch"
              aria-label="기록 검색"
              placeholder="제목과 내용에서 검색"
            /><select v-if="!activity" v-model="recordScope" aria-label="기록 범위">
              <option value="current">현재 공간</option>
              <option value="all">활동 포함 모든 기록</option></select
            ><button
              class="primary-button fit"
              type="button"
              :disabled="readOnly || busy"
              @click="go('records', eventId, 'new')"
            >
              ＋ 새 기록
            </button>
          </div>
          <p v-if="!currentRecords.length" class="empty">
            {{
              recordSearch
                ? '검색 결과가 없습니다.'
                : '아직 기록이 없습니다. 첫 기록을 작성해 보세요.'
            }}
          </p>
          <div v-for="item in currentRecords" :key="item.id" class="data-row records-row">
            <div>
              <h3>{{ item.title }}</h3>
              <small>{{ item.body.slice(0, 70) }}</small>
            </div>
            <span>{{ scopeName(item.eventId) }}</span
            ><small>{{ updated(item.updatedAt) }}</small
            ><button
              class="secondary-button"
              type="button"
              @click="go('records', eventId, item.id)"
            >
              열기
            </button>
          </div>
        </template>
      </section>
      <section v-else-if="tab === 'tasks'" aria-label="할 일">
        <div class="toolbar">
          <h2>할 일</h2>
          <select v-model="taskFilter" aria-label="보여줄 할 일">
            <option value="unfinished">미완료</option>
            <option value="finished">완료</option>
            <option value="all">모든 항목</option></select
          ><button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('task')"
          >
            ＋ 할 일 추가
          </button>
        </div>
        <p v-if="!tasks.length" class="empty">해당하는 할 일이 없습니다.</p>
        <div v-for="item in tasks" :key="item.id" class="data-row tasks-row">
          <label class="check-label"
            ><input
              type="checkbox"
              :checked="item.done"
              :disabled="readOnly || busy"
              @change="toggleDone('tasks', item.id)"
            /><span :class="{ done: item.done }">{{ item.title }}</span></label
          ><span>{{ name(item.assignee) }}</span
          ><span>{{ dateLabel(item.due) }}</span
          ><button
            class="secondary-button"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('task', item.id)"
          >
            수정
          </button>
        </div>
        <p class="hint">
          완료 체크한 항목은 미완료 필터에서 사라집니다. 완료 또는 모든 항목에서 확인할 수 있습니다.
        </p>
      </section>
      <section v-else-if="tab === 'calendar'" aria-label="일정 달력">
        <div class="toolbar">
          <h2>일정</h2>
          <button
            class="secondary-button"
            type="button"
            aria-label="이전 달"
            @click="moveMonth(-1)"
          >
            ‹</button
          ><strong>{{ month.replace('-', '년 ') }}월</strong
          ><button
            class="secondary-button"
            type="button"
            aria-label="다음 달"
            @click="moveMonth(1)"
          >
            ›</button
          ><button class="secondary-button" type="button" @click="month = today().slice(0, 7)">
            오늘</button
          ><button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('appointment')"
          >
            ＋ 일정 추가
          </button>
        </div>
        <p class="hint">한국 시간 기준 · 월요일 시작. 날짜를 눌러 일정을 추가할 수 있습니다.</p>
        <div class="calendar">
          <div v-for="day in ['월', '화', '수', '목', '금', '토', '일']" :key="day" class="weekday">
            {{ day }}
          </div>
          <div
            v-for="date in calendarCells"
            :key="date"
            class="calendar-cell"
            :class="{ outside: !date.startsWith(month), today: date === today() }"
          >
            <button
              class="day-number"
              type="button"
              :aria-label="date + ' 일정 추가'"
              :disabled="readOnly || busy"
              @click="openForm('appointment', '', { date })"
            >
              {{ Number(date.slice(8)) }}</button
            ><button
              v-for="item in dayAppointments(date)"
              :key="item.id"
              class="calendar-item"
              type="button"
              :disabled="readOnly || busy"
              @click="openForm('appointment', item.id)"
            >
              {{ item.allDay ? '종일' : item.time }} {{ item.title }}</button
            ><template v-if="!activity"
              ><button
                v-for="item in dayActivities(date)"
                :key="item.id"
                class="calendar-activity"
                type="button"
                @click="go('home', item.id)"
              >
                {{ item.title }} ↗
              </button></template
            >
          </div>
        </div>
      </section>
      <section v-else-if="tab === 'itinerary'" aria-label="여행 일정">
        <div class="toolbar">
          <h2>날짜별 여행 일정</h2>
          <button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy || !days.length"
            @click="openForm('appointment', '', { date: selectedDay })"
          >
            ＋ 일정 추가
          </button>
        </div>
        <div v-if="days.length" class="day-tabs">
          <button
            v-for="(date, index) in days"
            :key="date"
            class="secondary-button"
            :class="{ selected: date === selectedDay }"
            type="button"
            @click="selectedDay = date"
          >
            {{ index + 1 }}일차 · {{ dateLabel(date) }}
          </button>
        </div>
        <p v-else class="empty">
          날짜 미정 활동입니다. 여행 일정에는 날짜가 있는 활동이 필요합니다. 일정 기능을 추가하면
          개별 날짜의 약속을 저장할 수 있습니다.
        </p>
        <p v-if="days.length && !dayAppointments(selectedDay).length" class="empty">
          이 날짜의 일정이 없습니다.
        </p>
        <div
          v-for="item in dayAppointments(selectedDay)"
          :key="item.id"
          class="data-row itinerary-row"
        >
          <strong>{{ item.allDay ? '종일' : item.time }}</strong>
          <div>
            <h3>{{ item.title }}</h3>
            <small>{{ item.memo }}</small>
          </div>
          <span>{{ appointmentPlace(item) }}</span
          ><button
            class="secondary-button"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('appointment', item.id)"
          >
            수정
          </button>
        </div>
        <p v-if="days.length === 366" class="hint">긴 활동 기간은 처음 366일까지 표시합니다.</p>
      </section>
      <section v-else-if="tab === 'places'" aria-label="저장한 장소">
        <div class="toolbar">
          <h2>장소</h2>
          <input
            v-model="placeSearch"
            aria-label="저장한 장소 검색"
            placeholder="저장한 장소·주소 검색"
          /><button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('place')"
          >
            ＋ 장소 직접 추가
          </button>
        </div>
        <div class="places-layout">
          <div>
            <p v-if="!places.length" class="empty">저장한 장소 또는 검색 결과가 없습니다.</p>
            <article v-for="item in places" :key="item.id" class="place-row">
              <h3>{{ item.title }}</h3>
              <p>{{ item.address || '주소 미입력' }}</p>
              <small>{{
                item.lat !== null ? '좌표 ' + item.lat + ', ' + item.lon : '위치 미지정'
              }}</small>
              <div class="actions">
                <button
                  class="secondary-button"
                  type="button"
                  :disabled="readOnly || busy"
                  @click="openForm('place', item.id)"
                >
                  수정</button
                ><button
                  class="secondary-button"
                  type="button"
                  :disabled="
                    readOnly ||
                    busy ||
                    !(installed.includes('calendar') || installed.includes('itinerary'))
                  "
                  @click="putPlaceInSchedule(item.id)"
                >
                  일정에 넣기
                </button>
              </div>
            </article>
          </div>
          <div class="location-diagram">
            <h3>저장한 장소 위치 예시</h3>
            <p class="hint">실제 지도·외부 장소 검색은 추후 연결합니다.</p>
            <svg
              v-if="markerPlaces.length"
              viewBox="0 0 440 300"
              role="img"
              aria-label="입력한 좌표의 상대 위치 예시"
            >
              <path d="M20 150H420M220 20V280" stroke="#ddd" stroke-dasharray="4" />
              <g v-for="point in markerPlaces" :key="point.id">
                <circle :cx="point.x" :cy="point.y" r="14" fill="#34234f" />
                <text
                  :x="point.x"
                  :y="point.y + 5"
                  fill="white"
                  text-anchor="middle"
                  font-size="13"
                >
                  {{ point.number }}
                </text>
              </g>
              <text x="220" y="16" text-anchor="middle" fill="#777" font-size="11">북 ↑</text>
            </svg>
            <p v-else class="empty">좌표를 입력한 장소가 없습니다.</p>
            <p v-for="point in markerPlaces" :key="point.id" class="hint">
              {{ point.number }} · {{ point.title }}
            </p>
          </div>
        </div>
      </section>
      <section v-else-if="tab === 'packing'" aria-label="준비물">
        <div class="toolbar">
          <h2>{{ packing.filter((p) => p.done).length }} / {{ packing.length }}개 준비 완료</h2>
          <button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('packing')"
          >
            ＋ 준비물 추가
          </button>
        </div>
        <p v-if="!packing.length" class="empty">준비물을 추가해 보세요.</p>
        <div v-for="item in packing" :key="item.id" class="data-row packing-row">
          <label class="check-label"
            ><input
              type="checkbox"
              :checked="item.done"
              :disabled="readOnly || busy"
              @change="toggleDone('packing', item.id)"
            /><span :class="{ done: item.done }">{{ item.title }}</span></label
          ><span>{{ name(item.assignee) }}</span
          ><button
            class="secondary-button"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('packing', item.id)"
          >
            수정
          </button>
        </div>
      </section>
      <section v-else-if="tab === 'expenses'" aria-label="비용 정산">
        <div class="toolbar">
          <h2>총 지출 {{ money(balances?.total ?? 0) }}</h2>
          <button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('expense')"
          >
            ＋ 지출 추가
          </button>
        </div>
        <p v-if="!expenses.length" class="empty">아직 지출이 없습니다.</p>
        <div v-for="item in expenses" :key="item.id" class="data-row expenses-row">
          <div>
            <h3>{{ item.title }}</h3>
            <small>{{ dateLabel(item.date) }} · {{ item.beneficiaries.length }}명 균등 분담</small>
          </div>
          <span>{{ name(item.payer) }}</span
          ><strong>{{ money(item.amount) }}</strong
          ><button
            class="secondary-button"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('expense', item.id)"
          >
            수정
          </button>
        </div>
        <div v-if="balances" class="summary-grid section">
          <section>
            <h2>참여자별 잔액</h2>
            <div v-for="[id, amount] in balances.balances" :key="id" class="list-line">
              <span>{{ name(id) }}</span
              ><strong>{{
                amount === 0
                  ? '정산 없음'
                  : (amount > 0 ? '받을 금액 ' : '보낼 금액 ') + money(Math.abs(amount))
              }}</strong>
            </div>
          </section>
          <section>
            <h2>정산 제안</h2>
            <p v-if="!balances.transfers.length" class="muted">보낼 금액이 없습니다.</p>
            <div v-for="(transfer, index) in balances.transfers" :key="index" class="list-line">
              <span>{{ name(transfer.from) }} → {{ name(transfer.to) }}</span
              ><strong>{{ money(transfer.amount) }}</strong>
            </div>
            <p class="hint">
              원화 균등 분담 · 실제 송금은 실행하지 않습니다. 나머지는 고정된 참여자 ID 순서로 1원씩
              배분합니다.
            </p>
          </section>
        </div>
        <p v-else class="content-error">참여자와 지출 정보가 맞지 않아 정산할 수 없습니다.</p>
      </section>
      <section v-else-if="tab === 'polls'" aria-label="투표">
        <div class="toolbar">
          <h2>투표</h2>
          <button
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('poll')"
          >
            ＋ 새 투표 만들기
          </button>
        </div>
        <p v-if="!polls.length" class="empty">아직 투표가 없습니다.</p>
        <article v-for="poll in polls" :key="poll.id" class="poll-card">
          <div class="section-heading">
            <h3>{{ poll.title }}</h3>
            <span class="badge">{{
              nowClosed(poll) ? '마감됨' : poll.votes.self ? '참여 완료' : '진행 중'
            }}</span>
          </div>
          <p class="hint">
            {{
              poll.deadline
                ? '마감 ' + poll.deadline.replace('T', ' ') + ' (한국 시간)'
                : '생성자가 마감할 때까지 진행'
            }}
            · {{ Object.keys(poll.votes).length }}표 · 체험 결과
          </p>
          <label v-for="option in poll.options" :key="option.id" class="vote-option"
            ><span
              ><input
                type="radio"
                :name="poll.id"
                :checked="(voting[poll.id] ?? poll.votes.self) === option.id"
                :disabled="readOnly || busy || nowClosed(poll)"
                @change="voting[poll.id] = option.id"
              />
              {{ option.title }}</span
            ><strong
              >{{ Object.values(poll.votes).filter((v) => v === option.id).length }}표</strong
            ></label
          >
          <div class="actions">
            <button
              class="primary-button fit"
              type="button"
              :disabled="
                readOnly || busy || nowClosed(poll) || !(voting[poll.id] ?? poll.votes.self)
              "
              @click="vote(poll.id)"
            >
              {{ poll.votes.self ? '선택 변경' : '투표하기' }}</button
            ><button
              class="secondary-button"
              type="button"
              :disabled="readOnly || busy || nowClosed(poll)"
              @click="closePoll(poll.id)"
            >
              투표 마감
            </button>
          </div>
          <p class="hint">
            예시 참여자의 표가 포함되어 있습니다. 실제 다른 사용자의 투표와 공유되지 않습니다.
          </p>
        </article>
      </section>
      <section v-else-if="tab === 'people'" aria-label="체험 참여자">
        <div class="toolbar">
          <h2>{{ activity ? '활동 참여자' : '체험용 참여자 목록' }}</h2>
          <button
            v-if="activity"
            class="primary-button fit"
            type="button"
            :disabled="readOnly || busy"
            @click="openForm('participants')"
          >
            참여자 선택</button
          ><button v-else class="secondary-button" type="button" @click="go('invite')">
            실제 사용자 초대
          </button>
        </div>
        <p class="hint">
          실제 멤버 조회 API가 없어 현재 계정과 예시 참여자를 표시합니다. 이 목록은 실제 공간
          인원·권한과 다릅니다.
        </p>
        <div v-for="person in members" :key="person.id" class="list-line">
          <strong>{{ person.name }}</strong
          ><span>{{ person.example ? '예시 참여자' : '현재 계정 · 체험 사용자' }}</span>
        </div>
        <p v-if="!members.some((m) => m.id === 'self')" class="hint">
          본인이 활동 참여자에 포함되지 않아 투표할 수 없습니다. 참여자 선택에서 본인을 추가하세요.
        </p>
      </section>
      <section v-else-if="tab === 'modules'" aria-label="기능 추가">
        <h2>기능 추가</h2>
        <p class="hint">
          {{ activity?.title ?? space.title }}에 추가한 기능만 이 위치의 상단 메뉴에 나타납니다.
        </p>
        <div class="module-grid">
          <article v-for="item in catalog" :key="item.id" class="module-card">
            <div>
              <h3>{{ item.label }}</h3>
              <p>{{ item.description }}</p>
            </div>
            <button
              class="secondary-button"
              type="button"
              :disabled="readOnly || busy || installed.includes(item.id)"
              @click="install(item.id)"
            >
              {{ installed.includes(item.id) ? '사용 중' : '추가하기' }}
            </button>
          </article>
        </div>
        <h2 class="section">향후 기능</h2>
        <div class="module-grid">
          <article class="module-card">
            <div>
              <span class="badge">향후 예시</span>
              <h3>AI 기능 추천</h3>
              <p>기록 분석과 추천 근거 화면 예시</p>
            </div>
            <button class="secondary-button" type="button" @click="openForm('ai')">
              예시 보기
            </button>
          </article>
          <article class="module-card">
            <div>
              <span class="badge">향후 예시</span>
              <h3>실시간 공동 편집</h3>
              <p>동시 편집 화면 예시</p>
            </div>
            <button class="secondary-button" type="button" @click="openForm('collaboration')">
              예시 보기
            </button>
          </article>
        </div>
      </section>
      <section v-else-if="tab === 'invite'" aria-label="사용자 초대">
        <slot name="invitation" />
      </section>
    </template>
    <dialog
      v-if="formKind"
      ref="dialog"
      class="content-dialog"
      aria-labelledby="content-dialog-title"
      @cancel.prevent="closeForm"
      @click="backdropClick"
    >
      <div class="section-heading">
        <h2 id="content-dialog-title">{{ formTitle }}</h2>
        <button
          class="dialog-close"
          type="button"
          aria-label="창 닫기"
          :disabled="saving"
          @click="closeForm"
        >
          ×
        </button>
      </div>
      <p v-if="error" class="content-error" role="alert">{{ error }}</p>
      <template v-if="formKind === 'ai'"
        ><span class="badge">향후 기능 · 예시</span>
        <h3>비용 정산 기능을 추천하는 화면</h3>
        <p>예시 근거: “여행 준비 회의”에 비용 분담을 계획한다는 내용이 있습니다.</p>
        <div class="fixed-field">실제 분석 결과가 아닌 고정된 화면 예시입니다.</div>
        <p class="hint">
          AI 분석·외부 전송·자동 설치는 연결되지 않았습니다. 실제 도입 시 별도 설계·검증과 승인이
          필요합니다.
        </p>
        <button class="secondary-button" type="button" @click="closeForm">닫기</button></template
      >
      <template v-else-if="formKind === 'collaboration'"
        ><span class="badge">향후 기능 · 예시</span>
        <p>나 · 예시 참여자 수연 — 동시 접속 표시 예시</p>
        <textarea readonly aria-label="공동 편집 예시">
여행 준비 기록을 함께 편집하는 화면 예시입니다.</textarea>
        <p class="hint">현재는 읽기 전용 예시입니다. 실제 공동 편집·커서·연결·동기화는 없습니다.</p>
        <button class="secondary-button" type="button" @click="closeForm">닫기</button></template
      >
      <form v-else @submit.prevent="submitForm">
        <template v-if="formKind === 'activity'">
          <p class="steps">
            {{ step === 1 ? '1. 이름·기간 → 2. 사용할 기능' : '1. 이름·기간 ✓ → 2. 사용할 기능' }}
          </p>
          <template v-if="step === 1"
            ><div class="field">
              <label for="activity-type">활동 유형</label
              ><select id="activity-type" v-model="form.type">
                <option value="travel">여행</option>
                <option value="meeting">모임</option>
                <option value="study">스터디</option>
                <option value="custom">직접 구성</option>
              </select>
            </div>
            <div class="field">
              <label for="activity-title">활동 이름</label
              ><input id="activity-title" v-model="form.title" autofocus maxlength="80" required />
            </div>
            <div class="two-fields">
              <div class="field">
                <label for="activity-start">시작일 (선택)</label
                ><input id="activity-start" v-model="form.start" type="date" />
              </div>
              <div class="field">
                <label for="activity-end">종료일 (선택)</label
                ><input id="activity-end" v-model="form.end" type="date" />
              </div></div
          ></template>
          <template v-else
            ><strong>{{ form.title }}</strong>
            <p class="hint">{{ form.start || '미정' }} ~ {{ form.end || '미정' }}</p>
            <div class="check-grid">
              <label v-for="item in MODULES" :key="item.id"
                ><input v-model="form.modules" type="checkbox" :value="item.id" />
                {{ item.label }}</label
              >
            </div></template
          >
        </template>
        <template v-else-if="formKind === 'participants'"
          ><p class="hint">
            공간 멤버에서는 제거되지 않습니다. 제외한 사람의 할 일·준비물 담당은 미지정으로
            바뀝니다. 지출에 사용된 참여자는 제외할 수 없습니다.
          </p>
          <div class="check-grid">
            <label v-for="person in data?.members" :key="person.id"
              ><input v-model="form.participants" type="checkbox" :value="person.id" />
              {{ name(person.id) }}</label
            >
          </div></template
        >
        <template v-else>
          <div class="field">
            <label for="item-title">{{
              formKind === 'expense'
                ? '사용 내역'
                : formKind === 'poll'
                  ? '투표 제목'
                  : formKind === 'task'
                    ? '할 일'
                    : formKind === 'packing'
                      ? '준비물 이름'
                      : formKind === 'place'
                        ? '장소 이름'
                        : '일정 이름'
            }}</label
            ><input
              id="item-title"
              v-model="form.title"
              autofocus
              maxlength="120"
              required
              :disabled="saving"
            />
          </div>
          <template v-if="formKind === 'task' || formKind === 'packing'"
            ><div class="field">
              <label for="item-assignee">담당자</label
              ><select id="item-assignee" v-model="form.assignee">
                <option value="">미지정</option>
                <option v-for="person in members" :key="person.id" :value="person.id">
                  {{ name(person.id) }}
                </option>
              </select>
            </div>
            <div v-if="formKind === 'task'" class="field">
              <label for="item-due">마감일 (선택)</label
              ><input id="item-due" v-model="form.date" type="date" /></div
          ></template>
          <template v-if="formKind === 'appointment'">
            <div class="two-fields">
              <div class="field">
                <label for="item-date">날짜</label
                ><input
                  id="item-date"
                  v-model="form.date"
                  type="date"
                  :min="activity?.start || undefined"
                  :max="activity?.end || undefined"
                  required
                />
              </div>
              <div class="field">
                <label for="item-time">시간</label
                ><input
                  id="item-time"
                  v-model="form.time"
                  type="time"
                  :disabled="form.allDay"
                  :required="!form.allDay"
                />
              </div>
            </div>
            <label class="check-label"><input v-model="form.allDay" type="checkbox" /> 종일</label>
            <div class="field">
              <label for="item-place">장소 (선택)</label
              ><select id="item-place" v-model="form.placeId">
                <option value="">장소 미지정</option>
                <option v-for="place in allPlaces" :key="place.id" :value="place.id">
                  {{ place.title }}
                </option>
              </select>
            </div>
            <div class="field">
              <label for="item-memo">메모 (선택)</label
              ><textarea id="item-memo" v-model="form.body" maxlength="2000"></textarea>
            </div>
            <p class="hint">한국 시간 기준으로 저장합니다.</p>
          </template>
          <template v-if="formKind === 'place'"
            ><div class="field">
              <label for="place-address">주소 (선택)</label
              ><input id="place-address" v-model="form.address" maxlength="200" />
            </div>
            <div class="two-fields">
              <div class="field">
                <label for="place-lat">위도 (선택)</label
                ><input
                  id="place-lat"
                  v-model="form.lat"
                  type="number"
                  step="any"
                  min="-90"
                  max="90"
                />
              </div>
              <div class="field">
                <label for="place-lon">경도 (선택)</label
                ><input
                  id="place-lon"
                  v-model="form.lon"
                  type="number"
                  step="any"
                  min="-180"
                  max="180"
                />
              </div>
            </div>
            <p class="hint">외부 지도·장소 검색은 아직 연결하지 않았습니다.</p></template
          >
          <template v-if="formKind === 'expense'">
            <div class="two-fields">
              <div class="field">
                <label for="expense-amount">금액 (원)</label
                ><input
                  id="expense-amount"
                  v-model="form.amount"
                  type="number"
                  min="1"
                  max="1000000000000"
                  step="1"
                  required
                />
              </div>
              <div class="field">
                <label for="expense-date">지출 날짜</label
                ><input id="expense-date" v-model="form.date" type="date" required />
              </div>
            </div>
            <div class="field">
              <label for="expense-payer">결제한 사람</label
              ><select id="expense-payer" v-model="form.payer">
                <option v-for="person in members" :key="person.id" :value="person.id">
                  {{ name(person.id) }}
                </option>
              </select>
            </div>
            <fieldset>
              <legend>분담할 사람 · 한 명 이상</legend>
              <label v-for="person in members" :key="person.id" class="check-label"
                ><input v-model="form.beneficiaries" type="checkbox" :value="person.id" />
                {{ name(person.id) }}</label
              >
            </fieldset>
            <p class="hint">원화 균등 분담만 지원합니다. 송금은 실행하지 않습니다.</p>
          </template>
          <template v-if="formKind === 'poll'"
            ><div class="field">
              <label for="poll-options">선택지 · 줄마다 하나씩</label
              ><textarea
                id="poll-options"
                v-model="form.options"
                placeholder="선택지 1&#10;선택지 2"
                maxlength="1200"
                required
              ></textarea>
            </div>
            <div class="field">
              <label for="poll-deadline">마감 일시 (선택 · 한국 시간)</label
              ><input id="poll-deadline" v-model="form.deadline" type="datetime-local" />
            </div>
            <p class="hint">서로 다른 선택지 2~8개 · 미입력 시 수동 마감</p></template
          >
        </template>
        <div class="actions dialog-actions">
          <button class="secondary-button" type="button" :disabled="saving" @click="closeForm">
            취소</button
          ><button
            v-if="formKind === 'activity' && step === 2"
            class="secondary-button"
            type="button"
            :disabled="saving"
            @click="previousStep"
          >
            이전</button
          ><button class="primary-button fit" type="submit" :disabled="saving">
            {{
              saving
                ? '저장 중...'
                : formKind === 'activity'
                  ? step === 1
                    ? '다음: 사용할 기능'
                    : '활동 만들기'
                  : formKind === 'participants'
                    ? '참여자 저장'
                    : formKind === 'poll'
                      ? '투표 만들기'
                      : '저장'
            }}
          </button>
        </div>
      </form>
    </dialog>
  </div>
</template>

<style scoped>
.space-content h1 {
  margin: 0;
  font-size: 29px;
  letter-spacing: -1px;
  overflow-wrap: anywhere;
}
.space-content h2 {
  margin: 0;
  font-size: 18px;
}
.space-content h3 {
  margin: 0;
  font-size: 15px;
  overflow-wrap: anywhere;
}
.context-description {
  margin: 12px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.7;
}
.content-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0 24px;
  margin: 24px 0;
  border-bottom: 1px solid var(--line);
}
.content-tabs button {
  border: 0;
  border-bottom: 3px solid transparent;
  padding: 15px 2px;
  background: none;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
}
.content-tabs button.active {
  border-bottom-color: var(--navy-900);
  color: var(--navy-900);
  font-weight: 700;
}
.demo-banner {
  margin-bottom: 24px;
  padding: 12px 14px;
  border: 1px solid #ded8e5;
  border-radius: 4px;
  background: #f8f6fa;
  color: #655378;
  font-size: 12px;
  line-height: 1.8;
}
.content-notice,
.content-error,
.archive-banner {
  margin: 16px 0;
  padding: 13px 16px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.8;
}
.content-notice {
  background: #eef7f0;
  color: #24633f;
}
.content-error {
  background: #fff2f2;
  color: var(--danger);
}
.archive-banner {
  background: #f5f5f7;
}
.archive-banner button {
  margin-left: 12px;
}
.section {
  margin-top: 36px;
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.toolbar h2 {
  margin-right: auto;
}
.toolbar input {
  width: min(240px, 100%);
  height: 42px;
  border: 1px solid #d5d7dc;
  border-radius: 4px;
  padding: 0 12px;
}
.toolbar select,
.field select {
  min-height: 42px;
  border: 1px solid #d5d7dc;
  border-radius: 4px;
  padding: 8px 12px;
  color: var(--text);
  background: #fff;
}
.field select {
  width: 100%;
}
.primary-button.fit {
  width: auto;
  min-height: 42px;
  white-space: nowrap;
}
.secondary-button {
  min-height: 42px;
  padding: 10px 16px;
  border: 1px solid #d9dade;
  border-radius: 4px;
  background: #fff;
  color: #34363a;
  font-size: 14px;
  cursor: pointer;
}
.secondary-button:hover:not(:disabled) {
  border-color: var(--navy-900);
}
.text-link {
  border: 0;
  padding: 2px;
  background: none;
  color: var(--navy-900);
  font-size: 12px;
  cursor: pointer;
}
.hint,
.muted {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.8;
}
.muted {
  font-size: 14px;
}
small {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.empty {
  padding: 36px 16px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.8;
}
.summary-stats {
  display: flex;
  margin: 28px 0 34px;
  border-top: 1px solid #393a3c;
  border-bottom: 1px solid var(--line);
}
.summary-stats > div {
  flex: 1;
  padding: 24px 20px;
  border-right: 1px solid var(--line);
}
.summary-stats > div:last-child {
  border-right: 0;
}
.summary-stats dt {
  color: var(--text-muted);
  font-size: 12px;
}
.summary-stats dd {
  margin: 14px 0 0;
  font-size: 22px;
  font-weight: 600;
}
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}
.summary-grid > section {
  min-width: 0;
}
.list-line,
.list-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
  padding: 18px 10px;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: none;
  text-align: left;
  color: var(--text);
  font-size: 14px;
  overflow-wrap: anywhere;
}
.list-link {
  cursor: pointer;
}
.list-link:hover {
  background: #fafafa;
}
.list-link strong {
  font-weight: 600;
}
.list-line small,
.list-link small {
  flex-shrink: 0;
}
.data-row {
  display: grid;
  align-items: center;
  gap: 20px;
  padding: 22px 12px;
  border-bottom: 1px solid var(--line);
  font-size: 14px;
  overflow-wrap: anywhere;
}
.data-row:first-of-type {
  border-top: 1px solid #393a3c;
}
.data-row small {
  display: block;
  margin-top: 8px;
}
.activities-row,
.records-row {
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr) minmax(0, 1fr) auto;
}
.tasks-row {
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr) 85px auto;
}
.itinerary-row {
  grid-template-columns: 70px minmax(0, 1.5fr) minmax(0, 1fr) auto;
}
.expenses-row {
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) 120px auto;
}
.packing-row {
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr) auto;
}
.check-label {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  line-height: 1.8;
  cursor: pointer;
}
.check-label input,
input[type='checkbox'],
input[type='radio'] {
  accent-color: var(--navy-900);
}
.done {
  text-decoration: line-through;
  color: var(--text-muted);
}
.badge {
  display: inline-block;
  width: fit-content;
  padding: 5px 8px;
  border-radius: 3px;
  background: #f2eff5;
  color: #655378;
  font-size: 12px;
}
.record-editor {
  display: grid;
  gap: 24px;
  max-width: 760px;
}
.fixed-field {
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: #f8f8f9;
  font-size: 14px;
}
textarea {
  width: 100%;
  min-height: 120px;
  padding: 14px;
  border: 1px solid #d5d7dc;
  border-radius: 4px;
  font: inherit;
  color: var(--text);
  line-height: 1.8;
  resize: vertical;
}
.record-editor textarea {
  min-height: 280px;
}
.record-editor textarea[readonly] {
  background: #fafafa;
}
.calendar {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  border-top: 1px solid #393a3c;
  border-left: 1px solid var(--line);
}
.weekday {
  padding: 10px;
  background: #fafafa;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.calendar-cell {
  min-height: 100px;
  padding: 8px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.calendar-cell.outside {
  background: #fafafa;
}
.calendar-cell.today {
  box-shadow: inset 0 0 0 1px var(--navy-900);
}
.day-number {
  padding: 2px 4px;
  border: 0;
  background: none;
  font-size: 12px;
  cursor: pointer;
}
.calendar-item,
.calendar-activity {
  display: block;
  max-width: 100%;
  margin-top: 6px;
  border: 0;
  border-radius: 3px;
  padding: 5px 7px;
  background: #f1eef5;
  color: #4b365e;
  font-size: 11px;
  text-align: left;
  overflow-wrap: anywhere;
  cursor: pointer;
}
.calendar-activity {
  background: #eef3f2;
  color: #366057;
}
.day-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 26px;
}
.day-tabs button.selected {
  color: #fff;
  background: var(--navy-900);
  border-color: var(--navy-900);
}
.places-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
}
.place-row {
  padding: 20px 12px;
  border-top: 1px solid var(--line);
}
.place-row p {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.8;
}
.place-row .actions {
  margin-top: 14px;
}
.location-diagram {
  align-self: start;
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: #fafafa;
}
.location-diagram svg {
  display: block;
  width: 100%;
  max-height: 300px;
}
.poll-card {
  max-width: 760px;
  margin: 0 0 28px;
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 4px;
}
.vote-option {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 14px;
  border: 1px solid var(--line);
  border-radius: 4px;
  margin: 12px 0;
  font-size: 14px;
  cursor: pointer;
}
.poll-card .actions {
  margin-top: 20px;
}
.module-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin: 24px 0;
}
.module-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 4px;
}
.module-card p {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.8;
}
.module-card button {
  flex-shrink: 0;
}
.module-card .badge {
  margin-bottom: 10px;
}
.invite-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-size: 15px;
}
.invite-box p {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.8;
}
.activity-return {
  margin-bottom: 22px;
}
.activity-return button {
  border: 0;
  padding: 0;
  background: none;
  color: var(--navy-900);
  font-size: 13px;
  cursor: pointer;
}
.content-dialog {
  width: min(580px, calc(100% - 32px));
  max-height: calc(100svh - 40px);
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--text);
  background: #fff;
  overflow: auto;
  box-shadow: 0 10px 40px #0002;
}
.content-dialog::backdrop {
  background: #0006;
}
.content-dialog form {
  display: grid;
  gap: 20px;
}
.content-dialog .section-heading h2 {
  font-size: 22px;
}
.dialog-close {
  border: 0;
  padding: 0 4px;
  background: none;
  font-size: 24px;
  cursor: pointer;
}
.dialog-actions {
  justify-content: flex-end;
  margin-top: 12px;
}
.two-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.check-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 16px 0;
  font-size: 14px;
}
.steps {
  color: var(--navy-900);
  font-size: 13px;
}
fieldset {
  margin: 0;
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 16px;
  display: grid;
  gap: 12px;
}
legend {
  font-size: 13px;
  padding: 0 6px;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
@media (max-width: 1050px) {
  .data-row {
    gap: 12px;
  }
  .summary-grid,
  .module-grid,
  .places-layout {
    grid-template-columns: 1fr;
  }
  .module-card {
    padding: 18px;
  }
  .records-row {
    grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr) auto;
  }
  .records-row > small {
    display: none;
  }
  .calendar-cell {
    min-height: 80px;
    padding: 5px;
  }
  .calendar-item,
  .calendar-activity {
    padding: 4px;
    font-size: 10px;
  }
}
@media (max-width: 700px) {
  .space-content h1 {
    font-size: 26px;
  }
  .content-tabs {
    gap: 0 18px;
  }
  .toolbar {
    align-items: flex-start;
  }
  .toolbar h2 {
    width: 100%;
  }
  .data-row {
    grid-template-columns: 1fr auto;
  }
  .data-row > div:first-child,
  .data-row > label:first-child {
    grid-column: 1/-1;
  }
  .data-row > button:last-child {
    grid-column: 2;
    grid-row: 2/4;
  }
  .summary-stats > div {
    padding: 20px 12px;
  }
  .summary-stats dd {
    font-size: 20px;
  }
  .list-line,
  .list-link {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  .calendar {
    overflow-x: auto;
    grid-template-columns: repeat(7, minmax(85px, 1fr));
  }
  .calendar-cell {
    min-height: 95px;
  }
  .calendar-item,
  .calendar-activity {
    font-size: 11px;
  }
  .two-fields {
    grid-template-columns: 1fr;
  }
  .invite-box {
    align-items: flex-start;
    flex-direction: column;
  }
  .module-card {
    flex-wrap: wrap;
  }
  .content-dialog {
    padding: 22px;
  }
  .content-tabs button {
    font-size: 13px;
  }
}
</style>
