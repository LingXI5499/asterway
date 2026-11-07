<script setup>
defineProps({ nodes: Array });
// 图标只映射展示语义，导航数据和权限仍完全由服务端菜单决定。
function iconFor(node) {
  const route = node.routePath || "";
  const keys = [
    ["/dashboard", "home"],
    ["/growth/goals", "goal"],
    ["/growth/stages", "stage"],
    ["/growth/tasks", "task"],
    ["weekly", "week"],
    ["daily", "day"],
    ["pending", "inbox"],
    ["/execution", "execution"],
    ["categories", "folder"],
    ["/habits", "habit"],
    ["/notifications", "notification"],
    ["/statistics", "statistics"],
    ["/agent", "sparkle"],
    ["/system/users", "users"],
    ["/system/roles", "role"],
    ["permissions", "permission"],
    ["/system/menus", "menu"],
    ["sessions", "device"],
    ["logs", "history"],
    ["settings", "settings"],
    ["devices", "device"],
    ["profile", "user"],
  ];
  const match = keys.find(([part]) => route.includes(part));
  if (match) return match[1];
  return node.code?.startsWith("growth")
    ? "goal"
    : node.code?.includes("system")
      ? "settings"
      : "user";
}
</script>
<template>
  <template v-for="node in nodes" :key="node.id"
    ><el-sub-menu v-if="node.children?.length" :index="node.id"
      ><template #title
        ><el-icon><AppIcon :name="iconFor(node)" /></el-icon
        ><span>{{ node.name }}</span></template
      ><NavigationItems :nodes="node.children" /></el-sub-menu
    ><el-menu-item v-else-if="node.menuType === 'PAGE'" :index="node.routePath"
      ><el-icon><AppIcon :name="iconFor(node)" /></el-icon
      ><template #title>{{ node.name }}</template></el-menu-item
    ></template
  >
</template>
