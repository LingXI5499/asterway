<script setup>
import {
  computed,
  ref,
  onMounted,
  onBeforeUnmount,
  nextTick,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, session, clearSession } from "../api/http";
import { confirmAction, report } from "../api/ui";
import NavigationItems from "../components/NavigationItems.vue";
import { isManager, canGrow } from "../api/growth";
import { usePersonalRuntime, messages, execution } from "../stores/personal";
import ExecutionBar from "../components/personal/ExecutionBar.vue";
const screen = window.matchMedia("(max-width: 900px)");
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
const openedMenus = ref(readOpenedMenus());
const menuRef = ref(null);
function readOpenedMenus() {
  try {
    const value = JSON.parse(
      sessionStorage.getItem("asterway.sidebar.opened") || "[]",
    );
    return Array.isArray(value)
      ? value.filter((id) => typeof id === "string")
      : [];
  } catch {
    return [];
  }
}
async function toggleSidebar() {
  collapsed.value = !collapsed.value;
  if (!mobile.value)
    sessionStorage.setItem(
      "asterway.sidebar.collapsed",
      String(collapsed.value),
    );
  if (!collapsed.value) {
    await nextTick();
    const directories = new Set();
    function collect(nodes) {
      for (const node of nodes) {
        if (node.children.length) directories.add(node.id);
        collect(node.children);
      }
    }
    collect(menus.value);
    for (const id of [...openedMenus.value])
      if (directories.has(id)) menuRef.value?.open(id);
  }
}
function rememberMenu(id, open) {
  // 整体收起时保留原目录状态，再次展开时恢复。
  if (collapsed.value) return;
  openedMenus.value = open
    ? [...new Set([...openedMenus.value, id])]
    : openedMenus.value.filter((value) => value !== id);
  sessionStorage.setItem(
    "asterway.sidebar.opened",
    JSON.stringify(openedMenus.value),
  );
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
              /^(execution|habit|notification|dashboard|statistics)\//.test(
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
    await confirmAction("确定退出当前登录？");
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
  <div class="shell" :class="{ 'has-execution': !!execution.current }">
    <a class="skip-link" href="#main-content">跳至内容</a>
    <aside
      v-if="!session.user?.mustChangePassword"
      class="sidebar"
      :inert="mobile && collapsed ? true : undefined"
      :class="{ collapsed }"
    >
      <router-link to="/" class="logo">{{
        collapsed ? "A" : siteName
      }}</router-link
      ><el-menu
        ref="menuRef"
        :default-active="route.meta.menuPath || route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        :default-openeds="openedMenus"
        @open="(id) => rememberMenu(id, true)"
        @close="(id) => rememberMenu(id, false)"
        router
        background-color="transparent"
        text-color="#cbd8ec"
        active-text-color="#fff"
        ><NavigationItems :nodes="menus"
      /></el-menu>
    </aside>
    <button
      v-if="mobile && !collapsed"
      class="sidebar-backdrop"
      aria-label="关闭导航菜单"
      @click="toggleSidebar"
    ></button>
    <div class="workspace">
      <header class="topbar">
        <el-button
          v-if="!session.user?.mustChangePassword"
          text
          aria-label="展开或收起菜单"
          :aria-expanded="!collapsed"
          @click="toggleSidebar"
          >☰</el-button
        ><el-breadcrumb separator="/"
          ><el-breadcrumb-item
            v-for="(crumb, index) in crumbs"
            :key="index"
            :to="crumb.path ? { path: crumb.path } : undefined"
            >{{ crumb.name }}</el-breadcrumb-item
          ></el-breadcrumb
        >
        <div class="top-actions">
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
              @click="router.push('/notifications')"
              >消息</el-button
            >
          </el-badge>
          <el-button text @click="router.push('/account/profile')">{{
            session.user?.nickname || session.user?.username
          }}</el-button
          ><el-button text @click="logout">退出登录</el-button>
        </div>
      </header>
      <main id="main-content" class="content">
        <div class="page-heading">
          <h1>{{ route.meta.title }}</h1>
          <p class="muted">
            {{
              route.meta.description ||
              (route.meta.growth
                ? "从目标到行动，安排属于自己的成长节奏"
                : "账户与访问权限管理")
            }}
          </p>
        </div>
        <router-view
          :key="route.meta.stableQuery ? route.path : route.fullPath"
        />
      </main>
    </div>
    <ExecutionBar />
  </div>
</template>
