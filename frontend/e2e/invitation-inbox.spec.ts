import { test, expect, type Page } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import { mockRoomApi } from './room-fixture'

const pending = (id = 11, spaceId = 3) => ({
  invitationId: id,
  spaceId,
  spaceName: '여행 준비방',
  inviterName: '김수연',
  invitationStatus: 'PENDING',
})
async function setup(
  page: Page,
  options: {
    empty?: boolean
    member?: boolean
    failPost?: boolean
    partial?: boolean
    failGet?: boolean
    slow?: boolean
  } = {},
) {
  let items = options.empty ? [] : [pending()]
  let joined = !!options.member
  let posts = 0
  let getFailed = !!options.failGet
  await page.route('**/api/auth/me', (r) =>
    r.fulfill({
      json: {
        name: '테스터',
        loginId: 'inboxuser',
        email: 'test@example.com',
        phoneNumber: '010-1234-5678',
      },
    }),
  )
  await page.route('**/api/spaces/my', (r) =>
    options.partial && posts
      ? r.abort()
      : r.fulfill({
          json: joined
            ? [{ spaceId: 3, title: '여행 준비방', role: 'MEMBER', spaceMemberCount: 2 }]
            : [],
        }),
  )
  await page.route('**/api/invitations/received', (r) =>
    getFailed ? r.abort() : r.fulfill({ json: items }),
  )
  await page.route('**/api/invitations/join', async (r) => {
    posts++
    expect(r.request().postDataJSON()).toEqual({ invitationId: 11 })
    if (options.slow) await new Promise((resolve) => setTimeout(resolve, 1500))
    joined = true
    items = items.map((i) => ({ ...i, invitationStatus: 'ACCEPTED' }))
    if (options.failPost) {
      getFailed = true
      await r.abort()
    } else await r.fulfill({ json: 999 }) // member ID differs from target space ID
  })
  await page.route('**/api/invitations/deny', async (r) => {
    posts++
    items = items.map((i) => ({ ...i, invitationStatus: 'DENIED' }))
    await r.fulfill({ json: 11 })
  })
  await mockRoomApi(page)
  await page.goto('/spaces?panel=invitations')
  return {
    posts: () => posts,
    recover: () => {
      getFailed = false
    },
  }
}

test('renders pending badge, accepts once and uses invitation space ID for explicit navigation', async ({
  page,
}) => {
  const state = await setup(page)
  await expect(page.locator('.inbox-badge')).toHaveText('1')
  await page.getByRole('button', { name: '수락', exact: true }).click()
  await expect(page.getByText('여행 준비방 공간 참여는 완료됐어요.')).toBeVisible()
  await expect(page.locator('.inbox-badge')).toHaveText('0')
  await expect(page).toHaveURL(/panel=invitations/)
  await page.getByRole('tab', { name: /처리 완료/ }).click()
  await expect(page.getByText('수락 완료', { exact: true })).toBeVisible()
  await page
    .locator('.inbox-notice')
    .getByRole('button', { name: '공간으로 이동', exact: true })
    .click()
  await expect(page).toHaveURL(/space=3/)
  expect(state.posts()).toBe(1)
})

test('deny dialog supports keyboard cancellation and explicit confirmation', async ({ page }) => {
  const state = await setup(page)
  await page.getByRole('button', { name: '거절', exact: true }).click()
  await expect(page.getByRole('dialog')).toContainText('여행 준비방')
  await expect(page.getByRole('button', { name: '돌아가기' })).toBeFocused()
  await page.keyboard.press('Escape')
  expect(state.posts()).toBe(0)
  await expect(page.getByRole('button', { name: '거절', exact: true })).toBeFocused()
  await page.getByRole('button', { name: '거절', exact: true }).click()
  await page.getByRole('button', { name: '초대 거절', exact: true }).click()
  await expect(page.getByText('여행 준비방 초대를 거절했어요.')).toBeVisible()
  await page.getByRole('tab', { name: /처리 완료/ }).click()
  await expect(page.getByText('거절 완료', { exact: true })).toBeVisible()
  expect(state.posts()).toBe(1)
})

