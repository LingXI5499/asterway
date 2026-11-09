<!-- 渲染应用状态标签界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 值（value）字段使用可为空的文字（String）。 */ value: String, /* 已归档（archived）字段使用可为空的真假值（Boolean）。 */ archived: Boolean });
const /* 名称对应表（labels）保存表达式 { NOT_STARTED: "未开始", IN_PROGRESS: "进行中", PAUSED: "已暂停", COMPLETED: "已完成", ARCHIVED: "已归档", RUNNING: "执行中", FINISHED: "已… 的结果。 */ labels = {
  /* NOT开始时（NOT_STARTED）字段使用固定文字 '未开始'。 */
  NOT_STARTED: "未开始",
  /* IN进度（IN_PROGRESS）字段使用固定文字 '进行中'。 */
  IN_PROGRESS: "进行中",
  /* 已暂停（PAUSED）字段使用固定文字 '已暂停'。 */
  PAUSED: "已暂停",
  /* 已完成（COMPLETED）字段使用固定文字 '已完成'。 */
  COMPLETED: "已完成",
  /* 已归档（ARCHIVED）字段使用固定文字 '已归档'。 */
  ARCHIVED: "已归档",
  /* 运行中（RUNNING）字段使用固定文字 '执行中'。 */
  RUNNING: "执行中",
  /* FINISHED字段使用固定文字 '已结束'。 */
  FINISHED: "已结束",
  /* 页面是否仍处于活动状态（ACTIVE）字段使用固定文字 '进行中'。 */
  ACTIVE: "进行中",
  /* CLOSED字段使用固定文字 '已关闭'。 */
  CLOSED: "已关闭",
  /* 已结束（ENDED）字段使用固定文字 '已结束'。 */
  ENDED: "已结束",
  /* 成功（SUCCESS）字段使用固定文字 '成功'。 */
  SUCCESS: "成功",
  /* 失败（FAILURE）字段使用固定文字 '失败'。 */
  FAILURE: "失败",
  /* 错误（ERROR）字段使用固定文字 '异常'。 */
  ERROR: "异常",
  /* UNHANDLED字段使用固定文字 '待处理'。 */
  UNHANDLED: "待处理",
  /* 等待处理（PENDING）字段使用固定文字 '待处理'。 */
  PENDING: "待处理",
  /* 已确认（CONFIRMED）字段使用固定文字 '已确认'。 */
  CONFIRMED: "已确认",
  /* CONTINUED字段使用固定文字 '继续推进'。 */
  CONTINUED: "继续推进",
  /* INVALIDATED字段使用固定文字 '已失效'。 */
  INVALIDATED: "已失效",
};
const /* 当前状态（state）保存随相关状态变化自动重算的值。 */ state = computed(() => (props.archived ? "ARCHIVED" : props.value));
const /* 类型（type）保存随相关状态变化自动重算的值。 */ type = computed(() => {
  /* 检查条件 ["COMPLETED", "RUNNING", "SUCCESS", "CONFIRMED"].includes(state.value) 是否成立，成立时走这一分支。 */
  if (["COMPLETED", "RUNNING", "SUCCESS", "CONFIRMED"].includes(state.value))
    /* 返回固定文字 'success'，当前函数到此结束。 */
    return "success";
  /* 检查条件 ["PAUSED", "UNHANDLED", "PENDING"].includes(state.value) 是否成立，成立时走这一分支。 */
  if (["PAUSED", "UNHANDLED", "PENDING"].includes(state.value))
    /* 返回固定文字 'warning'，当前函数到此结束。 */
    return "warning";
  /* 检查条件 ["FAILURE", "ERROR"].includes(state.value) 是否成立，成立时走这一分支。 */
  if (["FAILURE", "ERROR"].includes(state.value)) /* 返回固定文字 'danger'，当前函数到此结束。 */ return "danger";
  /* 检查条件 ["IN_PROGRESS", "ACTIVE", "CONTINUED"].includes(state.value) 是否成立，成立时走这一分支。 */
  if (["IN_PROGRESS", "ACTIVE", "CONTINUED"].includes(state.value))
    /* 返回固定文字 'primary'，当前函数到此结束。 */
    return "primary";
  /* 返回固定文字 'info'，当前函数到此结束。 */
  return "info";
});
</script>
<template>
  <!-- 显示Element Plus 的 tag 组件；type 取自 type；effect 固定为 light；用样式类 aw-status 控制外观。 -->
  <el-tag :type="type" effect="light" class="aw-status"
    ><!-- 显示行内文字区域；仅当 state === 'RUNNING' 成立时渲染；用样式类 running-dot 控制外观。 --><span v-if="state === 'RUNNING'" class="running-dot" /><!-- 把 labels[state] || state || "—" 的当前值转成文字显示。 -->{{
      labels[state] || state || "—"
    }}</el-tag
  >
</template>
