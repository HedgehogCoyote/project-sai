import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
const require = createRequire(new URL('../../../frontend/package.json', import.meta.url))
const { chromium } = require('@playwright/test')
const browser = await chromium.launch({ headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 1000 }, deviceScaleFactor: 1 })
  for (const [name, hash] of [['editing', ''], ['conflict', '#conflict'], ['states', '#states']]) {
    await page.goto(new URL(`./preview.html${hash}`, import.meta.url).href)
    await page.evaluate((mode) => { document.body.className = mode }, hash.slice(1))
    await page.evaluate(async () => { await document.fonts.ready; await Promise.all([...document.images].map(img => img.decode())) })
    await page.waitForTimeout(250)
    await page.screenshot({ path: fileURLToPath(new URL(`./${name}.png`, import.meta.url)), fullPage: true })
  }
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(new URL('./preview.html', import.meta.url).href)
  await page.evaluate(() => { document.body.className = '' })
  await page.waitForTimeout(250)
  if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw new Error('Mobile overflow')
  await page.screenshot({ path: fileURLToPath(new URL('./mobile.png', import.meta.url)), fullPage: true })
} finally { await browser.close() }
