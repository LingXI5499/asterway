<script setup>
import { useRouter } from "vue-router";
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
          : '请检查地址，或返回个人中心。'
      "
      ><template #extra
        ><el-button @click="router.back()">返回上一页</el-button
        ><el-button type="primary" @click="router.push('/account/profile')"
          >个人中心</el-button
        ></template
      ></el-result
    >
  </section>
</template>
