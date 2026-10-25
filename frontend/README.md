# Asterway 前端

Vue 账户功能初版，包含登录、注册和账户中心页面，使用服务端会话与 Cookie CSRF Token。

```powershell
npm ci
npm run dev
npm run build
```

开发服务器默认端口为 5173，`/api` 代理到 `http://127.0.0.1:8088`。后端数据库连接通过环境变量或被忽略的本地配置提供；生产环境需另行配置同域反向代理与 HTTPS。

2026-10-25 已通过生产构建；本次未执行真实浏览器、HTTP 与 PostgreSQL 的完整业务验收。前端目前没有自动化测试脚本。
