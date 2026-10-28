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
const activeTab = ref('profile')
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
  <section class="account-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">个人设置</p>
        <h1>账户中心</h1>
        <p class="page-description">管理基本资料、账户安全和设备时区。</p>
      </div>
      <el-button
        v-if="account"
        :disabled="Boolean(busy) || hasPending || loading"
        @click="loadAccount(false)"
      >
        刷新资料
      </el-button>
    </div>
    <el-alert v-if="message" :title="message" type="success" show-icon :closable="false" />
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
    <el-card v-if="loading" shadow="never"><el-skeleton :rows="6" animated /></el-card>
    <template v-else-if="account">
      <el-card class="account-overview" shadow="never">
        <div class="identity-heading">
          <el-avatar :size="56">
            {{ Array.from(account.nickname || account.username)[0] }}
          </el-avatar>
          <div>
            <h2>{{ account.nickname }}</h2>
            <p class="muted">{{ account.username }}</p>
          </div>
          <el-tag effect="light">{{ account.role === 'USER' ? '普通用户' : account.role }}</el-tag>
        </div>
        <el-descriptions :column="1" class="overview-facts">
          <el-descriptions-item label="业务日期">{{ snapshot.businessDate }}</el-descriptions-item>
          <el-descriptions-item label="当前时区">
            {{ account.timeZone.activeZoneId }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-alert
        v-if="account.timeZone.pendingZoneId"
        :title="`待切换时区：${account.timeZone.pendingZoneId}`"
        :description="`生效时间：${account.timeZone.pendingEffectiveAt}`"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert
        v-if="snapshot.hasUnfinishedExecutions"
        title="仍有未结束执行"
        description="请先结束这些执行后，再修改密码或退出。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-card class="settings-card" shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本资料" name="profile">
            <div class="section-heading">
              <h2>基本资料</h2>
              <p>修改用户名与昵称，更新后立即生效。</p>
            </div>
            <el-form class="settings-form" label-position="top" @submit.prevent="saveProfile">
              <el-form-item label="用户名" for="profile-username" required>
                <el-input
                  id="profile-username"
                  v-model="username"
                  autocomplete="username"
                  :disabled="Boolean(busy) || hasPending"
                  required
                />
                <p class="field-note">修改后的用户名将用于下一次登录，当前会话继续有效。</p>
              </el-form-item>
              <el-form-item label="昵称" for="profile-nickname" required>
                <el-input
                  id="profile-nickname"
                  v-model="nickname"
                  :disabled="Boolean(busy) || hasPending"
                  required
                />
              </el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="busy === 'profile'"
                :disabled="Boolean(busy) || (hasPending && !profileIntent)"
              >
                {{ profileIntent ? '重试原保存请求' : '保存资料' }}
              </el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="账户安全" name="security">
            <div class="section-heading">
              <h2>修改密码</h2>
              <p>密码修改成功后，所有设备都需要重新登录。</p>
            </div>
            <el-form class="settings-form" label-position="top" @submit.prevent="changePassword">
              <el-form-item label="原密码" for="current-password" required>
                <el-input
                  id="current-password"
                  v-model="currentPassword"
                  type="password"
                  show-password
                  autocomplete="current-password"
                  :disabled="Boolean(busy) || hasPending"
                  required
                />
              </el-form-item>
              <el-form-item label="新密码" for="new-password" required>
                <el-input
                  id="new-password"
                  v-model="newPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="8～50 个字符"
                  :disabled="Boolean(busy) || hasPending"
                  required
                />
                <p v-if="newPassword" class="field-note">
                  密码强度：{{ passwordLevel }}，仅作提示。
                </p>
              </el-form-item>
              <el-form-item label="确认新密码" for="confirm-new-password" required>
                <el-input
                  id="confirm-new-password"
                  v-model="confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  :disabled="Boolean(busy) || hasPending"
                  required
                />
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="passwordConfirmed" :disabled="Boolean(busy) || hasPending">
                  我确认修改密码，并知晓所有设备都需要重新登录。
                </el-checkbox>
              </el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="busy === 'password'"
                :disabled="
                  Boolean(busy) ||
                  (hasPending && !passwordIntent) ||
                  (!passwordIntent && !passwordConfirmed)
                "
              >
                {{ passwordIntent ? '重试原改密请求' : '确认修改密码' }}
              </el-button>
            </el-form>
            <el-divider />
            <div class="section-heading">
              <h2>退出当前设备</h2>
              <p>仅结束当前设备的登录会话，不影响其他设备。</p>
            </div>
            <div class="logout-panel">
              <el-checkbox
                v-model="logoutConfirmed"
                :disabled="Boolean(busy) || (hasPending && !logoutIntent)"
              >
                我确认退出当前设备。
              </el-checkbox>
              <el-button
                type="danger"
                plain
                :loading="busy === 'logout'"
                :disabled="!logoutConfirmed || Boolean(busy) || (hasPending && !logoutIntent)"
                @click="logout"
              >
                {{ logoutIntent ? '重试原退出请求' : '退出登录' }}
              </el-button>
            </div>
          </el-tab-pane>
          <el-tab-pane label="时区设置" name="timezone">
            <div class="section-heading">
              <h2>设备时区</h2>
              <p>设备时区变更将在当前业务日结束后生效，保留原有历史记录。</p>
            </div>
            <el-descriptions :column="1" border class="zone-details">
              <el-descriptions-item label="当前生效时区">
                {{ account.timeZone.activeZoneId }}
              </el-descriptions-item>
              <el-descriptions-item label="设备识别时区">
                {{ detectTimeZone() || '未能识别' }}
              </el-descriptions-item>
              <el-descriptions-item label="待生效时区">
                {{ account.timeZone.pendingZoneId || '暂无变更' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-button
              type="primary"
              plain
              :loading="busy === 'zone'"
              :disabled="Boolean(busy) || (hasPending && !zoneIntent)"
              @click="synchronizeZone(false)"
            >
              {{ zoneIntent ? '重试原时区同步' : '同步设备时区' }}
            </el-button>
          </el-tab-pane>
        </el-tabs>
      </el-card>
      <el-alert
        v-if="pendingOperation?.accountId === account.id"
        title="有一项操作的结果需要核实"
        type="warning"
        :closable="false"
        show-icon
      >
        <el-button
          link
          type="primary"
          :loading="busy === 'result'"
          :disabled="Boolean(busy)"
          @click="verifyPendingResult"
        >
          核实上次操作结果
        </el-button>
      </el-alert>
      <el-button v-if="passwordIntent" :disabled="Boolean(busy)" @click="loginAndVerify">
        转到登录页核实身份与上次结果
      </el-button>
    </template>
    <el-card v-else shadow="never">
      <el-empty description="暂时无法读取账户资料" :image-size="100">
        <el-button type="primary" @click="loadAccount(false)">重新读取资料</el-button>
      </el-empty>
    </el-card>
  </section>
</template>
