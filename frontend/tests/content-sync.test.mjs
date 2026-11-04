import test from 'node:test'
import assert from 'node:assert/strict'
import {
  describeChange,
  emptyContent,
  mapMemberIds,
  mergeMembers,
  normalize,
} from '../src/features/content/contentSync.ts'

const state = () => ({
  ...emptyContent(),
  members: [{ id: 'self', name: '나', example: false }, { id: 'u7', name: '민재', example: false }],
  activities: [{ id: 'a', title: '부산', type: 'travel', start: '', end: '', status: 'ongoing', archived: false, modules: [], participants: ['self', 'u7'] }],
  tasks: [{ id: 't', eventId: null, title: '예약', assignee: 'self', due: '', done: false }],
  expenses: [{ id: 'e', eventId: 'a', title: '숙소', amount: 1000, payer: 'self', beneficiaries: ['self', 'u7'], date: '' }],
  polls: [{ id: 'p', eventId: 'a', title: '숙소', options: [], votes: { self: 'o1', u7: 'o2' }, deadline: '', closed: false }],
  footprints: [{ id: 'f', eventId: 'a', title: '부산', start: '', end: '', status: 'draft', photoIds: [], coverPhotoId: null, notes: [{ id: 'n', memberId: 'self', body: '좋았다', createdAt: '' }], souvenir: null, publishedAt: '' }],
})

test('viewer id maps to u<userId> for the server and back without touching others', () => {
  const toServer = mapMemberIds(state(), (id) => (id === 'self' ? 'u3' : id))
  assert.deepEqual(toServer.activities[0].participants, ['u3', 'u7'])
  assert.equal(toServer.tasks[0].assignee, 'u3')
  assert.equal(toServer.expenses[0].payer, 'u3')
  assert.deepEqual(toServer.polls[0].votes, { u3: 'o1', u7: 'o2' })
  assert.equal(toServer.footprints[0].notes[0].memberId, 'u3')
  const back = mapMemberIds(toServer, (id) => (id === 'u3' ? 'self' : id))
  assert.deepEqual(back, state())
})

test('server members replace local ones; people who left stay readable', () => {
  const merged = mergeMembers(
    [{ id: 'u9', name: '지민', example: false }, { id: 'm1', name: '예시', example: true }],
    [
      { userId: 3, loginId: 'me', name: '나', nickname: null, role: 'OWNER', joinedAt: '' },
      { userId: 7, loginId: 'mj', name: '민재', nickname: '골키퍼', role: 'MEMBER', joinedAt: '' },
    ],
    3,
  )
  assert.deepEqual(merged.map((m) => [m.id, m.name]), [
    ['self', '나'],
    ['u7', '골키퍼'],
    ['u9', '지민 (나감)'],
    ['m1', '예시'],
  ])
})

test('change summaries name the first changed collection with the right particle', () => {
  const before = state()
  const added = state()
  added.polls.push({ id: 'p2', eventId: null, title: '회식 장소', options: [], votes: {}, deadline: '', closed: false })
  assert.deepEqual(describeChange(before, added), { summary: '투표를 추가했습니다: 회식 장소', module: 'polls' })
  const edited = state()
  edited.activities[0].status = 'completed'
  assert.deepEqual(describeChange(before, edited), { summary: '활동을 고쳤습니다: 부산', module: 'activities' })
  const removed = state()
  removed.tasks = []
  assert.deepEqual(describeChange(before, removed), { summary: '할 일을 지웠습니다: 예약', module: 'tasks' })
  assert.deepEqual(describeChange(before, state()), {})
  assert.deepEqual(describeChange(null, state()), {})
})

test('documents from older clients gain missing collections', () => {
  const n = normalize({ version: 1, activities: [] })
  assert.deepEqual(n.footprints, [])
  assert.deepEqual(n.modules, ['records', 'tasks', 'calendar'])
})
