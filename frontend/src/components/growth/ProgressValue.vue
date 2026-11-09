<!-- 渲染进度值界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({ /* 值（value）字段使用表达式 [Number, String] 的结果。 */ value: [Number, String] });
</script>
<template>
  <!-- 显示行内文字区域；仅当 value === null || value === undefined 成立时渲染；用样式类 muted 控制外观。 -->
  <span v-if="value === null || value === undefined" class="muted"
    >暂无可计算进度</span
  ><!-- 显示Element Plus 的 progress 组件；前面条件均不成立时显示这个备用分支；percentage 取自 Math.min(100, Math.max(0, Number(value)))；stroke-width 取自 8；format 取自 (p) => p + '%'。 --><el-progress
    v-else
    :percentage="Math.min(100, Math.max(0, Number(value)))"
    :stroke-width="8"
    :format="(p) => p + '%'"
  />
</template>
