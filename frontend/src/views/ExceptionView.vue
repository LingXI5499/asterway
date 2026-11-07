<script setup>
import { useRouter } from "vue-router";
import { defaultPath } from "../router";
defineProps({ forbidden: Boolean });
const router = useRouter();
</script>
<template>
  <section class="card">
    <el-result
      :icon="forbidden ? 'warning' : 'info'"
      :title="forbidden ? '403 · 无访问权限' : '404 · 页面不存在'"
      :sub-title="
        forbidden
          ? '当前账户没有访问该页面的权限。'
          : '这个地址没有对应的页面，可以返回工作区继续。'
      "
      ><template #extra
        ><el-button @click="router.back()">返回上一页</el-button
        ><el-button type="primary" @click="router.push(defaultPath())"
          >返回工作区</el-button
        ></template
      ></el-result
    >
  </section>
</template>
