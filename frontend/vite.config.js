import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
export default defineConfig({
  plugins: [vue()],
  server: {
    // 固定本地端口，与后端允许来源保持一致，避免自动顺延后验证码被拦截。
    port: 5175,
    strictPort: true,
    proxy: {
      "/asterway": { target: "http://127.0.0.1:8089", changeOrigin: true },
    },
  },
});
