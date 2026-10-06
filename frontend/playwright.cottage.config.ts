import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  expect: { timeout: 10000 },
  workers: 2,
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:5193',
    headless: true,
    launchOptions: {
      args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--enable-webgl'],
    },
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1280, height: 900 } } },
  ],
})
