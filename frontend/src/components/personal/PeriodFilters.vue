<script setup>
import { computed } from "vue";
import { today } from "../../api/growth";
import ScopeFilters from "./ScopeFilters.vue";
const props = defineProps({
  filters: { type: Object, required: true },
  busy: Boolean,
});
defineEmits(["search", "reset"]);
const pickerType = computed(() =>
  props.filters.period === "MONTH"
    ? "month"
    : props.filters.period === "YEAR"
      ? "year"
      : "date",
);
function disabled(date) {
  const text = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
  return text > today();
}
</script>
<template>
  <div class="period-filters">
    <el-radio-group v-model="filters.period" class="period-choice">
      <el-radio-button value="TOTAL">全部</el-radio-button
      ><el-radio-button value="WEEK">周</el-radio-button
      ><el-radio-button value="MONTH">月</el-radio-button
      ><el-radio-button value="YEAR">年</el-radio-button
      ><el-radio-button value="RANGE">自定义</el-radio-button>
    </el-radio-group>
    <div class="toolbar">
      <el-date-picker
        v-if="filters.period !== 'TOTAL' && filters.period !== 'RANGE'"
        v-model="filters.anchorDate"
        :type="pickerType"
        value-format="YYYY-MM-DD"
        :disabled-date="disabled"
        :clearable="false"
        :placeholder="filters.period === 'WEEK' ? '选择该周任一天' : '选择周期'"
      />
      <el-date-picker
        v-if="filters.period === 'RANGE'"
        v-model="filters.dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        :disabled-date="disabled"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />
      <ScopeFilters :filters="filters" />
      <el-button type="primary" :loading="busy" @click="$emit('search')"
        >查询</el-button
      ><el-button @click="$emit('reset')">重置</el-button>
    </div>
  </div>
</template>
