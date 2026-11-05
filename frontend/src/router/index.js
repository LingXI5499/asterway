import { createRouter, createWebHistory } from "vue-router";
import { ElMessage } from "element-plus";
import { session, loadCurrent, hasPermission } from "../api/http";
import WorkspaceView from "../views/WorkspaceView.vue";
import { isManager, canGrow } from "../api/growth";
const page = (path, title, component, props, permission) => ({
  path,
  component,
  props,
  meta: { title, permission },
});
const personalPage = (path, title, component, permission, parent = null) => ({
  ...page(path, title, component, {}, permission),
  meta: {
    title,
    permission,
    growth: true,
    stableQuery: true,
    description: "以真实记录管理自己的行动与成长",
    menuPath: parent?.path || path,
    parent,
  },
});
const routes = [
  page("/login", "登录", () => import("../views/AuthView.vue"), {
    register: false,
  }),
  page("/register", "注册", () => import("../views/AuthView.vue"), {
    register: true,
  }),
  {
    path: "/",
    name: "workspace",
    component: WorkspaceView,
    children: [
      { path: "", name: "workspace-home", redirect: () => defaultPath() },
      page(
        "/change-password",
        "修改初始密码",
        () => import("../views/ProfileView.vue"),
        { forced: true },
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
        { kind: "devices" },
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
          { resource },
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
          { kind },
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
        { name: "执行工作台", path: "/execution" },
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
        { name: "习惯管理", path: "/habits" },
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
        { name: "历史统计", path: "/statistics" },
      ),
      {
        ...page(
          "/growth/goals",
          "目标管理",
          () => import("../views/growth/GoalsView.vue"),
          {},
          "goal:goal:list",
        ),
        meta: { title: "目标管理", permission: "goal:goal:list", growth: true },
      },
      {
        ...page(
          "/growth/goals/:id",
          "目标详情",
          () => import("../views/growth/GrowthDetailView.vue"),
          { kind: "goal" },
          "goal:goal:read",
        ),
        meta: { title: "目标详情", permission: "goal:goal:read", growth: true },
      },
      {
        ...page(
          "/growth/stages",
          "阶段管理",
          () => import("../views/growth/StagesView.vue"),
          {},
          "stage:stage:list",
        ),
        meta: {
          title: "阶段管理",
          permission: "stage:stage:list",
          growth: true,
        },
      },
      {
        ...page(
          "/growth/stages/:id",
          "阶段详情",
          () => import("../views/growth/GrowthDetailView.vue"),
          { kind: "stage" },
          "stage:stage:read",
        ),
        meta: {
          title: "阶段详情",
          permission: "stage:stage:read",
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
        meta: { title: "任务中心", permission: "task:task:list", growth: true },
      },
      {
        ...page(
          "/growth/tasks/:id",
          "任务详情",
          () => import("../views/growth/TaskDetailView.vue"),
          {},
          "task:task:read",
        ),
        meta: { title: "任务详情", permission: "task:task:read", growth: true },
      },
      {
        ...page(
          "/planning/weekly-plans",
          "周计划",
          () => import("../views/growth/PlansView.vue"),
          { daily: false },
          "planning:weekly:read",
        ),
        meta: {
          title: "周计划",
          permission: "planning:weekly:read",
          growth: true,
        },
      },
      {
        ...page(
          "/planning/daily-plans",
          "日计划",
          () => import("../views/growth/PlansView.vue"),
          { daily: true },
          "planning:daily:read",
        ),
        meta: {
          title: "日计划",
          permission: "planning:daily:read",
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
        meta: {
          title: "待处理任务",
          permission: "planning:pending:list",
          growth: true,
        },
      },
      page("/403", "无访问权限", () => import("../views/ExceptionView.vue"), {
        forbidden: true,
      }),
      page(
        "/:pathMatch(.*)*",
        "页面不存在",
        () => import("../views/ExceptionView.vue"),
        { forbidden: false },
      ),
    ],
  },
];
const router = createRouter({ history: createWebHistory(), routes });
// 服务端菜单只能映射到已实现的页面组件，不接受任意组件路径。
const installedMenus = new Map();
function syncMenuRoutes(nodes) {
  const active = new Set();
  function visit(entries) {
    for (const menu of entries) {
      if (
        menu.menuType === "PAGE" &&
        menu.enabled &&
        menu.routePath?.startsWith("/")
      ) {
        const aliases = {
          "planning/weekly": "/planning/weekly-plans",
          "planning/daily": "/planning/daily-plans",
          "planning/pending": "/planning/pending-tasks",
          "execution/index": "/execution",
          "habit/index": "/habits",
          "habit/categories": "/habits/categories",
          "notification/index": "/notifications",
          "dashboard/index": "/dashboard",
          "statistics/index": "/statistics",
        };
        const sourcePath =
          aliases[menu.componentKey] ||
          "/" + (menu.componentKey || "").replaceAll("_", "-");
        const source = routes[2].children.find((r) => r.path === sourcePath);
        if (source && menu.routePath !== sourcePath) {
          const name = "menu-" + menu.id;
          active.add(name);
          if (installedMenus.get(name)?.path !== menu.routePath) {
            installedMenus.get(name)?.remove();
            const remove = router.addRoute("workspace", {
              ...source,
              path: menu.routePath,
              name,
            });
            installedMenus.set(name, { path: menu.routePath, remove });
          }
        }
      }
      visit(menu.children || []);
    }
  }
  visit(nodes);
  for (const [name, item] of installedMenus)
    if (!active.has(name)) {
      item.remove();
      installedMenus.delete(name);
    }
}
router.beforeEach(async (to) => {
  if (["/login", "/register"].includes(to.path)) return true;
  if (!session.token) return "/login";
  try {
    await loadCurrent();
    syncMenuRoutes(session.menus);
    if (
      router.resolve(to.fullPath).matched.at(-1)?.path !==
      to.matched.at(-1)?.path
    )
      return to.fullPath;
  } catch (error) {
    if (error.status === 401) return "/login";
    ElMessage.error(error.message || "服务连接失败，请稍后重试");
    return "/login";
  }
  if (session.user.mustChangePassword && to.path !== "/change-password")
    return "/change-password";
  if (!session.user.mustChangePassword && to.path === "/change-password")
    return "/account/profile";
  if (
    to.redirectedFrom?.path === "/" &&
    to.path === "/account/profile" &&
    defaultPath() !== to.path
  )
    return defaultPath();
  if (to.meta.growth && !canGrow(to.meta.permission)) return "/403";
  if (to.meta.permission && !hasPermission(to.meta.permission)) return "/403";
  document.title = `${to.meta.title || "账户中心"} · Asterway`;
  return true;
});
window.addEventListener("asterway:unauthorized", () =>
  router.replace("/login"),
);
export function defaultPath() {
  if (canGrow("dashboard:board:read")) return "/dashboard";
  if (!isManager() && canGrow("goal:goal:list")) return "/growth/goals";
  if (isManager())
    for (const [code, path] of [
      ["identity:user:list", "/system/users"],
      ["identity:session:list", "/system/sessions"],
      ["identity:role:list", "/system/roles"],
      ["identity:login_log:list", "/system/login-logs"],
    ])
      if (hasPermission(code)) return path;
  return "/account/profile";
}
export default router;
