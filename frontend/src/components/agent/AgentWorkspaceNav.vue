<!-- 渲染成长助手工作区Nav界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
const /* links保存随相关状态变化自动重算的值。 */ links = computed(() => [
  ["/agent/memories", "长期记忆", "agent:memory:read", "memory"],
  ["/agent/knowledge", "知识库", "agent:knowledge:read", "book"],
  ["/agent/runs", "运行记录", "agent:run:read", "history"],
  ["/agent/reviews", "成长复盘", "agent:review:read", "statistics"],
].filter((item) => canGrow(item[2])));
</script>

<template>
  <!-- 显示导航区域；用样式类 assistant-navigation 控制外观；aria-label 提供无障碍说明 成长助手功能。 -->
  <nav class="assistant-navigation" aria-label="成长助手功能">
    <!-- 显示应用内部页面链接；按 item in links 为每条数据重复生成内容；key 取自 item[0]；to 取自 item[0]；class 取自 { selected: item[0] === '/agent' ? $route.path === item[0] : $route.path.startsWith(item[0]) }。 -->
    <RouterLink v-for="item in links" :key="item[0]" :to="item[0]"
      :class="{ selected: item[0] === '/agent' ? $route.path === item[0] : $route.path.startsWith(item[0]) }">
      <!-- 显示应用Icon（AppIcon）组件；name 取自 item[3]；size 取自 18。 -->
      <AppIcon :name="item[3]" :size="18" /><!-- 把 item[1] 的当前值转成文字显示。 -->{{ item[1] }}
    </RouterLink>
  </nav>
</template>

<style scoped>
/* 以下规则应用于 .assistant-navigation 选中的元素。 */
.assistant-navigation { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置下方外侧留白；取值 32px。 */ margin-bottom: 32px; /* 设置底部内侧留白；取值 16px。 */ padding-bottom: 16px; /* 设置底部边框；取值 1px solid var(—aw-border)。 */ border-bottom: 1px solid var(--aw-border); }
/* 以下规则应用于 a 选中的元素。 */
a { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */ display: inline-flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置内容与边框之间的内侧留白；取值 8px 12px。 */ padding: 8px 12px; /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置下划线等文字装饰；取值 none。 */ text-decoration: none; }
/* 以下规则应用于 a:hover, a.selected 选中的元素。 */
a:hover, a.selected { /* 设置背景颜色、图片或渐变；取值 var(—aw-surface-muted)。 */ background: var(--aw-surface-muted); /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); }
/* 以下规则应用于 a.selected 选中的元素。 */
a.selected { /* 设置字重；取值 600。 */ font-weight: 600; }
/* 以下规则应用于 a:focus-visible 选中的元素。 */
a:focus-visible { /* 设置不占布局尺寸的轮廓线；取值 2px solid var(—el-color-primary)。 */ outline: 2px solid var(--el-color-primary); /* 设置轮廓线与边界的距离；取值 2px。 */ outline-offset: 2px; }
</style>
