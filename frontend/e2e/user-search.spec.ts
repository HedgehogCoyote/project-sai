import { test, expect, type Page } from '@playwright/test'
import { mkdir } from 'node:fs/promises'

const users = [
  { userId: 12, loginId: 'suyeon', name: '김수연' },
  { userId: 24, loginId: 'suyoon', name: '이서윤' },
]
async function setup(page: Page) {
  await page.route('**/api/auth/me', (route) => route.fulfill({ json: {
    name: '테스터', loginId: 'saiuser', email: 'sai@example.com', phoneNumber: '010-1234-5678',
  } }))
  await page.route('**/api/spaces/my', (route) => route.fulfill({ json: [
    { spaceId: 1, title: '우리의 공간', role: 'OWNER', spaceMemberCount: 2 },
    { spaceId: 2, title: '다른 공간', role: 'MEMBER', spaceMemberCount: 1 },
  ] }))
  await page.goto('/spaces?space=1&view=invite')
}
async function search(page: Page, keyword = 'suy') {
  await page.getByLabel('로그인 아이디').fill(keyword)
  await page.getByRole('button', { name: '검색', exact: true }).click()
}

test('searches trimmed encoded login ID only on submit and clears selection on edit', async ({ page }) => {
  const keywords: string[] = []
  await page.route('**/api/users/search?*', (route) => {
    keywords.push(new URL(route.request().url()).searchParams.get('loginId')!)
    return route.fulfill({ json: users })
  })
  await setup(page)
  await page.getByLabel('로그인 아이디').fill('s')
  await expect(page.getByRole('button', { name: '검색', exact: true })).toBeDisabled()
  await page.getByLabel('로그인 아이디').fill('  su+y  ')
  expect(keywords).toEqual([])
  await page.getByLabel('로그인 아이디').press('Enter')
  await expect(page.getByRole('radio', { name: '김수연 @suyeon 선택' })).toBeVisible()
  expect(keywords).toEqual(['su+y'])
  await page.getByRole('radio', { name: '김수연 @suyeon 선택' }).check()
  await expect(page.getByRole('button', { name: '초대 보내기' })).toBeEnabled()
  await page.getByLabel('로그인 아이디').fill('other')
  await expect(page.getByRole('radio')).toHaveCount(0)
  await expect(page.getByRole('button', { name: '초대 보내기' })).toBeDisabled()
})

test('blocks self selection and posts the selected server ID once', async ({ page }) => {
  let invitations = 0
  await page.route('**/api/users/search?*', (route) => route.fulfill({ json: [
    { userId: 1, loginId: 'saiuser', name: '테스터' }, ...users,
  ] }))
  await page.route('**/api/invitations', async (route) => {
    invitations++
    expect(route.request().postDataJSON()).toEqual({ spaceId: 1, inviteeUserId: 24 })
    await route.fulfill({ status: 201, json: 4 })
  })
  await setup(page)
  await search(page)
  await expect(page.getByRole('radio', { name: '테스터 @saiuser 선택' })).toBeDisabled()
  await page.getByRole('radio', { name: '이서윤 @suyoon 선택' }).check()
  await page.getByRole('button', { name: '초대 보내기' }).click()
  await expect(page.getByText('이서윤(@suyoon) 님에게 초대를 보냈습니다.')).toBeVisible()
  await expect(page.getByRole('button', { name: '초대 보내기' })).toBeDisabled()
  expect(invitations).toBe(1)
})

