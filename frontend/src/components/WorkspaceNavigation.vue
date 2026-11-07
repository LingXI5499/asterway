<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { workspacePages, isPlanningPath } from "../composables/workspaceNavigation";

const props = defineProps({ nodes: { type: Array, default: () => [] }, collapsed: Boolean });
const route = useRoute();
const pages = computed(() => workspacePages(props.nodes));
const groups = computed(() => {
  const seen = new Set();
  const main = [], planning = [], management = [];
  for (const node of pages.value) {
    const path = node.routePath;
    if (path.startsWith("/agent/") || ["/habits/categories", "/statistics/timeline", "/execution/records"].includes(path)) continue;
    if (seen.has(path) || path === "/notifications") continue;
    seen.add(path);
    const item = { ...node, icon: iconFor(path) };
    if (path.startsWith("/system")) management.push(item);
    else if (path.startsWith("/account")) continue;
    else if (isPlanningPath(path)) planning.push(item);
    else main.push(item);
  }
  if (planning.length) {
    const first = planning.find(item => item.routePath === "/growth/goals") || planning[0];
    main.push({ ...first, name: "目标与计划", icon: "goal", planning: true });
  }
  const order = ["/agent", "/dashboard", "planning", "/execution", "/habits", "/statistics"];
  const rank = item => { const index = order.indexOf(item.planning ? "planning" : item.routePath); return index < 0 ? order.length : index; };
  main.sort((a, b) => rank(a) - rank(b));
  // 账户入口统一放在左下角，侧栏只展示工作内容。
  return [
    { name: "工作区", items: main },
    { name: "系统管理", items: management },
  ].filter(group => group.items.length);
});
function iconFor(path) {
  return [
    ["/agent", "sparkle"], ["/dashboard", "home"], ["/execution", "execution"],
    ["/habits", "habit"], ["/statistics", "statistics"], ["goals", "goal"],
    ["stages", "stage"], ["tasks", "task"], ["weekly", "week"], ["daily", "day"],
    ["pending", "inbox"], ["users", "users"], ["roles", "role"],
    ["permissions", "permission"], ["menus", "menu"], ["sessions", "device"],
    ["logs", "history"], ["settings", "settings"],
  ].find(([part]) => path.includes(part))?.[1] || "folder";
}
function selected(item) {
  return item.planning ? isPlanningPath(route.path) : route.path === item.routePath || route.path.startsWith(item.routePath + "/");
}
</script>

<template>
  <nav class="workspace-nav" aria-label="工作区页面">
    <section v-for="group in groups" :key="group.name" class="nav-group">
      <h2 v-if="!collapsed && group.name !== '工作区'" class="nav-group-label">{{ group.name }}</h2>
      <RouterLink v-for="item in group.items" :key="item.routePath" :to="item.routePath"
        class="workspace-nav-link" :class="{ selected: selected(item) }"
        :title="collapsed ? item.name : undefined" :aria-label="item.name"
        :aria-current="selected(item) ? 'page' : undefined">
        <AppIcon :name="item.icon" :size="18" /><span v-if="!collapsed">{{ item.name }}</span>
      </RouterLink>
    </section>
  </nav>
</template>
