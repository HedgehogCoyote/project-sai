import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
const require = createRequire(new URL('../../../frontend/package.json', import.meta.url))
const { chromium } = require('@playwright/test')
const browser = await chromium.launch({ headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 940 }, deviceScaleFactor: 1 })
  await page.goto(new URL('./preview.html', import.meta.url).href)
  await page.screenshot({ path: fileURLToPath(new URL('./search-selected.png', import.meta.url)), fullPage: true })
  await page.goto(new URL('./preview.html#states', import.meta.url).href)
  await page.evaluate(() => document.body.classList.add('states-mode'))
  await page.screenshot({ path: fileURLToPath(new URL('./search-states.png', import.meta.url)), fullPage: true })
} finally { await browser.close() }
