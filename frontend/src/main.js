/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 声明程序入口运行或构建配置。 */
import { createApp } from "vue";
/* 引入 ./plugins/element-plus 提供的组件、状态函数或请求方法。 */
import { registerElementPlus } from "./plugins/element-plus";
/* 引入 ./App.vue 提供的组件、状态函数或请求方法。 */
import App from "./App.vue";
/* 引入 ./router 提供的组件、状态函数或请求方法。 */
import router from "./router";
/* 引入 ./stores/uiPreferences 提供的组件、状态函数或请求方法。 */
import { initializeTheme } from "./stores/uiPreferences";
/* 引入 ./components/ui/AppIcon.vue 提供的组件、状态函数或请求方法。 */
import AppIcon from "./components/ui/AppIcon.vue";
/* 引入 ./components/ui/AppThemeSwitch.vue 提供的组件、状态函数或请求方法。 */
import AppThemeSwitch from "./components/ui/AppThemeSwitch.vue";
/* 引入 ./components/ui/AppEmptyState.vue 提供的组件、状态函数或请求方法。 */
import AppEmptyState from "./components/ui/AppEmptyState.vue";
/* 引入 ./components/ui/AppStatusTag.vue 提供的组件、状态函数或请求方法。 */
import AppStatusTag from "./components/ui/AppStatusTag.vue";
/* 引入 ./components/ui/AppPageHeader.vue 提供的组件、状态函数或请求方法。 */
import AppPageHeader from "./components/ui/AppPageHeader.vue";
/* 引入 import "element-plus/theme-chalk/dark/css-vars.css"; 提供的组件、状态函数或请求方法。 */
import "element-plus/theme-chalk/dark/css-vars.css";
/* 引入 import "@fontsource-variable/geist"; 提供的组件、状态函数或请求方法。 */
import "@fontsource-variable/geist";
/* 引入 import "./styles/tokens.css"; 提供的组件、状态函数或请求方法。 */
import "./styles/tokens.css";
/* 引入 import "./styles/themes.css"; 提供的组件、状态函数或请求方法。 */
import "./styles/themes.css";
/* 引入 import "./style.css"; 提供的组件、状态函数或请求方法。 */
import "./style.css";
/* 引入 import "./personal.css"; 提供的组件、状态函数或请求方法。 */
import "./personal.css";
/* 引入 import "./styles/element-overrides.css"; 提供的组件、状态函数或请求方法。 */
import "./styles/element-overrides.css";
/* 引入 import "./styles/global.css"; 提供的组件、状态函数或请求方法。 */
import "./styles/global.css";
/* 引入 import "./styles/workspace.css"; 提供的组件、状态函数或请求方法。 */
import "./styles/workspace.css";

// 统一使用简体中文组件，业务校验继续由各页面和服务端负责。
/* 调用 initializeTheme 完成初始化Theme处理。 */
initializeTheme();
const /* 应用（app）保存创建 应用（App）。 */ app = createApp(App);
/* 调用 Object.entries 完成entries处理。 */
Object.entries({
  /* 直接使用已有的应用Icon（AppIcon）作为同名字段。 */
  AppIcon,
  /* 直接使用已有的应用ThemeSwitch（AppThemeSwitch）作为同名字段。 */
  AppThemeSwitch,
  /* 直接使用已有的应用空状态当前状态（AppEmptyState）作为同名字段。 */
  AppEmptyState,
  /* 直接使用已有的应用状态标签（AppStatusTag）作为同名字段。 */
  AppStatusTag,
  /* 直接使用已有的应用当前页请求头（AppPageHeader）作为同名字段。 */
  AppPageHeader,
}).forEach(([name, component]) => app.component(name, component));
/* 调用 app.use 完成use处理。 */
app.use(router);
/* 调用 registerElementPlus 完成registerElementPlus处理。 */
registerElementPlus(app);
/* 调用 app.mount 完成mount处理。 */
app.mount("#app");
