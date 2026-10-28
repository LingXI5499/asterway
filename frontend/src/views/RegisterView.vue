<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CaptchaPuzzle from '../components/CaptchaPuzzle.vue'
import TimeZoneSelect from '../components/TimeZoneSelect.vue'
import { ApiError, apiRequest } from '../api/http'
import {
  createIntent,
  detectTimeZone,
  errorText,
  passwordStrength,
  resultUncertain,
  sendIntent,
  validateNickname,
  validatePassword,
  validateUsername,
} from '../api/account'

const router = useRouter()
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const zoneId = ref(detectTimeZone())
const proof = ref(null)
const captcha = ref(null)
const loading = ref(false)
const checking = ref(true)
const registrationOpen = ref(false)
const errorMessage = ref('')
const pending = ref(null)
const strength = computed(() => passwordStrength(password.value))

onMounted(async () => {
  try {
    const bootstrap = await apiRequest('/public/bootstrap')
    registrationOpen.value = bootstrap.registrationOpen
  } catch (error) {
    errorMessage.value = errorText(error)
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
      validatePassword(password.value)
      if (nickname.value !== '') {
        validateNickname(nickname.value)
      }
      if (confirmPassword.value !== password.value) {
        errorMessage.value = '两次输入的密码不一致。'
        return
      }
      if (!zoneId.value || !proof.value) {
        errorMessage.value = '请选择有效时区并完成滑动验证。'
        return
      }
      const body = {
        username: username.value,
        password: password.value,
        confirmPassword: confirmPassword.value,
        zoneId: zoneId.value,
        captchaChallengeId: proof.value.challengeId,
        captchaVerificationToken: proof.value.verificationToken,
      }
      if (nickname.value !== '') {
        body.nickname = nickname.value
      }
      pending.value = createIntent(body)
    }
    loading.value = true
    await sendIntent('/auth/register', 'POST', pending.value)
    password.value = ''
    confirmPassword.value = ''
    pending.value = null
    await router.replace({ path: '/login', query: { registered: '1' } })
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError || error instanceof TypeError ? errorText(error) : error.message
    if (resultUncertain(error) && pending.value) {
      errorMessage.value += ' 结果尚未确认，请重试原注册请求，勿重复创建账户。'
    } else {
      pending.value = null
      if (error.code === 'REGISTRATION_CLOSED') {
        registrationOpen.value = false
      }
      await captcha.value?.reset()
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-shell register-shell">
    <el-card class="auth-card register-card" shadow="never">
      <p class="eyebrow">开始使用 Asterway</p>
      <h1>创建账户</h1>
      <p class="page-description">填写基本信息，创建你的个人账户。</p>
      <el-skeleton v-if="checking" :rows="5" animated />
      <el-empty v-else-if="!registrationOpen" description="当前未开放注册" :image-size="88" />
      <el-form v-else class="account-form" label-position="top" @submit.prevent="submit">
        <div class="form-grid">
          <el-form-item label="用户名" for="register-username" required>
            <el-input
              id="register-username"
              v-model="username"
              autocomplete="username"
              placeholder="中文、英文字母或数字"
              :disabled="loading || Boolean(pending)"
              required
            />
          </el-form-item>
          <el-form-item label="昵称（可选）" for="register-nickname">
            <el-input
              id="register-nickname"
              v-model="nickname"
              placeholder="留空时使用用户名"
              :disabled="loading || Boolean(pending)"
            />
          </el-form-item>
        </div>
        <el-form-item label="密码" for="register-password" required>
          <el-input
            id="register-password"
            v-model="password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="8～50 个字符"
            :disabled="loading || Boolean(pending)"
            required
          />
          <p v-if="password" class="field-note">密码强度：{{ strength }}，仅作提示。</p>
        </el-form-item>
        <el-form-item label="确认密码" for="register-confirm-password" required>
          <el-input
            id="register-confirm-password"
            v-model="confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请再次输入密码"
            :disabled="loading || Boolean(pending)"
            required
          />
        </el-form-item>
        <TimeZoneSelect v-model="zoneId" :disabled="loading || Boolean(pending)" />
        <CaptchaPuzzle
          ref="captcha"
          v-model="proof"
          purpose="REGISTER"
          :disabled="loading || Boolean(pending)"
        />
        <el-button
          class="full-width"
          type="primary"
          size="large"
          native-type="submit"
          :loading="loading"
          :disabled="loading || (!proof && !pending)"
        >
          {{ pending ? '重试原注册请求' : '创建账户' }}
        </el-button>
      </el-form>
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
      />
      <p class="switch-link">
        已有账户？
        <RouterLink to="/login">前往登录</RouterLink>
      </p>
    </el-card>
  </section>
</template>
