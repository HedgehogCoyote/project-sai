import { test, expect, type Page, type Route } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import { mockRoomApi } from './room-fixture'
import { createFurniture } from '../src/features/room/model'
import { toLayout } from '../src/features/room/server'

async function setup(page: Page, role = 'OWNER', owner = 'roomtester') {
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({
      json: {
        name: '테스터',
        loginId: owner,
        email: 'test@example.com',
        phoneNumber: '010-1234-5678',
      },
    }),
  )
  await page.route('**/api/spaces/my', (route) =>
    route.fulfill({
      json: [
        { spaceId: 1, title: '우리 방', role, spaceMemberCount: 2 },
        { spaceId: 2, title: '다른 방', role: 'MANAGER', spaceMemberCount: 2 },
      ],
    }),
  )
  await page.goto('/spaces?space=1')
  await expect(page.locator('.room-board')).toBeVisible()
  await expect(page.getByText('공간의 가구 배치를 불러오고 있어요.')).toBeHidden()
}
const edit = (page: Page) => page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
const save = (page: Page) => page.getByRole('button', { name: '배치 저장', exact: true }).click()
async function failPut(route: Route) {
  if (route.request().method() === 'PUT') await route.abort()
  else await route.fallback()
}

test('empty server room stays empty and manager saves an explicit draft with CAS version', async ({
  page,
}) => {
  const rooms = await mockRoomApi(page)
  await setup(page, 'MANAGER')
  await expect(page.locator('.room-stage .room-object')).toHaveCount(0)
  expect(rooms.size).toBe(0)
  await edit(page)
  await page.getByRole('button', { name: '화분', exact: true }).click()
  await save(page)
  await expect(
    page.getByText('배치를 저장했어요. 공간 멤버와 같은 배치를 볼 수 있어요.'),
  ).toBeVisible()
  expect(rooms.get(1)?.version).toBe(1)
  expect(rooms.get(1)?.layout.elements).toHaveLength(1)
  await page.reload()
  await expect(page.getByRole('button', { name: '화분 장식', exact: true })).toBeVisible()
  await edit(page)
  await page.getByRole('button', { name: '가구 치우기', exact: true }).click()
  await save(page)
  await page.reload()
  await edit(page)
  await expect(page.getByText('가구를 선택하거나 추가하세요.')).toBeVisible()
  expect(rooms.get(1)?.layout.elements).toHaveLength(0)
})

test('member can view the server layout but cannot edit or import', async ({ page }) => {
  await mockRoomApi(page, true)
  await setup(page, 'MEMBER')
  await expect(page.getByRole('button', { name: '소파 할 일', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '방 꾸미기', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '기존 배치 가져오기' })).toHaveCount(0)
})

test('browser import never uploads automatically and activity bindings stay private across reload', async ({
  page,
}) => {
  const rooms = await mockRoomApi(page)
  const desk = {
    ...createFurniture('desk'),
    event: 'demo-trip',
    module: 'expenses',
    label: '개인 여행',
  }
  await page.addInitScript((item) => {
    const key = 'sai:room:v1:roomtester:1'
    if (!localStorage.getItem(key)) localStorage.setItem(key, JSON.stringify([item]))
  }, desk)
  await setup(page)
  expect(rooms.size).toBe(0)
  await page.getByRole('button', { name: '기존 배치 가져오기' }).click()
  expect(rooms.size).toBe(0)
  await expect(page.getByLabel('연결 대상')).toHaveValue('demo-trip')
  await save(page)
  await expect(
    page.getByText('배치를 저장했어요. 공간 멤버와 같은 배치를 볼 수 있어요.'),
  ).toBeVisible()
  expect(rooms.get(1)?.layout.elements[0]).toMatchObject({ module: '', label: '' })
  expect(rooms.get(1)?.layout.elements[0]).not.toHaveProperty('event')
  await page.reload()
  await edit(page)
  await expect(page.getByLabel('연결 대상')).toHaveValue('demo-trip')
  await expect(page.getByLabel('표시 이름')).toHaveValue('개인 여행')
  expect(await page.evaluate(() => localStorage.getItem('sai:room:v1:roomtester:1'))).not.toBeNull()
  await page.getByRole('button', { name: '변경 취소', exact: true }).click()
  await setup(page, 'MEMBER', 'another-account')
  await expect(page.getByRole('button', { name: '책상 장식', exact: true })).toBeVisible()
})

