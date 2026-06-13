import { expect, test } from '@playwright/test'

/**
 * 订票聊天 Fullstack E2E 测试（串行执行，共享同一浏览器会话状态）。
 *
 * 依赖：
 * - backend :8080（H2 种子数据 3 条未订阅）
 * - frontend :5173
 * - DEEPSEEK_API_KEY（聊天相关用例）
 */
test.describe.serial('订票聊天页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: '订票聊天 Fullstack 学习项目' })).toBeVisible()
  })

  test('初始加载：显示 3 条未订阅票，已订阅为空', async ({ page }) => {
    const subscribedColumn = page.locator('.booking-column').first()
    const unsubscribedColumn = page.locator('.booking-column').nth(1)

    await expect(subscribedColumn.getByRole('heading', { name: /已订阅 \(0\)/ })).toBeVisible()
    await expect(unsubscribedColumn.getByRole('heading', { name: /未订阅 \(3\)/ })).toBeVisible()

    await expect(unsubscribedColumn.getByText('北京-上海 G123')).toBeVisible()
    await expect(unsubscribedColumn.getByText('上海-深圳 D456')).toBeVisible()
    await expect(unsubscribedColumn.getByText('广州-北京 K789')).toBeVisible()

    await expect(subscribedColumn.getByText('暂无已订阅的票')).toBeVisible()
  })

  test('聊天订票：发送「我要订票」后左栏增加一张票', async ({ page }) => {
    test.skip(!process.env.DEEPSEEK_API_KEY, '需要 DEEPSEEK_API_KEY 环境变量')

    const chatInput = page.getByPlaceholder('输入消息，例如：我要订票')
    const sendButton = page.getByRole('button', { name: '发送' })

    await chatInput.fill('我要订票')
    await sendButton.click()

    // 等待请求完成：按钮不再显示「发送中...」，且列表已刷新
    await expect(sendButton).not.toHaveText('发送中...', { timeout: 60_000 })
    await expect(page.locator('.booking-column').first().getByRole('heading', { name: /已订阅 \(1\)/ })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.locator('.booking-column').first().getByText('北京-上海 G123')).toBeVisible()
    await expect(page.locator('.booking-column').nth(1).getByRole('heading', { name: /未订阅 \(2\)/ })).toBeVisible()
  })

  test('聊天取消：发送「取消订票 G123」后票回到右栏', async ({ page }) => {
    test.skip(!process.env.DEEPSEEK_API_KEY, '需要 DEEPSEEK_API_KEY 环境变量')

    // 前置：先订一张票（若上一用例未跑，此处保证有已订阅票）
    const subscribedHeading = page.locator('.booking-column').first().getByRole('heading')
    const headingText = await subscribedHeading.textContent()
    if (!headingText?.includes('已订阅 (1)')) {
      await page.getByPlaceholder('输入消息，例如：我要订票').fill('我要订票')
      await page.getByRole('button', { name: '发送' }).click()
      await expect(page.locator('.booking-column').first().getByRole('heading', { name: /已订阅 \(1\)/ })).toBeVisible({
        timeout: 45_000,
      })
    }

    await page.getByPlaceholder('输入消息，例如：我要订票').fill('取消订票 G123')
    const sendButton = page.getByRole('button', { name: /发送/ })
    await sendButton.click()

    await expect(sendButton).not.toHaveText('发送中...', { timeout: 60_000 })
    await expect(page.locator('.chat-message.assistant').last()).toBeVisible({ timeout: 15_000 })

    await expect(page.locator('.booking-column').first().getByRole('heading', { name: /已订阅 \(0\)/ })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.locator('.booking-column').nth(1).getByRole('heading', { name: /未订阅 \(3\)/ })).toBeVisible()
    await expect(page.locator('.booking-column').nth(1).getByText('北京-上海 G123')).toBeVisible()
  })

  test('健康检查 API 可访问', async ({ request }) => {
    const response = await request.get('http://localhost:8080/api/health')
    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body).toHaveProperty('deepseekConfigured')
    expect(body).toHaveProperty('deepseekReachable')
  })
})
