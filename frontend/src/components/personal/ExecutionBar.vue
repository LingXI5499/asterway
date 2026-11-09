<!-- 渲染执行计时Bar界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import {
  execution,
  elapsedSeconds,
  controlExecution,
} from "../../stores/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { duration } from "../../api/personal";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* collapsed保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  collapsed = ref(false);
/* control：调用 control 完成control处理。 */
async function control(action) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 controlExecution 完成control执行计时处理。 */
    await controlExecution(action, execution.current);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
</script>
<template>
  <!-- 显示侧边区域；仅当 execution.current && canGrow('execution:session:read') 成立时渲染；用样式类 execution-bar 控制外观；class 取自 { compact: collapsed }；aria-label 提供无障碍说明 当前执行。 -->
  <aside
    v-if="execution.current && canGrow('execution:session:read')"
    class="execution-bar"
    :class="{ compact: collapsed }"
    aria-label="当前执行"
  >
    <!-- 显示可点击按钮；用样式类 execution-bar-label 控制外观；click 事件触发时执行 router.push('/execution')；aria-label 取自 '进入执行工作台，' + execution.current.taskName。 -->
    <button
      class="execution-bar-label"
      @click="router.push('/execution')"
      :aria-label="'进入执行工作台，' + execution.current.taskName"
    >
      <!-- 显示行内文字区域；用样式类 running-dot 控制外观；aria-hidden 提供无障碍说明 true。 -->
      <span class="running-dot" aria-hidden="true"></span
      ><!-- 显示行内文字区域；用样式类 execution-bar-name 控制外观。 --><span class="execution-bar-name"><!-- 把 collapsed ? "执行中" : execution.current.taskName 的当前值转成文字显示。 -->{{
        collapsed ? "执行中" : execution.current.taskName
      }}</span
      ><!-- 显示重点文字。 --><strong><!-- 把 duration(elapsedSeconds()) 的当前值转成文字显示。 -->{{ duration(elapsedSeconds()) }}</strong>
    </button>
    <!-- 显示一个布局容器；用样式类 execution-bar-actions 控制外观。 -->
    <div class="execution-bar-actions">
      <!-- 显示Element Plus 的 button 组件；仅当 !collapsed && canGrow('execution:session:pause') 成立时渲染；size 固定为 small；disabled 取自 !!execution.busy；click 事件触发时执行 control('pause')。 -->
      <el-button
        v-if="!collapsed && canGrow('execution:session:pause')"
        size="small"
        :disabled="!!execution.busy"
        @click="control('pause')"
        >暂停</el-button
      >
      <!-- 显示Element Plus 的 button 组件；仅当 !collapsed && canGrow('execution:session:finish') 成立时渲染；size 固定为 small；disabled 取自 !!execution.busy；click 事件触发时执行 control('finish')。 -->
      <el-button
        v-if="!collapsed && canGrow('execution:session:finish')"
        size="small"
        :disabled="!!execution.busy"
        @click="control('finish')"
        >结束</el-button
      >
      <!-- 显示Element Plus 的 button 组件；size 固定为 small；启用 text 属性；aria-expanded 取自 !collapsed；click 事件触发时执行 collapsed = !collapsed。 -->
      <el-button
        size="small"
        text
        :aria-expanded="!collapsed"
        @click="collapsed = !collapsed"
        ><!-- 把 collapsed ? "展开" : "收起" 的当前值转成文字显示。 -->{{ collapsed ? "展开" : "收起" }}</el-button
      >
    </div>
    <!-- 显示行内文字区域；仅当 execution.error && !collapsed 成立时渲染；用样式类 execution-bar-warning 控制外观；role 固定为 status。 -->
    <span
      v-if="execution.error && !collapsed"
      class="execution-bar-warning"
      role="status"
      ><!-- 把 execution.error 的当前值转成文字显示。 -->{{ execution.error }}</span
    >
  </aside>
</template>
