<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CaptchaPuzzle from '../components/CaptchaPuzzle.vue'
import { ApiError, apiRequest } from '../api/http'
import {
  createIntent,
  errorText,
  resultUncertain,
  sendIntent,
  validateUsername,
} from '../api/account'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const proof = ref(null)
const captcha = ref(null)
const loading = ref(false)
const checking = ref(true)
const registrationOpen = ref(true)
const siteName = ref('Asterway')
const errorMessage = ref('')
const notice =
  new Map([
    ['account-unavailable', '账户不可用，请联系管理员。'],
    ['session-expired', '登录状态已失效，请重新登录。'],
    ['password-changed', '密码修改成功，请使用新密码登录。'],
    ['result-unknown', '上次操作结果尚未确认。登录后可核实原操作结果。'],
  ]).get(route.query.reason) || ''
const pending = ref(null)

onMounted(async () => {
  try {
    const bootstrap = await apiRequest('/public/bootstrap')
    siteName.value = bootstrap.siteName
    registrationOpen.value = bootstrap.registrationOpen
    await apiRequest('/auth/me')
    await router.replace('/home')
  } catch (error) {
    if (!(error instanceof ApiError) || ![401, 403].includes(error.status)) {
      errorMessage.value = errorText(error)
    }
  } finally {
    checking.value = false
  }
})

async function submit() {
  if (loading.value) {
    return
  }
  errorMessage.value = ''
  try {
    if (!pending.value) {
      validateUsername(username.value)
      if (!password.value || Array.from(password.value).length > 50 || !proof.value) {
        errorMessage.value = '请输入密码并完成滑动验证。'
        return
      }
      pending.value = createIntent({
        username: username.value,
        password: password.value,
        captchaChallengeId: proof.value.challengeId,
        captchaVerificationToken: proof.value.verificationToken,
      })
    }
    loading.value = true
    await sendIntent('/auth/login', 'POST', pending.value)
    password.value = ''
    pending.value = null
    await router.replace('/home')
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError || error instanceof TypeError ? errorText(error) : error.message
    if (resultUncertain(error) && pending.value) {
      // Cookie 已建立而响应丢失时，按当前服务端身份核实，不从去重结果重发 Cookie。
      try {
        await apiRequest('/auth/me')
        password.value = ''
        pending.value = null
        await router.replace('/home')
        return
      } catch {
        errorMessage.value += ' 结果尚未确认，可使用原操作标识重试。'
      }
    } else {
      pending.value = null
      await captcha.value?.reset()
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-shell">
    <el-card class="auth-card" shadow="never">
      <p class="eyebrow">欢迎回来</p>
      <h1>登录 {{ siteName }}</h1>
      <p class="page-description">登录后管理你的账户资料与安全设置。</p>
      <el-alert
        v-if="route.query.registered === '1'"
        title="注册成功，请登录。"
        type="success"
        show-icon
        :closable="false"
      />
      <el-alert v-if="notice" :title="notice" type="info" show-icon :closable="false" />
      <el-skeleton v-if="checking" :rows="4" animated />
      <el-form v-else class="account-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" for="login-username" required>
          <el-input
            id="login-username"
            v-model="username"
            autocomplete="username"
            placeholder="请输入用户名"
            size="large"
            :disabled="loading || Boolean(pending)"
            required
          />
        </el-form-item>
        <el-form-item label="密码" for="login-password" required>
          <el-input
            id="login-password"
            v-model="password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
            size="large"
            :disabled="loading || Boolean(pending)"
            required
          />
        </el-form-item>
        <CaptchaPuzzle
          ref="captcha"
          v-model="proof"
          purpose="LOGIN"
          :disabled="loading || Boolean(pending)"
        />
        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
        />
        <el-button
          class="full-width"
          type="primary"
          size="large"
          native-type="submit"
          :loading="loading"
          :disabled="loading || (!proof && !pending)"
        >
          {{ pending ? '重试原登录请求' : '登录' }}
        </el-button>
      </el-form>
      <p v-if="registrationOpen" class="switch-link">
        还没有账户？
        <RouterLink to="/register">创建账户</RouterLink>
      </p>
      <p v-else class="switch-link muted">当前未开放注册，已有账户仍可登录。</p>
    </el-card>
  </section>
</template>
