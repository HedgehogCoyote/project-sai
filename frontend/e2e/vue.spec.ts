import { test, expect } from '@playwright/test'

test('shows the SAI login experience', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByRole('heading', { name: '다시 만나서 반가워요' })).toBeVisible()
  await expect(page.getByLabel('아이디')).toBeVisible()
  await expect(page.getByLabel('비밀번호')).toBeVisible()
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible()
})

test('moves between login and signup', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('link', { name: '회원가입' }).click()

  await expect(page).toHaveURL(/\/signup$/)
  await expect(page.getByRole('heading', { name: '우리의 사이를 시작해요' })).toBeVisible()
})
