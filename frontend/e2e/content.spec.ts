import { test, expect, type Page } from '@playwright/test'

const menu = (page: Page) => page.getByRole('navigation', { name: '현재 공간 메뉴' })
const tab = async (page: Page, name: string) =>
  menu(page).getByRole('button', { name, exact: true }).click()
const save = async (page: Page) =>
  page.getByRole('dialog').getByRole('button', { name: '저장', exact: true }).click()
async function trip(page: Page) {
  await tab(page, '활동')
  await page
    .locator('.activities-row')
    .filter({ hasText: '제주 여행' })
    .getByRole('button', { name: '활동 열기' })
    .click()
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({
      json: {
        name: '테스터',
        loginId: 'contenttester',
        email: 'test@example.com',
        phoneNumber: '010-1234-5678',
      },
    }),
  )
  await page.route('**/api/spaces/my', (route) =>
    route.fulfill({
      json: [
        { spaceId: 1, title: '첫 공간', role: 'OWNER', spaceMemberCount: 4 },
        { spaceId: 2, title: '둘째 공간', role: 'MEMBER', spaceMemberCount: 2 },
      ],
    }),
  )
  await page.goto('/?space=1')
  await expect(page.getByRole('heading', { name: '공간 정보', exact: true })).toBeVisible()
})

test('기록 작성·편집·검색·새로고침 유지와 공간 데이터 분리', async ({ page }) => {
  await tab(page, '기록')
  await page.getByRole('button', { name: '＋ 새 기록' }).click()
  await page.getByLabel('제목', { exact: true }).fill('내가 작성한 기록')
  await page.getByLabel('내용', { exact: true }).fill('다음 여행에서 함께할 장소를 정리한다.')
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('이 브라우저에 저장했습니다.')
  await page.reload()
  await expect(page.getByLabel('제목', { exact: true })).toHaveValue('내가 작성한 기록')
  await page.getByLabel('내용', { exact: true }).fill('수정한 내용')
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await page.getByRole('button', { name: '목록으로', exact: true }).click()
  await page.getByRole('textbox', { name: '기록 검색' }).fill('수정한 내용')
  await expect(page.getByRole('heading', { name: '내가 작성한 기록' })).toBeVisible()
  await page
    .getByRole('complementary', { name: '공간 선택' })
    .getByRole('button', { name: '둘째 공간', exact: true })
    .click()
  await tab(page, '기록')
  await expect(page.getByRole('heading', { name: '내가 작성한 기록' })).toBeHidden()
})

test('미저장 기록은 공간 전환·로그아웃 전에 확인한다', async ({ page }) => {
  let loggedOut = false
  await page.route('**/api/auth/logout', (route) => {
    loggedOut = true
    return route.fulfill({ status: 204 })
  })
  await tab(page, '기록')
  await page.getByRole('button', { name: '＋ 새 기록' }).click()
  await page.getByLabel('제목', { exact: true }).fill('저장 전 입력')
  page.once('dialog', (dialog) => dialog.dismiss())
  await page
    .getByRole('complementary', { name: '공간 선택' })
    .getByRole('button', { name: '둘째 공간', exact: true })
    .click()
  await expect(page.getByLabel('제목', { exact: true })).toHaveValue('저장 전 입력')
  page.once('dialog', (dialog) => dialog.dismiss())
  await page.getByRole('button', { name: '로그아웃', exact: true }).click()
  expect(loggedOut).toBe(false)
  page.once('dialog', (dialog) => dialog.accept())
  await page
    .getByRole('complementary', { name: '공간 선택' })
    .getByRole('button', { name: '둘째 공간', exact: true })
    .click()
  await expect(page.getByRole('heading', { name: '둘째 공간', exact: true })).toBeVisible()
})

test('할 일 추가·수정·완료 필터가 저장 결과와 일치한다', async ({ page }) => {
  await tab(page, '할 일')
  await page.getByRole('button', { name: '＋ 할 일 추가' }).click()
  await page.getByRole('dialog').getByLabel('할 일', { exact: true }).fill('티켓 확인')
  await page.getByLabel('담당자', { exact: true }).selectOption('self')
  await save(page)
  const row = page.locator('.tasks-row').filter({ hasText: '티켓 확인' })
  await row.getByRole('button', { name: '수정' }).click()
  await page.getByRole('dialog').getByLabel('할 일', { exact: true }).fill('티켓 결제 확인')
  await save(page)
  await page.getByRole('checkbox', { name: '티켓 결제 확인' }).click()
  await expect(page.getByRole('checkbox', { name: '티켓 결제 확인' })).toBeHidden()
  await page.getByLabel('보여줄 할 일').selectOption('finished')
  await expect(page.getByRole('checkbox', { name: '티켓 결제 확인' })).toBeChecked()
  await page.reload()
  await page.getByLabel('보여줄 할 일').selectOption('finished')
  await expect(page.getByRole('checkbox', { name: '티켓 결제 확인' })).toBeChecked()
})

