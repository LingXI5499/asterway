// 后端 API 的统一前缀，所有请求都从这里开始
const API_PREFIX = '/api/v1'

// 保留状态码和业务错误码，页面据此区分身份失效与可重试错误。
export class ApiError extends Error {
  constructor(message, status, code, data, traceId) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.data = data
    this.traceId = traceId
  }
}

// 从浏览器 Cookie 里按名字读取值
function getCookie(name) {
  const prefix = `${name}=`

  // 在 document.cookie 里找以 "name=" 开头的那一项
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(prefix))

  if (!cookie) {
    return null
  }

  // 去掉前缀，返回解码后的值
  return decodeURIComponent(cookie.substring(prefix.length))
}

// 初始化 CSRF Token：调一次后端接口，让后端把 XSRF-TOKEN 写进 Cookie
let csrfInitialization = null

export async function ensureCsrf() {
  if (!csrfInitialization) {
    csrfInitialization = initializeCsrf().finally(() => {
      csrfInitialization = null
    })
  }
  await csrfInitialization
}

async function initializeCsrf() {
  const response = await fetch(`${API_PREFIX}/auth/csrf`, {
    method: 'GET',
    credentials: 'same-origin',
  })

  const result = await response.json()

  if (!response.ok || result.code !== 'OK') {
    throw new Error(result.message || 'CSRF 初始化失败')
  }
}

// 统一的请求封装，所有页面都通过它发请求。
// 自动处理：CSRF Token、Content-Type、统一响应解析、错误抛出。
export async function apiRequest(
  path,
  { method = 'GET', body, idempotencyKey, headers = {} } = {},
) {
  const verb = method.toUpperCase()

  // 判断是否为"写"请求（需要 CSRF 保护的请求）
  const isWrite = !['GET', 'HEAD', 'OPTIONS'].includes(verb)

  // 写请求且还没有 CSRF Token 时，先初始化一次
  if (isWrite && !getCookie('XSRF-TOKEN')) {
    await ensureCsrf()
  }

  const requestHeaders = new Headers(headers)

  requestHeaders.set('Accept', 'application/json')

  // 有请求体时声明 JSON 格式
  if (body !== undefined) {
    requestHeaders.set('Content-Type', 'application/json')
  }

  // 写请求统一从 Cookie 读 token 放进 X-CSRF-TOKEN 请求头
  if (isWrite) {
    const csrfToken = getCookie('XSRF-TOKEN')

    if (!csrfToken) {
      throw new Error('缺少 CSRF Token，请刷新页面重试')
    }

    requestHeaders.set('X-CSRF-TOKEN', csrfToken)
    if (!requestHeaders.has('Idempotency-Key')) {
      requestHeaders.set('Idempotency-Key', idempotencyKey || crypto.randomUUID())
    }
  }

  // 发起请求
  const response = await fetch(`${API_PREFIX}${path}`, {
    method: verb,
    headers: requestHeaders,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  // 解析统一响应结构 { code, message, data, traceId }
  const result = await response.json().catch(() => null)

  // 非 2xx 或业务 code 不是 OK 都视为失败，抛出带文案的错误
  if (!response.ok || !result || result.code !== 'OK') {
    throw new ApiError(
      result?.message || `请求失败，HTTP ${response.status}`,
      response.status,
      result?.code,
      result?.data,
      result?.traceId,
    )
  }

  // 成功时只返回业务数据 data
  return result.data
}
