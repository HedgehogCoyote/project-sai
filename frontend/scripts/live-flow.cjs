// Live full-flow check against a running backend (8080) and Vite (5173) with a real database.
// Usage: node scripts/live-flow.cjs   (PW_CHROMIUM=/path/to/chrome to use a specific browser)
const { chromium } = require('@playwright/test');
const out = process.env.OUT_DIR || require('os').tmpdir() + '/sai-live-flow';
require('fs').mkdirSync(out, { recursive: true });
const BASE = 'http://localhost:5173';
const stamp = Date.now().toString().slice(-6);
const log = (...a) => console.log('•', ...a);

async function signup(page, who) {
  await page.fill('#name', who.name);
  await page.fill('#loginId', who.loginId);
  await page.fill('#email', who.loginId + '@example.com');
  await page.fill('#phoneNumber', '01012345678');
  await page.fill('#password', 'password123');
  await page.click('form button[type=submit]');
}
async function tab(page, label) {
  await page.locator('nav.content-tabs button', { hasText: label }).first().click();
}

(async () => {
  const browser = await chromium.launch({ executablePath: process.env.PW_CHROMIUM || undefined, args: ['--headless=new', '--no-sandbox', '--use-gl=swiftshader', '--enable-unsafe-swiftshader'] });
  const A = await (await browser.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
  const B = await (await browser.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
  for (const p of [A, B]) p.on('pageerror', e => console.log('PAGEERROR', e.message));
  const step = async (name, fn) => {
    try { await fn(); log('ok', name); }
    catch (e) { console.log('FAIL', name, e.message.split('\n')[0]); await A.screenshot({ path: `${out}/fail-A.png`, fullPage: true }); await B.screenshot({ path: `${out}/fail-B.png`, fullPage: true }); await browser.close(); process.exit(1); }
  };
  const a = { name: '지민', loginId: 'jimin' + stamp }, b = { name: '민재', loginId: 'minjae' + stamp };

  await step('A signs up', async () => { await A.goto(BASE + '/signup'); await signup(A, a); await A.waitForURL('**/spaces'); });
  await step('A creates a space', async () => {
    await A.getByRole('button', { name: '＋ 새 공간 만들기' }).first().click();
    await A.fill('#spaceTitle', '부산 동기');
    await A.getByRole('button', { name: '공간 만들기', exact: true }).click();
    await A.locator('h1', { hasText: '부산 동기' }).waitFor();
  });
  await step('A sets relation and colours', async () => {
    await tab(A, '설정');
    await A.getByRole('radio', { name: /친구/ }).click();
    await A.getByRole('radio', { name: /살구·하늘/ }).click();
    await A.getByRole('button', { name: '설정 저장' }).click();
    await A.getByText('공간 설정을 저장했습니다.').waitFor();
  });
  let link;
  await step('A creates an invite link', async () => {
    await tab(A, '사람');
    await A.getByRole('button', { name: '링크 만들기' }).click();
    link = (await A.locator('.ws-link-url').first().textContent()).trim();
    if (!link.includes('/invite/')) throw new Error('bad link ' + link);
  });
  await step('B opens the link, signs up and joins', async () => {
    await B.goto(link);
    await B.locator('h1', { hasText: '부산 동기' }).waitFor();
    await B.screenshot({ path: `${out}/join-preview.png` });
    await B.getByRole('link', { name: '처음이라면 가입하기' }).click();
    await signup(B, b);
    await B.waitForURL('**/invite/**');
    await B.getByRole('button', { name: /님으로 들어가기/ }).click();
    await B.locator('h1', { hasText: '부산 동기' }).waitFor();
  });
  await step('A creates an activity through the UI', async () => {
    await tab(A, '활동');
    await A.getByRole('button', { name: '＋ 활동 만들기' }).click();
    await A.fill('#activity-title', '부산 여행');
    await A.getByRole('button', { name: '다음: 사용할 기능' }).click();
    await A.getByRole('button', { name: '활동 만들기', exact: true }).click();
    await A.locator('h1', { hasText: '부산 여행' }).waitFor();
  });
  await step('B sees the shared activity after reload', async () => {
    await B.reload();
    await tab(B, '활동');
    await B.getByText('부산 여행').first().waitFor();
  });
  await step('B uploads a photo to the album', async () => {
    await B.screenshot({ path: `${out}/sample-photo.png`, clip: { x: 0, y: 0, width: 400, height: 300 } });
    await tab(B, '사진첩');
    await B.locator('input[type=file]').setInputFiles(`${out}/sample-photo.png`);
    await B.locator('.ws-photo img').first().waitFor();
  });
  await step('A gets a notification and unread badge', async () => {
    await A.goto(BASE + '/spaces');
    await A.getByRole('button', { name: /알림/ }).locator('.inbox-badge').waitFor();
    await A.getByRole('button', { name: /알림/ }).click();
    await A.getByText(`${b.name}님이 사진을 올렸습니다.`).first().waitFor();
    await A.screenshot({ path: `${out}/notifications.png` });
  });
  await step('A posts a pinned notice; B sees it on the space home', async () => {
    await A.getByText(`${b.name}님이 사진을 올렸습니다.`).first().click();
    await tab(A, '공지');
    await A.fill('textarea', '토요일 2시 해운대역 2번 출구');
    await A.getByRole('button', { name: '공지 올리기' }).click();
    await A.locator('.ws-card', { hasText: '토요일 2시' }).waitFor();
    await B.goto(BASE + '/spaces');
    await B.getByRole('button', { name: '부산 동기 들어가기' }).click();
    await B.locator('.pinned', { hasText: '토요일 2시' }).waitFor();
  });
  await step('B comments on the activity', async () => {
    await tab(B, '활동');
    await B.getByRole('button', { name: '활동 열기' }).first().click();
    await B.getByLabel('댓글 입력').fill('저도 갈게요!');
    await B.getByRole('button', { name: '남기기' }).last().click();
    await B.locator('.ws-comment', { hasText: '저도 갈게요!' }).waitFor();
  });
  await step('A completes the activity and publishes a Foot Print with a polaroid', async () => {
    await A.goto(BASE + '/spaces');
    await A.getByRole('button', { name: '부산 동기 들어가기' }).click();
    await tab(A, '활동');
    await A.getByRole('button', { name: '활동 열기' }).first().click();
    await A.locator('.ws-comment', { hasText: '저도 갈게요!' }).waitFor();
    await A.getByRole('button', { name: '완료로 표시' }).click();
    await A.getByRole('button', { name: 'Foot Print 만들기' }).click();
    await A.locator('input[type=file]').setInputFiles(`${out}/sample-photo.png`);
    await A.locator('.ws-photo img').first().waitFor();
    await A.locator('.ws-photos .ws-photo').first().click();
    await A.getByRole('button', { name: /고른 사진 1장 저장/ }).click();
    await A.getByLabel('한 줄 후기').fill('새벽 광안대교 보면서 먹은 컵라면');
    await A.getByRole('button', { name: '남기기' }).first().click();
    await A.locator('.ws-comment', { hasText: '컵라면' }).waitFor();
    await A.getByRole('radio', { name: /폴라로이드/ }).click();
    await A.locator('label.ws-label .ws-photo').first().click();
    await A.getByRole('button', { name: '게시하고 방에 놓기' }).click();
    await A.getByText('공간 방 선반에 놓였습니다').waitFor();
    await A.screenshot({ path: `${out}/footprint.png`, fullPage: true });
  });
  await step('B sees the souvenir in the room and opens the Foot Print from it', async () => {
    await B.goto(BASE + '/spaces');
    await B.getByRole('button', { name: '부산 동기 들어가기' }).click();
    const souvenir = B.locator('.three-object-label.souvenir', { hasText: '부산 여행' });
    await souvenir.waitFor({ timeout: 30000 });
    await B.waitForTimeout(1500);
    await B.locator('.three-room').screenshot({ path: `${out}/room-souvenir.png` });
    await souvenir.click();
    await B.getByText('새벽 광안대교').waitFor();
  });
  await step('A makes B a manager', async () => {
    await A.goto(BASE + '/spaces');
    await A.getByRole('button', { name: '부산 동기 들어가기' }).click();
    await tab(A, '사람');
    await A.getByLabel(`${b.name} 역할`).selectOption('MANAGER');
    await A.getByText(`${b.name}님의 역할을 바꿨습니다.`).waitFor();
    await A.screenshot({ path: `${out}/members.png`, fullPage: true });
  });
  await step('A finds the memory in the archive and takes the souvenir off the shelf', async () => {
    await tab(A, '추억');
    const card = A.locator('.memory', { hasText: '부산 여행' });
    await card.waitFor();
    await card.getByRole('button', { name: /선반에서 내리기/ }).click();
    await card.getByRole('button', { name: /선반에 놓기/ }).waitFor();
    await A.screenshot({ path: `${out}/memories.png`, fullPage: true });
    await tab(A, '공간 홈');
    await A.locator('.three-room[data-ready="true"]').waitFor({ timeout: 30000 });
    if (await A.locator('.three-object-label.souvenir').count()) throw new Error('souvenir still on the shelf');
  });
  await step('A space list shows colours', async () => {
    await A.goto(BASE + '/spaces');
    await A.locator('.space-row .ws-mark').first().waitFor();
    await A.screenshot({ path: `${out}/space-list.png` });
  });
  await browser.close();
  log('ALL PASSED');
})();