test('종일 일정 생성·달 이동·오늘 복귀·일정 수정', async ({ page }) => {
  await tab(page, '일정')
  await page.getByRole('button', { name: '＋ 일정 추가', exact: true }).click()
  await page.getByLabel('일정 이름').fill('종일 모임')
  await page.getByRole('checkbox', { name: '종일', exact: true }).check()
  await expect(page.getByLabel('시간', { exact: true })).toBeDisabled()
  await save(page)
  await expect(page.getByRole('button', { name: '종일 종일 모임' })).toBeVisible()
  await page.getByRole('button', { name: '다음 달', exact: true }).click()
  await expect(page.getByRole('button', { name: '종일 종일 모임' })).toBeHidden()
  await page.getByRole('button', { name: '오늘', exact: true }).click()
  await page.getByRole('button', { name: '종일 종일 모임' }).click()
  await page.getByLabel('일정 이름').fill('수정한 종일 모임')
  await save(page)
  await expect(page.getByRole('button', { name: '종일 수정한 종일 모임' })).toBeVisible()
})

test('활동 생성 단계의 기간 검증·입력 유지·선택한 기능 설치', async ({ page }) => {
  await tab(page, '활동')
  await page.getByRole('button', { name: '＋ 활동 만들기' }).click()
  await page.getByLabel('활동 이름').fill('새 여행')
  await page.getByLabel('시작일 (선택)').fill('2026-10-18')
  await page.getByLabel('종료일 (선택)').fill('2026-10-16')
  await page.getByRole('button', { name: '다음: 사용할 기능' }).click()
  await expect(page.getByRole('alert')).toHaveText('종료일은 시작일보다 빠를 수 없습니다.')
  await page.getByLabel('종료일 (선택)').fill('2026-10-20')
  await page.getByRole('button', { name: '다음: 사용할 기능' }).click()
  await page.getByRole('button', { name: '이전', exact: true }).click()
  await expect(page.getByLabel('활동 이름')).toHaveValue('새 여행')
  await page.getByRole('button', { name: '다음: 사용할 기능' }).click()
  await page.getByRole('button', { name: '활동 만들기', exact: true }).click()
  await expect(page.getByRole('heading', { name: '새 여행', exact: true })).toBeVisible()
  await expect(menu(page).getByRole('button', { name: '여행 일정', exact: true })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('heading', { name: '새 여행', exact: true })).toBeVisible()
})

test('활동 완료·보관·보관 해제와 기록 읽기 전용', async ({ page }) => {
  await trip(page)
  await page.getByRole('button', { name: '완료로 표시', exact: true }).click()
  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: '보관하기', exact: true }).click()
  await expect(page.getByText('보관한 활동입니다.', { exact: false })).toBeVisible()
  await tab(page, '기록')
  await expect(page.getByRole('button', { name: '＋ 새 기록' })).toBeDisabled()
  await page
    .locator('.records-row')
    .filter({ hasText: '여행 준비 회의' })
    .getByRole('button', { name: '열기' })
    .click()
  await expect(page.getByLabel('내용', { exact: true })).toHaveAttribute('readonly', '')
  await expect(page.getByRole('button', { name: '저장', exact: true })).toBeHidden()
  await page.getByRole('button', { name: '목록으로', exact: true }).click()
  await tab(page, '활동 홈')
  await page.getByRole('button', { name: '보관 해제', exact: true }).click()
  await expect(page.getByRole('definition').filter({ hasText: '완료' })).toBeVisible()
  await tab(page, '기록')
  await expect(page.getByRole('button', { name: '＋ 새 기록' })).toBeEnabled()
})

test('장소 직접 입력은 0도 좌표를 허용하고 여행 일정에 연결한다', async ({ page }) => {
  await trip(page)
  await tab(page, '장소')
  await page.getByRole('button', { name: '＋ 장소 직접 추가' }).click()
  await page.getByLabel('장소 이름').fill('새 방문 장소')
  await page.getByLabel('주소 (선택)').fill('체험 주소')
  await page.getByLabel('위도 (선택)').fill('0')
  await page.getByLabel('경도 (선택)').fill('120')
  await save(page)
  const place = page.locator('.place-row').filter({ hasText: '새 방문 장소' })
  await expect(place).toContainText('좌표 0, 120')
  await place.getByRole('button', { name: '일정에 넣기' }).click()
  await expect(page.getByLabel('일정 이름')).toHaveValue('새 방문 장소')
  await save(page)
  await tab(page, '여행 일정')
  await expect(page.getByRole('heading', { name: '새 방문 장소' })).toBeVisible()
})

test('준비물 추가·완료·수정과 집계', async ({ page }) => {
  await trip(page)
  await tab(page, '준비물')
  await page.getByRole('button', { name: '＋ 준비물 추가' }).click()
  await page.getByLabel('준비물 이름').fill('여권')
  await save(page)
  await page.getByRole('checkbox', { name: '여권', exact: true }).check()
  await expect(page.getByRole('heading', { name: '3 / 6개 준비 완료' })).toBeVisible()
  await page
    .locator('.packing-row')
    .filter({ hasText: '여권' })
    .getByRole('button', { name: '수정' })
    .click()
  await page.getByLabel('준비물 이름').fill('여권 원본')
  await save(page)
  await expect(page.getByRole('checkbox', { name: '여권 원본' })).toBeChecked()
})

