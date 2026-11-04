<script setup>
import { RouterView } from "vue-router";
</script>
<!-- 后台子路由共享同一个布局，仅登录与注册切换时重新创建认证页。 -->
<template>
  <RouterView v-slot="{ Component, route }">
    <component :is="Component" :key="route.matched[0]?.path" />
  </RouterView>
</template>
