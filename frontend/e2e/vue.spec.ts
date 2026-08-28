import { test, expect } from '@playwright/test'

test('shows the SAI login experience', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByRole('heading', { name: '다시 만나서 반가워요' })).toBeVisible()
  await expect(page.getByLabel('아이디')).toBeVisible()
  await expect(page.getByLabel('비밀번호', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible()
})

test('moves between login and signup', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('link', { name: '회원가입' }).click()

  await expect(page).toHaveURL(/\/signup$/)
  await expect(page.getByRole('heading', { name: '우리의 사이를 시작해요' })).toBeVisible()
})

test('shows spaces returned by the backend contract', async ({ page }) => {
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      json: { email: 'sai@example.com', name: '테스터', phoneNumber: '010-1234-5678', loginId: 'saiuser' },
    })
  })
  await page.route('**/api/spaces/my', async (route) => {
    await route.fulfill({
      json: [{ spaceId: 1, title: '우리의 공간', role: 'OWNER', spaceMemberCount: 2 }],
    })
  })

  await page.goto('/')

  await expect(page.getByRole('heading', { name: '우리의 공간' })).toBeVisible()
  await expect(page.getByText('2명이 함께하는 공간입니다.')).toBeVisible()
  await expect(page.getByText('소유자')).toBeVisible()
})

test('creates a space and refreshes the list', async ({ page }) => {
  let spaces = [{ spaceId: 1, title: '첫 공간', role: 'OWNER', spaceMemberCount: 1 }]

  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      json: { email: 'sai@example.com', name: '테스터', phoneNumber: '010-1234-5678', loginId: 'saiuser' },
    })
  })
  await page.route('**/api/spaces/my', async (route) => route.fulfill({ json: spaces }))
  await page.route('**/api/spaces', async (route) => {
    const request = route.request()
    const body = request.postDataJSON() as { title: string }
    spaces = [...spaces, { spaceId: 2, title: body.title, role: 'OWNER', spaceMemberCount: 1 }]
    await route.fulfill({ status: 201, json: { spaceId: 2 } })
  })

  await page.goto('/')
  await page.getByRole('button', { name: '새 공간 만들기', exact: true }).click()
  await page.getByLabel('공간 이름').fill('새로운 공간')
  await page.getByRole('button', { name: '공간 만들기', exact: true }).click()

  await expect(page.getByRole('heading', { name: '새로운 공간' })).toBeVisible()
})

test('invites a user with the backend invitation payload', async ({ page }) => {
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      json: { email: 'sai@example.com', name: '테스터', phoneNumber: '010-1234-5678', loginId: 'saiuser' },
    })
  })
  await page.route('**/api/spaces/my', async (route) => {
    await route.fulfill({
      json: [{ spaceId: 3, title: '초대할 공간', role: 'OWNER', spaceMemberCount: 1 }],
    })
  })
  await page.route('**/api/invitations', async (route) => {
    expect(route.request().postDataJSON()).toEqual({ spaceId: 3, inviteeUserId: 12 })
    await route.fulfill({ status: 201, json: 7 })
  })

  await page.goto('/')
  await page.getByLabel('사용자 ID').fill('12')
  await page.getByRole('button', { name: '초대 보내기' }).click()

  await expect(page.getByText('12번 사용자에게 초대를 보냈습니다.')).toBeVisible()
})
