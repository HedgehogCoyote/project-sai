import type { ContentState, ModuleId } from './types'

export interface ContentRepository {
  load(owner: string, spaceId: number, displayName: string): Promise<ContentState>
  save(owner: string, spaceId: number, state: ContentState): Promise<void>
}
export function contentKey(owner: string, spaceId: number): string {
  return `sai:content:v1:${encodeURIComponent(owner)}:${spaceId}`
}
function dateAfter(days: number): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const day = ['year', 'month', 'day'].map((k) => parts.find((p) => p.type === k)?.value).join('-')
  const date = new Date(day + 'T00:00:00Z')
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}
export function seedContent(displayName: string): ContentState {
  const eventId = 'demo-trip',
    firstDay = dateAfter(11),
    nextDay = dateAfter(13)
  return {
    version: 1,
    members: [
      { id: 'self', name: displayName || '나', example: false },
      { id: 'm1', name: '수연', example: true },
      { id: 'm2', name: '민수', example: true },
      { id: 'm3', name: '도현', example: true },
    ],
    modules: ['records', 'tasks', 'calendar'],
    activities: [
      {
        id: eventId,
        title: '제주 여행',
        type: 'travel',
        start: firstDay,
        end: nextDay,
        status: 'ongoing',
        archived: false,
        modules: ['records', 'itinerary', 'places', 'packing', 'expenses', 'polls'],
        participants: ['self', 'm1', 'm2', 'm3'],
      },
      {
        id: 'demo-meeting',
        title: '다음 모임',
        type: 'meeting',
        start: '',
        end: '',
        status: 'ongoing',
        archived: false,
        modules: ['records', 'tasks', 'calendar', 'polls'],
        participants: ['self', 'm1'],
      },
    ],
    records: [
      {
        id: 'demo-note',
        eventId: null,
        title: '모임 운영 메모',
        body: '모임에 필요한 기능을 추가하고 함께할 활동을 만들어 보세요.',
        updatedAt: new Date().toISOString(),
      },
      {
        id: 'demo-trip-note',
        eventId,
        title: '여행 준비 회의',
        body: '첫날은 공항에서 만나 렌터카를 픽업한다.\n숙소와 식당 후보는 투표로 정한다.',
        updatedAt: new Date().toISOString(),
      },
    ],
    tasks: [
      {
        id: 'demo-task1',
        eventId: null,
        title: '숙소 예약 확인',
        assignee: 'self',
        due: dateAfter(5),
        done: false,
      },
      {
        id: 'demo-task2',
        eventId: null,
        title: '모임 장소 정하기',
        assignee: 'm2',
        due: dateAfter(7),
        done: false,
      },
    ],
    appointments: [
      {
        id: 'demo-appointment',
        eventId: null,
        title: '여행 준비 회의',
        date: dateAfter(7),
        time: '19:00',
        allDay: false,
        memo: '준비물과 예산 확인',
        placeId: '',
      },
      {
        id: 'demo-trip1',
        eventId,
        title: '렌터카 픽업',
        date: firstDay,
        time: '10:00',
        allDay: false,
        memo: '예약 번호를 확인하세요.',
        placeId: 'demo-place1',
      },
      {
        id: 'demo-trip2',
        eventId,
        title: '해변 산책',
        date: firstDay,
        time: '15:00',
        allDay: false,
        memo: '우천 시 일정 변경',
        placeId: 'demo-place2',
      },
    ],
    places: [
      {
        id: 'demo-place1',
        eventId,
        title: '제주공항',
        address: '제주시 공항로 2 (예시)',
        lat: 33.5104,
        lon: 126.4914,
      },
      {
        id: 'demo-place2',
        eventId,
        title: '협재해수욕장',
        address: '제주시 한림읍 (예시)',
        lat: 33.394,
        lon: 126.239,
      },
    ],
    packing: [
      { id: 'demo-pack1', eventId, title: '항공권 확인', assignee: 'self', done: true },
      { id: 'demo-pack2', eventId, title: '숙소 예약 확인', assignee: 'm1', done: true },
      { id: 'demo-pack3', eventId, title: '보조 배터리', assignee: '', done: false },
      { id: 'demo-pack4', eventId, title: '우산', assignee: 'self', done: false },
      { id: 'demo-pack5', eventId, title: '충전기', assignee: '', done: false },
    ],
    expenses: [
      {
        id: 'demo-expense1',
        eventId,
        title: '숙소',
        amount: 120000,
        payer: 'self',
        beneficiaries: ['self', 'm1', 'm2', 'm3'],
        date: firstDay,
      },
      {
        id: 'demo-expense2',
        eventId,
        title: '식사',
        amount: 80000,
        payer: 'm1',
        beneficiaries: ['self', 'm1', 'm2', 'm3'],
        date: firstDay,
      },
    ],
    polls: [
      {
        id: 'demo-poll',
        eventId,
        title: '첫날 저녁 메뉴',
        options: [
          { id: 'a', title: '흑돼지' },
          { id: 'b', title: '해산물' },
          { id: 'c', title: '국수' },
        ],
        votes: { m1: 'a', m2: 'a', m3: 'b' },
        deadline: '',
        closed: false,
      },
    ],
  }
}
type ObjectValue = Record<string, unknown>
const object = (value: unknown): value is ObjectValue =>
  typeof value === 'object' && value !== null && !Array.isArray(value)
