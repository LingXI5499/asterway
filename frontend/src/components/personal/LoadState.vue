<!-- 渲染加载当前状态界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({
  /* 正在加载（loading）字段使用可为空的真假值（Boolean）。 */
  loading: Boolean,
  /* 错误（error）字段使用可为空的文字（String）。 */
  error: String,
  /* 记录列表（rows）字段使用表达式 { type: Number, default: 5 } 的结果。 */
  rows: { /* 类型（type）字段使用数值（Number）。 */ type: Number, /* 默认值（default）字段使用固定数值 5。 */ default: 5 },
});
/* 声明当前组件可以通知父组件的事件。 */
defineEmits(["retry"]);
</script>
<template>
  <!-- 显示一个布局容器；仅当 error 成立时渲染；用样式类 inline-error 控制外观；role 固定为 alert。 -->
  <div v-if="error" class="inline-error" role="alert">
    <!-- 显示行内文字区域。 -->
    <span><!-- 把 error 的当前值转成文字显示。 -->{{ error }}</span
    ><!-- 显示Element Plus 的 button 组件；启用 text 属性；类型设为 primary；click 事件触发时执行 $emit('retry')。 --><el-button text type="primary" @click="$emit('retry')">重新加载</el-button>
  </div>
  <!-- 显示Element Plus 的 skeleton 组件；仅当 loading 成立时渲染；rows 取自 rows；启用 animated 属性。 -->
  <el-skeleton v-if="loading" :rows="rows" animated />
  <!-- 显示由父组件补充内容的插槽；前一条件不成立且 !error 成立时渲染。 -->
  <slot v-else-if="!error" />
</template>
