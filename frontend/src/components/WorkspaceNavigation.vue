<!-- 渲染工作区Navigation界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute } from "vue-router";
/* 引入 ../composables/workspaceNavigation 提供的组件、状态函数或请求方法。 */
import { workspacePages, isPlanningPath } from "../composables/workspaceNavigation";

const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* nodes字段使用表达式 { type: Array, default: () => [] } 的结果。 */ nodes: { /* 类型（type）字段使用Array。 */ type: Array, /* 默认值（default）字段使用表达式 () => [] 的结果。 */ default: () => [] }, /* collapsed字段使用可为空的真假值（Boolean）。 */ collapsed: Boolean });
const /* route保存取得当前页面路径和查询参数。 */ route = useRoute();
const /* 页数（pages）保存随相关状态变化自动重算的值。 */ pages = computed(() => workspacePages(props.nodes));
const /* 分组列表（groups）保存随相关状态变化自动重算的值。 */ groups = computed(() => {
  const /* 已读（seen）保存新建 设置（Set） 实例。 */ seen = new Set();
  const /* 程序入口（main）保存一个空列表，用来保存后续多条数据。 */ main = [], /* 规划（planning）保存一个空列表，用来保存后续多条数据。 */ planning = [], /* management保存一个空列表，用来保存后续多条数据。 */ management = [];
  /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
  for (const /* 声明 节点（node），稍后再填入处理结果。 */ node of pages.value) {
    const /* 路径（path）保存表达式 node.routePath 的结果。 */ path = node.routePath;
    /* 检查条件 path.startsWith("/agent/") || ["/habits/categories", "/statistics/timeline", "/execution/records"].includes(path) 是否成立，成立时走这一分支。 */
    if (path.startsWith("/agent/") || ["/habits/categories", "/statistics/timeline", "/execution/records"].includes(path)) continue;
    /* 检查条件 seen.has(path) || path === "/notifications" 是否成立，成立时走这一分支。 */
    if (seen.has(path) || path === "/notifications") continue;
    /* 加入集合或登记一条待保存记录。 */
    seen.add(path);
    const /* 单条数据（item）保存表达式 { ...node, icon: iconFor(path) } 的结果。 */ item = { ...node, /* icon字段使用调用 iconFor 完成iconFor处理。 */ icon: iconFor(path) };
    /* 检查条件 path.startsWith("/system") 是否成立，成立时走这一分支。 */
    if (path.startsWith("/system")) /* 调用 management.push 完成push处理。 */ management.push(item);
    else /* 检查条件 path.startsWith("/account") 是否成立，成立时走这一分支。 */ if (path.startsWith("/account")) continue;
    else /* 检查条件 isPlanningPath(path) 是否成立，成立时走这一分支。 */ if (isPlanningPath(path)) /* 调用 planning.push 完成push处理。 */ planning.push(item);
    else /* 调用 main.push 完成push处理。 */ main.push(item);
  }
  /* 检查条件 planning.length 是否成立，成立时走这一分支。 */
  if (planning.length) {
    const /* 首次（first）保存优先使用前面的有效值，没有时采用后面的备用值：planning.find(item => item.routePath === "/growth/goals") || planning[0]。 */ first = planning.find(item => item.routePath === "/growth/goals") || planning[0];
    /* 调用 main.push 完成push处理。 */
    main.push({ ...first, /* 名称（name）字段使用固定文字 '目标与计划'。 */ name: "目标与计划", /* icon字段使用固定文字 'goal'。 */ icon: "goal", /* 规划（planning）字段使用真，表示条件成立或开关打开。 */ planning: true });
  }
  const /* 顺序（order）保存按表达式计算得到的值：["/agent", "/dashboard", "planning", "/execution", "/habits", "/statistics"]。 */ order = ["/agent", "/dashboard", "planning", "/execution", "/habits", "/statistics"];
  const /* rank保存根据前面的条件，从两个候选结果中选择一个：item => { const index = order.indexOf(item.planning ? "planning" : item.routePath); return index < 0…。 */ rank = item => { const /* 索引（index）保存根据前面的条件，从两个候选结果中选择一个：order.indexOf(item.planning ? "planning" : item.routePath)。 */ index = order.indexOf(item.planning ? "planning" : item.routePath); /* 返回根据前面的条件，从两个候选结果中选择一个：index < 0 ? order.length : index，当前函数到此结束。 */ return index < 0 ? order.length : index; };
  /* 按指定规则排列数据。 */
  main.sort((a, b) => rank(a) - rank(b));
  // 账户入口统一放在左下角，侧栏只展示工作内容。
  /* 返回集合或文字的长度，当前函数到此结束。 */
  return [
    { /* 名称（name）字段使用固定文字 '工作区'。 */ name: "工作区", /* 数据列表（items）字段使用程序入口（main）。 */ items: main },
    { /* 名称（name）字段使用固定文字 '系统管理'。 */ name: "系统管理", /* 数据列表（items）字段使用management。 */ items: management },
  ].filter(group => group.items.length);
});
/* iconFor：调用 iconFor 完成iconFor处理。 */
function iconFor(path) {
  /* 返回按表达式计算得到的值：[ ["/agent", "sparkle"], ["/dashboard", "home"], ["/execution", "execution"], ["/habits", "habit"], ["/statistics", "sta…，当前函数到此结束。 */
  return [
    ["/agent", "sparkle"], ["/dashboard", "home"], ["/execution", "execution"],
    ["/habits", "habit"], ["/statistics", "statistics"], ["goals", "goal"],
    ["stages", "stage"], ["tasks", "task"], ["weekly", "week"], ["daily", "day"],
    ["pending", "inbox"], ["users", "users"], ["roles", "role"],
    ["permissions", "permission"], ["menus", "menu"], ["sessions", "device"],
    ["logs", "history"], ["settings", "settings"],
  ].find(([part]) => path.includes(part))?.[1] || "folder";
}
/* 已选（selected）：调用 selected 完成已选处理。 */
function selected(item) {
  /* 返回根据前面的条件，从两个候选结果中选择一个：item.planning ? isPlanningPath(route.path) : route.path === item.routePath || route.path.startsWith(…，当前函数到此结束。 */
  return item.planning ? isPlanningPath(route.path) : route.path === item.routePath || route.path.startsWith(item.routePath + "/");
}
</script>

