import { createRouter, createWebHistory } from 'vue-router'

import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'

// 路由配置：定义页面路径与对应组件的映射关系
const router = createRouter({
  history: createWebHistory(),

  routes: [
    // 根路径重定向到登录页
    {
      path: '/',
      redirect: '/login'
    },
    // 登录页
    {
      path: '/login',
      component: LoginView
    },
    // 注册页
    {
      path: '/register',
      component: RegisterView
    },
    // 账户中心（登录后）
    {
      path: '/home',
      component: HomeView
    }
  ]
})

export default router
