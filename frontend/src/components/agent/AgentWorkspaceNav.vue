<script setup>
import { computed } from "vue";
import { canGrow } from "../../api/growth";
const links = computed(() => [
  ["/agent/memories", "长期记忆", "agent:memory:read", "memory"],
  ["/agent/knowledge", "知识资料", "agent:knowledge:read", "book"],
  ["/agent/runs", "运行记录", "agent:run:read", "history"],
  ["/agent/reviews", "成长复盘", "agent:review:read", "statistics"],
].filter((item) => canGrow(item[2])));
</script>

<template>
  <nav class="assistant-navigation" aria-label="成长助手功能">
    <RouterLink v-for="item in links" :key="item[0]" :to="item[0]"
      :class="{ selected: item[0] === '/agent' ? $route.path === item[0] : $route.path.startsWith(item[0]) }">
      <AppIcon :name="item[3]" :size="18" />{{ item[1] }}
    </RouterLink>
  </nav>
</template>

<style scoped>
.assistant-navigation { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 32px; padding-bottom: 16px; border-bottom: 1px solid var(--aw-border); }
a { display: inline-flex; align-items: center; gap: 8px; color: var(--aw-text-secondary); padding: 8px 12px; border-radius: 8px; font-size: 14px; line-height: 20px; text-decoration: none; }
a:hover, a.selected { background: var(--aw-surface-muted); color: var(--aw-text); }
a.selected { font-weight: 600; }
a:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
</style>
