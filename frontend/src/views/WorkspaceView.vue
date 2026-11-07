<script setup>
import {
  computed,
  ref,
  onMounted,
  onBeforeUnmount,
  nextTick,
  watch,
  provide,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, session, clearSession, hasPermission } from "../api/http";
import { confirmAction, report } from "../api/ui";
import WorkspaceNavigation from "../components/WorkspaceNavigation.vue";
import { isManager, canGrow } from "../api/growth";
import { usePersonalRuntime, messages, execution } from "../stores/personal";
import ExecutionBar from "../components/personal/ExecutionBar.vue";
import { workspacePages, isPlanningPath } from "../composables/workspaceNavigation";
const screen = window.matchMedia("(max-width: 899px)");
const mobile = ref(screen.matches);
usePersonalRuntime();
const route = useRoute(),
  router = useRouter(),
  collapsed = ref(
    mobile.value ||
      sessionStorage.getItem("asterway.sidebar.collapsed") === "true",
  ),
  siteName = ref("Asterway");
// 菜单是布局状态，与业务页面的数据加载相互独立。
const sidebarRef = ref(null);
const sidebarButton = ref(null);
const assistant = computed(() => route.path === "/agent" || route.path.startsWith("/agent/"));
const planning = computed(() => isPlanningPath(route.path));
const planningLinks = computed(() => workspacePages(menus.value).filter(item => isPlanningPath(item.routePath)));
const SIDEBAR_MIN = 200, SIDEBAR_MAX = 480, SIDEBAR_DEFAULT = 256;
function savedSidebarWidth() {
  try {
    const value = Number(localStorage.getItem("asterway.ui.sidebarWidth"));
    return value >= SIDEBAR_MIN && value <= SIDEBAR_MAX ? value : SIDEBAR_DEFAULT;
  } catch { return SIDEBAR_DEFAULT; }
}
const sidebarWidth = ref(savedSidebarWidth());
const resizing = ref(false);
let resizeOrigin = null;
function persistSidebarWidth() {
  try { localStorage.setItem("asterway.ui.sidebarWidth", String(sidebarWidth.value)); } catch {}
}
function resizeStart(event) {
  if (mobile.value || collapsed.value || event.button !== 0) return;
  resizeOrigin = { pointer: event.pointerId, x: event.clientX, width: sidebarWidth.value };
  event.currentTarget.setPointerCapture(event.pointerId);
  resizing.value = true;
  event.preventDefault();
}
function resizeMove(event) {
  if (!resizeOrigin || resizeOrigin.pointer !== event.pointerId) return;
  sidebarWidth.value = Math.round(Math.max(SIDEBAR_MIN, Math.min(SIDEBAR_MAX, resizeOrigin.width + event.clientX - resizeOrigin.x)));
}
function resizeEnd() {
  if (!resizeOrigin) return;
  resizeOrigin = null;
  resizing.value = false;
  persistSidebarWidth();
}
function resizeKey(event) {
  const step = event.shiftKey ? 32 : 16;
  const change = { ArrowLeft: -step, ArrowRight: step }[event.key];
  if (change == null && !["Home", "End", "Enter"].includes(event.key)) return;
  event.preventDefault();
  sidebarWidth.value = event.key === "Home" ? SIDEBAR_MIN : event.key === "End" ? SIDEBAR_MAX : event.key === "Enter" ? SIDEBAR_DEFAULT : Math.max(SIDEBAR_MIN, Math.min(SIDEBAR_MAX, sidebarWidth.value + change));
  persistSidebarWidth();
}
function resetSidebarWidth() { sidebarWidth.value = SIDEBAR_DEFAULT; persistSidebarWidth(); }
async function toggleSidebar() {
  collapsed.value = !collapsed.value;
  if (!mobile.value)
    sessionStorage.setItem(
      "asterway.sidebar.collapsed",
      String(collapsed.value),
    );
  if (!collapsed.value) {
    await nextTick();
    if (mobile.value)
      sidebarRef.value?.querySelector("a,button,input,summary,[tabindex='0']")?.focus();
  } else if (mobile.value) {
    await nextTick();
    sidebarButton.value?.$el?.focus();
  }
}
provide("workspaceSidebar", {
  toggle: toggleSidebar,
  close: () => { if (mobile.value) collapsed.value = true; },
});
function sidebarKeydown(event) {
  if (!mobile.value || collapsed.value) return;
  if (event.key === "Escape") {
    event.preventDefault();
    toggleSidebar();
    return;
  }
  if (event.key !== "Tab") return;
  const nodes = [
    ...sidebarRef.value.querySelectorAll("a[href],button,input,summary,[tabindex='0']"),
  ].filter((el) => el.getClientRects().length && !el.disabled);
  const first = nodes[0],
    last = nodes.at(-1);
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last?.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first?.focus();
  }
}
function userCommand(command) {
  if (command === "logout") logout();
  else router.push(command);
}
const menus = computed(() => {
  function filter(nodes) {
    return nodes
      .filter(
        (n) =>
          n.visible &&
          n.enabled &&
          n.menuType !== "BUTTON" &&
          !(
            isManager() &&
            (n.code?.startsWith("growth_") ||
              n.componentKey?.startsWith("growth/") ||
              n.componentKey?.startsWith("planning/") ||
              /^(execution|habit|notification|dashboard|statistics|agent)\//.test(
                n.componentKey || "",
              ))
          ),
      )
      .map((n) => ({ ...n, children: filter(n.children || []) }));
  }
  return filter(session.menus);
});
const crumbs = computed(() => {
  if (!route.meta.growth)
    return [
      { name: "账户中心", path: "/account/profile" },
      { name: route.meta.title },
    ];
  const parts = [
    {
      name: "个人工作区",
      path: canGrow("dashboard:board:read") ? "/dashboard" : "/growth/goals",
    },
  ];
  if (route.meta.parent) parts.push(route.meta.parent);
  if (/^\/growth\/(goals|stages|tasks)\/[^/]+$/.test(route.path)) {
    const resource = route.path.split("/")[2];
    parts.push({
      name: { goals: "目标管理", stages: "阶段管理", tasks: "任务中心" }[
        resource
      ],
      path: "/growth/" + resource,
    });
  }
  parts.push({ name: route.meta.title });
  return parts;
});
async function logout() {
  try {
    await confirmAction("确定退出当前登录？", "退出登录");
    await api("/me/sessions/current", { method: "DELETE" });
    clearSession();
    router.replace("/login");
  } catch (e) {
    report(e);
  }
}
onMounted(async () => {
  try {
    siteName.value = (await api("/public/account-settings")).siteName;
  } catch {}
});
const resize = (event) => {
  mobile.value = event.matches;
  collapsed.value =
    event.matches ||
    sessionStorage.getItem("asterway.sidebar.collapsed") === "true";
};
screen.addEventListener("change", resize);
onBeforeUnmount(() => screen.removeEventListener("change", resize));
watch(
  () => route.path,
  () => {
    if (mobile.value) collapsed.value = true;
  },
);
watch(isManager, (manager) => {
  if (manager && route.meta.growth) router.replace("/");
});
</script>
<template>
  <div class="shell" :style="{ '--aw-sidebar-width': sidebarWidth + 'px' }" :class="{ 'has-execution': !!execution.current, 'assistant-shell': assistant, 'sidebar-is-collapsed': collapsed, 'is-resizing': resizing }">
    <a class="skip-link" href="#main-content">跳至内容</a>
    <aside
      id="workspace-navigation"
      ref="sidebarRef"
      v-if="!session.user?.mustChangePassword"
      class="sidebar"
      :inert="mobile && collapsed ? true : undefined"
      :class="{ collapsed }"
      aria-label="主导航"
      @keydown="sidebarKeydown"
    >
      <div class="sidebar-brand"><router-link to="/" class="logo" :aria-label="siteName + ' 工作区首页'">
        <span class="brand-symbol">A</span
        ><span v-if="!collapsed || mobile" class="brand-copy"
          >{{ siteName }}<small>让成长有迹可循</small></span>
        </router-link>
        <el-button v-if="!collapsed || mobile" text class="sidebar-close icon-button" aria-label="收起导航" @click="toggleSidebar"><AppIcon name="closeSidebar" /></el-button>
      </div>
      <div id="assistant-new-chat" v-show="assistant && (!collapsed || mobile)"></div>
      <div class="sidebar-scroll">
        <WorkspaceNavigation :nodes="menus" :collapsed="collapsed && !mobile" />
        <div id="assistant-conversations" v-show="assistant && (!collapsed || mobile)" class="sidebar-conversations"></div>
      </div>
      <div class="sidebar-account">
        <el-dropdown trigger="click" placement="top-start" @command="userCommand">
          <button class="sidebar-user" type="button" aria-label="打开账户菜单">
            <span class="avatar">{{ (session.user?.nickname || session.user?.username || "A").slice(0, 1).toUpperCase() }}</span>
            <span v-if="!collapsed || mobile" class="sidebar-user-copy"><strong>{{ session.user?.nickname || session.user?.username }}</strong><small>个人工作区</small></span>
            <AppIcon v-if="!collapsed || mobile" name="more" :size="18" />
          </button>
          <template #dropdown><el-dropdown-menu>
            <el-dropdown-item v-if="hasPermission('identity:profile:read')" command="/account/profile">个人资料</el-dropdown-item>
            <el-dropdown-item v-if="hasPermission('identity:profile:read')" command="/account/profile?tab=password">修改密码</el-dropdown-item>
            <el-dropdown-item v-if="hasPermission('identity:device:list')" command="/account/devices">我的设备</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu></template>
        </el-dropdown>
      </div>
      <div v-if="!mobile && !collapsed" class="sidebar-resize-handle" role="separator" aria-label="调整侧栏宽度" aria-orientation="vertical" :aria-valuemin="SIDEBAR_MIN" :aria-valuemax="SIDEBAR_MAX" :aria-valuenow="sidebarWidth" tabindex="0" title="拖动调整宽度，双击恢复默认" @pointerdown="resizeStart" @pointermove="resizeMove" @pointerup="resizeEnd" @pointercancel="resizeEnd" @lostpointercapture="resizeEnd" @keydown="resizeKey" @dblclick="resetSidebarWidth"></div>
    </aside>
    <button
      v-if="mobile && !collapsed"
      class="sidebar-backdrop"
      aria-label="关闭导航菜单"
      @click="toggleSidebar"
    ></button>
    <div class="workspace" :inert="mobile && !collapsed ? true : undefined">
      <header class="topbar">
        <el-button
          ref="sidebarButton"
          v-if="!session.user?.mustChangePassword && (collapsed || mobile)"
          text
          aria-label="展开或收起菜单"
          :aria-expanded="!collapsed"
          aria-controls="workspace-navigation"
          class="icon-button"
          @click="toggleSidebar"
          ><AppIcon
            :name="collapsed ? 'openSidebar' : 'closeSidebar'" /></el-button
        ><span v-if="assistant" class="workspace-title">成长助手<span class="workspace-title-caption">一起，把想法变成行动</span></span><el-breadcrumb v-else separator="/"
          ><el-breadcrumb-item
            v-for="(crumb, index) in crumbs"
            :key="index"
            :to="crumb.path ? { path: crumb.path } : undefined"
            >{{ crumb.name }}</el-breadcrumb-item
          ></el-breadcrumb
        >
        <div class="top-actions">
          <AppThemeSwitch />
          <el-badge
            v-if="
              !session.user?.mustChangePassword &&
              canGrow('notification:message:list')
            "
            :value="messages.unread"
            :max="99"
            :hidden="!messages.unread"
          >
            <el-button
              text
              :title="messages.error || '站内消息'"
              :aria-label="'站内消息，' + messages.unread + ' 条未读'"
              class="icon-button"
              @click="router.push('/notifications')"
              ><AppIcon name="notification"
            /></el-button>
          </el-badge>
          <el-dropdown v-if="session.user?.mustChangePassword" trigger="click" @command="userCommand">
            <button
              class="user-trigger"
              type="button"
              aria-label="打开用户菜单"
            >
              <span class="avatar">{{
                (session.user?.nickname || session.user?.username || "A")
                  .slice(0, 1)
                  .toUpperCase()
              }}</span>
              <span>{{
                session.user?.nickname || session.user?.username
              }}</span>
            </button>
            <template #dropdown
              ><el-dropdown-menu>
                <template v-if="!session.user?.mustChangePassword">
                  <el-dropdown-item
                    v-if="hasPermission('identity:profile:read')"
                    command="/account/profile"
                    ><span class="user-menu-item"
                      ><AppIcon name="user" />个人资料</span
                    ></el-dropdown-item
                  >
                  <el-dropdown-item
                    v-if="hasPermission('identity:profile:read')"
                    command="/account/profile?tab=password"
                    ><span class="user-menu-item"
                      ><AppIcon name="security" />修改密码</span
                    ></el-dropdown-item
                  >
                  <el-dropdown-item
                    v-if="hasPermission('identity:device:list')"
                    command="/account/devices"
                    ><span class="user-menu-item"
                      ><AppIcon name="device" />我的设备</span
                    ></el-dropdown-item
                  >
                </template>
                <el-dropdown-item divided command="logout"
                  ><span class="user-menu-item"
                    ><AppIcon name="logout" />退出登录</span
                  ></el-dropdown-item
                >
              </el-dropdown-menu></template
            >
          </el-dropdown>
        </div>
      </header>
      <main
        id="main-content"
        class="content"
        :data-page="route.path.split('/')[1]"
        tabindex="-1"
      >
        <nav v-if="planning" class="workspace-section-tabs" aria-label="目标与计划">
          <RouterLink v-for="item in planningLinks" :key="item.routePath" :to="item.routePath" :class="{ selected: route.path === item.routePath || route.path.startsWith(item.routePath + '/') }" :aria-current="route.path === item.routePath || route.path.startsWith(item.routePath + '/') ? 'page' : undefined">{{ item.name }}</RouterLink>
        </nav>
        <AppPageHeader
          v-if="!assistant"
          :title="route.meta.title"
          :description="route.meta.description"
        />
        <router-view v-slot="{ Component }">
          <KeepAlive include="AgentChatView">
            <component :is="Component" :key="assistant ? 'assistant-workspace' : route.meta.stableQuery ? route.path : route.fullPath" />
          </KeepAlive>
        </router-view>
      </main>
    </div>
    <RouterLink v-if="!assistant && !session.user?.mustChangePassword && canGrow('agent:chat:read')" to="/agent" class="global-assistant-entry" aria-label="打开成长助手"><AppIcon name="sparkle" :size="20" /><span>成长助手</span><AppIcon name="arrow" :size="16" /></RouterLink>
    <ExecutionBar />
  </div>
</template>
