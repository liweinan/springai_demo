/**
 * 通用 fetch 封装。
 * 所有 API 请求都通过本文件发出，统一处理 JSON 和错误。
 */

/**
 * 发送 GET 请求并解析 JSON。
 *
 * @param url 相对路径，例如 /api/bookings?status=SUBSCRIBED
 * @returns 解析后的 JSON 对象
 */
export async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }
  return response.json() as Promise<T>
}

/**
 * 发送 POST 请求（JSON body）并解析 JSON。
 *
 * @param url 相对路径
 * @param body 请求体对象，会自动 JSON.stringify
 * @returns 解析后的 JSON 对象
 */
export async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status} ${response.statusText}`)
  }
  return response.json() as Promise<T>
}
