import { createApp } from 'vue'
import { registerElementPlus } from './plugins/element-plus'
import App from './App.vue'
import router from './router'
import './style.css'

// 统一使用简体中文组件，业务校验继续由各页面和服务端负责。
const app = createApp(App)
app.use(router)
registerElementPlus(app)
app.mount('#app')
