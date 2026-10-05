import type { Activity, Expense, Poll } from './types'

export function today(): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  return ['year', 'month', 'day'].map((type) => parts.find((p) => p.type === type)?.value).join('-')
}
export function validDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const date = new Date(value + 'T00:00:00Z')
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
}
export function addDays(value: string, amount: number): string {
  const date = new Date(value + 'T00:00:00Z')
  date.setUTCDate(date.getUTCDate() + amount)
  return date.toISOString().slice(0, 10)
}
export function dateRange(start: string, end: string): string[] {
  if (!start && !end) return []
  const from = start || end
  const to = end || start
  if (!validDate(from) || !validDate(to) || from > to) return []
  const result: string[] = []
  // 긴 활동 기간은 한 번에 366일까지 표시한다.
  for (let day = from; day <= to && result.length < 366; day = addDays(day, 1)) {
    result.push(day)
    if (day === to) break
  }
  return result
}
export function coordinates(latInput: string | number, lonInput: string | number) {
  const latText = String(latInput).trim(),
    lonText = String(lonInput).trim()
  if (!!latText !== !!lonText) throw new Error('위도와 경도를 함께 입력하거나 둘 다 비워 주세요.')
  const lat = latText === '' ? null : Number(latText),
    lon = lonText === '' ? null : Number(lonText)
  if (
    (lat !== null && (!Number.isFinite(lat) || Math.abs(lat) > 90)) ||
    (lon !== null && (!Number.isFinite(lon) || Math.abs(lon) > 180))
  )
    throw new Error('위도 -90~90, 경도 -180~180 범위로 입력해 주세요.')
  return { lat, lon }
}
export function validatePeriod(start: string, end: string): void {
  if ((start && !validDate(start)) || (end && !validDate(end)))
    throw new Error('유효한 날짜를 입력해 주세요.')
  if (start && end && start > end) throw new Error('종료일은 시작일보다 빠를 수 없습니다.')
}
export function pollClosed(poll: Poll, now = Date.now()): boolean {
  return poll.closed || (!!poll.deadline && new Date(poll.deadline + ':00+09:00').getTime() <= now)
}
export function validatePoll(options: string[], deadline: string): void {
  const choices = options.map((s) => s.trim()).filter(Boolean)
  if (choices.length < 2 || choices.length > 8) throw new Error('선택지는 2~8개 입력해 주세요.')
  if (new Set(choices).size !== choices.length) throw new Error('서로 다른 선택지를 입력해 주세요.')
  if (
    deadline &&
    (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(deadline) ||
      !validDate(deadline.slice(0, 10)) ||
      !/^([01]\d|2[0-3]):[0-5]\d$/.test(deadline.slice(11)) ||
      new Date(deadline + ':00+09:00').getTime() <= Date.now())
  )
    throw new Error('마감은 한국 시간 기준 미래 일시로 입력해 주세요.')
}
export function assertEditable(activities: Activity[], eventId: string | null): void {
  if (eventId !== null) {
    const activity = activities.find((a) => a.id === eventId)
    if (!activity) throw new Error('활동을 찾을 수 없습니다.')
    if (activity.archived) throw new Error('보관한 활동은 읽기 전용입니다. 보관을 해제해 주세요.')
  }
}
export function validateParticipants(
  activity: Activity,
  selected: string[],
  expenses: Expense[],
): void {
  if (!selected.length) throw new Error('참여자를 한 명 이상 선택해 주세요.')
  const used = new Set(
    expenses.filter((e) => e.eventId === activity.id).flatMap((e) => [e.payer, ...e.beneficiaries]),
  )
  if ([...used].some((id) => !selected.includes(id)))
    throw new Error('지출에 사용된 참여자는 제외할 수 없습니다. 지출을 먼저 수정해 주세요.')
}
export function splitAmount(amount: number, beneficiaries: string[]): Map<string, number> {
  if (!Number.isSafeInteger(amount) || amount <= 0 || amount > 1_000_000_000_000)
    throw new Error('금액은 1원~1조 원의 정수로 입력해 주세요.')
  const ids = [...new Set(beneficiaries)].sort()
  if (!ids.length) throw new Error('분담자를 한 명 이상 선택해 주세요.')
  const base = Math.floor(amount / ids.length),
    remainder = amount % ids.length
  return new Map(ids.map((id, index) => [id, base + (index < remainder ? 1 : 0)]))
}
export function settlement(expenses: Expense[], participants: string[]) {
  const balances = new Map([...new Set(participants)].sort().map((id) => [id, 0]))
  let total = 0
  for (const expense of expenses) {
    if (!balances.has(expense.payer) || expense.beneficiaries.some((id) => !balances.has(id)))
      throw new Error('지출의 참여자 정보를 확인해 주세요.')
    const shares = splitAmount(expense.amount, expense.beneficiaries)
    total += expense.amount
    if (!Number.isSafeInteger(total)) throw new Error('합산 금액이 계산 범위를 초과했습니다.')
    balances.set(expense.payer, (balances.get(expense.payer) ?? 0) + expense.amount)
    for (const [id, share] of shares) balances.set(id, (balances.get(id) ?? 0) - share)
  }
  const positive = [...balances].filter(([, n]) => n > 0).map(([id, n]) => ({ id, amount: n }))
  const negative = [...balances].filter(([, n]) => n < 0).map(([id, n]) => ({ id, amount: -n }))
  const transfers: { from: string; to: string; amount: number }[] = []
  let i = 0,
    j = 0
  while (i < negative.length && j < positive.length) {
    const from = negative[i]!,
      to = positive[j]!
    const amount = Math.min(from.amount, to.amount)
    transfers.push({ from: from.id, to: to.id, amount })
    from.amount -= amount
    to.amount -= amount
    if (!from.amount) i++
    if (!to.amount) j++
  }
  return { total, balances, transfers }
}
