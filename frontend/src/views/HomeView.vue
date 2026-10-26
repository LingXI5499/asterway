<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError, apiRequest } from '../api/http'
import {
  clearPendingOperation,
  createIntent,
  detectTimeZone,
  errorText,
  passwordStrength,
  readPendingOperation,
  rememberPendingOperation,
  resultUncertain,
  sendIntent,
  validateNickname,
  validatePassword,
  validateUsername,
} from '../api/account'

const router = useRouter()
const snapshot = ref(null)
const account = computed(() => snapshot.value?.account || null)
const loading = ref(true)
const busy = ref('')
const errorMessage = ref('')
const message = ref('')
const username = ref('')
const nickname = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordConfirmed = ref(false)
const logoutConfirmed = ref(false)
const profileIntent = ref(null)
const passwordIntent = ref(null)
const zoneIntent = ref(null)
const logoutIntent = ref(null)
const pendingOperation = ref(readPendingOperation())
const passwordLevel = computed(() => passwordStrength(newPassword.value))
const hasPending = computed(() =>
  Boolean(profileIntent.value || passwordIntent.value || zoneIntent.value || logoutIntent.value),
)
let disposed = false
let automaticZoneAttempted = false

function updateAccount(value) {
  snapshot.value = { ...snapshot.value, account: value }
  username.value = value.username
  nickname.value = value.nickname
}

async function loadAccount(autoZone = true) {
  if (busy.value || disposed) {
    return
  }
  loading.value = true
  snapshot.value = null
  errorMessage.value = ''
  try {
    const current = await apiRequest('/auth/me')
    if (disposed) {
      return
    }
    snapshot.value = current
    username.value = current.account.username
    nickname.value = current.account.nickname
    const detected = detectTimeZone()
    const zone = current.account.timeZone
    if (
      autoZone &&
      !automaticZoneAttempted &&
      detected &&
      detected !== zone.activeZoneId &&
      detected !== zone.pendingZoneId
    ) {
      automaticZoneAttempted = true
      await synchronizeZone(true)
    }
  } catch (error) {
    await handleReadError(error)
  } finally {
    loading.value = false
  }
}

async function handleReadError(error) {
  if (disposed) {
    return
  }
  errorMessage.value = errorText(error)
  if (error instanceof ApiError && (error.status === 401 || error.code === 'ACCOUNT_UNAVAILABLE')) {
    snapshot.value = null
    const reason = error.code === 'ACCOUNT_UNAVAILABLE' ? 'account-unavailable' : 'session-expired'
    await router.replace({ path: '/login', query: { reason } })
  }
}

async function mutationFailed(error, intent, operation) {
  errorMessage.value =
    error instanceof ApiError || error instanceof TypeError ? errorText(error) : error.message
  if (intent.value && resultUncertain(error)) {
    rememberPendingOperation(account.value.id, intent.value.key, operation)
    pendingOperation.value = readPendingOperation()
    errorMessage.value += ' 结果尚未确认，请重试原请求或重新登录后核实结果。'
    return
  }
  intent.value = null
  if (error instanceof ApiError && (error.status === 401 || error.code === 'ACCOUNT_UNAVAILABLE')) {
    const unknown = readPendingOperation() !== null
    snapshot.value = null
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    const reason = unknown
      ? 'result-unknown'
      : error.code === 'ACCOUNT_UNAVAILABLE'
        ? 'account-unavailable'
        : 'session-expired'
    await router.replace({ path: '/login', query: { reason } })
  } else if (error.code === 'VERSION_CONFLICT') {
    message.value = '资料已在其他页面更新，请点击刷新资料后重新操作。'
  }
}

async function saveProfile() {
  if (busy.value) {
    return
  }
  errorMessage.value = ''
  message.value = ''
  try {
    if (!profileIntent.value) {
      validateUsername(username.value)
      validateNickname(nickname.value)
      profileIntent.value = createIntent({
        expectedVersion: account.value.version,
        username: username.value,
        nickname: nickname.value,
      })
    }
    busy.value = 'profile'
    const result = await sendIntent('/auth/profile', 'PATCH', profileIntent.value)
    updateAccount(result.account)
    message.value = result.changed ? '资料已保存。修改后的用户名立即用于登录。' : '资料没有变化。'
    profileIntent.value = null
    clearResolvedOperation('API-AUTH-007')
  } catch (error) {
    await mutationFailed(error, profileIntent, 'API-AUTH-007')
  } finally {
    busy.value = ''
  }
}

