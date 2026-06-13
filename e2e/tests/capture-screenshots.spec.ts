/**
 * 生成 README 用截图，输出到 docs/screenshots/
 * 运行：DEEPSEEK_API_KEY=xxx pnpm run capture-screenshots
 */
import { expect, test } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const screenshotDir = path.join(
  fileURLToPath(new URL('.', import.meta.url)),
  '../../docs/screenshots',
)

test.describe.serial('README 截图', () => {
  test('01 初始页面', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: '订票聊天 Fullstack 学习项目' })).toBeVisible()
    await page.screenshot({
      path: path.join(screenshotDir, '01-initial-page.png'),
      fullPage: true,
    })
  })

  test('02 聊天订票 + 03 取消订票（同一会话）', async ({ page }) => {
    test.skip(!process.env.DEEPSEEK_API_KEY, '需要 DEEPSEEK_API_KEY')

    await page.goto('/')

    // 订票
    await page.getByPlaceholder('输入消息，例如：我要订票').fill('我要订票')
    await page.getByRole('button', { name: /发送/ }).click()
    await expect(page.locator('.booking-column').first().getByRole('heading', { name: /已订阅 \(1\)/ })).toBeVisible({
      timeout: 60_000,
    })
    await page.screenshot({
      path: path.join(screenshotDir, '02-after-booking.png'),
      fullPage: true,
    })

    // 取消：用第一张已订阅票的标题关键词
    const bookedTitle = await page.locator('.booking-column').first().locator('.booking-item').first().textContent()
    const keyword = bookedTitle?.match(/G\d+|D\d+|K\d+/)?.[0] ?? 'G123'
    await page.getByPlaceholder('输入消息，例如：我要订票').fill(`取消订票 ${keyword}`)
    await page.getByRole('button', { name: /发送/ }).click()
    await expect(page.locator('.booking-column').first().getByRole('heading', { name: /已订阅 \(0\)/ })).toBeVisible({
      timeout: 60_000,
    })
    await page.screenshot({
      path: path.join(screenshotDir, '03-after-cancel.png'),
      fullPage: true,
    })
  })
})