test('지출 추가·수정과 정산 총액 갱신', async ({ page }) => {
  await trip(page)
  await tab(page, '비용 정산')
  await page.getByRole('button', { name: '＋ 지출 추가' }).click()
  await page.getByLabel('사용 내역').fill('소액 간식')
  await page.getByLabel('금액 (원)').fill('1001')
  await save(page)
  await expect(page.getByRole('heading', { name: '총 지출 201,001원' })).toBeVisible()
  await page
    .locator('.expenses-row')
    .filter({ hasText: '소액 간식' })
    .getByRole('button', { name: '수정' })
    .click()
  await page.getByLabel('금액 (원)').fill('1003')
  await save(page)
  await page.reload()
  await expect(page.getByRole('heading', { name: '총 지출 201,003원' })).toBeVisible()
})

test('지출 기록이 있는 참여자는 활동에서 제거할 수 없다', async ({ page }) => {
  await trip(page)
  await tab(page, '참여자')
  await page.getByRole('button', { name: '참여자 선택' }).click()
  await page.getByRole('dialog').getByRole('checkbox', { name: '예시 · 도현' }).uncheck()
  await page.getByRole('button', { name: '참여자 저장' }).click()
  await expect(page.getByRole('alert')).toContainText('지출에 사용된 참여자는 제외할 수 없습니다.')
})

test('투표 생성의 중복 검증·선택 변경·마감 후 차단', async ({ page }) => {
  await trip(page)
  await tab(page, '투표')
  await page.getByRole('button', { name: '＋ 새 투표 만들기' }).click()
  await page.getByLabel('투표 제목').fill('새 투표')
  await page.getByLabel('선택지 · 줄마다 하나씩').fill('A\nA')
  await page.getByRole('dialog').getByRole('button', { name: '투표 만들기', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('서로 다른 선택지를 입력해 주세요.')
  await page.getByLabel('선택지 · 줄마다 하나씩').fill('A\nB')
  await page.getByRole('dialog').getByRole('button', { name: '투표 만들기', exact: true }).click()
  const poll = page.locator('.poll-card').filter({ hasText: '새 투표' })
  await poll.getByRole('radio', { name: 'A 0표' }).check()
  await poll.getByRole('button', { name: '투표하기' }).click()
  await expect(poll).toContainText('1표 · 체험 결과')
  await poll.getByRole('radio', { name: 'B 0표' }).check()
  await poll.getByRole('button', { name: '선택 변경' }).click()
  await expect(poll).toContainText('1표 · 체험 결과')
  await expect(poll.getByRole('radio', { name: 'B 1표' })).toBeChecked()
  await poll.getByRole('button', { name: '투표 마감' }).click()
  await expect(poll.getByRole('radio', { name: 'B 1표' })).toBeDisabled()
  await page.reload()
  await expect(page.locator('.poll-card').filter({ hasText: '새 투표' })).toContainText('마감됨')
})

test('기능 설치는 해당 공간에만 반영되며 향후 예시는 읽기 중심이다', async ({ page }) => {
  await expect(menu(page).getByRole('button', { name: '비용 정산', exact: true })).toBeHidden()
  await tab(page, '기능 추가')
  await page
    .locator('.module-card')
    .filter({ has: page.getByRole('heading', { name: '비용 정산', exact: true }) })
    .getByRole('button', { name: '추가하기' })
    .click()
  await expect(menu(page).getByRole('button', { name: '비용 정산', exact: true })).toBeVisible()
  await page.reload()
  await expect(menu(page).getByRole('button', { name: '비용 정산', exact: true })).toBeVisible()
  await page
    .locator('.module-card')
    .filter({ has: page.getByRole('heading', { name: 'AI 기능 추천', exact: true }) })
    .getByRole('button', { name: '예시 보기' })
    .click()
  await expect(page.getByRole('dialog')).toContainText(
    '실제 분석 결과가 아닌 고정된 화면 예시입니다.',
  )
  await page.getByRole('dialog').getByRole('button', { name: '닫기', exact: true }).click()
  await page
    .locator('.module-card')
    .filter({ has: page.getByRole('heading', { name: '실시간 공동 편집', exact: true }) })
    .getByRole('button', { name: '예시 보기' })
    .click()
  await expect(page.getByLabel('공동 편집 예시')).toHaveAttribute('readonly', '')
  await page.getByRole('dialog').getByRole('button', { name: '닫기', exact: true }).click()
  await page
    .getByRole('complementary', { name: '공간 선택' })
    .getByRole('button', { name: '둘째 공간', exact: true })
    .click()
  await expect(menu(page).getByRole('button', { name: '비용 정산', exact: true })).toBeHidden()
})
