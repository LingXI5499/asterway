import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

// 应用入口：创建 Vue 应用，挂载路由，渲染到 #app 节点
createApp(App)
  .use(router)
  .mount('#app')
