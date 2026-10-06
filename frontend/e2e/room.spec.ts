import { test, expect } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('**/api/auth/me', (r) =>
    r.fulfill({
      json: {
        name: '테스터',
        loginId: 'roomtester',
        email: 'room@example.com',
        phoneNumber: '010-1234-5678',
      },
    }),
  )
  await page.route('**/api/spaces/my', (r) =>
    r.fulfill({
      json: [
        { spaceId: 1, title: '우리 방', role: 'OWNER', spaceMemberCount: 4 },
        { spaceId: 2, title: '두 번째 방', role: 'MEMBER', spaceMemberCount: 2 },
      ],
    }),
  )
  await page.goto('/spaces?space=1')
  await expect(page.locator('.three-room[data-ready="true"]')).toBeVisible()
})

test('3D furniture rotation persists per space, links to tools, and retains cancelled drafts', async ({
  page,
}) => {
  await page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
  await page.getByRole('button', { name: '소파 할 일', exact: true }).click()
  await page.getByRole('button', { name: '↻ 90° 회전', exact: true }).click()
  await expect(page.getByText('차지하는 격자 · 2 × 3칸')).toBeVisible()
  await page.getByRole('button', { name: '배치 저장', exact: true }).click()
  await page.reload()
  await page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
  await page.getByRole('button', { name: '소파 할 일', exact: true }).click()
  await expect(page.getByText('차지하는 격자 · 2 × 3칸')).toBeVisible()
  await page.getByLabel('표시 이름').fill('우리 할 일')
  await page.getByRole('button', { name: '변경 취소', exact: true }).click()
  await page.getByRole('button', { name: '소파 할 일', exact: true }).click()
  await expect(page.getByRole('heading', { name: '할 일', exact: true })).toBeVisible()
  await page
    .getByRole('complementary', { name: '공간 선택' })
    .getByRole('button', { name: '두 번째 방', exact: true })
    .click()
  await page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
  await page.getByRole('button', { name: '소파 할 일', exact: true }).click()
  await expect(page.getByText('차지하는 격자 · 3 × 2칸')).toBeVisible()
})

test('3D drag uses grid snapping and palette changes retain furniture selection', async ({
  page,
}) => {
  await page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
  const plant = page.getByRole('button', { name: '화분 장식', exact: true })
  await plant.click()
  const box = await plant.boundingBox()
  expect(box).not.toBeNull()
  await page.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2)
  await page.mouse.down()
  await page.mouse.move(box!.x + box!.width / 2 + 50, box!.y + box!.height / 2 + 25, { steps: 8 })
  await page.mouse.up()
  await expect(page.getByText('현재 위치: 2, 1')).toBeVisible()
  await page.getByRole('button', { name: '☀ 낮', exact: true }).click()
  await expect(page.getByText('현재 위치: 2, 1')).toBeVisible()
  await page.getByRole('button', { name: '가구 위로 이동', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('방 밖이거나 다른 가구가 놓인 칸이에요.')
  await page.getByRole('button', { name: '변경 취소', exact: true }).click()
  await page.getByRole('button', { name: '책상 정산', exact: true }).click()
  await expect(page.getByRole('region', { name: '비용 정산', exact: true })).toBeVisible()
})

test('room layout fits desktop and mobile widths', async ({ page }) => {
  await page.screenshot({ path: 'test-results/sai-space-3d.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.locator('.three-room[data-ready="true"]')).toBeVisible()
  await expect(page.getByRole('button', { name: '방 꾸미기', exact: true })).toBeVisible()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1,
    ),
  ).toBe(true)
  await page.getByRole('button', { name: '방 꾸미기', exact: true }).click()
  await expect(page.getByRole('heading', { name: '가구 추가', exact: true })).toBeVisible()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1,
    ),
  ).toBe(true)
  await page.screenshot({ path: 'test-results/sai-space-mobile.png', fullPage: true })
})
