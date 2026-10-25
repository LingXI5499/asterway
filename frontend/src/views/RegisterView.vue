<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../api/http'

const router = useRouter()

// 表单字段
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')

// 加载状态与错误提示
const loading = ref(false)
const errorMessage = ref('')

// 提交注册表单
async function submit() {
  loading.value = true
  errorMessage.value = ''

  try {
    // 组装请求体，昵称为空时不传（后端会回退为用户名）
    const body = {
      username: username.value,
      password: password.value,
      confirmPassword: confirmPassword.value
    }

    if (nickname.value.trim()) {
      body.nickname = nickname.value
    }

    await apiRequest('/auth/register', {
      method: 'POST',
      body
    })

    // 清空敏感字段
    password.value = ''
    confirmPassword.value = ''

    // 注册成功跳转登录页，带 registered=1 提示
    await router.push({
      path: '/login',
      query: { registered: '1' }
    })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <h1>创建 Asterway 账户</h1>
    <p class="subtitle">注册后使用用户名和密码登录。</p>

    <form @submit.prevent="submit">
      <label>
        用户名
        <input
          v-model="username"
          required
          minlength="1"
          maxlength="30"
          autocomplete="username"
          placeholder="中文、英文字母或数字"
        />
      </label>

      <label>
        昵称（可选）
        <input
          v-model="nickname"
          maxlength="30"
          placeholder="默认使用用户名"
        />
      </label>

      <label>
        密码
        <input
          v-model="password"
          type="password"
          required
          minlength="8"
          maxlength="50"
          autocomplete="new-password"
        />
      </label>

      <label>
        确认密码
        <input
          v-model="confirmPassword"
          type="password"
          required
          minlength="8"
          maxlength="50"
          autocomplete="new-password"
        />
      </label>

      <p v-if="errorMessage" class="error">
        {{ errorMessage }}
      </p>

      <button :disabled="loading">
        {{ loading ? '注册中…' : '注册' }}
      </button>
    </form>

    <p class="switch-link">
      已有账户？
      <RouterLink to="/login">去登录</RouterLink>
    </p>
  </section>
</template>
