import test from 'node:test'
import assert from 'node:assert/strict'
import { createFurniture } from '../src/features/room/model.ts'
import { RoomLocalData, fromLayout, readResponse, toLayout } from '../src/features/room/server.ts'
const memory = () => {
  const values = new Map()
  return {
    values,
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
}

test('server conversion removes private activity bindings and reconstructs rotated footprints', () => {
  const sofa = {
    ...createFurniture('sofa'),
    rotation: 1,
    w: 2,
    h: 3,
    event: 'demo-trip',
    module: 'expenses',
    label: '개인 여행',
  }
  const layout = toLayout([sofa])
  assert.deepEqual(Object.keys(layout.elements[0]), [
    'id',
    'kind',
    'x',
    'y',
    'rotation',
    'module',
    'label',
  ])
  assert.equal(layout.elements[0].module, '')
  assert.equal(layout.elements[0].label, '')
  const restored = fromLayout(layout)
  assert.equal(restored[0].event, undefined)
  assert.equal(restored[0].w, 2)
  assert.equal(restored[0].h, 3)
})
test('shared tool bindings persist, but stale private binding cannot override a shared tool', () => {
  const desk = { ...createFurniture('desk'), module: 'tasks', label: '공용' }
  const restored = fromLayout(toLayout([desk]), {
    [desk.id]: { event: 'old', module: 'expenses', label: '개인' },
  })
  assert.deepEqual(restored, [desk])
})
test('server responses reject wrong spaces, unsafe versions, invalid footprints and UUIDs', () => {
  const response = {
    spaceId: 1,
    version: 0,
    layout: toLayout([]),
    updatedBy: null,
    updatedAt: null,
  }
  assert.deepEqual(readResponse(response, 1), response)
  assert.throws(() => readResponse(response, 2))
  assert.throws(() => readResponse({ ...response, version: Number.MAX_SAFE_INTEGER + 1 }, 1))
  assert.throws(() => toLayout([{ ...createFurniture('cat'), id: 'legacy-not-a-uuid' }]))
  assert.throws(() =>
    fromLayout({ ...toLayout([]), elements: [{ ...createFurniture('desk'), x: 7 }] }),
  )
})
test('write-ahead backup recovers an interrupted save and preserves original browser layout', () => {
  const storage = memory(),
    local = new RoomLocalData('tester', 1, storage)
  const cat = { ...createFurniture('cat'), event: 'trip', module: 'records', label: '여행' }
  storage.setItem(local.originalKey, JSON.stringify([cat]))
  local.prepare(4, [cat])
  assert.deepEqual(local.draft(), { version: 4, furniture: [cat] })
  local.finalize([cat])
  assert.equal(local.draft(), null)
  assert.deepEqual(fromLayout(toLayout([cat]), local.bindings()), [cat])
  assert.deepEqual(local.original(), [cat])
  assert.deepEqual(new RoomLocalData('another', 1, storage).bindings(), {})
})
test('corrupt sidecars are preserved and storage failures cannot silently discard recovery', () => {
  const storage = memory(),
    local = new RoomLocalData('tester', 1, storage)
  storage.setItem(local.bindingsKey, '{corrupt')
  assert.throws(() => local.prepare(0, []))
  assert.equal(storage.getItem(local.bindingsKey), '{corrupt')
  assert.equal(local.draft(), null)
  const failed = new RoomLocalData('tester', 2, {
    ...storage,
    setItem: () => {
      throw new Error('quota')
    },
  })
  assert.throws(() => failed.prepare(0, []), /quota/)
})
test('partial finalize leaves recovery, while archived conflict drafts remain accessible', () => {
  const storage = memory(),
    local = new RoomLocalData('tester', 1, storage)
  local.prepare(1, [])
  const failed = new RoomLocalData('tester', 1, {
    ...storage,
    setItem: () => {
      throw new Error('quota')
    },
  })
  assert.throws(() => failed.finalize([]), /quota/)
  assert.deepEqual(local.draft(), { version: 1, furniture: [] })
  local.archive(1, [])
  assert.equal([...storage.values.keys()].filter((key) => key.includes(':backup:')).length, 1)
})
