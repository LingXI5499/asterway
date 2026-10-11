<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from './store/app';
import { usePrototypeStore } from './store/prototype';
import { ui } from './store/ui';
import { parseDate } from './domain/dates';
import Icon from './components/Icon.vue';
import Modal from './components/Modal.vue';
import EntityEditor from './components/EntityEditor.vue';
import GoalDetail from './components/GoalDetail.vue';
import FocusTimer from './components/FocusTimer.vue';
import SearchPanel from './components/SearchPanel.vue';
const store = useAppStore(),
  route = useRoute(),
  mobileOpen = ref(false),
  addOpen = ref(false);
const proto = usePrototypeStore();
const personalNav = [
  { path: '/today', label: '今日总览', icon: 'dashboard' },
  { path: '/goals', label: '我的目标', icon: 'target' },
  { path: '/planner', label: '计划日程', icon: 'calendar' },
  { path: '/habits', label: '习惯追踪', icon: 'checkCircle' },
  { path: '/insights', label: '成长复盘', icon: 'chart' },
  { path: '/assistant', label: '成长伙伴', icon: 'sparkles' },
  { path: '/templates', label: '成长模板', icon: 'library' },
  { path: '/knowledge', label: '个人知识库', icon: 'notebook' },
];
const navigationGroups = [
  { title: '个人成长', items: personalNav },
  {
    title: '一起成长',
    items: [
      { path: '/community', label: '成长社区', icon: 'globe' },
      { path: '/circles', label: '成长社群', icon: 'users' },
      { path: '/messages', label: '消息中心', icon: 'message' },
      { path: '/challenges', label: '挑战与榜单', icon: 'trophy' },
    ],
  },
  {
    title: '我的身份',
    items: [
      { path: '/achievements', label: '段位与成就', icon: 'award' },
      { path: '/membership', label: '会员与权益', icon: 'crown' },
      { path: '/profile/me', label: '我的主页', icon: 'user' },
    ],
  },
];
watch(
  () => route.path,
  () => {
    mobileOpen.value = false;
    addOpen.value = false;
  },
);
function keyboard(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault();
    ui.open('search');
  }
  if (e.key === 'Escape') {
    addOpen.value = false;
    mobileOpen.value = false;
  }
}
onMounted(() => window.addEventListener('keydown', keyboard));
onBeforeUnmount(() => window.removeEventListener('keydown', keyboard));
function confirm() {
  const action = ui.confirmAction;
  ui.close();
  action?.();
}
function openQuick(kind: 'goal' | 'task' | 'habit') {
  ui.open(kind);
  addOpen.value = false;
}
</script>
<template>
  <div
    :class="[
      'app-shell',
      { collapsed: store.data.profile.sidebarCollapsed, 'mobile-open': mobileOpen },
    ]"
  >
    <button
      v-if="mobileOpen"
      class="sidebar-backdrop"
      aria-label="关闭导航"
      @click="mobileOpen = false"
    />
    <aside class="sidebar">
      <RouterLink to="/today" class="brand"
        ><span class="brand-symbol"><Icon name="sparkles" :size="25" /></span
        ><span class="brand-name">Asterway<small>星行纪 · 成长由你</small></span></RouterLink
      >
      <nav aria-label="主导航" class="grouped-nav">
        <div v-for="group in navigationGroups" :key="group.title" class="nav-group">
          <div class="nav-label">{{ group.title }}</div>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            :title="item.label"
            ><Icon :name="item.icon" /><span>{{ item.label }}</span
            ><small v-if="item.path === '/assistant'" class="demo-dot">DEMO</small></RouterLink
          >
        </div>
      </nav>
      <div class="sidebar-bottom">
        <div class="sidebar-prototype">
          <span class="demo-dot">v0.2 PROTOTYPE</span><small>个人成长 · AI 伙伴 · 自律社区</small>
        </div>
        <RouterLink to="/settings" class="settings-link"
          ><Icon name="settings" /><span>设置与数据</span></RouterLink
        >
        <div class="profile">
          <span class="avatar">{{ store.data.profile.displayName.slice(0, 1) }}</span>
          <div>
            <strong>{{ store.data.profile.displayName }}</strong
            ><small>Lv. {{ String(store.level).padStart(2, '0') }} · 探索自己的方向</small>
          </div>
          <button
            class="icon-button collapse-button"
            aria-label="折叠或展开导航"
            @click="store.data.profile.sidebarCollapsed = !store.data.profile.sidebarCollapsed"
          >
            <Icon name="collapse" :size="17" />
          </button>
        </div>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <button class="icon-button mobile-menu" aria-label="打开导航" @click="mobileOpen = true">
          <Icon name="menu" />
        </button>
        <div class="breadcrumb">
          <span>我的空间</span><Icon name="right" :size="14" /><strong>{{
            route.meta.title
          }}</strong>
        </div>
        <div class="topbar-actions">
          <span class="topbar-date">{{
            parseDate(store.today).toLocaleDateString('zh-CN', {
              month: 'long',
              day: 'numeric',
              weekday: 'long',
            })
          }}</span
          ><button class="search-trigger" aria-label="全局搜索" @click="ui.open('search')">
            <Icon name="search" :size="17" /><span>搜索</span><kbd>⌘ K</kbd>
          </button>
          <div class="add-wrapper">
            <button
              class="button primary"
              aria-label="快速添加"
              :aria-expanded="addOpen"
              @click="addOpen = !addOpen"
            >
              <Icon name="plus" :size="17" /><span>快速添加</span>
            </button>
            <div v-if="addOpen" class="dropdown">
              <button
                v-for="(label, kind) in { goal: '新建目标', task: '新建任务', habit: '新建习惯' }"
                :key="kind"
                @click="openQuick(kind)"
              >
                <Icon
                  :name="kind === 'goal' ? 'target' : kind === 'task' ? 'calendar' : 'checkCircle'"
                  :size="17"
                />{{ label }}
              </button>
            </div>
            <button
              v-if="addOpen"
              class="dropdown-backdrop"
              aria-label="关闭添加菜单"
              @click="addOpen = false"
            />
          </div>
          <RouterLink to="/settings" class="avatar topbar-avatar" aria-label="个人设置">{{
            store.data.profile.displayName.slice(0, 1)
          }}</RouterLink>
        </div>
      </header>
      <div v-if="store.storageError" class="error-banner" role="alert">
        {{ store.storageError }} <RouterLink to="/settings">前往导出</RouterLink>
      </div>
      <div v-if="proto.storageError || proto.recoveryError" class="error-banner" role="alert">
        {{ proto.storageError || proto.recoveryError
        }}<RouterLink to="/settings">数据设置</RouterLink>
      </div>
      <main class="main-content">
        <div v-if="store.recoveryError" class="recovery card">
          <Icon name="shield" :size="40" />
          <h1>先找回你的成长记录</h1>
          <p>{{ store.recoveryError }}</p>
          <p>原存储内容仍保留。可在设置导入备份，或确认重置。</p>
          <RouterLink to="/settings" class="button primary">打开数据设置</RouterLink>
        </div>
        <RouterView v-if="!store.recoveryError || route.path === '/settings'" />
      </main>
    </div>
  </div>
  <EntityEditor
    v-if="ui.modal && ['goal', 'task', 'habit'].includes(ui.modal.kind)"
    :key="`${ui.modal.kind}-${ui.modal.id || 'new'}`"
  />
  <GoalDetail v-if="ui.modal?.kind === 'goalDetail'" :key="ui.modal.id" />
  <FocusTimer v-if="ui.modal?.kind === 'focus'" /><SearchPanel v-if="ui.modal?.kind === 'search'" />
  <Modal v-if="ui.modal?.kind === 'confirm'" :title="ui.confirmTitle" @close="ui.close()"
    ><p>{{ ui.confirmBody }}</p>
    <div class="modal-actions">
      <button class="button secondary" @click="ui.close()">取消</button
      ><button class="button primary" @click="confirm">确认操作</button>
    </div></Modal
  >
  <div class="toast-stack" aria-live="polite">
    <div v-for="toast in store.toasts" :key="toast.id" :class="['toast', { error: toast.error }]">
      <Icon :name="toast.error ? 'shield' : 'checkCircle'" :size="18" /><span>{{
        toast.message
      }}</span
      ><button
        class="icon-button"
        aria-label="关闭提示"
        @click="store.toasts = store.toasts.filter((t) => t.id !== toast.id)"
      >
        <Icon name="close" :size="14" />
      </button>
    </div>
  </div>
</template>
