import { ApiError, apiRequest } from './http'

let pendingOperation = null

// 同一次用户意图保留原键和请求参数，只在内存中保存，不写浏览器持久存储。
export function createIntent(body) {
  const intent = {
    key: crypto.randomUUID(),
    body: structuredClone(body),
  }
  return intent
}

export async function sendIntent(path, method, intent) {
  const result = await apiRequest(path, {
    method,
    body: intent.body,
    idempotencyKey: intent.key,
  })
  return result
}

export function resultUncertain(error) {
  const uncertain = !(error instanceof ApiError) || error.status >= 500
  return uncertain
}

export function errorText(error) {
  if (error instanceof ApiError) {
    const violations = error.data?.violations
    if (Array.isArray(violations) && violations.length > 0) {
      const message = violations.map((item) => item.message).join('；')
      return message
    }
    const seconds = error.data?.retryAfterSeconds
    const message = seconds ? `${error.message}（请等待约 ${seconds} 秒）` : error.message
    return message
  }
  return '网络连接失败，请稍后重试。'
}

export function detectTimeZone() {
  try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
    return zone || ''
  } catch {
    return ''
  }
}

export function validateUsername(value) {
  const length = Array.from(value).length
  if (length < 1 || length > 30 || !/^[A-Za-z0-9\p{Script=Han}]+$/u.test(value)) {
    throw new Error('用户名须为1到30个字符，只能使用英文字母、数字和汉字。')
  }
}

export function validateNickname(value) {
  const length = Array.from(value).length
  if (length < 1 || length > 30 || !value.trim()) {
    throw new Error('昵称须为1到30个字符，不能全为空白。')
  }
}

export function validatePassword(value) {
  const length = Array.from(value).length
  if (length < 8 || length > 50 || !value.trim()) {
    throw new Error('密码须为8到50个字符，不能全为空白。')
  }
}

// 强度仅作提示，不作为拒绝密码的依据。
export function passwordStrength(value) {
  let kinds = 0
  kinds += /[a-z]/.test(value) ? 1 : 0
  kinds += /[A-Z]/.test(value) ? 1 : 0
  kinds += /[0-9]/.test(value) ? 1 : 0
  kinds += /[^A-Za-z0-9]/u.test(value) ? 1 : 0
  const names = ['弱', '弱', '低', '高', '强']
  const strength = names[kinds]
  return strength
}

export function rememberPendingOperation(accountId, key, operation) {
  pendingOperation = { accountId, key, operation }
}

export function readPendingOperation() {
  return pendingOperation
}

export function clearPendingOperation() {
  pendingOperation = null
}
