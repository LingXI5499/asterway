<!-- 渲染当前页Pagination界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({ /* 当前状态（state）字段使用表达式 { type: Object, required: true } 的结果。 */ state: { /* 类型（type）字段使用对象（Object）。 */ type: Object, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true } });
/* 声明当前组件可以通知父组件的事件。 */
defineEmits(["change"]);
</script>
<template>
  <!-- 显示Element Plus 的 pagination 组件；用样式类 pagination 控制外观；与 state.page 双向同步（current-page）；与 state.size 双向同步（page-size）；total 取自 state.total；page-sizes 取自 [10, 20, 50, 100]；layout 固定为 total,sizes,prev,pager,next；change 事件触发时执行 $emit('change')。 -->
  <el-pagination
    class="pagination"
    v-model:current-page="state.page"
    v-model:page-size="state.size"
    :total="state.total"
    :page-sizes="[10, 20, 50, 100]"
    layout="total,sizes,prev,pager,next"
    @change="$emit('change')"
  />
</template>