test('empty results and search errors offer retry', async ({ page }) => {
  let calls = 0
  await page.route('**/api/users/search?*', (route) => {
    calls++
    return calls === 1
      ? route.fulfill({ status: 503, json: { message: '잠시 후 다시 검색해 주세요.' } })
      : route.fulfill({ json: [] })
  })
  await setup(page)
  await search(page)
  await expect(page.getByRole('alert')).toHaveText('잠시 후 다시 검색해 주세요.')
  await page.getByRole('button', { name: '다시 검색' }).click()
  await expect(page.getByText(/일치하는 사용자가 없습니다/)).toBeVisible()
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('invitation errors preserve the selected user for retry', async ({ page }) => {
  await page.route('**/api/users/search?*', (route) => route.fulfill({ json: users }))
  await page.route('**/api/invitations', (route) => route.fulfill({
    status: 409, json: { message: '이미 초대를 보낸 사용자입니다.' },
  }))
  await setup(page)
  await search(page)
  await page.getByRole('radio', { name: '김수연 @suyeon 선택' }).check()
  await page.getByRole('button', { name: '초대 보내기' }).click()
  await expect(page.getByRole('alert')).toHaveText('이미 초대를 보낸 사용자입니다.')
  await expect(page.getByRole('radio', { name: '김수연 @suyeon 선택' })).toBeChecked()
})

test('pending invitation locks input and selection and blocks duplicate sends', async ({ page }) => {
  let release!: () => void
  let invitations = 0
  const held = new Promise<void>((resolve) => { release = resolve })
  await page.route('**/api/users/search?*', (route) => route.fulfill({ json: users }))
  await page.route('**/api/invitations', async (route) => {
    invitations++
    await held
    await route.fulfill({ status: 201, json: 4 })
  })
  try {
    await setup(page)
    await search(page)
    await page.getByRole('radio', { name: '김수연 @suyeon 선택' }).check()
    await page.getByRole('button', { name: '초대 보내기' }).click()
    await expect(page.getByRole('button', { name: '초대 중…' })).toBeDisabled()
    await expect(page.getByLabel('로그인 아이디')).toBeDisabled()
    await expect(page.getByRole('radio', { name: '이서윤 @suyoon 선택' })).toBeDisabled()
    expect(invitations).toBe(1)
    release()
    await expect(page.getByText('김수연(@suyeon) 님에게 초대를 보냈습니다.')).toBeVisible()
  } finally { release() }
})

test('typing a new query cancels the older search and prevents stale results', async ({ page }) => {
  let release!: () => void
  const held = new Promise<void>((resolve) => { release = resolve })
  await page.route('**/api/users/search?*', async (route) => {
    if (new URL(route.request().url()).searchParams.get('loginId') === 'old') {
      await held
      await route.fulfill({ json: [{ userId: 33, loginId: 'olduser', name: '이전 사용자' }] }).catch(() => {})
    } else await route.fulfill({ json: users })
  })
  try {
    await setup(page)
    await search(page, 'old')
    await expect(page.getByText('사용자를 찾고 있습니다.')).toBeVisible()
    await search(page, 'suy')
    await expect(page.getByRole('radio', { name: '김수연 @suyeon 선택' })).toBeVisible()
    release()
    await expect(page.getByRole('radio', { name: '이전 사용자 @olduser 선택' })).toHaveCount(0)
  } finally { release() }
})

test('switching spaces cancels pending search and resets all results', async ({ page }) => {
  let release!: () => void
  const held = new Promise<void>((resolve) => { release = resolve })
  await page.route('**/api/users/search?*', async (route) => {
    await held
    await route.fulfill({ json: users }).catch(() => {})
  })
  try {
    await setup(page)
    await search(page)
    await expect(page.getByText('사용자를 찾고 있습니다.')).toBeVisible()
    await page.getByRole('button', { name: '다른 공간', exact: true }).click()
    await page.getByRole('navigation', { name: '현재 공간 메뉴' }).getByRole('button', { name: '사용자 초대', exact: true }).click()
    release()
    await expect(page.getByLabel('로그인 아이디')).toHaveValue('')
    await expect(page.getByRole('radio')).toHaveCount(0)
  } finally { release() }
})

test('expired search session redirects to login with the current route', async ({ page }) => {
  await page.route('**/api/users/search?*', (route) => route.fulfill({ status: 401, json: { message: '로그인이 필요합니다.' } }))
  await setup(page)
  await search(page)
  await expect(page).toHaveURL(/\/login\?redirect=/)
})

test('captures implemented desktop and mobile search with selected user', async ({ page }) => {
  await page.route('**/api/users/search?*', (route) => route.fulfill({ json: users }))
  await setup(page)
  await search(page)
  await page.getByRole('radio', { name: '김수연 @suyeon 선택' }).check()
  await mkdir('../docs/mockups/user-search/implementation', { recursive: true })
  const desktopInput = await page.getByLabel('로그인 아이디').boundingBox()
  expect(desktopInput!.width).toBeGreaterThan(300)
  await page.screenshot({ path: '../docs/mockups/user-search/implementation/desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.getByRole('button', { name: '초대 보내기' })).toBeVisible()
  const mobileInput = await page.getByLabel('로그인 아이디').boundingBox()
  expect(mobileInput!.width).toBeGreaterThan(150)
  const hasOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
  expect(hasOverflow).toBe(false)
  await page.screenshot({ path: '../docs/mockups/user-search/implementation/mobile.png', fullPage: true })
})
