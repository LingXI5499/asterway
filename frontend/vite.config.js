/* 引入 vite 提供的组件、状态函数或请求方法。 */
/* 声明vite中的config运行或构建配置。 */
import { defineConfig } from "vite";
/* 引入 @vitejs/plugin-vue 提供的组件、状态函数或请求方法。 */
import vue from "@vitejs/plugin-vue";
/* 把下面的对象或组件作为该文件默认对外提供的内容。 */
export default defineConfig({
  /* plugins字段使用表达式 [vue()] 的结果。 */
  plugins: [vue()],
  /* server字段使用按表达式计算得到的值：{ // 固定本地端口，与后端允许来源保持一致，避免自动顺延后验证码被拦截。 port: 5175, strictPort: true, proxy: { "/asterway": { target: "http://127.0.0.1:8…。 */
  server: {
    // 固定本地端口，与后端允许来源保持一致，避免自动顺延后验证码被拦截。
    /* port字段使用固定数值 5175。 */
    port: 5175,
    /* strictPort字段使用真，表示条件成立或开关打开。 */
    strictPort: true,
    /* proxy字段使用按表达式计算得到的值：{ "/asterway": { target: "http://127.0.0.1:8089", changeOrigin: true }, }。 */
    proxy: {
      /* asterway（/asterway）字段使用按表达式计算得到的值：{ target: "http://127.0.0.1:8089", changeOrigin: true }。 */
      "/asterway": { /* 目标值（target）字段使用固定文字 'http://127.0.0.1:8089'。 */ target: "http://127.0.0.1:8089", /* 修改原始（changeOrigin）字段使用真，表示条件成立或开关打开。 */ changeOrigin: true },
    },
  },
});
