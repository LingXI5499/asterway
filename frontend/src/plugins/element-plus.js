/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
/* 声明elementplus运行或构建配置。 */
import ElementPlus from "element-plus";
/* 引入 element-plus/es/locale/lang/zh-cn 提供的组件、状态函数或请求方法。 */
import zhCn from "element-plus/es/locale/lang/zh-cn";
/* 引入 import "element-plus/dist/index.css"; 提供的组件、状态函数或请求方法。 */
import "element-plus/dist/index.css";
export /* registerElementPlus：调用 registerElementPlus 完成registerElementPlus处理。 */ function registerElementPlus(app) {
  /* 调用 app.use 完成use处理。 */
  app.use(ElementPlus, { /* locale字段使用zhCn。 */ locale: zhCn });
}