test('empty and failed GET are different; no unknown zero badge', async ({ page }) => {
  await setup(page, { empty: true })
  await expect(page.getByText('아직 받은 초대가 없어요.')).toBeVisible()
  await page.getByRole('tab', { name: /처리 완료/ }).click()
  await expect(page.getByText('처리 완료된 초대가 없어요.')).toBeVisible()
  await setup(page, { failGet: true })
  await expect(page.locator('.inbox-error')).toBeVisible()
  await expect(page.locator('.inbox-badge')).toHaveCount(0)
  await expect(page.getByText('아직 받은 초대가 없어요.')).toHaveCount(0)
})

test('uncertain POST locks actions until confirmed refresh without reposting', async ({ page }) => {
  const state = await setup(page, { failPost: true })
  await page.getByRole('button', { name: '수락', exact: true }).click()
  await expect(page.getByRole('button', { name: '초대 상태 확인' })).toBeEnabled()
  expect(state.posts()).toBe(1)
  state.recover()
  await page.getByRole('button', { name: '초대 상태 확인' }).click()
  await expect(page.getByText('아직 받은 초대가 없어요.')).toBeVisible()
  await page.getByRole('tab', { name: /처리 완료/ }).click()
  await expect(page.getByText('수락 완료', { exact: true })).toBeVisible()
  expect(state.posts()).toBe(1)
})

