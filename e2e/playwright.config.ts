import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E 配置。
 * 默认复用已启动的 backend(8080) 和 frontend(5173)；
 * 若未启动，可通过 reuseExistingServer: !process.env.CI 自动拉起（需 DEEPSEEK_API_KEY）。
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  timeout: 60_000,
  expect: { timeout: 15_000 },
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'mvn -q spring-boot:run',
      cwd: '../backend',
      url: 'http://localhost:8080/api/bookings?status=UNSUBSCRIBED',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
      env: {
        DEEPSEEK_API_KEY: process.env.DEEPSEEK_API_KEY ?? '',
      },
    },
    {
      command: 'pnpm run dev',
      cwd: '../frontend',
      url: 'http://localhost:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
})