<template>
  <!-- 显示导航区域；用样式类 workspace-nav 控制外观；aria-label 提供无障碍说明 工作区页面。 -->
  <nav class="workspace-nav" aria-label="工作区页面">
    <!-- 显示一个有独立含义的内容区；按 group in groups 为每条数据重复生成内容；key 取自 group.name；用样式类 nav-group 控制外观。 -->
    <section v-for="group in groups" :key="group.name" class="nav-group">
      <!-- 显示二级标题；仅当 !collapsed && group.name !== '工作区' 成立时渲染；用样式类 nav-group-label 控制外观。 -->
      <h2 v-if="!collapsed && group.name !== '工作区'" class="nav-group-label"><!-- 把 group.name 的当前值转成文字显示。 -->{{ group.name }}</h2>
      <!-- 显示应用内部页面链接；按 item in group.items 为每条数据重复生成内容；key 取自 item.routePath；to 取自 item.routePath；用样式类 workspace-nav-link 控制外观；class 取自 { selected: selected(item) }；title 取自 collapsed ? item.name : undefined；aria-label 取自 item.name；aria-current 取自 selected(item) ? 'page' : undefined。 -->
      <RouterLink v-for="item in group.items" :key="item.routePath" :to="item.routePath"
        class="workspace-nav-link" :class="{ selected: selected(item) }"
        :title="collapsed ? item.name : undefined" :aria-label="item.name"
        :aria-current="selected(item) ? 'page' : undefined">
        <!-- 显示应用Icon（AppIcon）组件；name 取自 item.icon；size 取自 18。 -->
        <AppIcon :name="item.icon" :size="18" /><!-- 显示行内文字区域；仅当 !collapsed 成立时渲染。 --><span v-if="!collapsed"><!-- 把 item.name 的当前值转成文字显示。 -->{{ item.name }}</span>
      </RouterLink>
    </section>
  </nav>
</template>
