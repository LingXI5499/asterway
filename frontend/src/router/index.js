/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
/* 声明索引运行或构建配置。 */
import { createRouter, createWebHistory } from "vue-router";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { session, loadCurrent, hasPermission } from "../api/http";
/* 引入 ../views/WorkspaceView.vue 提供的组件、状态函数或请求方法。 */
import WorkspaceView from "../views/WorkspaceView.vue";
import { planningPath, planningRedirect, planningPermissions } from "../api/planning";
/* 引入 ../api/growth 提供的组件、状态函数或请求方法。 */
import { isManager, canGrow } from "../api/growth";
const /* 当前页（page）保存表达式 (path, title, component, props, permission) => ({ path, component, props, meta: { title, permission }, }) 的结果。 */ page = (path, title, component, props, permission) => ({
  /* 直接使用已有的路径（path）作为同名字段。 */
  path,
  /* 直接使用已有的component作为同名字段。 */
  component,
  /* 直接使用已有的父组件传入的数据（props）作为同名字段。 */
  props,
  /* meta字段使用表达式 { title, permission } 的结果。 */
  meta: { /* 直接使用已有的标题（title）作为同名字段。 */ title, /* 直接使用已有的权限（permission）作为同名字段。 */ permission },
});
const /* 个人当前页（personalPage）保存调用 (path, title, component, permission, parent = null) => ({ ...page 完成当前页处理。 */ personalPage = (path, title, component, permission, parent = null) => ({
  ...page(path, title, component, {}, permission),
  /* meta字段使用沿 { title, permission, growth: true, stableQuery: true, menuPath: parent?.path || path, parent, } 读取数据；中间对象为空时返回空值。 */
  meta: {
    /* 直接使用已有的标题（title）作为同名字段。 */
    title,
    /* 直接使用已有的权限（permission）作为同名字段。 */
    permission,
    /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */
    growth: true,
    /* stable查询条件（stableQuery）字段使用真，表示条件成立或开关打开。 */
    stableQuery: true,
    /* 菜单路径（menuPath）字段使用沿 parent?.path || path 读取数据；中间对象为空时返回空值。 */
    menuPath: parent?.path || path,
    /* 直接使用已有的上级（parent）作为同名字段。 */
    parent,
  },
});
const /* routes保存按表达式计算得到的值：[ page("/login", "登录", () => import("../views/AuthView.vue"), { register: false, }), page("/register", "注册", () => impor…。 */ routes = [
  page("/login", "登录", () => import("../views/AuthView.vue"), {
    /* register字段使用假，表示条件不成立或开关关闭。 */
    register: false,
  }),
  page("/register", "注册", () => import("../views/AuthView.vue"), {
    /* register字段使用真，表示条件成立或开关打开。 */
    register: true,
  }),
  {
    /* 路径（path）字段使用固定文字 '/'。 */
    path: "/",
    /* 名称（name）字段使用固定文字 'workspace'。 */
    name: "workspace",
    /* component字段使用工作区页面（WorkspaceView）。 */
    component: WorkspaceView,
    /* 下级列表（children）字段使用按表达式计算得到的值：[ { path: "", name: "workspace-home", redirect: () => defaultPath() }, page( "/change-password", "修改初始密码", () => import(…。 */
    children: [
      { /* 路径（path）字段使用固定文字 ''。 */ path: "", /* 名称（name）字段使用固定文字 'workspace-home'。 */ name: "workspace-home", /* redirect字段使用表达式 () => defaultPath() 的结果。 */ redirect: () => defaultPath() },
      page(
        "/change-password",
        "修改初始密码",
        () => import("../views/ProfileView.vue"),
        { /* 被强制（forced）字段使用真，表示条件成立或开关打开。 */ forced: true },
      ),
      page(
        "/account/profile",
        "个人中心",
        () => import("../views/ProfileView.vue"),
        {},
        "identity:profile:read",
      ),
      page(
        "/account/devices",
        "我的设备",
        () => import("../views/RecordsView.vue"),
        { /* 种类（kind）字段使用固定文字 'devices'。 */ kind: "devices" },
        "identity:device:list",
      ),
      ...[
        ["users", "用户管理", "user"],
        ["permissions", "权限管理", "permission"],
        ["menus", "菜单管理", "menu"],
      ].map(([resource, title, subject]) =>
        page(
          "/system/" + resource,
          title,
          () => import("../views/ManagementView.vue"),
          { /* 直接使用已有的resource作为同名字段。 */ resource },
          `identity:${subject}:list`,
        ),
      ),
      page(
        "/system/roles",
        "角色管理",
        () => import("../views/RolesView.vue"),
        {},
        "identity:role:list",
      ),
      ...[
        ["sessions", "在线会话", "session"],
        ["login-logs", "登录日志", "login_log"],
        ["operation-logs", "操作日志", "operation_log"],
      ].map(([kind, title, subject]) =>
        page(
          "/system/" + kind,
          title,
          () => import("../views/RecordsView.vue"),
          { /* 直接使用已有的种类（kind）作为同名字段。 */ kind },
          `identity:${subject}:list`,
        ),
      ),
      page(
        "/system/account-settings",
        "账户设置",
        () => import("../views/SettingsView.vue"),
        {},
        "identity:setting:read",
      ),
      personalPage(
        "/dashboard",
        "今日看板",
        () => import("../views/dashboard/DashboardView.vue"),
        "dashboard:board:read",
      ),
      personalPage(
        "/execution",
        "执行工作台",
        () => import("../views/execution/ExecutionView.vue"),
        "execution:session:list",
      ),
      personalPage(
        "/execution/records",
        "执行记录",
        () => import("../views/execution/ExecutionRecordsView.vue"),
        "execution:session:list",
        { /* 名称（name）字段使用固定文字 '执行工作台'。 */ name: "执行工作台", /* 路径（path）字段使用固定文字 '/execution'。 */ path: "/execution" },
      ),
      personalPage(
        "/habits",
        "习惯管理",
        () => import("../views/habit/HabitsView.vue"),
        "habit:habit:list",
      ),
      personalPage(
        "/habits/categories",
        "习惯分类",
        () => import("../views/habit/HabitCategoriesView.vue"),
        "habit:category:list",
        { /* 名称（name）字段使用固定文字 '习惯管理'。 */ name: "习惯管理", /* 路径（path）字段使用固定文字 '/habits'。 */ path: "/habits" },
      ),
      personalPage(
        "/notifications",
        "站内消息",
        () => import("../views/notification/NotificationsView.vue"),
        "notification:message:list",
      ),
      personalPage(
        "/statistics",
        "历史统计",
        () => import("../views/statistics/StatisticsView.vue"),
        "statistics:report:read",
      ),
      personalPage(
        "/statistics/timeline",
        "成长时间线",
        () => import("../views/statistics/TimelineView.vue"),
        "statistics:report:read",
        { /* 名称（name）字段使用固定文字 '历史统计'。 */ name: "历史统计", /* 路径（path）字段使用固定文字 '/statistics'。 */ path: "/statistics" },
      ),
      {
        ...personalPage("/agent", "成长助手", () => import("../views/agent/AgentChatView.vue"), "agent:chat:read"),
        // 资料作为对话工作区的内嵌面板，切换时保留会话、输入和阅读位置。
        /* 下级列表（children）字段使用按表达式计算得到的值：[ personalPage("memories", "长期记忆", () => import("../views/agent/AgentMemoriesView.vue"), "agent:memory:read"), personalP…。 */
        children: [
          personalPage("memories", "长期记忆", () => import("../views/agent/AgentMemoriesView.vue"), "agent:memory:read"),
          personalPage("knowledge", "知识资料", () => import("../views/agent/AgentKnowledgeView.vue"), "agent:knowledge:read"),
          personalPage("knowledge/:id", "文档详情", () => import("../views/agent/AgentKnowledgeDetailView.vue"), "agent:knowledge:read"),
          personalPage("runs", "运行记录", () => import("../views/agent/AgentRunsView.vue"), "agent:run:read"),
          personalPage("reviews", "成长复盘", () => import("../views/agent/AgentReviewsView.vue"), "agent:review:read"),
        ],
      },
      {
        ...page(
          "/growth/goals",
          "目标管理",
          () => import("../views/growth/GoalsView.vue"),
          {},
          "goal:goal:list",
        ),
        /* meta字段使用表达式 { title: "目标管理", permission: "goal:goal:list", growth: true } 的结果。 */
        meta: { /* 标题（title）字段使用固定文字 '目标管理'。 */ title: "目标管理", /* 权限（permission）字段使用固定文字 'goal:goal:list'。 */ permission: "goal:goal:list", /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */ growth: true },
      },
      {
        ...page(
          "/growth/goals/:id",
          "目标详情",
          () => import("../views/growth/GrowthDetailView.vue"),
          { /* 种类（kind）字段使用固定文字 'goal'。 */ kind: "goal" },
          "goal:goal:read",
        ),
        /* meta字段使用表达式 { title: "目标详情", permission: "goal:goal:read", growth: true } 的结果。 */
        meta: { /* 标题（title）字段使用固定文字 '目标详情'。 */ title: "目标详情", /* 权限（permission）字段使用固定文字 'goal:goal:read'。 */ permission: "goal:goal:read", /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */ growth: true },
      },
      {
        ...page(
          "/growth/stages",
          "阶段管理",
          () => import("../views/growth/StagesView.vue"),
          {},
          "stage:stage:list",
        ),
        /* meta字段使用表达式 { title: "阶段管理", permission: "stage:stage:list", growth: true, } 的结果。 */
        meta: {
          /* 标题（title）字段使用固定文字 '阶段管理'。 */
          title: "阶段管理",
          /* 权限（permission）字段使用固定文字 'stage:stage:list'。 */
          permission: "stage:stage:list",
          /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */
          growth: true,
        },
      },
      {
        ...page(
          "/growth/stages/:id",
          "阶段详情",
          () => import("../views/growth/GrowthDetailView.vue"),
          { /* 种类（kind）字段使用固定文字 'stage'。 */ kind: "stage" },
          "stage:stage:read",
        ),
        /* meta字段使用表达式 { title: "阶段详情", permission: "stage:stage:read", growth: true, } 的结果。 */
        meta: {
          /* 标题（title）字段使用固定文字 '阶段详情'。 */
          title: "阶段详情",
          /* 权限（permission）字段使用固定文字 'stage:stage:read'。 */
          permission: "stage:stage:read",
          /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */
          growth: true,
        },
      },
      {
        ...page(
          "/growth/tasks",
          "任务中心",
          () => import("../views/growth/TasksView.vue"),
          {},
          "task:task:list",
        ),
        /* meta字段使用表达式 { title: "任务中心", permission: "task:task:list", growth: true } 的结果。 */
        meta: { /* 标题（title）字段使用固定文字 '任务中心'。 */ title: "任务中心", /* 权限（permission）字段使用固定文字 'task:task:list'。 */ permission: "task:task:list", /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */ growth: true },
      },
      {
        ...page(
          "/growth/tasks/:id",
          "任务详情",
          () => import("../views/growth/TaskDetailView.vue"),
          {},
          "task:task:read",
        ),
        /* meta字段使用表达式 { title: "任务详情", permission: "task:task:read", growth: true } 的结果。 */
        meta: { /* 标题（title）字段使用固定文字 '任务详情'。 */ title: "任务详情", /* 权限（permission）字段使用固定文字 'task:task:read'。 */ permission: "task:task:read", /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */ growth: true },
      },
      {
        ...page(
          "/planning/weekly-plans",
          "周计划",
          () => import("../views/growth/PlansView.vue"),
          { /* 每日（daily）字段使用假，表示条件不成立或开关关闭。 */ daily: false },
          "planning:weekly:read",
        ),
        /* meta字段使用表达式 { title: "周计划", permission: "planning:weekly:read", growth: true, } 的结果。 */
        meta: {
          /* 标题（title）字段使用固定文字 '周计划'。 */
          title: "周计划",
          /* 权限（permission）字段使用固定文字 'planning:weekly:read'。 */
          permission: "planning:weekly:read",
          /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */
          growth: true,
        },
      },
      {
        ...page(
          "/planning/daily-plans",
          "日计划",
          () => import("../views/growth/PlansView.vue"),
          { /* 每日（daily）字段使用真，表示条件成立或开关打开。 */ daily: true },
          "planning:daily:read",
        ),
        /* meta字段使用表达式 { title: "日计划", permission: "planning:daily:read", growth: true, } 的结果。 */
        meta: {
          /* 标题（title）字段使用固定文字 '日计划'。 */
          title: "日计划",
          /* 权限（permission）字段使用固定文字 'planning:daily:read'。 */
          permission: "planning:daily:read",
          /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */
          growth: true,
        },
      },
      {
        ...page(
          "/planning/pending-tasks",
          "待处理任务",
          () => import("../views/growth/PendingView.vue"),
          {},
          "planning:pending:list",
        ),
        /* meta字段使用表达式 { title: "待处理任务", permission: "planning:pending:list", growth: true, } 的结果。 */
        meta: {
          /* 标题（title）字段使用固定文字 '待处理任务'。 */
          title: "待处理任务",
          /* 权限（permission）字段使用固定文字 'planning:pending:list'。 */
          permission: "planning:pending:list",
          /* 成长业务（growth）字段使用真，表示条件成立或开关打开。 */
          growth: true,
        },
      },
      page("/403", "无访问权限", () => import("../views/ExceptionView.vue"), {
        /* forbidden字段使用真，表示条件成立或开关打开。 */
        forbidden: true,
      }),
      page(
        "/:pathMatch(.*)*",
        "页面不存在",
        () => import("../views/ExceptionView.vue"),
        { /* forbidden字段使用假，表示条件不成立或开关关闭。 */ forbidden: false },
      ),
    ],
  },
];
// 同一页面按区域校验原有权限，任务只读账号也能通过旧详情链接进入。
routes[2].children.push({
  path: planningPath,
  component: () => import("../views/growth/PlansView.vue"),
  meta: { title: "计划", growth: true, planWorkspace: true, stableQuery: true },
});
for (const record of routes[2].children) {
  if (["/planning/weekly-plans", "/planning/daily-plans", "/planning/pending-tasks", "/growth/tasks", "/growth/tasks/:id"].includes(record.path)) {
    const source = record.path;
    record.redirect = to => planningRedirect(source, to);
    delete record.component;
    delete record.props;
  }
}
const /* HTTP 路由登记器（router）保存创建 HTTP 路由登记器（Router）。 */ router = createRouter({ /* 历史（history）字段使用创建 Web历史（WebHistory）。 */ history: createWebHistory(), /* 直接使用已有的routes作为同名字段。 */ routes });
// 服务端菜单只能映射到已实现的页面组件，不接受任意组件路径。
const /* installed菜单列表（installedMenus）保存新建 按键保存值的对应表（Map） 实例。 */ installedMenus = new Map();
/* sync菜单Routes（syncMenuRoutes）：调用 syncMenuRoutes 完成sync菜单Routes处理。 */
function syncMenuRoutes(nodes) {
  const /* 页面是否仍处于活动状态（active）保存新建 设置（Set） 实例。 */ active = new Set();
  /* visit：调用 visit 完成visit处理。 */
  function visit(entries) {
    /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
    for (const /* 声明 菜单（menu），稍后再填入处理结果。 */ menu of entries) {
      /* 检查条件 menu.menuType === "PAGE" && menu.enabled && menu.routePath?.startsWith("/") 是否成立，成立时走这一分支。 */
      if (
        menu.menuType === "PAGE" &&
        menu.enabled &&
        menu.routePath?.startsWith("/")
      ) {
        const /* aliases保存按表达式计算得到的值：{ "planning/weekly": "/planning/weekly-plans", "planning/daily": "/planning/daily-plans", "planning/pending": "/planning…。 */ aliases = {
          /* 规划每周（planning/weekly）字段使用固定文字 '/planning/weekly-plans'。 */
          "planning/weekly": "/planning/weekly-plans",
          /* 规划每日（planning/daily）字段使用固定文字 '/planning/daily-plans'。 */
          "planning/daily": "/planning/daily-plans",
          /* 规划等待处理（planning/pending）字段使用固定文字 '/planning/pending-tasks'。 */
          "planning/pending": "/planning/pending-tasks",
          /* 执行计时索引（execution/index）字段使用固定文字 '/execution'。 */
          "execution/index": "/execution",
          /* 习惯索引（habit/index）字段使用固定文字 '/habits'。 */
          "habit/index": "/habits",
          /* 习惯分类列表（habit/categories）字段使用固定文字 '/habits/categories'。 */
          "habit/categories": "/habits/categories",
          /* 通知索引（notification/index）字段使用固定文字 '/notifications'。 */
          "notification/index": "/notifications",
          /* dashboard索引（dashboard/index）字段使用固定文字 '/dashboard'。 */
          "dashboard/index": "/dashboard",
          /* 统计索引（statistics/index）字段使用固定文字 '/statistics'。 */
          "statistics/index": "/statistics",
          /* 成长助手索引（agent/index）字段使用固定文字 '/agent'。 */
          "agent/index": "/agent",
          /* 成长助手长期记忆列表（agent/memories）字段使用固定文字 '/agent/memories'。 */
          "agent/memories": "/agent/memories",
          /* 成长助手知识资料（agent/knowledge）字段使用固定文字 '/agent/knowledge'。 */
          "agent/knowledge": "/agent/knowledge",
          /* 成长助手运行记录列表（agent/runs）字段使用固定文字 '/agent/runs'。 */
          "agent/runs": "/agent/runs",
          /* 成长助手复盘列表（agent/reviews）字段使用固定文字 '/agent/reviews'。 */
          "agent/reviews": "/agent/reviews",
        };
        const /* 来源路径（sourcePath）保存优先使用前面的有效值，没有时采用后面的备用值：aliases[menu.componentKey] || "/" + (menu.componentKey || "").replaceAll("_", "-")。 */ sourcePath =
          aliases[menu.componentKey] ||
          "/" + (menu.componentKey || "").replaceAll("_", "-");
        const /* 来源（source）保存取得第一条符合条件的数据。 */ source = routes[2].children.find((r) => r.path === sourcePath);
        /* 检查条件 source && menu.routePath !== sourcePath 是否成立，成立时走这一分支。 */
        if (source && menu.routePath !== sourcePath) {
          const /* 名称（name）保存按表达式计算得到的值："menu-" + menu.id。 */ name = "menu-" + menu.id;
          /* 加入集合或登记一条待保存记录。 */
          active.add(name);
          /* 检查条件 installedMenus.get(name)?.path !== menu.routePath 是否成立，成立时走这一分支。 */
          if (installedMenus.get(name)?.path !== menu.routePath) {
            /* 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。 */
            installedMenus.get(name)?.remove();
            const /* 移除（remove）保存调用 router.addRoute 完成加入Route处理。 */ remove = router.addRoute("workspace", {
              ...source,
              /* 路径（path）字段使用表达式 menu.routePath 的结果。 */
              path: menu.routePath,
              /* 直接使用已有的名称（name）作为同名字段。 */
              name,
            });
            /* 调用 installedMenus.set 完成设置处理。 */
            installedMenus.set(name, { /* 路径（path）字段使用表达式 menu.routePath 的结果。 */ path: menu.routePath, /* 直接使用已有的移除（remove）作为同名字段。 */ remove });
          }
        }
      }
      /* 调用 visit 完成visit处理。 */
      visit(menu.children || []);
    }
  }
  /* 调用 visit 完成visit处理。 */
  visit(nodes);
  /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
  for (const /* 声明 名称单条数据（[name, item]），稍后再填入处理结果。 */ [name, item] of installedMenus)
    /* 检查页面已离开或当前处理已停用，成立时走这一分支。 */
    if (!active.has(name)) {
      /* 调用 item.remove 完成移除处理。 */
      item.remove();
      /* 调用 installedMenus.delete 完成删除处理。 */
      installedMenus.delete(name);
    }
}
/* 调用 router.beforeEach 完成变化前Each处理。 */
router.beforeEach(async (to) => {
  /* 检查条件 ["/login", "/register"].includes(to.path) 是否成立，成立时走这一分支。 */
  if (["/login", "/register"].includes(to.path)) /* 返回真，表示条件成立或开关打开，当前函数到此结束。 */ return true;
  /* 检查条件 !session.token 是否成立，成立时走这一分支。 */
  if (!session.token) /* 返回固定文字 '/login'，当前函数到此结束。 */ return "/login";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待加载 当前（Current）。 */
    await loadCurrent();
    /* 调用 syncMenuRoutes 完成sync菜单Routes处理。 */
    syncMenuRoutes(session.menus);
    /* 检查条件 router.resolve(to.fullPath).matched.at(-1)?.path !== to.matched.at(-1)?.path 是否成立，成立时走这一分支。 */
    if (
      router.resolve(to.fullPath).matched.at(-1)?.path !==
      to.matched.at(-1)?.path
    )
      /* 返回表达式 to.fullPath 的结果，当前函数到此结束。 */
      return to.fullPath;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查条件 error.status === 401 是否成立，成立时走这一分支。 */
    if (error.status === 401) /* 返回固定文字 '/login'，当前函数到此结束。 */ return "/login";
    /* 调用 ElMessage.error 完成错误处理。 */
    ElMessage.error(error.message || "服务连接失败，请稍后重试");
    /* 返回固定文字 '/login'，当前函数到此结束。 */
    return "/login";
  }
  /* 检查条件 session.user.mustChangePassword && to.path !== "/change-password" 是否成立，成立时走这一分支。 */
  if (session.user.mustChangePassword && to.path !== "/change-password")
    /* 返回固定文字 '/change-password'，当前函数到此结束。 */
    return "/change-password";
  /* 检查条件 !session.user.mustChangePassword && to.path === "/change-password" 是否成立，成立时走这一分支。 */
  if (!session.user.mustChangePassword && to.path === "/change-password")
    /* 返回固定文字 '/account/profile'，当前函数到此结束。 */
    return "/account/profile";
  /* 检查条件 to.redirectedFrom?.path === "/" && to.path === "/account/profile" && defaultPath() !== to.path 是否成立，成立时走这一分支。 */
  if (
    to.redirectedFrom?.path === "/" &&
    to.path === "/account/profile" &&
    defaultPath() !== to.path
  )
    /* 返回调用 defaultPath 完成默认值路径处理，当前函数到此结束。 */
    return defaultPath();
  /* 检查当前用户是否具有需要的权限：to.meta.growth && !canGrow(to.meta.permission)，成立时走这一分支。 */
  if (to.meta.planWorkspace && !planningPermissions.some(canGrow)) return "/403";
  if (to.meta.growth && !to.meta.planWorkspace && !canGrow(to.meta.permission)) /* 返回固定文字 '/403'，当前函数到此结束。 */ return "/403";
  /* 检查当前用户是否具有需要的权限：to.meta.permission && !hasPermission(to.meta.permission)，成立时走这一分支。 */
  if (to.meta.permission && !hasPermission(to.meta.permission)) /* 返回固定文字 '/403'，当前函数到此结束。 */ return "/403";
  /* 把优先使用前面的有效值，没有时采用后面的备用值：`${to.meta.title || "账户中心"} · Asterway`写入 document.title。 */
  document.title = `${to.meta.title || "账户中心"} · Asterway`;
  /* 返回真，表示条件成立或开关打开，当前函数到此结束。 */
  return true;
});
/* 调用 window.addEventListener 完成加入事件Listener处理。 */
window.addEventListener("asterway:unauthorized", () =>
  router.replace("/login"),
);
export /* 默认值路径（defaultPath）：调用 defaultPath 完成默认值路径处理。 */ function defaultPath() {
  /* 检查当前用户是否具有需要的权限：canGrow("agent:chat:read")，成立时走这一分支。 */
  if (canGrow("agent:chat:read")) /* 返回固定文字 '/agent'，当前函数到此结束。 */ return "/agent";
  /* 检查当前用户是否具有需要的权限：canGrow("dashboard:board:read")，成立时走这一分支。 */
  if (canGrow("dashboard:board:read")) /* 返回固定文字 '/dashboard'，当前函数到此结束。 */ return "/dashboard";
  /* 检查当前用户是否具有需要的权限：!isManager() && canGrow("goal:goal:list")，成立时走这一分支。 */
  if (!isManager() && canGrow("goal:goal:list")) /* 返回固定文字 '/growth/goals'，当前函数到此结束。 */ return "/growth/goals";
  /* 检查条件 isManager() 是否成立，成立时走这一分支。 */
  if (isManager())
    /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
    for (const /* 声明 编码路径（[code, path]），稍后再填入处理结果。 */ [code, path] of [
      ["identity:user:list", "/system/users"],
      ["identity:session:list", "/system/sessions"],
      ["identity:role:list", "/system/roles"],
      ["identity:login_log:list", "/system/login-logs"],
    ])
      /* 检查当前用户是否具有需要的权限：hasPermission(code)，成立时走这一分支。 */
      if (hasPermission(code)) /* 返回路径（path），当前函数到此结束。 */ return path;
  /* 返回固定文字 '/account/profile'，当前函数到此结束。 */
  return "/account/profile";
}
/* 把下面的对象或组件作为该文件默认对外提供的内容。 */
export default router;
