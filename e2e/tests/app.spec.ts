import { expect, test, type Locator, type Page } from '@playwright/test'

/**
 * 订票聊天 Fullstack E2E 测试（串行执行，共享同一浏览器会话状态）。
 *
 * 依赖：
 * - backend :8080（H2 种子数据 3 条未订阅）
 * - frontend :5173
 * - DEEPSEEK_API_KEY（聊天相关用例）
 *
 * 聊天相关用例不假定 AI 会订/取消哪一张具体票，只断言列表数量与票在栏位间的移动。
 */

const subscribedColumn = (page: Page) => page.locator('.booking-column').first()
const unsubscribedColumn = (page: Page) => page.locator('.booking-column').nth(1)

async function getSubscribedTicketTitles(page: Page): Promise<string[]> {
  const items = subscribedColumn(page).locator('.booking-item.subscribed')
  const count = await items.count()
  if (count === 0) {
    return []
  }
  return items.allTextContents()
}

/** 从「北京-上海 G123」提取 G123，供 cancelSubscription 模糊匹配 */
function extractTicketCode(title: string): string {
  const match = title.match(/[A-Z]\d+/)
  if (!match) {
    throw new Error(`无法从票名提取取消关键词: ${title}`)
  }
  return match[0]
}

async function waitForChatIdle(sendButton: Locator) {
  await expect(sendButton).not.toHaveText('发送中...', { timeout: 60_000 })
}

async function sendChatMessage(page: Page, message: string) {
  const chatInput = page.getByPlaceholder('输入消息，例如：我要订票')
  const sendButton = page.getByRole('button', { name: '发送' })
  await chatInput.fill(message)
  await sendButton.click()
  await waitForChatIdle(sendButton)
  return sendButton
}

/** 若当前没有已订阅票，则发送「我要订票」并等待左栏出现 1 张票 */
async function ensureOneSubscribedTicket(page: Page): Promise<string> {
  let titles = await getSubscribedTicketTitles(page)
  if (titles.length === 1) {
    return titles[0]
  }

  await sendChatMessage(page, '我要订票')
  await expect(subscribedColumn(page).getByRole('heading', { name: /已订阅 \(1\)/ })).toBeVisible({
    timeout: 45_000,
  })

  titles = await getSubscribedTicketTitles(page)
  expect(titles).toHaveLength(1)
  return titles[0]
}

test.describe.serial('订票聊天页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: '订票聊天 Fullstack 学习项目' })).toBeVisible()
  })

  test('初始加载：显示 3 条未订阅票，已订阅为空', async ({ page }) => {
    await expect(subscribedColumn(page).getByRole('heading', { name: /已订阅 \(0\)/ })).toBeVisible()
    await expect(unsubscribedColumn(page).getByRole('heading', { name: /未订阅 \(3\)/ })).toBeVisible()

    await expect(unsubscribedColumn(page).getByText('北京-上海 G123')).toBeVisible()
    await expect(unsubscribedColumn(page).getByText('上海-深圳 D456')).toBeVisible()
    await expect(unsubscribedColumn(page).getByText('广州-北京 K789')).toBeVisible()

    await expect(subscribedColumn(page).getByText('暂无已订阅的票')).toBeVisible()
  })

  test('聊天订票：发送「我要订票」后左栏增加一张票', async ({ page }) => {
    test.skip(!process.env.DEEPSEEK_API_KEY, '需要 DEEPSEEK_API_KEY 环境变量')

    const unsubscribedBefore = await unsubscribedColumn(page).locator('.booking-item').allTextContents()
    expect(unsubscribedBefore).toHaveLength(3)

    await sendChatMessage(page, '我要订票')

    await expect(subscribedColumn(page).getByRole('heading', { name: /已订阅 \(1\)/ })).toBeVisible({
      timeout: 15_000,
    })
    await expect(unsubscribedColumn(page).getByRole('heading', { name: /未订阅 \(2\)/ })).toBeVisible()

    const subscribedTitle = (await getSubscribedTicketTitles(page))[0]
    expect(subscribedTitle).toBeTruthy()
    expect(unsubscribedBefore).toContain(subscribedTitle)
    await expect(unsubscribedColumn(page).getByText(subscribedTitle)).not.toBeVisible()
  })

  test('聊天取消：取消当前已订阅的票后回到右栏', async ({ page }) => {
    test.skip(!process.env.DEEPSEEK_API_KEY, '需要 DEEPSEEK_API_KEY 环境变量')

    const subscribedTitle = await ensureOneSubscribedTicket(page)
    const cancelKeyword = extractTicketCode(subscribedTitle)

    await sendChatMessage(page, `取消订票 ${cancelKeyword}`)
    await expect(page.locator('.chat-message.assistant').last()).toBeVisible({ timeout: 15_000 })

    await expect(subscribedColumn(page).getByRole('heading', { name: /已订阅 \(0\)/ })).toBeVisible({
      timeout: 15_000,
    })
    await expect(unsubscribedColumn(page).getByRole('heading', { name: /未订阅 \(3\)/ })).toBeVisible()
    await expect(unsubscribedColumn(page).getByText(subscribedTitle)).toBeVisible()
    await expect(subscribedColumn(page).getByText(subscribedTitle)).not.toBeVisible()
  })

  test('健康检查 API 可访问', async ({ request }) => {
    const response = await request.get('http://localhost:8080/api/health')
    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body).toHaveProperty('deepseekConfigured')
    expect(body).toHaveProperty('deepseekReachable')
  })
})