const strings = (value: unknown): value is string[] =>
  Array.isArray(value) && value.every((v) => typeof v === 'string')
const textFields = (value: ObjectValue, keys: string[]) =>
  keys.every((k) => typeof value[k] === 'string')
const scoped = (value: ObjectValue) =>
  typeof value.id === 'string' && (value.eventId === null || typeof value.eventId === 'string')
const moduleIds = [
  'records',
  'tasks',
  'calendar',
  'itinerary',
  'places',
  'packing',
  'expenses',
  'polls',
]
const modules = (value: unknown) =>
  strings(value) &&
  value.every((v) => moduleIds.includes(v)) &&
  new Set(value).size === value.length
/** Corrupt or unknown versions are reported, never silently replaced with samples. */
export function isContentState(value: unknown): value is ContentState {
  if (!object(value) || value.version !== 1 || !modules(value.modules)) return false
  const checks: Record<string, (item: ObjectValue) => boolean> = {
    members: (v) => textFields(v, ['id', 'name']) && typeof v.example === 'boolean',
    activities: (v) =>
      textFields(v, ['id', 'title', 'type', 'start', 'end']) &&
      ['travel', 'meeting', 'study', 'custom'].includes(String(v.type)) &&
      ['ongoing', 'completed'].includes(String(v.status)) &&
      typeof v.archived === 'boolean' &&
      modules(v.modules) &&
      strings(v.participants),
    records: (v) => scoped(v) && textFields(v, ['title', 'body', 'updatedAt']),
    tasks: (v) =>
      scoped(v) && textFields(v, ['title', 'assignee', 'due']) && typeof v.done === 'boolean',
    appointments: (v) =>
      scoped(v) &&
      textFields(v, ['title', 'date', 'time', 'memo', 'placeId']) &&
      typeof v.allDay === 'boolean',
    places: (v) =>
      scoped(v) &&
      textFields(v, ['title', 'address']) &&
      (v.lat === null ||
        (typeof v.lat === 'number' && Number.isFinite(v.lat) && Math.abs(v.lat) <= 90)) &&
      (v.lon === null ||
        (typeof v.lon === 'number' && Number.isFinite(v.lon) && Math.abs(v.lon) <= 180)),
    packing: (v) =>
      scoped(v) && textFields(v, ['title', 'assignee']) && typeof v.done === 'boolean',
    expenses: (v) =>
      scoped(v) &&
      textFields(v, ['title', 'payer', 'date']) &&
      Number.isSafeInteger(v.amount) &&
      Number(v.amount) > 0 &&
      Number(v.amount) <= 1_000_000_000_000 &&
      strings(v.beneficiaries) &&
      v.beneficiaries.length > 0,
    polls: (v) =>
      scoped(v) &&
      textFields(v, ['title', 'deadline']) &&
      typeof v.closed === 'boolean' &&
      Array.isArray(v.options) &&
      v.options.length >= 2 &&
      v.options.every((o) => object(o) && textFields(o, ['id', 'title'])) &&
      object(v.votes) &&
      Object.values(v.votes).every((x) => typeof x === 'string'),
  }
  for (const [key, check] of Object.entries(checks)) {
    const items = value[key]
    if (!Array.isArray(items) || !items.every((v) => object(v) && check(v))) return false
    if (new Set(items.map((v) => v.id)).size !== items.length) return false
  }
  const state = value as unknown as ContentState
  const memberIds = new Set(state.members.map((m) => m.id)),
    activityIds = new Set(state.activities.map((a) => a.id))
  if (!memberIds.has('self')) return false
  if (
    state.activities.some(
      (a) => !a.participants.length || a.participants.some((id) => !memberIds.has(id)),
    )
  )
    return false
  for (const items of [
    state.records,
    state.tasks,
    state.appointments,
    state.places,
    state.packing,
    state.expenses,
    state.polls,
  ])
    if (items.some((item) => item.eventId !== null && !activityIds.has(item.eventId))) return false
  if (
    [...state.tasks, ...state.packing].some(
      (item) => !!item.assignee && !memberIds.has(item.assignee),
    )
  )
    return false
  if (
    state.expenses.some(
      (e) =>
        !memberIds.has(e.payer) ||
        e.beneficiaries.some((id) => !memberIds.has(id)) ||
        new Set(e.beneficiaries).size !== e.beneficiaries.length,
    )
  )
    return false
  if (
    state.polls.some((p) => {
      const options = new Set(p.options.map((o) => o.id))
      return (
        options.size !== p.options.length ||
        Object.entries(p.votes).some(
          ([member, option]) => !memberIds.has(member) || !options.has(option),
        )
      )
    })
  )
    return false
  return true
}
export class BrowserContentRepository implements ContentRepository {
  private storage: () => Pick<Storage, 'getItem' | 'setItem'>
  constructor(storage: () => Pick<Storage, 'getItem' | 'setItem'> = () => window.localStorage) {
    this.storage = storage
  }
  async load(owner: string, spaceId: number, displayName: string): Promise<ContentState> {
    let raw: string | null
    try {
      raw = this.storage().getItem(contentKey(owner, spaceId))
    } catch {
      throw new Error(
        '브라우저 저장소에 접근할 수 없습니다. 저장소 설정을 확인하고 다시 시도해 주세요.',
      )
    }
    if (raw === null) {
      const data = seedContent(displayName)
      await this.save(owner, spaceId, data)
      return data
    }
    let parsed: unknown
    try {
      parsed = JSON.parse(raw)
    } catch {
      throw new Error('저장된 체험 데이터를 읽을 수 없습니다. 기존 데이터는 덮어쓰지 않았습니다.')
    }
    if (!isContentState(parsed))
      throw new Error('저장된 데이터 형식이 맞지 않습니다. 기존 데이터는 덮어쓰지 않았습니다.')
    return parsed
  }
  async save(owner: string, spaceId: number, state: ContentState): Promise<void> {
    if (!isContentState(state)) throw new Error('데이터 형식이 올바르지 않아 저장하지 않았습니다.')
    try {
      this.storage().setItem(contentKey(owner, spaceId), JSON.stringify(state))
    } catch {
      throw new Error(
        '이 브라우저에 저장하지 못했습니다. 저장 공간·설정을 확인하고 다시 저장해 주세요. 입력은 유지됩니다.',
      )
    }
  }
}
