import test from 'node:test'
import assert from 'node:assert/strict'
import {
  splitAmount,
  coordinates,
  settlement,
  validatePeriod,
  validDate,
  dateRange,
  pollClosed,
  validatePoll,
  validateParticipants,
  assertEditable,
} from '../src/features/content/logic.ts'
import {
  BrowserContentRepository,
  contentKey,
  seedContent,
  isContentState,
} from '../src/features/content/repository.ts'

function memoryStorage() {
  const entries = new Map()
  return {
    entries,
    getItem: (key) => entries.get(key) ?? null,
    setItem: (key, value) => entries.set(key, value),
  }
}
test('원 단위 나머지 배분은 입력 순서가 아닌 참여자 ID 순서로 고정된다', () => {
  assert.deepEqual(
    [...splitAmount(10, ['c', 'a', 'b'])],
    [
      ['a', 4],
      ['b', 3],
      ['c', 3],
    ],
  )
  assert.deepEqual(
    [...splitAmount(10, ['b', 'c', 'a'])],
    [
      ['a', 4],
      ['b', 3],
      ['c', 3],
    ],
  )
})
test('불가능한 금액과 빈 분담자는 거부한다', () => {
  for (const value of [0, -1, 1.5, Number.NaN, Number.MAX_SAFE_INTEGER])
    assert.throws(() => splitAmount(value, ['a']))
  assert.throws(() => splitAmount(10, []))
})
test('정산 제안을 적용하면 모든 순잔액이 0이 된다', () => {
  const members = ['a', 'b', 'c', 'd']
  for (let amount = 1; amount < 110; amount++) {
    const expenses = [
      { amount, payer: 'a', beneficiaries: ['b', 'c', 'd'] },
      { amount: amount * 3 + 1, payer: 'b', beneficiaries: ['a', 'b'] },
      { amount: 7, payer: 'd', beneficiaries: ['a', 'c'] },
    ]
    const result = settlement(expenses, members)
    assert.equal(
      [...result.balances.values()].reduce((sum, value) => sum + value, 0),
      0,
    )
    const cleared = new Map(result.balances)
    for (const t of result.transfers) {
      assert.ok(t.amount > 0)
      assert.notEqual(t.from, t.to)
      cleared.set(t.from, cleared.get(t.from) + t.amount)
      cleared.set(t.to, cleared.get(t.to) - t.amount)
    }
    assert.ok([...cleared.values()].every((value) => value === 0))
  }
})
test('샘플 지출은 계산 가능한 잔액과 총액을 제공한다', () => {
  const data = seedContent('테스터'),
    result = settlement(
      data.expenses,
      data.members.map((m) => m.id),
    )
  assert.equal(result.total, 200000)
  assert.equal(result.balances.get('self'), 70000)
  assert.equal(result.balances.get('m1'), 30000)
  assert.equal(result.balances.get('m2'), -50000)
})
test('정산에 알 수 없는 참여자를 포함할 수 없다', () => {
  assert.throws(() => settlement([{ amount: 10, payer: 'unknown', beneficiaries: ['a'] }], ['a']))
})
test('윤년·날짜 미정·기간 역전·정확한 날짜별 목록을 검증한다', () => {
  assert.equal(validDate('2028-02-29'), true)
  assert.equal(validDate('2026-02-29'), false)
  assert.equal(validDate('2026-02-31'), false)
  assert.doesNotThrow(() => validatePeriod('', ''))
  assert.throws(() => validatePeriod('2026-10-18', '2026-10-16'))
  assert.deepEqual(dateRange('2026-10-16', '2026-10-18'), [
    '2026-10-16',
    '2026-10-17',
    '2026-10-18',
  ])
  assert.deepEqual(dateRange('', ''), [])
})
test('투표는 한국 시간의 마감 직전과 이후를 구분한다', () => {
  const poll = { closed: false, deadline: '2026-10-16T12:00' }
  assert.equal(pollClosed(poll, Date.parse('2026-10-16T02:59:59Z')), false)
  assert.equal(pollClosed(poll, Date.parse('2026-10-16T03:00:00Z')), true)
  assert.equal(pollClosed({ ...poll, closed: true }, 0), true)
  assert.throws(() => validatePoll(['같은 값', '같은 값'], ''))
  assert.throws(() => validatePoll(['한 개'], ''))
})
test('지출에 사용한 참여자를 제거하거나 보관 활동을 수정할 수 없다', () => {
  const data = seedContent('테스터'),
    activity = data.activities[0]
  assert.throws(() => validateParticipants(activity, ['self'], data.expenses))
  assert.doesNotThrow(() => validateParticipants(activity, activity.participants, data.expenses))
  assert.throws(() => assertEditable([{ ...activity, archived: true }], activity.id))
})
test('계정·공간별 저장 키가 분리되고 저장 결과는 새 저장소 인스턴스에서도 유지된다', async () => {
  const storage = memoryStorage(),
    repo = new BrowserContentRepository(() => storage)
  const first = await repo.load('user:a', 1, '테스터')
  first.records.push({
    id: 'written',
    eventId: null,
    title: '직접 쓴 기록',
    body: '본문',
    updatedAt: new Date().toISOString(),
  })
  await repo.save('user:a', 1, first)
  const restored = await new BrowserContentRepository(() => storage).load('user:a', 1, '테스터')
  assert.ok(restored.records.some((r) => r.id === 'written'))
  assert.ok(!(await repo.load('user:a', 2, '테스터')).records.some((r) => r.id === 'written'))
  assert.ok(!(await repo.load('user:b', 1, '테스터')).records.some((r) => r.id === 'written'))
  assert.notEqual(contentKey('a:b', 1), contentKey('a', 1))
})
test('저장 실패는 이전 스냅샷을 유지하고 재시도할 수 있다', async () => {
  const storage = memoryStorage()
  let fail = false
  const repo = new BrowserContentRepository(() => ({
    ...storage,
    setItem: (key, value) => {
      if (fail) throw new Error('quota')
      storage.setItem(key, value)
    },
  }))
  const data = await repo.load('a', 1, '테스터'),
    before = storage.getItem(contentKey('a', 1))
  data.records[0].title = '다시 저장할 입력'
  fail = true
  await assert.rejects(repo.save('a', 1, data), /입력은 유지/)
  assert.equal(storage.getItem(contentKey('a', 1)), before)
  fail = false
  await repo.save('a', 1, data)
  assert.equal((await repo.load('a', 1, '테스터')).records[0].title, '다시 저장할 입력')
})
test('손상되거나 버전이 다른 데이터는 예시로 덮어쓰지 않는다', async () => {
  const storage = memoryStorage(),
    repo = new BrowserContentRepository(() => storage),
    key = contentKey('a', 1)
  for (const raw of ['not-json', JSON.stringify({ ...seedContent('나'), version: 999 })]) {
    storage.setItem(key, raw)
    await assert.rejects(repo.load('a', 1, '나'), /덮어쓰지/)
    assert.equal(storage.getItem(key), raw)
  }
})
test('형식 오류와 저장소 접근 실패를 명시적으로 처리한다', async () => {
  const data = seedContent('나')
  assert.equal(isContentState(data), true)
  assert.equal(isContentState({ ...data, records: [{ id: 'broken' }] }), false)
  const repo = new BrowserContentRepository(() => {
    throw new Error('blocked')
  })
  await assert.rejects(repo.load('a', 1, '나'), /접근할 수 없습니다/)
})

test('위도 0도·경도 0도와 빈 좌표를 구분한다', () => {
  assert.deepEqual(coordinates(0, 120), { lat: 0, lon: 120 })
  assert.deepEqual(coordinates(-20, 0), { lat: -20, lon: 0 })
  assert.deepEqual(coordinates('', ''), { lat: null, lon: null })
  assert.throws(() => coordinates('', 120))
})
test('최대 연도 끝의 기간도 마지막 날짜에서 종료한다', () => {
  assert.deepEqual(dateRange('9999-12-30', '9999-12-31'), ['9999-12-30', '9999-12-31'])
})