async function changePassword() {
  if (busy.value) {
    return
  }
  errorMessage.value = ''
  try {
    if (!passwordIntent.value) {
      validatePassword(newPassword.value)
      if (
        !currentPassword.value ||
        newPassword.value !== confirmPassword.value ||
        !passwordConfirmed.value
      ) {
        errorMessage.value = '请填写原密码，确认两次新密码一致，并勾选操作确认。'
        return
      }
      passwordIntent.value = createIntent({
        expectedVersion: account.value.version,
        currentPassword: currentPassword.value,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value,
        confirmed: true,
      })
    }
    busy.value = 'password'
    await sendIntent('/auth/password/change', 'POST', passwordIntent.value)
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    passwordIntent.value = null
    snapshot.value = null
    clearResolvedOperation('API-AUTH-008')
    await router.replace({ path: '/login', query: { reason: 'password-changed' } })
  } catch (error) {
    await mutationFailed(error, passwordIntent, 'API-AUTH-008')
  } finally {
    busy.value = ''
  }
}

async function synchronizeZone(automatic = false) {
  if (busy.value || !account.value) {
    return
  }
  const detected = detectTimeZone()
  if (!detected) {
    errorMessage.value = '未能自动识别设备时区，请检查浏览器设置后重试。'
    return
  }
  errorMessage.value = ''
  zoneIntent.value =
    zoneIntent.value ||
    createIntent({
      detectedZoneId: detected,
      expectedVersion: account.value.version,
    })
  busy.value = 'zone'
  try {
    const result = await sendIntent('/auth/device-time-zone', 'PUT', zoneIntent.value)
    updateAccount(result.account)
    message.value = result.changed
      ? '设备时区已同步，变更将在当前业务日结束后生效。'
      : automatic
        ? ''
        : '设备时区与当前设置一致。'
    zoneIntent.value = null
    clearResolvedOperation('API-AUTH-010')
  } catch (error) {
    await mutationFailed(error, zoneIntent, 'API-AUTH-010')
  } finally {
    busy.value = ''
  }
}

async function logout() {
  if (busy.value || !logoutConfirmed.value) {
    return
  }
  errorMessage.value = ''
  logoutIntent.value = logoutIntent.value || createIntent({ confirmed: true })
  busy.value = 'logout'
  try {
    await sendIntent('/auth/logout', 'POST', logoutIntent.value)
    logoutIntent.value = null
    snapshot.value = null
    clearResolvedOperation('API-AUTH-009')
    await router.replace('/login')
  } catch (error) {
    await mutationFailed(error, logoutIntent, 'API-AUTH-009')
  } finally {
    busy.value = ''
  }
}

function clearResolvedOperation(operation) {
  if (readPendingOperation()?.operation === operation) {
    clearPendingOperation()
    pendingOperation.value = null
  }
}

async function verifyPendingResult() {
  const pending = pendingOperation.value
  if (!pending || busy.value || pending.accountId !== account.value?.id) {
    return
  }
  busy.value = 'result'
  errorMessage.value = ''
  try {
    const result = await apiRequest(
      `/operations/${pending.key}?operation=${encodeURIComponent(pending.operation)}`,
    )
    if (result.state === 'SUCCEEDED') {
      message.value =
        pending.operation === 'API-AUTH-008' ? '上次密码修改已完成。' : '上次操作已完成。'
      clearPendingOperation()
      pendingOperation.value = null
    } else if (result.state === 'REJECTED') {
      message.value = '上次操作未通过，请刷新资料后重新操作。'
      clearPendingOperation()
      pendingOperation.value = null
    } else {
      message.value = '上次操作仍在处理中，请稍后再核实。'
    }
  } catch (error) {
    errorMessage.value =
      error.code === 'OPERATION_RESULT_NOT_FOUND'
        ? '暂未查询到有效结果，不能据此判断操作是否已发生。'
        : errorText(error)
  } finally {
    busy.value = ''
  }
}

async function loginAndVerify() {
  passwordIntent.value = null
  currentPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  // 这里只切换页面，不把仍有效的服务器会话说成已退出。
  await router.replace({ path: '/login', query: { reason: 'result-unknown' } })
}

function refreshWhenVisible() {
  if (document.visibilityState === 'visible' && !loading.value && !hasPending.value) {
    void loadAccount(false)
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', refreshWhenVisible)
  window.addEventListener('online', refreshWhenVisible)
  void loadAccount()
})

