<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import {
  execution,
  elapsedSeconds,
  controlExecution,
} from "../../stores/personal";
import { canGrow } from "../../api/growth";
import { duration } from "../../api/personal";
import { report } from "../../api/ui";
const router = useRouter(),
  collapsed = ref(false);
async function control(action) {
  try {
    await controlExecution(action, execution.current);
  } catch (e) {
    report(e);
  }
}
</script>
<template>
  <aside
    v-if="execution.current && canGrow('execution:session:read')"
    class="execution-bar"
    :class="{ compact: collapsed }"
    aria-label="当前执行"
  >
    <button
      class="execution-bar-label"
      @click="router.push('/execution')"
      :aria-label="'进入执行工作台，' + execution.current.taskName"
    >
      <span class="running-dot" aria-hidden="true"></span
      ><span class="execution-bar-name">{{
        collapsed ? "执行中" : execution.current.taskName
      }}</span
      ><strong>{{ duration(elapsedSeconds()) }}</strong>
    </button>
    <div class="execution-bar-actions">
      <el-button
        v-if="!collapsed && canGrow('execution:session:pause')"
        size="small"
        :disabled="!!execution.busy"
        @click="control('pause')"
        >暂停</el-button
      >
      <el-button
        v-if="!collapsed && canGrow('execution:session:finish')"
        size="small"
        :disabled="!!execution.busy"
        @click="control('finish')"
        >结束</el-button
      >
      <el-button
        size="small"
        text
        :aria-expanded="!collapsed"
        @click="collapsed = !collapsed"
        >{{ collapsed ? "展开" : "收起" }}</el-button
      >
    </div>
    <span
      v-if="execution.error && !collapsed"
      class="execution-bar-warning"
      role="status"
      >{{ execution.error }}</span
    >
  </aside>
</template>
