<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiRequest } from '../api/http'

const router = useRouter()

// 当前账户信息（从 /me 接口获取）
const account = ref(null)
const errorMessage = ref('')
const loading = ref(true)

// 页面挂载时查询当前登录状态。
// 关键：刷新页面后仍能通过服务端会话拿到账户信息，而不是依赖前端内存。
onMounted(async () => {
  try {
    account.value = await apiRequest('/auth/me')
  } catch (error) {
    // 未登录或会话失效，跳回登录页
    errorMessage.value = error.message
    await router.replace('/login')
  } finally {
    loading.value = false
  }
})

// 退出登录
async function logout() {
  errorMessage.value = ''

  try {
    await apiRequest('/auth/logout', {
      method: 'POST'
    })

    // 退出后回到登录页
    await router.replace('/login')
  } catch (error) {
    errorMessage.value = error.message
  }
}
</script>

<template>
  <section class="auth-card">
    <h1>账户中心</h1>

    <p v-if="loading">正在读取登录状态…</p>

    <!-- 登录成功时展示账户信息 -->
    <template v-else-if="account">
      <p>账户 ID：{{ account.accountId }}</p>
      <p>用户名：{{ account.username }}</p>
      <p>昵称：{{ account.nickname }}</p>
      <p>角色：{{ account.role }}</p>

      <button @click="logout">退出登录</button>
    </template>

    <p v-if="errorMessage" class="error">
      {{ errorMessage }}
    </p>
  </section>
</template>
