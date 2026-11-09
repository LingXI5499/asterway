<!-- 渲染Navigation数据列表界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({ /* nodes字段使用Array。 */ nodes: Array });
// 图标只映射展示语义，导航数据和权限仍完全由服务端菜单决定。
/* iconFor：调用 iconFor 完成iconFor处理。 */
function iconFor(node) {
  const /* route保存优先使用前面的有效值，没有时采用后面的备用值：node.routePath || ""。 */ route = node.routePath || "";
  const /* 键列表（keys）保存按表达式计算得到的值：[ ["/dashboard", "home"], ["/growth/goals", "goal"], ["/growth/stages", "stage"], ["/growth/tasks", "task"], ["weekly", …。 */ keys = [
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
  const /* match保存取得第一条符合条件的数据。 */ match = keys.find(([part]) => route.includes(part));
  /* 检查条件 match 是否成立，成立时走这一分支。 */
  if (match) /* 返回表达式 match[1] 的结果，当前函数到此结束。 */ return match[1];
  /* 返回沿 node.code?.startsWith("growth") ? "goal" : node.code?.includes("system") ? "settings" : "user" 读取数据；中间对象为空时返回空值，当前函数到此结束。 */
  return node.code?.startsWith("growth")
    ? "goal"
    : node.code?.includes("system")
      ? "settings"
      : "user";
}
</script>
<template>
  <!-- 显示只组织渲染规则、不额外生成普通容器的模板；按 node in nodes 为每条数据重复生成内容；key 取自 node.id。 -->
  <template v-for="node in nodes" :key="node.id"
    ><!-- 显示Element Plus 的 sub-menu 组件；仅当 node.children?.length 成立时渲染；index 取自 node.id。 --><el-sub-menu v-if="node.children?.length" :index="node.id"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 title 插槽。 --><template #title
        ><!-- 显示Element Plus 的 icon 组件。 --><el-icon><!-- 显示应用Icon（AppIcon）组件；name 取自 iconFor(node)。 --><AppIcon :name="iconFor(node)" /></el-icon
        ><!-- 显示行内文字区域。 --><span><!-- 把 node.name 的当前值转成文字显示。 -->{{ node.name }}</span></template
      ><!-- 显示Navigation数据列表（NavigationItems）组件；nodes 取自 node.children。 --><NavigationItems :nodes="node.children" /></el-sub-menu
    ><!-- 显示Element Plus 的 menu-item 组件；前一条件不成立且 node.menuType === 'PAGE' 成立时渲染；index 取自 node.routePath。 --><el-menu-item v-else-if="node.menuType === 'PAGE'" :index="node.routePath"
      ><!-- 显示Element Plus 的 icon 组件。 --><el-icon><!-- 显示应用Icon（AppIcon）组件；name 取自 iconFor(node)。 --><AppIcon :name="iconFor(node)" /></el-icon
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 title 插槽。 --><template #title><!-- 把 node.name 的当前值转成文字显示。 -->{{ node.name }}</template></el-menu-item
    ></template
  >
</template>
