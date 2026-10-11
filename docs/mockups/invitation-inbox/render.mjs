import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
const require = createRequire(new URL('../../../frontend/package.json', import.meta.url))
const { chromium } = require('@playwright/test')
const browser = await chromium.launch({ headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 950 }, deviceScaleFactor: 1 })
  await page.goto(new URL('./preview.html', import.meta.url).href)
  for (const [name, mode] of [['desktop', ''], ['deny-confirmation', 'deny'], ['accepted', 'accepted'], ['history', 'history'], ['states', 'states']]) {
    await page.evaluate(mode => window.setMode(mode), mode)
    await page.evaluate(async () => { await document.fonts.ready })
    await page.waitForTimeout(200)
    await page.screenshot({ path: fileURLToPath(new URL(`./${name}.png`, import.meta.url)), fullPage: true })
  }
  await page.setViewportSize({ width: 390, height: 844 })
  await page.evaluate(() => window.setMode(''))
  await page.waitForTimeout(200)
  if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw new Error('Mobile overflow')
  await page.screenshot({ path: fileURLToPath(new URL('./mobile.png', import.meta.url)), fullPage: true })
} finally { await browser.close() }
