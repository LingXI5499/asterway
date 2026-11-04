<script setup>
import { computed, ref, onMounted, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, session, clearSession } from "../api/http";
import { confirmAction, report } from "../api/ui";
import NavigationItems from "../components/NavigationItems.vue";
const route = useRoute(),
  router = useRouter(),
  collapsed = ref(
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
  sessionStorage.setItem("asterway.sidebar.collapsed", String(collapsed.value));
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
      .filter((n) => n.visible && n.enabled && n.menuType !== "BUTTON")
      .map((n) => ({ ...n, children: filter(n.children || []) }));
  }
  return filter(session.menus);
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
</script>
<template>
  <div class="shell">
    <a class="skip-link" href="#main-content">跳至内容</a>
    <aside
      v-if="!session.user?.mustChangePassword"
      class="sidebar"
      :class="{ collapsed }"
    >
      <router-link to="/account/profile" class="logo">{{
        collapsed ? "A" : siteName
      }}</router-link
      ><el-menu
        ref="menuRef"
        :default-active="route.path"
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
    <div class="workspace">
      <header class="topbar">
        <el-button
          v-if="!session.user?.mustChangePassword"
          text
          aria-label="展开或收起菜单"
          @click="toggleSidebar"
          >☰</el-button
        ><el-breadcrumb separator="/"
          ><el-breadcrumb-item :to="{ path: '/account/profile' }"
            >账户中心</el-breadcrumb-item
          ><el-breadcrumb-item>{{
            route.meta.title
          }}</el-breadcrumb-item></el-breadcrumb
        >
        <div class="top-actions">
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
            {{ route.meta.description || "账户与访问权限管理" }}
          </p>
        </div>
        <router-view :key="route.fullPath" />
      </main>
    </div>
  </div>
</template>