test('409 preserves draft, inspects latest without PUT and requires confirmation to start fresh', async ({
  page,
}) => {
  const rooms = await mockRoomApi(page, true)
  await setup(page)
  await edit(page)
  await page.getByRole('button', { name: '소파 할 일', exact: true }).click()
  await page.getByLabel('표시 이름').fill('내 초안')
  rooms.set(1, {
    spaceId: 1,
    version: 2,
    layout: toLayout([]),
    updatedBy: 2,
    updatedAt: '2026-10-11T03:40:00Z',
  })
  await save(page)
  await expect(page.getByRole('alert')).toContainText('다른 멤버가 배치를 변경했어요.')
  await expect(page.getByLabel('표시 이름')).toHaveValue('내 초안')
  await expect(page.getByRole('button', { name: '배치 저장', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: '최신 배치 확인' }).click()
  await expect(page.getByText('최신 저장 배치 미리보기 · 내 초안은 유지돼요.')).toBeVisible()
  expect(rooms.get(1)?.version).toBe(2)
  await page.getByRole('button', { name: '내 초안으로 돌아가기' }).click()
  await expect(page.getByLabel('표시 이름')).toHaveValue('내 초안')
  await page.getByRole('button', { name: '최신 배치 확인' }).click()
  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: '최신 배치로 새 편집' }).click()
  await expect(page.getByText('가구를 선택하거나 추가하세요.')).toBeVisible()
  expect(
    await page.evaluate(() =>
      Object.keys(localStorage).some(
        (key) => key.includes('room-draft') && key.includes(':backup:'),
      ),
    ),
  ).toBe(true)
})

test('network save failure retains draft and successful retry persists it', async ({ page }) => {
  await mockRoomApi(page)
  await setup(page)
  await edit(page)
  await page.getByRole('button', { name: '화분', exact: true }).click()
  await page.route('**/api/spaces/*/room', failPut)
  await save(page)
  await expect(page.getByText('저장하지 못했어요. 편집 내용은 그대로 남아 있어요.')).toBeVisible()
  await expect(page.getByRole('heading', { name: '화분', exact: true })).toBeVisible()
  await page.unroute('**/api/spaces/*/room', failPut)
  await save(page)
  await expect(
    page.getByText('배치를 저장했어요. 공간 멤버와 같은 배치를 볼 수 있어요.'),
  ).toBeVisible()
})

test('storage failure blocks PUT before losing activity bindings', async ({ page }) => {
  const rooms = await mockRoomApi(page)
  await setup(page)
  await edit(page)
  await page.getByRole('button', { name: '화분', exact: true }).click()
  await page.evaluate(() => {
    Storage.prototype.setItem = () => {
      throw new Error('저장 공간 부족')
    }
  })
  await save(page)
  await expect(page.getByText('저장 공간 부족')).toBeVisible()
  expect(rooms.size).toBe(0)
  await expect(page.getByRole('heading', { name: '화분', exact: true })).toBeVisible()
})

test('saving locks inputs and prevents duplicate PUT or leaving the space', async ({ page }) => {
  const rooms = await mockRoomApi(page)
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  let puts = 0
  await page.route('**/api/spaces/*/room', async (route) => {
    if (route.request().method() === 'PUT') {
      puts++
      await gate
    }
    await route.fallback()
  })
  await setup(page)
  await edit(page)
  await page.getByRole('button', { name: '화분', exact: true }).click()
  await save(page)
  await expect(page.getByRole('button', { name: '저장 중…', exact: true })).toBeDisabled()
  await expect(page.getByLabel('표시 이름')).toBeDisabled()
  await page.getByRole('button', { name: '다른 방', exact: true }).click()
  await expect(page).toHaveURL(/space=1/)
  expect(puts).toBe(1)
  release()
  await expect(
    page.getByText('배치를 저장했어요. 공간 멤버와 같은 배치를 볼 수 있어요.'),
  ).toBeVisible()
  expect(rooms.get(1)?.version).toBe(1)
})

test('lost PUT response recovers saved private bindings with GET rather than resending', async ({
  page,
}) => {
  const rooms = await mockRoomApi(page)
  const item = {
    ...createFurniture('cat'),
    event: 'demo-trip',
    module: 'expenses',
    label: '복구할 연결',
  }
  await page.addInitScript(
    ({ furniture, layout }) => {
      localStorage.setItem(
        'sai:room-draft:v1:roomtester:1',
        JSON.stringify({ version: 0, furniture: [furniture] }),
      )
      // Layout is installed as a server response in the test; this does not upload anything.
      void layout
    },
    { furniture: item, layout: toLayout([item]) },
  )
  rooms.set(1, { spaceId: 1, version: 1, layout: toLayout([item]), updatedBy: 1, updatedAt: null })
  await setup(page)
  await expect(page.getByText('이전에 저장한 배치와 활동 연결을 복구했어요.')).toBeVisible()
  await edit(page)
  await expect(page.getByLabel('표시 이름')).toHaveValue('복구할 연결')
  expect(rooms.get(1)?.version).toBe(1)
})

