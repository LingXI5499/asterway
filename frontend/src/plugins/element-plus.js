import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";
export function registerElementPlus(app) {
  app.use(ElementPlus, { locale: zhCn });
}
