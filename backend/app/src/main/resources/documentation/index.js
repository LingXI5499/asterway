// 继续使用应用自带的 Swagger UI 资源，不依赖外部 CDN。
const operationMethods = new Set([
  'get',
  'post',
  'put',
  'patch',
  'delete',
  'head',
  'options',
  'trace',
])
const statusElement = document.getElementById('documentation-status')

document.getElementById('reload-docs').addEventListener('click', () => window.location.reload())
document.getElementById('service-origin').textContent = window.location.host

function markActiveGroup() {
  let currentTag = ''
  try {
    currentTag = decodeURIComponent(window.location.hash.split('/')[1] || '')
  } catch {
    currentTag = ''
  }
  const links = document.querySelectorAll('#tag-navigation a')
  links.forEach((link, index) => {
    const active = link.dataset.tag === currentTag || (!currentTag && index === 0)
    link.classList.toggle('is-active', active)
    link.setAttribute('aria-current', active ? 'location' : 'false')
  })
}

window.addEventListener('hashchange', markActiveGroup)

function renderNavigation(specification) {
  const groups = new Map()
  let count = 0
  for (const path of Object.values(specification.paths || {})) {
    for (const [method, operation] of Object.entries(path)) {
      if (!operationMethods.has(method)) {
        continue
      }
      count++
      for (const tag of operation.tags || ['其他接口']) {
        groups.set(tag, (groups.get(tag) || 0) + 1)
      }
    }
  }
  const navigation = document.getElementById('tag-navigation')
  for (const [tag, total] of groups) {
    const link = document.createElement('a')
    link.href = `#/${encodeURIComponent(tag)}`
    link.dataset.tag = tag
    const label = document.createElement('span')
    label.textContent = tag
    const badge = document.createElement('span')
    badge.className = 'group-count'
    badge.textContent = total
    link.append(label, badge)
    navigation.append(link)
  }
  markActiveGroup()
  document.getElementById('endpoint-count').textContent = `${count} 个接口`
}

// 调试仅访问同源接口；令牌每次从当前会话获取，不持久化登录凭据。
async function attachCsrf(request) {
  const url = new URL(request.url, window.location.href)
  const method = (request.method || 'GET').toUpperCase()
  if (
    url.origin !== window.location.origin ||
    !url.pathname.startsWith('/api/v1/') ||
    ['GET', 'HEAD', 'OPTIONS'].includes(method)
  ) {
    return request
  }
  const response = await fetch('../api/v1/auth/csrf', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw new Error('无法获取 CSRF 令牌，请刷新页面后重试。')
  }
  const result = await response.json()
  if (result.code !== 'OK' || !result.data?.headerName || !result.data?.token) {
    throw new Error('当前会话的 CSRF 令牌不可用。')
  }
  request.headers = { ...request.headers, [result.data.headerName]: result.data.token }
  return request
}

async function initializeDocumentation() {
  try {
    const response = await fetch('../v3/api-docs', { credentials: 'same-origin' })
    if (!response.ok) {
      throw new Error('接口定义加载失败，请确认后端服务已经启动。')
    }
    const specification = await response.json()
    renderNavigation(specification)
    window.ui = SwaggerUIBundle({
      spec: specification,
      dom_id: '#swagger-ui',
      layout: 'BaseLayout',
      deepLinking: true,
      filter: true,
      docExpansion: 'list',
      defaultModelsExpandDepth: 0,
      defaultModelExpandDepth: 2,
      displayRequestDuration: true,
      operationsSorter: 'method',
      persistAuthorization: false,
      withCredentials: true,
      validatorUrl: null,
      syntaxHighlight: { activated: true, theme: 'idea' },
      requestInterceptor: attachCsrf,
      onComplete() {
        statusElement.hidden = true
      },
    })
  } catch (error) {
    statusElement.textContent = error.message || '文档暂时不可用，请稍后刷新。'
    statusElement.classList.add('is-error')
  }
}

void initializeDocumentation()
