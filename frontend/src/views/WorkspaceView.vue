<!-- 渲染工作区页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import {
  computed,
  ref,
  onMounted,
  onBeforeUnmount,
  nextTick,
  watch,
  provide,
} from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute, useRouter } from "vue-router";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api, session, clearSession, hasPermission } from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../api/ui";
/* 引入 ../components/WorkspaceNavigation.vue 提供的组件、状态函数或请求方法。 */
import WorkspaceNavigation from "../components/WorkspaceNavigation.vue";
/* 引入 ../api/growth 提供的组件、状态函数或请求方法。 */
import { isManager, canGrow } from "../api/growth";
/* 引入 ../stores/personal 提供的组件、状态函数或请求方法。 */
import { usePersonalRuntime, messages, execution } from "../stores/personal";
/* 引入 ../components/personal/ExecutionBar.vue 提供的组件、状态函数或请求方法。 */
import ExecutionBar from "../components/personal/ExecutionBar.vue";
/* 引入 ../composables/workspaceNavigation 提供的组件、状态函数或请求方法。 */
import { workspacePages, isPlanningPath, planningNavigation } from "../composables/workspaceNavigation";
const /* screen保存调用 window.matchMedia 完成matchMedia处理。 */ screen = window.matchMedia("(max-width: 899px)");
const /* mobile保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ mobile = ref(screen.matches);
/* 调用 usePersonalRuntime 完成use个人Runtime处理。 */
usePersonalRuntime();
const /* route保存取得当前页面路径和查询参数。 */ route = useRoute(),
  /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */
  router = useRouter(),
  /* collapsed保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  collapsed = ref(
    mobile.value ||
      sessionStorage.getItem("asterway.sidebar.collapsed") === "true",
  ),
  /* site名称（siteName）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  siteName = ref("Asterway");
// 菜单是布局状态，与业务页面的数据加载相互独立。
const /* sidebar响应式引用（sidebarRef）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ sidebarRef = ref(null);
const /* sidebarButton保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ sidebarButton = ref(null);
const /* assistant保存随相关状态变化自动重算的值。 */ assistant = computed(() => route.path === "/agent" || route.path.startsWith("/agent/"));
const /* 规划（planning）保存随相关状态变化自动重算的值。 */ planning = computed(() => isPlanningPath(route.path));
const /* 规划Links（planningLinks）保存随依赖自动重算的筛选结果或数量。 */ planningLinks = computed(() => planningNavigation(workspacePages(menus.value)));
const /* SIDEBAR最小（SIDEBAR_MIN）保存固定数值 200。 */ SIDEBAR_MIN = 200, /* SIDEBAR最大（SIDEBAR_MAX）保存固定数值 480。 */ SIDEBAR_MAX = 480, /* SIDEBAR默认值（SIDEBAR_DEFAULT）保存固定数值 256。 */ SIDEBAR_DEFAULT = 256;
/* 已保存Sidebar宽度（savedSidebarWidth）：保存 dSidebar宽度（dSidebarWidth）。 */
function savedSidebarWidth() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 值（value）保存把输入转换成数值，供比较、计算或按编号查询。 */ value = Number(localStorage.getItem("asterway.ui.sidebarWidth"));
    /* 返回根据前面的条件，从两个候选结果中选择一个：value >= SIDEBAR_MIN && value <= SIDEBAR_MAX ? value : SIDEBAR_DEFAULT，当前函数到此结束。 */
    return value >= SIDEBAR_MIN && value <= SIDEBAR_MAX ? value : SIDEBAR_DEFAULT;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch { /* 返回SIDEBAR默认值（SIDEBAR_DEFAULT），当前函数到此结束。 */ return SIDEBAR_DEFAULT; }
}
const /* sidebar宽度（sidebarWidth）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ sidebarWidth = ref(savedSidebarWidth());
const /* resizing保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ resizing = ref(false);
let /* resize原始（resizeOrigin）保存空值，表示当前还没有对应内容。 */ resizeOrigin = null;
/* 持久保存Sidebar宽度（persistSidebarWidth）：调用 persistSidebarWidth 完成持久保存Sidebar宽度处理。 */
function persistSidebarWidth() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { /* 设置 单条数据（Item） 的值。 */ localStorage.setItem("asterway.ui.sidebarWidth", String(sidebarWidth.value)); } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {}
}
/* resize开始（resizeStart）：调用 resizeStart 完成resize开始处理。 */
function resizeStart(event) {
  /* 检查条件 mobile.value || collapsed.value || event.button !== 0 是否成立，成立时走这一分支。 */
  if (mobile.value || collapsed.value || event.button !== 0) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把表达式 { pointer: event.pointerId, x: event.clientX, width: sidebarWidth.value } 的结果写入 resizeOrigin。 */
  resizeOrigin = { /* pointer字段使用表达式 event.pointerId 的结果。 */ pointer: event.pointerId, /* x字段使用表达式 event.clientX 的结果。 */ x: event.clientX, /* 宽度（width）字段使用表达式 sidebarWidth.value 的结果。 */ width: sidebarWidth.value };
  /* 设置 PointerCapture 的值。 */
  event.currentTarget.setPointerCapture(event.pointerId);
  /* 把真，表示条件成立或开关打开写入 resizing.value。 */
  resizing.value = true;
  /* 调用 event.preventDefault 完成prevent默认值处理。 */
  event.preventDefault();
}
/* resizeMove：调用 resizeMove 完成resizeMove处理。 */
function resizeMove(event) {
  /* 检查条件 !resizeOrigin || resizeOrigin.pointer !== event.pointerId 是否成立，成立时走这一分支。 */
  if (!resizeOrigin || resizeOrigin.pointer !== event.pointerId) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把调用 Math.round 完成轮处理写入 sidebarWidth.value。 */
  sidebarWidth.value = Math.round(Math.max(SIDEBAR_MIN, Math.min(SIDEBAR_MAX, resizeOrigin.width + event.clientX - resizeOrigin.x)));
}
/* resize结束（resizeEnd）：调用 resizeEnd 完成resize结束处理。 */
function resizeEnd() {
  /* 检查条件 !resizeOrigin 是否成立，成立时走这一分支。 */
  if (!resizeOrigin) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把空值，表示当前还没有对应内容写入 resizeOrigin。 */
  resizeOrigin = null;
  /* 把假，表示条件不成立或开关关闭写入 resizing.value。 */
  resizing.value = false;
  /* 调用 persistSidebarWidth 完成持久保存Sidebar宽度处理。 */
  persistSidebarWidth();
}
/* resize键（resizeKey）：调用 resizeKey 完成resize键处理。 */
function resizeKey(event) {
  const /* step保存根据前面的条件，从两个候选结果中选择一个：event.shiftKey ? 32 : 16。 */ step = event.shiftKey ? 32 : 16;
  const /* 修改（change）保存表达式 { ArrowLeft: -step, ArrowRight: step }[event.key] 的结果。 */ change = { /* ArrowLeft字段使用表达式 -step 的结果。 */ ArrowLeft: -step, /* ArrowRight字段使用step。 */ ArrowRight: step }[event.key];
  /* 检查change == null && !["Home", "End", "Enter"].includes(event.key)，即对应内容是否不存在，成立时走这一分支。 */
  if (change == null && !["Home", "End", "Enter"].includes(event.key)) /* 返回空内容，当前函数到此结束。 */ return;
  /* 调用 event.preventDefault 完成prevent默认值处理。 */
  event.preventDefault();
  /* 把根据前面的条件，从两个候选结果中选择一个：event.key === "Home" ? SIDEBAR_MIN : event.key === "End" ? SIDEBAR_MAX : event.key === "Enter" ? SID…写入 sidebarWidth.value。 */
  sidebarWidth.value = event.key === "Home" ? SIDEBAR_MIN : event.key === "End" ? SIDEBAR_MAX : event.key === "Enter" ? SIDEBAR_DEFAULT : Math.max(SIDEBAR_MIN, Math.min(SIDEBAR_MAX, sidebarWidth.value + change));
  /* 调用 persistSidebarWidth 完成持久保存Sidebar宽度处理。 */
  persistSidebarWidth();
}
/* resetSidebar宽度（resetSidebarWidth）：调用 resetSidebarWidth 完成resetSidebar宽度处理。 */
function resetSidebarWidth() { /* 把SIDEBAR默认值（SIDEBAR_DEFAULT）写入 sidebarWidth.value。 */ sidebarWidth.value = SIDEBAR_DEFAULT; /* 调用 persistSidebarWidth 完成持久保存Sidebar宽度处理。 */ persistSidebarWidth(); }
/* toggleSidebar：调用 toggleSidebar 完成toggleSidebar处理。 */
async function toggleSidebar() {
  /* 把条件判断的结果：!collapsed.value写入 collapsed.value。 */
  collapsed.value = !collapsed.value;
  /* 检查条件 !mobile.value 是否成立，成立时走这一分支。 */
  if (!mobile.value)
    /* 设置 单条数据（Item） 的值。 */
    sessionStorage.setItem(
      "asterway.sidebar.collapsed",
      String(collapsed.value),
    );
  /* 检查条件 !collapsed.value 是否成立，成立时走这一分支。 */
  if (!collapsed.value) {
    /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
    await nextTick();
    /* 检查条件 mobile.value 是否成立，成立时走这一分支。 */
    if (mobile.value)
      /* 调用 sidebarRef.value?.querySelector 完成查询条件Selector处理。 */
      sidebarRef.value?.querySelector("a,button,input,summary,[tabindex='0']")?.focus();
  } else /* 检查条件 mobile.value 是否成立，成立时走这一分支。 */ if (mobile.value) {
    /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
    await nextTick();
    /* 调用 sidebarButton.value?.$el?.focus 完成focus处理。 */
    sidebarButton.value?.$el?.focus();
  }
}
/* 调用 provide 完成provide处理。 */
provide("workspaceSidebar", {
  /* toggle字段使用toggleSidebar。 */
  toggle: toggleSidebar,
  /* 关闭（close）字段使用表达式 () => { if (mobile.value) collapsed.value = true; } 的结果。 */
  close: () => { /* 检查条件 mobile.value 是否成立，成立时走这一分支。 */ if (mobile.value) /* 把真，表示条件成立或开关打开写入 collapsed.value。 */ collapsed.value = true; },
});
/* sidebarKeydown：调用 sidebarKeydown 完成sidebarKeydown处理。 */
function sidebarKeydown(event) {
  /* 检查条件 !mobile.value || collapsed.value 是否成立，成立时走这一分支。 */
  if (!mobile.value || collapsed.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 event.key === "Escape" 是否成立，成立时走这一分支。 */
  if (event.key === "Escape") {
    /* 调用 event.preventDefault 完成prevent默认值处理。 */
    event.preventDefault();
    /* 调用 toggleSidebar 完成toggleSidebar处理。 */
    toggleSidebar();
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 检查条件 event.key !== "Tab" 是否成立，成立时走这一分支。 */
  if (event.key !== "Tab") /* 返回空内容，当前函数到此结束。 */ return;
  const /* nodes保存集合或文字的长度。 */ nodes = [
    ...sidebarRef.value.querySelectorAll("a[href],button,input,summary,[tabindex='0']"),
  ].filter((el) => el.getClientRects().length && !el.disabled);
  const /* 首次（first）保存表达式 nodes[0] 的结果。 */ first = nodes[0],
    /* 最后一次（last）保存调用 nodes.at 完成at处理。 */
    last = nodes.at(-1);
  /* 检查条件 event.shiftKey && document.activeElement === first 是否成立，成立时走这一分支。 */
  if (event.shiftKey && document.activeElement === first) {
    /* 调用 event.preventDefault 完成prevent默认值处理。 */
    event.preventDefault();
    /* 调用 last?.focus 完成focus处理。 */
    last?.focus();
  } else /* 检查条件 !event.shiftKey && document.activeElement === last 是否成立，成立时走这一分支。 */ if (!event.shiftKey && document.activeElement === last) {
    /* 调用 event.preventDefault 完成prevent默认值处理。 */
    event.preventDefault();
    /* 调用 first?.focus 完成focus处理。 */
    first?.focus();
  }
}
/* 用户Command（userCommand）：调用 userCommand 完成用户Command处理。 */
function userCommand(command) {
  /* 检查条件 command === "logout" 是否成立，成立时走这一分支。 */
  if (command === "logout") /* 调用 logout 完成退出登录处理。 */ logout();
  else /* 调用 router.push 完成push处理。 */ router.push(command);
}
const /* 菜单列表（menus）保存随依赖自动重算的筛选结果或数量。 */ menus = computed(() => {
  /* 筛选（filter）：只保留符合条件的条目。 */
  function filter(nodes) {
    /* 返回沿 nodes .filter( (n) => n.visible && n.enabled && n.menuType !== "BUTTON" && !( isManager() && (n.code?.startsWith("g… 读取数据；中间对象为空时返回空值，当前函数到此结束。 */
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
      .map((n) => ({ ...n, /* 下级列表（children）字段使用优先使用前面的有效值，没有时采用后面的备用值：filter(n.children || [])。 */ children: filter(n.children || []) }));
  }
  /* 返回只保留符合条件的条目，当前函数到此结束。 */
  return filter(session.menus);
});
const /* crumbs保存随相关状态变化自动重算的值。 */ crumbs = computed(() => {
  /* 检查条件 !route.meta.growth 是否成立，成立时走这一分支。 */
  if (!route.meta.growth)
    /* 返回按表达式计算得到的值：[ { name: "账户中心", path: "/account/profile" }, { name: route.meta.title }, ]，当前函数到此结束。 */
    return [
      { /* 名称（name）字段使用固定文字 '账户中心'。 */ name: "账户中心", /* 路径（path）字段使用固定文字 '/account/profile'。 */ path: "/account/profile" },
      { /* 名称（name）字段使用表达式 route.meta.title 的结果。 */ name: route.meta.title },
    ];
  const /* parts保存根据前面的条件，从两个候选结果中选择一个：[ { name: "个人工作区", path: canGrow("dashboard:board:read") ? "/dashboard" : "/growth/goals", }, ]。 */ parts = [
    {
      /* 名称（name）字段使用固定文字 '个人工作区'。 */
      name: "个人工作区",
      /* 路径（path）字段使用根据前面的条件，从两个候选结果中选择一个：canGrow("dashboard:board:read") ? "/dashboard" : "/growth/goals"。 */
      path: canGrow("dashboard:board:read") ? "/dashboard" : "/growth/goals",
    },
  ];
  /* 检查条件 route.meta.parent 是否成立，成立时走这一分支。 */
  if (route.meta.parent) /* 调用 parts.push 完成push处理。 */ parts.push(route.meta.parent);
  /* 检查条件 /^\/growth\/(goals|stages|tasks)\/[^/]+$/.test(route.path) 是否成立，成立时走这一分支。 */
  if (/^\/growth\/(goals|stages|tasks)\/[^/]+$/.test(route.path)) {
    const /* resource保存按指定分隔规则拆分文字。 */ resource = route.path.split("/")[2];
    /* 调用 parts.push 完成push处理。 */
    parts.push({
      /* 名称（name）字段使用表达式 { goals: "目标管理", stages: "阶段管理", tasks: "任务中心" }[ resource ] 的结果。 */
      name: { /* 目标列表（goals）字段使用固定文字 '目标管理'。 */ goals: "目标管理", /* 阶段列表（stages）字段使用固定文字 '阶段管理'。 */ stages: "阶段管理", /* 任务列表（tasks）字段使用固定文字 '任务中心'。 */ tasks: "任务中心" }[
        resource
      ],
      /* 路径（path）字段使用按表达式计算得到的值："/growth/" + resource。 */
      path: "/growth/" + resource,
    });
  }
  /* 调用 parts.push 完成push处理。 */
  parts.push({ /* 名称（name）字段使用表达式 route.meta.title 的结果。 */ name: route.meta.title });
  /* 返回parts，当前函数到此结束。 */
  return parts;
});
/* 退出登录（logout）：调用 logout 完成退出登录处理。 */
async function logout() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction("确定退出当前登录？", "退出登录");
    /* 等待调用 api 完成接口处理。 */
    await api("/me/sessions/current", { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" });
    /* 调用 clearSession 完成清除会话处理。 */
    clearSession();
    /* 生成保留原对象其余字段的新副本，只替换明确指定的值。 */
    router.replace("/login");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(async () => {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把按表达式计算得到的值：(await api("/public/account-settings")).siteName写入 siteName.value。 */
    siteName.value = (await api("/public/account-settings")).siteName;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {}
});
const /* resize保存优先使用前面的有效值，没有时采用后面的备用值：(event) => { mobile.value = event.matches; collapsed.value = event.matches || sessionStorage.getItem("asterway…。 */ resize = (event) => {
  /* 把表达式 event.matches 的结果写入 mobile.value。 */
  mobile.value = event.matches;
  /* 把优先使用前面的有效值，没有时采用后面的备用值：event.matches || sessionStorage.getItem("asterway.sidebar.collapsed") === "true"写入 collapsed.value。 */
  collapsed.value =
    event.matches ||
    sessionStorage.getItem("asterway.sidebar.collapsed") === "true";
};
/* 调用 screen.addEventListener 完成加入事件Listener处理。 */
screen.addEventListener("change", resize);
/* 移除 事件Listener（EventListener）。 */
onBeforeUnmount(() => screen.removeEventListener("change", resize));
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => route.path,
  () => {
    /* 检查条件 mobile.value 是否成立，成立时走这一分支。 */
    if (mobile.value) /* 把真，表示条件成立或开关打开写入 collapsed.value。 */ collapsed.value = true;
  },
);
/* 生成保留原对象其余字段的新副本，只替换明确指定的值。 */
watch(isManager, (manager) => {
  /* 检查条件 manager && route.meta.growth 是否成立，成立时走这一分支。 */
  if (manager && route.meta.growth) /* 生成保留原对象其余字段的新副本，只替换明确指定的值。 */ router.replace("/");
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 shell 控制外观；style 取自 { '—aw-sidebar-width': sidebarWidth + 'px' }；class 取自 { 'has-execution': !!execution.current, 'assistant-shell': assistant, 'sidebar-is-collapsed': collapsed, …。 -->
  <div class="shell" :style="{ '--aw-sidebar-width': sidebarWidth + 'px' }" :class="{ 'has-execution': !!execution.current, 'assistant-shell': assistant, 'sidebar-is-collapsed': collapsed, 'is-resizing': resizing }">
    <!-- 显示链接；用样式类 skip-link 控制外观；href 固定为 #main-content。 -->
    <a class="skip-link" href="#main-content">跳至内容</a>
    <!-- 显示侧边区域；id 固定为 workspace-navigation；把组件或元素引用保存为 sidebarRef，供脚本读取；仅当 !session.user?.mustChangePassword 成立时渲染；用样式类 sidebar 控制外观；inert 取自 mobile && collapsed ? true : undefined；class 取自 { collapsed }；aria-label 提供无障碍说明 主导航；keydown 事件触发时执行 sidebarKeydown。 -->
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
      <!-- 显示一个布局容器；用样式类 sidebar-brand 控制外观。 -->
      <div class="sidebar-brand"><!-- 显示routerlink（router-link）组件；to 固定为 /；用样式类 logo 控制外观；aria-label 取自 siteName + ' 工作区首页'。 --><router-link to="/" class="logo" :aria-label="siteName + ' 工作区首页'">
        <!-- 显示行内文字区域；用样式类 brand-symbol 控制外观。 -->
        <span class="brand-symbol">A</span
        ><!-- 显示行内文字区域；仅当 !collapsed || mobile 成立时渲染；用样式类 brand-copy 控制外观。 --><span v-if="!collapsed || mobile" class="brand-copy"
          ><!-- 把 siteName 的当前值转成文字显示。 -->{{ siteName }}<!-- 显示辅助说明文字。 --><small>让成长有迹可循</small></span>
        </router-link>
        <!-- 显示Element Plus 的 button 组件；仅当 !collapsed || mobile 成立时渲染；启用 text 属性；用样式类 sidebar-close icon-button 控制外观；aria-label 提供无障碍说明 收起导航；click 事件触发时执行 toggleSidebar。 -->
        <el-button v-if="!collapsed || mobile" text class="sidebar-close icon-button" aria-label="收起导航" @click="toggleSidebar"><!-- 显示应用Icon（AppIcon）组件；name 固定为 closeSidebar。 --><AppIcon name="closeSidebar" /></el-button>
      </div>
      <!-- 显示一个布局容器；id 固定为 assistant-new-chat；由 assistant && (!collapsed || mobile) 决定显示或隐藏，组件仍保留。 -->
      <div id="assistant-new-chat" v-show="assistant && (!collapsed || mobile)"></div>
      <!-- 显示一个布局容器；用样式类 sidebar-scroll 控制外观。 -->
      <div class="sidebar-scroll">
        <!-- 显示工作区Navigation（WorkspaceNavigation）组件；nodes 取自 menus；collapsed 取自 collapsed && !mobile。 -->
        <WorkspaceNavigation :nodes="menus" :collapsed="collapsed && !mobile" />
        <!-- 显示一个布局容器；id 固定为 assistant-conversations；由 assistant && (!collapsed || mobile) 决定显示或隐藏，组件仍保留；用样式类 sidebar-conversations 控制外观。 -->
        <div id="assistant-conversations" v-show="assistant && (!collapsed || mobile)" class="sidebar-conversations"></div>
      </div>
      <!-- 显示一个布局容器；用样式类 sidebar-account 控制外观。 -->
      <div class="sidebar-account">
        <!-- 显示Element Plus 的 dropdown 组件；trigger 固定为 click；placement 固定为 top-start；command 事件触发时执行 userCommand。 -->
        <el-dropdown trigger="click" placement="top-start" @command="userCommand">
          <!-- 显示可点击按钮；用样式类 sidebar-user 控制外观；类型设为 button；aria-label 提供无障碍说明 打开账户菜单。 -->
          <button class="sidebar-user" type="button" aria-label="打开账户菜单">
            <!-- 显示行内文字区域；用样式类 avatar 控制外观。 -->
            <span class="avatar"><!-- 把 (session.user?.nickname || session.user?.username || "A").slice(0, 1).toUpperCase() 的当前值转成文字显示。 -->{{ (session.user?.nickname || session.user?.username || "A").slice(0, 1).toUpperCase() }}</span>
            <!-- 显示行内文字区域；仅当 !collapsed || mobile 成立时渲染；用样式类 sidebar-user-copy 控制外观。 -->
            <span v-if="!collapsed || mobile" class="sidebar-user-copy"><!-- 显示重点文字。 --><strong><!-- 把 session.user?.nickname || session.user?.username 的当前值转成文字显示。 -->{{ session.user?.nickname || session.user?.username }}</strong><!-- 显示辅助说明文字。 --><small>个人工作区</small></span>
            <!-- 显示应用Icon（AppIcon）组件；仅当 !collapsed || mobile 成立时渲染；name 固定为 more；size 取自 18。 -->
            <AppIcon v-if="!collapsed || mobile" name="more" :size="18" />
          </button>
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
          <template #dropdown><!-- 显示Element Plus 的 dropdown-menu 组件。 --><el-dropdown-menu>
            <!-- 显示Element Plus 的 dropdown-item 组件；仅当 hasPermission('identity:profile:read') 成立时渲染；command 固定为 /account/profile。 -->
            <el-dropdown-item v-if="hasPermission('identity:profile:read')" command="/account/profile">个人资料</el-dropdown-item>
            <!-- 显示Element Plus 的 dropdown-item 组件；仅当 hasPermission('identity:profile:read') 成立时渲染；command 固定为 /account/profile?tab=password。 -->
            <el-dropdown-item v-if="hasPermission('identity:profile:read')" command="/account/profile?tab=password">修改密码</el-dropdown-item>
            <!-- 显示Element Plus 的 dropdown-item 组件；仅当 hasPermission('identity:device:list') 成立时渲染；command 固定为 /account/devices。 -->
            <el-dropdown-item v-if="hasPermission('identity:device:list')" command="/account/devices">我的设备</el-dropdown-item>
            <!-- 显示Element Plus 的 dropdown-item 组件；启用 divided 属性；command 固定为 logout。 -->
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu></template>
        </el-dropdown>
      </div>
      <!-- 显示一个布局容器；仅当 !mobile && !collapsed 成立时渲染；用样式类 sidebar-resize-handle 控制外观；role 固定为 separator；aria-label 提供无障碍说明 调整侧栏宽度；aria-orientation 提供无障碍说明 vertical；aria-valuemin 取自 SIDEBAR_MIN；aria-valuemax 取自 SIDEBAR_MAX；aria-valuenow 取自 sidebarWidth；tabindex 固定为 0；title 固定为 拖动调整宽度，双击恢复默认；pointerdown 事件触发时执行 resizeStart；pointermove 事件触发时执行 resizeMove；pointerup 事件触发时执行 resizeEnd；pointercancel 事件触发时执行 resizeEnd；lostpointercapture 事件触发时执行 resizeEnd；keydown 事件触发时执行 resizeKey；dblclick 事件触发时执行 resetSidebarWidth。 -->
      <div v-if="!mobile && !collapsed" class="sidebar-resize-handle" role="separator" aria-label="调整侧栏宽度" aria-orientation="vertical" :aria-valuemin="SIDEBAR_MIN" :aria-valuemax="SIDEBAR_MAX" :aria-valuenow="sidebarWidth" tabindex="0" title="拖动调整宽度，双击恢复默认" @pointerdown="resizeStart" @pointermove="resizeMove" @pointerup="resizeEnd" @pointercancel="resizeEnd" @lostpointercapture="resizeEnd" @keydown="resizeKey" @dblclick="resetSidebarWidth"></div>
    </aside>
    <!-- 显示可点击按钮；仅当 mobile && !collapsed 成立时渲染；用样式类 sidebar-backdrop 控制外观；aria-label 提供无障碍说明 关闭导航菜单；click 事件触发时执行 toggleSidebar。 -->
    <button
      v-if="mobile && !collapsed"
      class="sidebar-backdrop"
      aria-label="关闭导航菜单"
      @click="toggleSidebar"
    ></button>
    <!-- 显示一个布局容器；用样式类 workspace 控制外观；inert 取自 mobile && !collapsed ? true : undefined。 -->
    <div class="workspace" :inert="mobile && !collapsed ? true : undefined">
      <!-- 显示顶部区域；用样式类 topbar 控制外观。 -->
      <header class="topbar">
        <!-- 显示Element Plus 的 button 组件；把组件或元素引用保存为 sidebarButton，供脚本读取；仅当 !session.user?.mustChangePassword && (collapsed || mobile) 成立时渲染；启用 text 属性；aria-label 提供无障碍说明 展开或收起菜单；aria-expanded 取自 !collapsed；aria-controls 提供无障碍说明 workspace-navigation；用样式类 icon-button 控制外观；click 事件触发时执行 toggleSidebar。 -->
        <el-button
          ref="sidebarButton"
          v-if="!session.user?.mustChangePassword && (collapsed || mobile)"
          text
          aria-label="展开或收起菜单"
          :aria-expanded="!collapsed"
          aria-controls="workspace-navigation"
          class="icon-button"
          @click="toggleSidebar"
          ><!-- 显示应用Icon（AppIcon）组件；name 取自 collapsed ? 'openSidebar' : 'closeSidebar'。 --><AppIcon
            :name="collapsed ? 'openSidebar' : 'closeSidebar'" /></el-button
        ><!-- 显示行内文字区域；仅当 assistant 成立时渲染；用样式类 workspace-title 控制外观。 --><span v-if="assistant" class="workspace-title">成长助手<!-- 显示行内文字区域；用样式类 workspace-title-caption 控制外观。 --><span class="workspace-title-caption">一起，把想法变成行动</span></span><!-- 显示Element Plus 的 breadcrumb 组件；前面条件均不成立时显示这个备用分支；separator 固定为 /。 --><el-breadcrumb v-else separator="/"
          ><!-- 显示Element Plus 的 breadcrumb-item 组件；按 (crumb, index) in crumbs 为每条数据重复生成内容；key 取自 index；to 取自 crumb.path ? { path: crumb.path } : undefined。 --><el-breadcrumb-item
            v-for="(crumb, index) in crumbs"
            :key="index"
            :to="crumb.path ? { path: crumb.path } : undefined"
            ><!-- 把 crumb.name 的当前值转成文字显示。 -->{{ crumb.name }}</el-breadcrumb-item
          ></el-breadcrumb
        >
        <!-- 显示一个布局容器；用样式类 top-actions 控制外观。 -->
        <div class="top-actions">
          <!-- 显示应用ThemeSwitch（AppThemeSwitch）组件。 -->
          <AppThemeSwitch />
          <!-- 显示Element Plus 的 badge 组件；仅当 !session.user?.mustChangePassword && canGrow('notification:message:list') 成立时渲染；value 取自 messages.unread；max 取自 99；hidden 取自 !messages.unread。 -->
          <el-badge
            v-if="
              !session.user?.mustChangePassword &&
              canGrow('notification:message:list')
            "
            :value="messages.unread"
            :max="99"
            :hidden="!messages.unread"
          >
            <!-- 显示Element Plus 的 button 组件；启用 text 属性；title 取自 messages.error || '站内消息'；aria-label 取自 '站内消息，' + messages.unread + ' 条未读'；用样式类 icon-button 控制外观；click 事件触发时执行 router.push('/notifications')。 -->
            <el-button
              text
              :title="messages.error || '站内消息'"
              :aria-label="'站内消息，' + messages.unread + ' 条未读'"
              class="icon-button"
              @click="router.push('/notifications')"
              ><!-- 显示应用Icon（AppIcon）组件；name 固定为 notification。 --><AppIcon name="notification"
            /></el-button>
          </el-badge>
          <!-- 显示Element Plus 的 dropdown 组件；仅当 session.user?.mustChangePassword 成立时渲染；trigger 固定为 click；command 事件触发时执行 userCommand。 -->
          <el-dropdown v-if="session.user?.mustChangePassword" trigger="click" @command="userCommand">
            <!-- 显示可点击按钮；用样式类 user-trigger 控制外观；类型设为 button；aria-label 提供无障碍说明 打开用户菜单。 -->
            <button
              class="user-trigger"
              type="button"
              aria-label="打开用户菜单"
            >
              <!-- 显示行内文字区域；用样式类 avatar 控制外观。 -->
              <span class="avatar"><!-- 把 (session.user?.nickname || session.user?.username || "A") .slice(0, 1) .toUpperCase() 的当前值转成文字显示。 -->{{
                (session.user?.nickname || session.user?.username || "A")
                  .slice(0, 1)
                  .toUpperCase()
              }}</span>
              <!-- 显示行内文字区域。 -->
              <span><!-- 把 session.user?.nickname || session.user?.username 的当前值转成文字显示。 -->{{
                session.user?.nickname || session.user?.username
              }}</span>
            </button>
            <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
            <template #dropdown
              ><!-- 显示Element Plus 的 dropdown-menu 组件。 --><el-dropdown-menu>
                <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 !session.user?.mustChangePassword 成立时渲染。 -->
                <template v-if="!session.user?.mustChangePassword">
                  <!-- 显示Element Plus 的 dropdown-item 组件；仅当 hasPermission('identity:profile:read') 成立时渲染；command 固定为 /account/profile。 -->
                  <el-dropdown-item
                    v-if="hasPermission('identity:profile:read')"
                    command="/account/profile"
                    ><!-- 显示行内文字区域；用样式类 user-menu-item 控制外观。 --><span class="user-menu-item"
                      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 user。 --><AppIcon name="user" />个人资料</span
                    ></el-dropdown-item
                  >
                  <!-- 显示Element Plus 的 dropdown-item 组件；仅当 hasPermission('identity:profile:read') 成立时渲染；command 固定为 /account/profile?tab=password。 -->
                  <el-dropdown-item
                    v-if="hasPermission('identity:profile:read')"
                    command="/account/profile?tab=password"
                    ><!-- 显示行内文字区域；用样式类 user-menu-item 控制外观。 --><span class="user-menu-item"
                      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 security。 --><AppIcon name="security" />修改密码</span
                    ></el-dropdown-item
                  >
                  <!-- 显示Element Plus 的 dropdown-item 组件；仅当 hasPermission('identity:device:list') 成立时渲染；command 固定为 /account/devices。 -->
                  <el-dropdown-item
                    v-if="hasPermission('identity:device:list')"
                    command="/account/devices"
                    ><!-- 显示行内文字区域；用样式类 user-menu-item 控制外观。 --><span class="user-menu-item"
                      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 device。 --><AppIcon name="device" />我的设备</span
                    ></el-dropdown-item
                  >
                </template>
                <!-- 显示Element Plus 的 dropdown-item 组件；启用 divided 属性；command 固定为 logout。 -->
                <el-dropdown-item divided command="logout"
                  ><!-- 显示行内文字区域；用样式类 user-menu-item 控制外观。 --><span class="user-menu-item"
                    ><!-- 显示应用Icon（AppIcon）组件；name 固定为 logout。 --><AppIcon name="logout" />退出登录</span
                  ></el-dropdown-item
                >
              </el-dropdown-menu></template
            >
          </el-dropdown>
        </div>
      </header>
      <!-- 显示主要内容区；id 固定为 main-content；用样式类 content 控制外观；data-page 取自 route.path.split('/')[1]；tabindex 固定为 -1。 -->
      <main
        id="main-content"
        class="content"
        :data-page="route.path.split('/')[1]"
        tabindex="-1"
      >
        <!-- 显示导航区域；仅当 planning 成立时渲染；用样式类 workspace-section-tabs 控制外观；aria-label 提供无障碍说明 目标与计划。 -->
        <nav v-if="planning" class="workspace-section-tabs" aria-label="目标与计划">
          <!-- 显示应用内部页面链接；按 item in planningLinks 为每条数据重复生成内容；key 取自 item.routePath；to 取自 item.routePath；class 取自 { selected: route.path === item.routePath || route.path.startsWith(item.routePath + '/') }；aria-current 取自 route.path === item.routePath || route.path.startsWith(item.routePath + '/') ? 'page' : undefined。 -->
          <RouterLink v-for="item in planningLinks" :key="item.routePath" :to="item.routePath" :class="{ selected: route.path === item.routePath || route.path.startsWith(item.routePath + '/') }" :aria-current="route.path === item.routePath || route.path.startsWith(item.routePath + '/') ? 'page' : undefined"><!-- 把 item.name 的当前值转成文字显示。 -->{{ item.name }}</RouterLink>
        </nav>
        <!-- 显示应用当前页请求头（AppPageHeader）组件；仅当 !assistant 成立时渲染；title 取自 route.meta.title；description 取自 route.meta.description。 -->
        <AppPageHeader
          v-if="!assistant && !route.meta.planWorkspace"
          :title="route.meta.title"
          :description="route.meta.description"
        />
        <!-- 显示router页面（router-view）组件；定义 默认 插槽，接收 { Component }。 -->
        <router-view v-slot="{ Component }">
          <!-- 显示离开后仍保留实例状态的组件缓存；include 固定为 AgentChatView。 -->
          <!-- 动态组件由 Component 决定；key 在助手页固定，在其他页按路由规则计算。KeepAlive 内只能放这一个组件，说明注释放在外面。 -->
          <KeepAlive include="AgentChatView">
            <component :is="Component" :key="assistant ? 'assistant-workspace' : route.meta.stableQuery ? route.path : route.fullPath" />
          </KeepAlive>
        </router-view>
      </main>
    </div>
    <!-- 显示应用内部页面链接；仅当 !assistant && !session.user?.mustChangePassword && canGrow('agent:chat:read') 成立时渲染；to 固定为 /agent；用样式类 global-assistant-entry 控制外观；aria-label 提供无障碍说明 打开成长助手。 -->
    <RouterLink v-if="!assistant && !session.user?.mustChangePassword && canGrow('agent:chat:read')" to="/agent" class="global-assistant-entry" aria-label="打开成长助手"><!-- 显示应用Icon（AppIcon）组件；name 固定为 sparkle；size 取自 20。 --><AppIcon name="sparkle" :size="20" /><!-- 显示行内文字区域。 --><span>成长助手</span><!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 16。 --><AppIcon name="arrow" :size="16" /></RouterLink>
    <!-- 显示执行计时Bar（ExecutionBar）组件。 -->
    <ExecutionBar />
  </div>
</template>
