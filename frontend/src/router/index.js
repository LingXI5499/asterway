import { createRouter, createWebHistory } from "vue-router";
import { ElMessage } from "element-plus";
import { session, loadCurrent, hasPermission } from "../api/http";
import WorkspaceView from "../views/WorkspaceView.vue";
const page = (path, title, component, props, permission) => ({
  path,
  component,
  props,
  meta: { title, permission },
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
      { path: "", redirect: "/account/profile" },
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
        const sourcePath = "/" + (menu.componentKey || "").replaceAll("_", "-");
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
  if (to.meta.permission && !hasPermission(to.meta.permission)) return "/403";
  document.title = `${to.meta.title || "账户中心"} · Asterway`;
  return true;
});
window.addEventListener("asterway:unauthorized", () =>
  router.replace("/login"),
);
export default router;