test('403 retains edit, GET failures can retry, and 401 expires the session', async ({ page }) => {
  await mockRoomApi(page)
  await page.route('**/api/spaces/*/room', (route) => route.fulfill({ status: 503, json: {} }))
  await setup(page)
  await expect(page.getByText('배치를 불러오지 못했어요. 다시 불러와주세요.')).toBeVisible()
  await page.unroute('**/api/spaces/*/room')
  await mockRoomApi(page)
  await page.getByRole('button', { name: '다시 불러오기' }).click()
  await edit(page)
  await page.getByRole('button', { name: '화분', exact: true }).click()
  await page.route('**/api/spaces/*/room', (route) => route.fulfill({ status: 403, json: {} }))
  await save(page)
  await expect(
    page.getByText('이 공간의 배치를 변경하거나 조회할 권한이 없어요. 편집 초안은 유지됩니다.'),
  ).toBeVisible()
  await expect(page.getByRole('heading', { name: '화분', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '변경 취소', exact: true }).click()
  await page.route('**/api/spaces/*/room', (route) => route.fulfill({ status: 401, json: {} }))
  await page.reload()
  await expect(page).toHaveURL(/\/login(?:\?|$)/)
})

test('actual Vue desktop and mobile captures include sharing and private binding notices', async ({
  page,
}) => {
  await mockRoomApi(page, true)
  await setup(page)
  await edit(page)
  await page.getByRole('button', { name: '책상 정산', exact: true }).click()
  await mkdir('../docs/mockups/room-server-sync/implementation', { recursive: true })
  await page.screenshot({
    path: '../docs/mockups/room-server-sync/implementation/desktop.png',
    fullPage: true,
  })
  await page.setViewportSize({ width: 390, height: 844 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({
    path: '../docs/mockups/room-server-sync/implementation/mobile.png',
    fullPage: true,
  })
})

test('late response from previous space never changes the new room', async ({ page }) => {
  const rooms = await mockRoomApi(page)
  const cat = createFurniture('cat')
  rooms.set(2, {
    spaceId: 2,
    version: 1,
    layout: toLayout([cat]),
    updatedBy: null,
    updatedAt: null,
  })
  await setup(page)
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  let requested = false
  await page.route('**/api/spaces/1/room', async (route) => {
    requested = true
    await gate
    await route
      .fulfill({
        json: {
          spaceId: 1,
          version: 9,
          layout: toLayout([createFurniture('desk')]),
          updatedBy: null,
          updatedAt: null,
        },
      })
      .catch(() => {})
  })
  await page.reload()
  await expect.poll(() => requested).toBe(true)
  await page.getByRole('button', { name: '다른 방', exact: true }).click()
  await expect(page.getByRole('button', { name: '고양이 장식', exact: true })).toBeVisible()
  release()
  await expect(page.getByRole('button', { name: '책상 장식', exact: true })).toHaveCount(0)
  await expect(page).toHaveURL(/space=2/)
})

test('partial success recovers private binding without another PUT and corrupt import preserves original', async ({
  page,
}) => {
  const rooms = await mockRoomApi(page)
  await setup(page)
  await edit(page)
  await page.getByRole('button', { name: '책상', exact: true }).click()
  await page.getByLabel('연결 대상').selectOption('demo-trip')
  await page.getByLabel('연결할 기능').selectOption('expenses')
  await page.getByLabel('표시 이름').fill('복구 연결')
  await page.evaluate(() => {
    const write = Storage.prototype.setItem
    Storage.prototype.setItem = function (key, value) {
      if (key.includes('room-bindings')) throw new Error('Sidecar failure')
      return write.call(this, key, value)
    }
  })
  await save(page)
  await expect(
    page.getByText('공간 배치는 저장됐지만 활동 연결 보존을 완료하지 못했어요.', { exact: false }),
  ).toBeVisible()
  expect(rooms.get(1)?.version).toBe(1)
  await page.reload()
  await expect(page.getByText('이전에 저장한 배치와 활동 연결을 복구했어요.')).toBeVisible()
  await edit(page)
  await expect(page.getByLabel('표시 이름')).toHaveValue('복구 연결')
  expect(rooms.get(1)?.version).toBe(1)
  await page.getByRole('button', { name: '변경 취소', exact: true }).click()
  await page.evaluate(() => localStorage.setItem('sai:room:v1:roomtester:1', 'corrupt original'))
  await page.reload()
  await expect(
    page.getByText('기존 브라우저 배치를 읽지 못했어요. 가져오지 않고 원본을 보존합니다.'),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: '방 꾸미기', exact: true })).toBeEnabled()
  expect(await page.evaluate(() => localStorage.getItem('sai:room:v1:roomtester:1'))).toBe(
    'corrupt original',
  )
})
