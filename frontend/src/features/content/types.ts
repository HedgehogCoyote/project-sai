export type ModuleId =
  | 'records'
  | 'tasks'
  | 'calendar'
  | 'itinerary'
  | 'places'
  | 'packing'
  | 'expenses'
  | 'polls'
  | 'anniversaries'
  | 'wishlist'
  | 'dues'
  | 'attendance'
export type ScopeId = string | null
export type Member = { id: string; name: string; example: boolean }
export type Activity = {
  id: string
  title: string
  type: 'travel' | 'meeting' | 'study' | 'custom'
  start: string
  end: string
  status: 'ongoing' | 'completed'
  archived: boolean
  modules: ModuleId[]
  participants: string[]
}
export type RecordItem = {
  id: string
  eventId: ScopeId
  title: string
  body: string
  updatedAt: string
}
export type TaskItem = {
  id: string
  eventId: ScopeId
  title: string
  assignee: string
  due: string
  done: boolean
}
export type Appointment = {
  id: string
  eventId: ScopeId
  title: string
  date: string
  time: string
  allDay: boolean
  memo: string
  placeId: string
}
export type Place = {
  id: string
  eventId: ScopeId
  title: string
  address: string
  lat: number | null
  lon: number | null
}
export type PackingItem = {
  id: string
  eventId: ScopeId
  title: string
  assignee: string
  done: boolean
}
export type Expense = {
  id: string
  eventId: ScopeId
  title: string
  amount: number
  payer: string
  beneficiaries: string[]
  date: string
}
export type Poll = {
  id: string
  eventId: ScopeId
  title: string
  options: { id: string; title: string }[]
  votes: Record<string, string>
  deadline: string
  closed: boolean
}
/** Couples: a date to count toward (yearly) or count from (D+). */
export type Anniversary = { id: string; title: string; date: string; yearly: boolean }
export type WishItem = { id: string; title: string; note: string; done: boolean; addedBy: string }
/** Clubs: one collection period with who has paid. */
export type DuesPeriod = { id: string; title: string; amount: number; due: string; paid: string[] }
export type AttendanceSession = { id: string; title: string; date: string; present: string[] }
export type FootPrintNote = { id: string; memberId: string; body: string; createdAt: string }
/** A finished activity's shared record: photos, short notes and the souvenir placed in the room. */
export type FootPrint = {
  id: string
  eventId: string
  title: string
  start: string
  end: string
  status: 'draft' | 'published'
  photoIds: number[]
  coverPhotoId: number | null
  notes: FootPrintNote[]
  souvenir: { kind: string; photoId: number | null } | null
  publishedAt: string
}
export type ContentState = {
  version: 1
  members: Member[]
  modules: ModuleId[]
  activities: Activity[]
  records: RecordItem[]
  tasks: TaskItem[]
  appointments: Appointment[]
  places: Place[]
  packing: PackingItem[]
  expenses: Expense[]
  polls: Poll[]
  footprints?: FootPrint[]
  anniversaries?: Anniversary[]
  wishlist?: WishItem[]
  dues?: DuesPeriod[]
  attendance?: AttendanceSession[]
}
export const MODULES: {
  id: ModuleId
  label: string
  description: string
  activityOnly?: boolean
}[] = [
  { id: 'records', label: '기록', description: '메모를 작성하고 제목과 내용으로 찾습니다.' },
  { id: 'tasks', label: '할 일', description: '담당자·마감일·완료 상태를 관리합니다.' },
  { id: 'calendar', label: '일정', description: '약속과 활동 기간을 달력으로 봅니다.' },
  {
    id: 'itinerary',
    label: '여행 일정',
    description: '날짜별 방문 일정을 관리합니다.',
    activityOnly: true,
  },
  { id: 'places', label: '장소', description: '장소를 직접 저장하고 일정에 연결합니다.' },
  {
    id: 'packing',
    label: '준비물',
    description: '챙길 물건과 담당자를 확인합니다.',
    activityOnly: true,
  },
  { id: 'expenses', label: '비용 정산', description: '지출과 원화 균등 분담을 계산합니다.' },
  { id: 'polls', label: '투표', description: '선택지를 만들고 의견을 모읍니다.' },
  { id: 'anniversaries', label: '기념일', description: '처음 만난 날, 생일 같은 날까지 D-day를 셉니다.' },
  { id: 'wishlist', label: '위시리스트', description: '함께 하고 싶은 일과 가고 싶은 곳을 모읍니다.' },
  { id: 'dues', label: '회비', description: '회비 기간별로 누가 냈는지 확인합니다.' },
  { id: 'attendance', label: '출석', description: '모임마다 출석을 체크하고 출석률을 봅니다.' },
]
