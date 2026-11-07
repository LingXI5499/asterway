import { computed, reactive } from "vue";

// 只保存展示偏好，不参与登录会话、权限或任何业务状态。
const key = "asterway.ui.theme";
const modes = new Set(["system", "light", "dark"]);
const system = window.matchMedia("(prefers-color-scheme: dark)");
function savedMode() {
  try {
    const value = localStorage.getItem(key);
    return modes.has(value) ? value : "system";
  } catch {
    return "system";
  }
}
export const uiPreferences = reactive({
  theme: savedMode(),
  systemDark: system.matches,
});
export const resolvedTheme = computed(() =>
  uiPreferences.theme === "system"
    ? uiPreferences.systemDark
      ? "dark"
      : "light"
    : uiPreferences.theme,
);
function applyTheme() {
  const value = resolvedTheme.value;
  document.documentElement.dataset.theme = value;
  document.documentElement.classList.toggle("dark", value === "dark");
  document.documentElement.style.colorScheme = value;
}
export function setTheme(value) {
  if (!modes.has(value)) return;
  uiPreferences.theme = value;
  try {
    localStorage.setItem(key, value);
  } catch {}
  applyTheme();
}
function systemChanged(event) {
  uiPreferences.systemDark = event.matches;
  applyTheme();
}
function storageChanged(event) {
  if (event.key !== key) return;
  uiPreferences.theme = savedMode();
  applyTheme();
}
export function initializeTheme() {
  applyTheme();
  system.addEventListener("change", systemChanged);
  window.addEventListener("storage", storageChanged);
}
if (import.meta.hot)
  import.meta.hot.dispose(() => {
    system.removeEventListener("change", systemChanged);
    window.removeEventListener("storage", storageChanged);
  });

// ECharts 使用当前 CSS Token，主题变化时由各图表重新绘制。
export function chartTheme() {
  const style = getComputedStyle(document.documentElement);
  const token = (name) => style.getPropertyValue(name).trim();
  return {
    colors: [token("--aw-brand"), token("--aw-accent"), token("--aw-warning")],
    text: token("--aw-text-secondary"),
    font: token("--aw-font"),
    border: token("--aw-border"),
    surface: token("--aw-surface"),
    muted: token("--aw-surface-muted"),
  };
}
