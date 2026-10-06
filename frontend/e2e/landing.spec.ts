import { test, expect } from '@playwright/test'

test('public landing works when the backend is unavailable and opens signup', async ({ page }) => {
  await page.route('**/api/auth/me', (route) => route.abort())
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '가까워지는 순간, 새로운 공간.' })).toBeVisible()
  await page.getByRole('link', { name: '시작하기' }).click()
  await expect(page).toHaveURL(/\/signup$/)
  await expect(page.getByRole('heading', { name: '회원가입' })).toBeVisible()
  await page.getByRole('link', { name: 'SAI 메인으로 돌아가기' }).click()
  await expect(page).toHaveURL(/\/$/)
})

test('login from landing leads to authenticated spaces', async ({ page }) => {
  let loggedIn = false
  await page.route('**/api/auth/me', (route) =>
    route.fulfill(
      loggedIn
        ? {
            json: {
              name: '테스터',
              loginId: 'testuser',
              email: 'test@example.com',
              phoneNumber: '010-1234-5678',
            },
          }
        : { status: 401, json: {} },
    ),
  )
  await page.route('**/api/auth/login', (route) => {
    loggedIn = true
    return route.fulfill({ json: { userId: 1 } })
  })
  await page.route('**/api/spaces/my', (route) => route.fulfill({ json: [] }))
  await page.goto('/')
  await page.getByRole('navigation', { name: '계정' }).getByRole('link', { name: '로그인' }).click()
  await page.getByLabel('아이디').fill('testuser')
  await page.getByLabel('비밀번호', { exact: true }).fill('password123')
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page).toHaveURL(/\/spaces$/)
  await expect(page.getByRole('heading', { name: '내 공간' })).toBeVisible()
  await page.goto('/')
  await expect(page.getByRole('link', { name: '내 공간으로' })).toBeVisible()
})

test('landing fits mobile and replays its animation', async ({ page }) => {
  await page.route('**/api/auth/me', (route) => route.fulfill({ status: 401, json: {} }))
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await page.getByRole('button', { name: '원 애니메이션 일시정지' }).click()
  await expect(page.getByRole('button', { name: '원 애니메이션 재생' })).toBeVisible()
  await page.getByRole('button', { name: '원 애니메이션 다시 보기' }).click()
  await expect(page.getByRole('button', { name: '원 애니메이션 일시정지' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
