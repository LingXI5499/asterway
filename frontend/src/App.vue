<!-- 渲染应用界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { RouterView } from "vue-router";
</script>
<!-- 后台子路由共享同一个布局，仅登录与注册切换时重新创建认证页。 -->
<template>
  <!-- 显示当前路由对应页面的显示位置；定义 默认 插槽，接收 { Component, route }。 -->
  <RouterView v-slot="{ Component, route }">
    <!-- 显示component组件；is 取自 Component；key 取自 route.matched[0]?.path。 -->
    <component :is="Component" :key="route.matched[0]?.path" />
  </RouterView>
</template>
