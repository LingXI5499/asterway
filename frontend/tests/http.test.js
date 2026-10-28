import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { ApiError, apiRequest } from '../src/api/http.js'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
})

// 页面依赖这些元数据区分重新登录、账户停用和服务临时异常。
test('保留本人资料查询的错误状态和业务错误码', async () => {
  const cases = [
    [401, 'AUTH_REQUIRED'],
    [403, 'ACCOUNT_UNAVAILABLE'],
    [503, 'SERVICE_UNAVAILABLE']
  ]

  for (const [status, code] of cases) {
    globalThis.fetch = async () => {
      const body = JSON.stringify({ code, message: '读取失败', data: null })
      const response = new Response(body, { status })
      return response
    }

    await assert.rejects(apiRequest('/auth/me'), error => {
      assert.ok(error instanceof ApiError)
      assert.equal(error.status, status)
      assert.equal(error.code, code)
      return true
    })
  }
})

test('网络故障保留为可重试的技术异常', async () => {
  globalThis.fetch = async () => {
    throw new TypeError('网络不可用')
  }

  await assert.rejects(apiRequest('/auth/me'), error => {
    assert.equal(error instanceof ApiError, false)
    assert.equal(error.message, '网络不可用')
    return true
  })
})

test('成功读取资料时使用同源会话并只返回业务数据', async () => {
  const account = { accountId: 'account-1', username: 'CurrentName', nickname: '最新昵称' }
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/v1/auth/me')
    assert.equal(options.credentials, 'same-origin')
    const body = JSON.stringify({ code: 'OK', data: account })
    const response = new Response(body, { status: 200 })
    return response
  }

  const result = await apiRequest('/auth/me')
  assert.deepEqual(result, account)
})
