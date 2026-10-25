import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 构建配置
export default defineConfig({
  plugins: [vue()],

  server: {
    // 前端开发服务器端口
    port: 5173,

    // 代理配置：把 /api 开头的请求转发到后端，
    // 这样前端和后端对浏览器来说同源，开发阶段无需处理跨域。
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8088',
        changeOrigin: true
      }
    }
  }
})
