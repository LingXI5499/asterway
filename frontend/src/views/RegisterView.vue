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
  <section class="auth-card">
    <h1>创建账户</h1>
    <p v-if="checking">正在读取注册状态…</p>
    <p v-else-if="!registrationOpen">当前未开放注册。</p>
    <form v-else @submit.prevent="submit">
      <fieldset :disabled="loading || Boolean(pending)">
        <label>
          用户名
          <input
            v-model="username"
            required
            autocomplete="username"
            placeholder="中文、英文字母或数字"
          />
        </label>
        <label>
          昵称（可选）
          <input v-model="nickname" placeholder="留空时使用本次用户名" />
        </label>
        <label>
          密码
          <input v-model="password" type="password" required autocomplete="new-password" />
        </label>
        <p v-if="password">密码强度：{{ strength }}（仅作提示）</p>
        <label>
          确认密码
          <input v-model="confirmPassword" type="password" required autocomplete="new-password" />
        </label>
        <TimeZoneSelect v-model="zoneId" />
      </fieldset>
      <CaptchaPuzzle ref="captcha" v-model="proof" purpose="REGISTER" />
      <button :disabled="loading || (!proof && !pending)">
        {{ loading ? '注册中…' : pending ? '重试原注册请求' : '注册' }}
      </button>
    </form>
    <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
    <p class="switch-link">
      已有账户？
      <RouterLink to="/login">去登录</RouterLink>
    </p>
  </section>
</template>
