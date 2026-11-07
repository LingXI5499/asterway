import { createApp } from "vue";
import { registerElementPlus } from "./plugins/element-plus";
import App from "./App.vue";
import router from "./router";
import { initializeTheme } from "./stores/uiPreferences";
import AppIcon from "./components/ui/AppIcon.vue";
import AppThemeSwitch from "./components/ui/AppThemeSwitch.vue";
import AppEmptyState from "./components/ui/AppEmptyState.vue";
import AppStatusTag from "./components/ui/AppStatusTag.vue";
import AppPageHeader from "./components/ui/AppPageHeader.vue";
import "element-plus/theme-chalk/dark/css-vars.css";
import "@fontsource-variable/geist";
import "./styles/tokens.css";
import "./styles/themes.css";
import "./style.css";
import "./personal.css";
import "./styles/element-overrides.css";
import "./styles/global.css";
import "./styles/workspace.css";

// 统一使用简体中文组件，业务校验继续由各页面和服务端负责。
initializeTheme();
const app = createApp(App);
Object.entries({
  AppIcon,
  AppThemeSwitch,
  AppEmptyState,
  AppStatusTag,
  AppPageHeader,
}).forEach(([name, component]) => app.component(name, component));
app.use(router);
registerElementPlus(app);
app.mount("#app");
