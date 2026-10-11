import { createRouter, createWebHashHistory } from 'vue-router'
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/today' },
    {
      path: '/today',
      component: () => import('./pages/TodayPage.vue'),
      meta: { title: '今日总览' },
    },
    {
      path: '/goals',
      component: () => import('./pages/GoalsPage.vue'),
      meta: { title: '我的目标' },
    },
    {
      path: '/planner',
      component: () => import('./pages/PlannerPage.vue'),
      meta: { title: '计划日程' },
    },
    {
      path: '/habits',
      component: () => import('./pages/HabitsPage.vue'),
      meta: { title: '习惯追踪' },
    },
    {
      path: '/insights',
      component: () => import('./pages/InsightsPage.vue'),
      meta: { title: '成长复盘' },
    },
    {
      path: '/assistant',
      component: () => import('./pages/AssistantPage.vue'),
      meta: { title: '成长伙伴' },
    },
    {
      path: '/settings',
      component: () => import('./pages/SettingsPage.vue'),
      meta: { title: '设置与数据' },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('./pages/NotFoundPage.vue'),
      meta: { title: '页面未找到' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