test('successful accept with failed membership refresh remains partial success', async ({
  page,
}) => {
  const state = await setup(page, { partial: true })
  await page.getByRole('button', { name: '수락', exact: true }).click()
  await expect(page.getByText('여행 준비방 공간 참여는 완료됐어요.')).toBeVisible()
  await expect(page.locator('.inbox-error')).toBeVisible()
  await expect(page.getByRole('button', { name: '공간으로 이동', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: '초대 상태 확인' }).click()
  expect(state.posts()).toBe(1)
})

test('already joined pending invitation cannot be accepted', async ({ page }) => {
  const state = await setup(page, { member: true })
  await expect(page.getByText('이미 참여 중인 공간이에요.')).toBeVisible()
  await expect(page.getByRole('button', { name: '수락', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '공간으로 이동', exact: true })).toBeEnabled()
  await page.getByRole('button', { name: '거절', exact: true }).click()
  await page.getByRole('button', { name: '초대 거절', exact: true }).click()
  expect(state.posts()).toBe(1)
})

test('processing locks navigation logout refresh and duplicate actions', async ({ page }) => {
  const state = await setup(page, { slow: true })
  await page.getByRole('button', { name: '내 공간으로', exact: true }).click()
  await page.getByRole('button', { name: /받은 초대/ }).click()
  await page.getByRole('button', { name: '수락', exact: true }).click()
  await expect(page.getByRole('button', { name: '처리 중...' })).toBeDisabled()
  await expect(page.getByRole('button', { name: '내 공간으로', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: '로그아웃', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: '새로고침', exact: true })).toBeDisabled()
  await page.goBack()
  await expect(page).toHaveURL(/panel=invitations/)
  await expect(page.getByText('여행 준비방 공간 참여는 완료됐어요.')).toBeVisible()
  expect(state.posts()).toBe(1)
})

test('401 returns to login with inbox redirect', async ({ page }) => {
  await setup(page)
  await expect(page.getByRole('button', { name: '수락', exact: true })).toBeEnabled()
  await page.route('**/api/invitations/received', (r) =>
    r.fulfill({ status: 401, json: { message: '로그인 만료' } }),
  )
  await page.getByRole('button', { name: '새로고침', exact: true }).click()
  await expect(page).toHaveURL(/login\?redirect=/)
})

test('actual Vue captures and 390px layout have no horizontal overflow', async ({ page }) => {
  await setup(page)
  await expect(page.locator('.inbox-card')).toBeVisible()
  await mkdir('../docs/mockups/invitation-inbox/implementation', { recursive: true })
  await page.screenshot({
    path: '../docs/mockups/invitation-inbox/implementation/desktop.png',
    fullPage: true,
  })
  await page.setViewportSize({ width: 390, height: 844 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({
    path: '../docs/mockups/invitation-inbox/implementation/mobile.png',
    fullPage: true,
  })
})

test('opening inbox preserves unsaved room guard', async ({ page }) => {
  await setup(page, { member: true })
  await page.getByRole('button', { name: '공간으로 이동', exact: true }).click()
  // MEMBER cannot edit; reopen with OWNER membership for the existing room guard.
  await page.route('**/api/spaces/my', (r) =>
    r.fulfill({ json: [{ spaceId: 3, title: '여행 준비방', role: 'OWNER', spaceMemberCount: 2 }] }),
  )
  await page.reload()
  await page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
  await page.getByRole('button', { name: '화분', exact: true }).click()
  page.once('dialog', (d) => d.dismiss())
  await page.getByRole('button', { name: /받은 초대/ }).click()
  await expect(page).toHaveURL(/space=3/)
  await expect(page.getByRole('button', { name: '배치 저장', exact: true })).toBeVisible()
})

test('invalid invitation response cannot enable processing or invent a zero count', async ({
  page,
}) => {
  const state = await setup(page)
  await expect(page.getByRole('button', { name: '수락', exact: true })).toBeEnabled()
  await page.route('**/api/invitations/received', (r) =>
    r.fulfill({ json: [{ ...pending(), invitationStatus: 'EXPIRED' }] }),
  )
  await page.getByRole('button', { name: '새로고침', exact: true }).click()
  await expect(page.getByText('초대 목록을 불러오지 못했습니다.')).toBeVisible()
  await expect(page.getByRole('button', { name: '수락', exact: true })).toHaveCount(0)
  await expect(page.locator('.inbox-badge')).toHaveCount(0)
  expect(state.posts()).toBe(0)
})

test('server state changed in another tab is reconciled after rejected POST', async ({ page }) => {
  await setup(page)
  await expect(page.getByRole('button', { name: '수락', exact: true })).toBeEnabled()
  let posts = 0
  await page.route('**/api/invitations/join', async (r) => {
    posts++
    await page.route('**/api/invitations/received', (r) =>
      r.fulfill({ json: [{ ...pending(), invitationStatus: 'DENIED' }] }),
    )
    await r.fulfill({ status: 400, json: { message: '이미 처리된 초대입니다.' } })
  })
  await page.getByRole('button', { name: '수락', exact: true }).click()
  await expect(page.locator('.inbox-error')).toContainText('최신 초대 상태를 확인했어요.')
  await page.getByRole('tab', { name: /처리 완료/ }).click()
  await expect(page.getByText('거절 완료', { exact: true })).toBeVisible()
  expect(posts).toBe(1)
})

test('late refresh from previous account cannot redirect after logout', async ({ page }) => {
  await setup(page)
  await expect(page.getByRole('button', { name: '수락', exact: true })).toBeEnabled()
  let release!: () => void
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/invitations/received', async (r) => {
    await gate
    await r.fulfill({ status: 401, json: { message: '오래된 세션 응답' } })
  })
  await page.route('**/api/auth/logout', (r) => r.fulfill({ status: 204 }))
  await page.getByRole('button', { name: '새로고침', exact: true }).click()
  await expect(page.getByText('초대를 불러오는 중...')).toBeVisible()
  await page.getByRole('button', { name: '로그아웃', exact: true }).click()
  await expect(page).toHaveURL(/\/login$/)
  release()
  await page.waitForResponse((r) => r.url().endsWith('/api/invitations/received'))
  await expect(page).toHaveURL(/\/login$/)
})
