<!-- 渲染Period筛选条件界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { today } from "../../api/growth";
/* 引入 ./ScopeFilters.vue 提供的组件、状态函数或请求方法。 */
import ScopeFilters from "./ScopeFilters.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 筛选条件（filters）字段使用表达式 { type: Object, required: true } 的结果。 */
  filters: { /* 类型（type）字段使用对象（Object）。 */ type: Object, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true },
  /* 正在处理（busy）字段使用可为空的真假值（Boolean）。 */
  busy: Boolean,
});
/* 声明当前组件可以通知父组件的事件。 */
defineEmits(["search", "reset"]);
const /* picker类型（pickerType）保存随相关状态变化自动重算的值。 */ pickerType = computed(() =>
  props.filters.period === "MONTH"
    ? "month"
    : props.filters.period === "YEAR"
      ? "year"
      : "date",
);
/* 已禁用（disabled）：调用 disabled 完成已禁用处理。 */
function disabled(date) {
  const /* 文字（text）保存读取 Full年（FullYear） 的值。 */ text = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
  /* 返回条件判断的结果：text > today()，当前函数到此结束。 */
  return text > today();
}
</script>
<template>
  <!-- 显示一个布局容器；用样式类 period-filters 控制外观。 -->
  <div class="period-filters">
    <!-- 显示Element Plus 的 radio-group 组件；与 filters.period 双向同步；用样式类 period-choice 控制外观。 -->
    <el-radio-group v-model="filters.period" class="period-choice">
      <!-- 显示Element Plus 的 radio-button 组件；value 固定为 TOTAL。 -->
      <el-radio-button value="TOTAL">全部</el-radio-button
      ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 WEEK。 --><el-radio-button value="WEEK">周</el-radio-button
      ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 MONTH。 --><el-radio-button value="MONTH">月</el-radio-button
      ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 YEAR。 --><el-radio-button value="YEAR">年</el-radio-button
      ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 RANGE。 --><el-radio-button value="RANGE">自定义</el-radio-button>
    </el-radio-group>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 date-picker 组件；仅当 filters.period !== 'TOTAL' && filters.period !== 'RANGE' 成立时渲染；与 filters.anchorDate 双向同步；type 取自 pickerType；value-format 固定为 YYYY-MM-DD；disabled-date 取自 disabled；clearable 取自 false；placeholder 取自 filters.period === 'WEEK' ? '选择该周任一天' : '选择周期'。 -->
      <el-date-picker
        v-if="filters.period !== 'TOTAL' && filters.period !== 'RANGE'"
        v-model="filters.anchorDate"
        :type="pickerType"
        value-format="YYYY-MM-DD"
        :disabled-date="disabled"
        :clearable="false"
        :placeholder="filters.period === 'WEEK' ? '选择该周任一天' : '选择周期'"
      />
      <!-- 显示Element Plus 的 date-picker 组件；仅当 filters.period === 'RANGE' 成立时渲染；与 filters.dates 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；disabled-date 取自 disabled；start-placeholder 固定为 开始日期；end-placeholder 固定为 结束日期。 -->
      <el-date-picker
        v-if="filters.period === 'RANGE'"
        v-model="filters.dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        :disabled-date="disabled"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />
      <!-- 显示数据范围筛选条件（ScopeFilters）组件；filters 取自 filters。 -->
      <ScopeFilters :filters="filters" />
      <!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 busy；click 事件触发时执行 $emit('search')。 -->
      <el-button type="primary" :loading="busy" @click="$emit('search')"
        >查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 $emit('reset')。 --><el-button @click="$emit('reset')">重置</el-button>
    </div>
  </div>
</template>
