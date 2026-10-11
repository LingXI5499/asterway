import { createRouter, createWebHashHistory } from 'vue-router';
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/methods',
      component: () => import('./pages/MethodsPage.vue'),
      meta: { title: '效率计划' },
    },
    {
      path: '/focus',
      component: () => import('./pages/FocusPage.vue'),
      meta: { title: '专注空间' },
    },
    { path: '/', redirect: '/today' },
    {
      path: '/templates',
      component: () => import('./pages/TemplatesPage.vue'),
      meta: { title: '成长模板' },
    },
    {
      path: '/community',
      component: () => import('./pages/CommunityPage.vue'),
      meta: { title: '成长社区' },
    },
    {
      path: '/circles',
      component: () => import('./pages/CirclesPage.vue'),
      meta: { title: '成长社群' },
    },
    {
      path: '/messages',
      component: () => import('./pages/MessagesPage.vue'),
      meta: { title: '消息中心' },
    },
    {
      path: '/knowledge',
      component: () => import('./pages/KnowledgePage.vue'),
      meta: { title: '个人知识库' },
    },
    {
      path: '/challenges',
      component: () => import('./pages/ChallengesPage.vue'),
      meta: { title: '挑战与榜单' },
    },
    {
      path: '/achievements',
      component: () => import('./pages/AchievementsPage.vue'),
      meta: { title: '段位与成就' },
    },
    {
      path: '/membership',
      component: () => import('./pages/MembershipPage.vue'),
      meta: { title: '会员与权益' },
    },
    {
      path: '/profile/:id',
      component: () => import('./pages/ProfilePage.vue'),
      meta: { title: '成长主页' },
    },
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
});
