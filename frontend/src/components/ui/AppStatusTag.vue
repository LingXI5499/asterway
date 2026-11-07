<script setup>
import { computed } from "vue";
const props = defineProps({ value: String, archived: Boolean });
const labels = {
  NOT_STARTED: "未开始",
  IN_PROGRESS: "进行中",
  PAUSED: "已暂停",
  COMPLETED: "已完成",
  ARCHIVED: "已归档",
  RUNNING: "执行中",
  FINISHED: "已结束",
  ACTIVE: "进行中",
  CLOSED: "已关闭",
  ENDED: "已结束",
  SUCCESS: "成功",
  FAILURE: "失败",
  ERROR: "异常",
  UNHANDLED: "待处理",
  PENDING: "待处理",
  CONFIRMED: "已确认",
  CONTINUED: "继续推进",
  INVALIDATED: "已失效",
};
const state = computed(() => (props.archived ? "ARCHIVED" : props.value));
const type = computed(() => {
  if (["COMPLETED", "RUNNING", "SUCCESS", "CONFIRMED"].includes(state.value))
    return "success";
  if (["PAUSED", "UNHANDLED", "PENDING"].includes(state.value))
    return "warning";
  if (["FAILURE", "ERROR"].includes(state.value)) return "danger";
  if (["IN_PROGRESS", "ACTIVE", "CONTINUED"].includes(state.value))
    return "primary";
  return "info";
});
</script>
<template>
  <el-tag :type="type" effect="light" class="aw-status"
    ><span v-if="state === 'RUNNING'" class="running-dot" />{{
      labels[state] || state || "—"
    }}</el-tag
  >
</template>
