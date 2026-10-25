<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiRequest, ensureCsrf } from '../api/http'

const router = useRouter()
const route = useRoute()

// 表单字段
const username = ref('')
const password = ref('')

// 加载状态与错误提示
const loading = ref(false)
const errorMessage = ref('')

// 是否刚从注册页跳转过来（query.registered === '1'）
const registered = ref(route.query.registered === '1')

// 页面挂载时先初始化 CSRF Token，否则登录（写请求）会失败
onMounted(async () => {
  try {
    await ensureCsrf()
  } catch (error) {
    errorMessage.value = error.message
  }
})

// 提交登录表单
async function submit() {
  loading.value = true
  errorMessage.value = ''

  try {
    await apiRequest('/auth/login', {
      method: 'POST',
      body: {
        username: username.value,
        password: password.value
      }
    })

    // 清空密码，避免留在内存里
    password.value = ''

    // 登录成功后安全策略刷新了 CSRF Token，
    // 重新获取一次，再进入受保护页面（否则后续写请求会 403）
    await ensureCsrf()

    await router.push('/home')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <h1>登录 Asterway</h1>
    <p class="subtitle">使用已注册的账户继续。</p>

    <!-- 注册成功跳转过来时的提示 -->
    <p v-if="registered" class="success">
      注册成功，请登录。
    </p>

    <!-- 阻止默认提交，走自定义 submit -->
    <form @submit.prevent="submit">
      <label>
        用户名
        <input
          v-model="username"
          required
          maxlength="30"
          autocomplete="username"
        />
      </label>

      <label>
        密码
        <input
          v-model="password"
          type="password"
          required
          maxlength="50"
          autocomplete="current-password"
        />
      </label>

      <p v-if="errorMessage" class="error">
        {{ errorMessage }}
      </p>

      <button :disabled="loading">
        {{ loading ? '登录中…' : '登录' }}
      </button>
    </form>

    <p class="switch-link">
      没有账户？
      <RouterLink to="/register">去注册</RouterLink>
    </p>
  </section>
</template>
