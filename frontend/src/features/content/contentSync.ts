// Pure helpers for sharing space content: member id mapping, merging and change summaries.
import type { SpaceMember } from '@/services/workspace'
import type { ContentState, Member } from './types'

/** Member ids in shared content are `u<userId>`; the UI keeps calling the viewer 'self'. */
export const memberId = (userId: number) => `u${userId}`

export function emptyContent(): ContentState {
  return {
    version: 1,
    members: [],
    modules: ['records', 'tasks', 'calendar'],
    activities: [],
    records: [],
    tasks: [],
    appointments: [],
    places: [],
    packing: [],
    expenses: [],
    polls: [],
    footprints: [],
    anniversaries: [],
    wishlist: [],
    dues: [],
    attendance: [],
  }
}

/** Fills collections a document saved by an older client may lack. */
export function normalize(raw: Partial<ContentState>): ContentState {
  const base = emptyContent()
  return { ...base, ...raw, version: 1 } as ContentState
}

/** Rewrites every member reference with `f`: member ids, assignees, payers, participants, votes. */
export function mapMemberIds(state: ContentState, f: (id: string) => string): ContentState {
  const s = JSON.parse(JSON.stringify(state)) as ContentState
  s.members = s.members.map((m) => ({ ...m, id: f(m.id) }))
  s.activities.forEach((a) => (a.participants = a.participants.map(f)))
  s.tasks.forEach((t) => (t.assignee = t.assignee ? f(t.assignee) : t.assignee))
  s.packing.forEach((p) => (p.assignee = p.assignee ? f(p.assignee) : p.assignee))
  s.expenses.forEach((e) => {
    e.payer = f(e.payer)
    e.beneficiaries = e.beneficiaries.map(f)
  })
  s.polls.forEach((p) => {
    p.votes = Object.fromEntries(Object.entries(p.votes).map(([k, v]) => [f(k), v]))
  })
  s.footprints?.forEach((fp) => fp.notes.forEach((n) => (n.memberId = f(n.memberId))))
  s.wishlist?.forEach((w) => (w.addedBy = f(w.addedBy)))
  s.dues?.forEach((d) => (d.paid = d.paid.map(f)))
  s.attendance?.forEach((a) => (a.present = a.present.map(f)))
  return s
}

/** Real members first; people referenced by old content who left stay so their records still read. */
export function mergeMembers(content: Member[], server: SpaceMember[], selfUserId: number): Member[] {
  const real: Member[] = server.map((m) => ({
    id: m.userId === selfUserId ? 'self' : memberId(m.userId),
    name: m.nickname || m.name,
    example: false,
  }))
  const known = new Set(real.map((m) => m.id))
  const rest = content
    .filter((m) => !known.has(m.id))
    .map((m) => (m.example || !m.id.startsWith('u') ? m : { ...m, name: m.name.endsWith('(나감)') ? m.name : m.name + ' (나감)' }))
  return [...real, ...rest]
}

const COLLECTIONS: [keyof ContentState, string, string][] = [
  ['activities', '활동', 'activities'],
  ['footprints', 'Foot Print', 'footprints'],
  ['polls', '투표', 'polls'],
  ['appointments', '일정', 'calendar'],
  ['records', '기록', 'records'],
  ['tasks', '할 일', 'tasks'],
  ['expenses', '지출', 'expenses'],
  ['packing', '준비물', 'packing'],
  ['places', '장소', 'places'],
  ['anniversaries', '기념일', 'anniversaries'],
  ['wishlist', '위시리스트', 'wishlist'],
  ['dues', '회비', 'dues'],
  ['attendance', '출석', 'attendance'],
]

function particle(word: string) {
  const last = word.charCodeAt(word.length - 1)
  if (last < 0xac00 || last > 0xd7a3) return '를'
  return (last - 0xac00) % 28 ? '을' : '를'
}

/** One line for the space feed describing the first meaningful change, e.g. "투표를 추가했습니다: 숙소". */
export function describeChange(prev: ContentState | null, next: ContentState): { summary?: string; module?: string } {
  if (!prev) return {}
  for (const [key, label, module] of COLLECTIONS) {
    const before = (prev[key] ?? []) as { id: string; title?: string }[]
    const after = (next[key] ?? []) as { id: string; title?: string }[]
    const old = new Map(before.map((x) => [x.id, x]))
    const added = after.find((x) => !old.has(x.id))
    if (added) return { summary: `${label}${particle(label)} 추가했습니다: ${added.title ?? ''}`.trim(), module }
    const nextIds = new Set(after.map((x) => x.id))
    const removed = before.find((x) => !nextIds.has(x.id))
    if (removed) return { summary: `${label}${particle(label)} 지웠습니다: ${removed.title ?? ''}`.trim(), module }
    const changed = after.find((x) => JSON.stringify(x) !== JSON.stringify(old.get(x.id)))
    if (changed) return { summary: `${label}${particle(label)} 고쳤습니다: ${changed.title ?? ''}`.trim(), module }
  }
  if (JSON.stringify(prev.modules) !== JSON.stringify(next.modules))
    return { summary: '공간 기능 구성을 바꿨습니다.', module: 'modules' }
  return {}
}

