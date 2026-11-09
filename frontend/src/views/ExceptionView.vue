<!-- 渲染Exception页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../router 提供的组件、状态函数或请求方法。 */
import { defaultPath } from "../router";
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({ /* forbidden字段使用可为空的真假值（Boolean）。 */ forbidden: Boolean });
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter();
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示Element Plus 的 result 组件；icon 取自 forbidden ? 'warning' : 'info'；title 取自 forbidden ? '403 · 无访问权限' : '404 · 页面不存在'；sub-title 取自 forbidden ? '当前账户没有访问该页面的权限。' : '这个地址没有对应的页面，可以返回工作区继续。'。 -->
    <el-result
      :icon="forbidden ? 'warning' : 'info'"
      :title="forbidden ? '403 · 无访问权限' : '404 · 页面不存在'"
      :sub-title="
        forbidden
          ? '当前账户没有访问该页面的权限。'
          : '这个地址没有对应的页面，可以返回工作区继续。'
      "
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 extra 插槽。 --><template #extra
        ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 router.back()。 --><el-button @click="router.back()">返回上一页</el-button
        ><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 router.push(defaultPath())。 --><el-button type="primary" @click="router.push(defaultPath())"
          >返回工作区</el-button
        ></template
      ></el-result
    >
  </section>
</template>