onUnmounted(() => {
  disposed = true
  snapshot.value = null
  currentPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  passwordIntent.value = null
  document.removeEventListener('visibilitychange', refreshWhenVisible)
  window.removeEventListener('online', refreshWhenVisible)
})
</script>

<template>
  <section class="auth-card account-card">
    <h1>账户中心</h1>
    <p v-if="loading">正在读取账户资料…</p>
    <template v-else-if="account">
      <dl>
        <dt>用户名</dt>
        <dd>{{ account.username }}</dd>
        <dt>昵称</dt>
        <dd>{{ account.nickname }}</dd>
        <dt>角色</dt>
        <dd>{{ account.role }}</dd>
        <dt>业务日期</dt>
        <dd>{{ snapshot.businessDate }}</dd>
        <dt>当前时区</dt>
        <dd>{{ account.timeZone.activeZoneId }}</dd>
      </dl>
      <p v-if="account.timeZone.pendingZoneId">
        待切换时区：{{ account.timeZone.pendingZoneId }}，生效时间：{{
          account.timeZone.pendingEffectiveAt
        }}
      </p>
      <p v-if="snapshot.hasUnfinishedExecutions" class="error">
        仍有未结束执行，请先结束这些执行后再修改密码或退出。
      </p>
      <div class="button-row">
        <button
          :disabled="Boolean(busy) || hasPending"
          class="secondary"
          @click="loadAccount(false)"
        >
          刷新资料
        </button>
        <button
          :disabled="Boolean(busy) || (hasPending && !zoneIntent)"
          class="secondary"
          @click="synchronizeZone(false)"
        >
          {{ zoneIntent ? '重试原时区同步' : '同步设备时区' }}
        </button>
      </div>
      <form @submit.prevent="saveProfile">
        <h2>修改资料</h2>
        <fieldset :disabled="Boolean(busy) || hasPending">
          <label>
            用户名
            <input v-model="username" required autocomplete="username" />
          </label>
          <label>
            昵称
            <input v-model="nickname" required />
          </label>
        </fieldset>
        <button :disabled="Boolean(busy) || (hasPending && !profileIntent)">
          {{ profileIntent ? '重试原保存请求' : '保存资料' }}
        </button>
      </form>
      <details>
        <summary>修改密码</summary>
        <form @submit.prevent="changePassword">
          <fieldset :disabled="Boolean(busy) || hasPending">
            <label>
              原密码
              <input
                v-model="currentPassword"
                type="password"
                required
                autocomplete="current-password"
              />
            </label>
            <label>
              新密码
              <input v-model="newPassword" type="password" required autocomplete="new-password" />
            </label>
            <p v-if="newPassword">密码强度：{{ passwordLevel }}（仅作提示）</p>
            <label>
              确认新密码
              <input
                v-model="confirmPassword"
                type="password"
                required
                autocomplete="new-password"
              />
            </label>
            <label class="checkbox-label">
              <input v-model="passwordConfirmed" type="checkbox" />
              我确认修改密码，并知晓所有设备都需要重新登录。
            </label>
          </fieldset>
          <button
            :disabled="
              Boolean(busy) ||
              (hasPending && !passwordIntent) ||
              (!passwordIntent && !passwordConfirmed)
            "
          >
            {{ passwordIntent ? '重试原改密请求' : '确认修改密码' }}
          </button>
        </form>
      </details>
      <fieldset :disabled="Boolean(busy) || (hasPending && !logoutIntent)">
        <legend>退出当前会话</legend>
        <label class="checkbox-label">
          <input v-model="logoutConfirmed" type="checkbox" />
          我确认退出当前设备。
        </label>
        <button :disabled="!logoutConfirmed" @click="logout">
          {{ logoutIntent ? '重试原退出请求' : '退出登录' }}
        </button>
      </fieldset>
      <div v-if="pendingOperation?.accountId === account.id" class="result-notice">
        <p>有一项操作的结果需要核实。</p>
        <button :disabled="Boolean(busy)" @click="verifyPendingResult">核实上次操作结果</button>
      </div>
      <button
        v-if="passwordIntent"
        class="secondary"
        :disabled="Boolean(busy)"
        @click="loginAndVerify"
      >
        转到登录页核实身份与上次结果
      </button>
    </template>
    <p v-if="message" class="success" role="status">{{ message }}</p>
    <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
    <button v-if="!loading && !account" @click="loadAccount(false)">重新读取资料</button>
  </section>
</template>
