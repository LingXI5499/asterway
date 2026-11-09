/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 保存并共享页面Preferences页面状态，相关组件读取同一份数据。 */
import { computed, reactive } from "vue";

// 只保存展示偏好，不参与登录会话、权限或任何业务状态。
const /* 键（key）保存固定文字 'asterway.ui.theme'。 */ key = "asterway.ui.theme";
const /* modes保存新建 设置（Set） 实例。 */ modes = new Set(["system", "light", "dark"]);
const /* 系统（system）保存调用 window.matchMedia 完成matchMedia处理。 */ system = window.matchMedia("(prefers-color-scheme: dark)");
/* 已保存Mode（savedMode）：保存 dMode。 */
function savedMode() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 值（value）保存读取 单条数据（Item） 的值。 */ value = localStorage.getItem(key);
    /* 返回根据前面的条件，从两个候选结果中选择一个：modes.has(value) ? value : "system"，当前函数到此结束。 */
    return modes.has(value) ? value : "system";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    /* 返回固定文字 'system'，当前函数到此结束。 */
    return "system";
  }
}
export const /* 页面Preferences（uiPreferences）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ uiPreferences = reactive({
  /* theme字段使用保存 dMode。 */
  theme: savedMode(),
  /* 系统Dark（systemDark）字段使用表达式 system.matches 的结果。 */
  systemDark: system.matches,
});
export const /* resolvedTheme保存随相关状态变化自动重算的值。 */ resolvedTheme = computed(() =>
  uiPreferences.theme === "system"
    ? uiPreferences.systemDark
      ? "dark"
      : "light"
    : uiPreferences.theme,
);
/* applyTheme：调用 applyTheme 完成applyTheme处理。 */
function applyTheme() {
  const /* 值（value）保存表达式 resolvedTheme.value 的结果。 */ value = resolvedTheme.value;
  /* 把值（value）写入 document.documentElement.dataset.theme。 */
  document.documentElement.dataset.theme = value;
  /* 调用 document.documentElement.classList.toggle 完成toggle处理。 */
  document.documentElement.classList.toggle("dark", value === "dark");
  /* 把值（value）写入 document.documentElement.style.colorScheme。 */
  document.documentElement.style.colorScheme = value;
}
export /* 设置Theme（setTheme）：设置 Theme 的值。 */ function setTheme(value) {
  /* 检查条件 !modes.has(value) 是否成立，成立时走这一分支。 */
  if (!modes.has(value)) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把值（value）写入 uiPreferences.theme。 */
  uiPreferences.theme = value;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 设置 单条数据（Item） 的值。 */
    localStorage.setItem(key, value);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {}
  /* 调用 applyTheme 完成applyTheme处理。 */
  applyTheme();
}
/* 系统已修改（systemChanged）：调用 systemChanged 完成系统已修改处理。 */
function systemChanged(event) {
  /* 把表达式 event.matches 的结果写入 uiPreferences.systemDark。 */
  uiPreferences.systemDark = event.matches;
  /* 调用 applyTheme 完成applyTheme处理。 */
  applyTheme();
}
/* 存储已修改（storageChanged）：调用 storageChanged 完成存储已修改处理。 */
function storageChanged(event) {
  /* 检查条件 event.key !== key 是否成立，成立时走这一分支。 */
  if (event.key !== key) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把保存 dMode写入 uiPreferences.theme。 */
  uiPreferences.theme = savedMode();
  /* 调用 applyTheme 完成applyTheme处理。 */
  applyTheme();
}
export /* 初始化Theme（initializeTheme）：调用 initializeTheme 完成初始化Theme处理。 */ function initializeTheme() {
  /* 调用 applyTheme 完成applyTheme处理。 */
  applyTheme();
  /* 调用 system.addEventListener 完成加入事件Listener处理。 */
  system.addEventListener("change", systemChanged);
  /* 调用 window.addEventListener 完成加入事件Listener处理。 */
  window.addEventListener("storage", storageChanged);
}
/* 检查条件 import.meta.hot 是否成立，成立时走这一分支。 */
if (import.meta.hot)
  /* 关闭连接池并释放数据库连接。 */
  import.meta.hot.dispose(() => {
    /* 移除 事件Listener（EventListener）。 */
    system.removeEventListener("change", systemChanged);
    /* 移除 事件Listener（EventListener）。 */
    window.removeEventListener("storage", storageChanged);
  });

// ECharts 使用当前 CSS Token，主题变化时由各图表重新绘制。
export /* chartTheme：调用 chartTheme 完成chartTheme处理。 */ function chartTheme() {
  const /* style保存读取 自动重算值Style（ComputedStyle） 的值。 */ style = getComputedStyle(document.documentElement);
  const /* 身份凭据（token）保存读取 Property值（PropertyValue） 的值。 */ token = (name) => style.getPropertyValue(name).trim();
  /* 返回表达式 { colors: [token("—aw-brand"), token("—aw-accent"), token("—aw-warning")], text: token("—aw-text-secondary"), font: … 的结果，当前函数到此结束。 */
  return {
    /* colors字段使用表达式 [token("—aw-brand"), token("—aw-accent"), token("—aw-warning")] 的结果。 */
    colors: [token("--aw-brand"), token("--aw-accent"), token("--aw-warning")],
    /* 文字（text）字段使用调用 token 完成身份凭据处理。 */
    text: token("--aw-text-secondary"),
    /* font字段使用调用 token 完成身份凭据处理。 */
    font: token("--aw-font"),
    /* border字段使用调用 token 完成身份凭据处理。 */
    border: token("--aw-border"),
    /* surface字段使用调用 token 完成身份凭据处理。 */
    surface: token("--aw-surface"),
    /* muted字段使用调用 token 完成身份凭据处理。 */
    muted: token("--aw-surface-muted"),
  };
}
