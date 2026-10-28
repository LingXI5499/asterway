<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<template>
  <el-config-provider :locale="zhCn">
    <header class="site-header">
      <RouterLink to="/" class="brand" aria-label="Asterway 首页">
        <span class="brand-mark" aria-hidden="true">A</span>
        <span>Asterway</span>
      </RouterLink>
      <span class="header-caption">账户管理</span>
    </header>
    <main class="app-container">
      <RouterView />
    </main>
  </el-config-provider>
</template>
