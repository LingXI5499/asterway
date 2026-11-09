<!-- 渲染成长业务任务表界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 任务与计划查询共用表格，依据动作权限、归档和只读状态显示操作。
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { states, priorities, positions, canGrow } from "../../api/growth";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../../api/http";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { canStartTask } from "../../api/personal";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import { execution, startExecution } from "../../stores/personal";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* begin：开始一组需要一起提交或回滚的数据库操作。 */
async function begin(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    // 计划行的 version 属于安排，开始执行必须重新取得任务自己的版本。
    const /* 任务（task）保存根据前面的条件，从两个候选结果中选择一个：row.taskId ? await api("/tasks/" + row.taskId) : row。 */ task = row.taskId ? await api("/tasks/" + row.taskId) : row;
    const /* 值（value）保存等待 调用 startExecution 完成开始执行计时处理 完成后取得结果。 */ value = await startExecution(task);
    /* 把固定状态或编码 IN_PROGRESS（IN进度）写入 row.status。 */
    row.status = "IN_PROGRESS";
    /* 检查条件 !row.taskId 是否成立，成立时走这一分支。 */
    if (!row.taskId) /* 把表达式 value.taskVersion 的结果写入 row.version。 */ row.version = value.taskVersion;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 调用 report 完成复盘报告处理。 */
    report(error);
  }
}
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 记录列表（rows）字段使用Array。 */ rows: Array, /* 正在加载（loading）字段使用可为空的真假值（Boolean）。 */ loading: Boolean, /* 是否只读（readOnly）字段使用可为空的真假值（Boolean）。 */ readOnly: Boolean });
/* 声明当前组件可以通知父组件的事件。 */
defineEmits([
  "detail",
  "edit",
  "status",
  "remove",
  "assign",
  "unassign",
  "history",
]);
</script>
<template>
  <!-- 显示Element Plus 的 table 组件；data 取自 rows；loading 指令使用 loading；empty-text 固定为 暂无任务，先创建任务或调整筛选条件。 -->
  <el-table
    :data="rows"
    v-loading="loading"
    empty-text="暂无任务，先创建任务或调整筛选条件"
    ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
      ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 '暂无任务，先创建任务或调整筛选条件'。 --><AppEmptyState
        :description="'暂无任务，先创建任务或调整筛选条件'" /></template
    ><!-- 显示Element Plus 的 table-column 组件；label 固定为 任务；min-width 固定为 200。 --><el-table-column label="任务" min-width="200"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 $emit('detail', row)。 --><el-button
          v-if="canGrow('task:task:read')"
          link
          type="primary"
          @click="$emit('detail', row)"
          ><!-- 把 row.taskName || row.name 的当前值转成文字显示。 -->{{ row.taskName || row.name }}</el-button
        ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.taskName || row.name 的当前值转成文字显示。 -->{{ row.taskName || row.name }}</span
        ><!-- 显示Element Plus 的 tag 组件；仅当 row.archivedAt || row.goalArchivedAt 成立时渲染；类型设为 info；size 固定为 small。 --><el-tag
          v-if="row.archivedAt || row.goalArchivedAt"
          type="info"
          size="small"
          >目标已归档</el-tag
        ></template
      ></el-table-column
    ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 goalName；label 固定为 来源目标；min-width 固定为 140；启用 show-overflow-tooltip 属性。 --><el-table-column
      prop="goalName"
      label="来源目标"
      min-width="140"
      show-overflow-tooltip
    /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 stageName；label 固定为 来源阶段；min-width 固定为 140；启用 show-overflow-tooltip 属性。 --><el-table-column
      prop="stageName"
      label="来源阶段"
      min-width="140"
      show-overflow-tooltip
    /><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 100。 --><el-table-column label="状态" width="100"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
        ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template></el-table-column
    ><!-- 显示Element Plus 的 table-column 组件；label 固定为 优先级；width 固定为 90。 --><el-table-column label="优先级" width="90"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 priorities[row.priority] 的当前值转成文字显示。 -->{{
        priorities[row.priority]
      }}</template></el-table-column
    ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 weight；label 固定为 权重；width 固定为 70。 --><el-table-column prop="weight" label="权重" width="70" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 安排；min-width 固定为 130。 --><el-table-column
      label="安排"
      min-width="130"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 row.planDate || positions[row.assignmentStatus] || (row.dailyPlanId ? "已安排到日" : "已安排到周") 的当前值转成文字显示。 -->{{
        row.planDate ||
        positions[row.assignmentStatus] ||
        (row.dailyPlanId ? "已安排到日" : "已安排到周")
      }}</template></el-table-column
    ><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；min-width 固定为 300；fixed 固定为 right。 --><el-table-column label="操作" min-width="300" fixed="right"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
        ><!-- 显示Element Plus 的 button 组件；仅当 canStartTask() && canGrow('task:task:read') 成立时渲染；启用 link 属性；类型设为 success；disabled 取自 readOnly || !!execution.busy || !!execution.current || row.status === 'COMPLETED' || !!(row.archivedAt ||…；click 事件触发时执行 begin(row)。 --><el-button
          v-if="canStartTask() && canGrow('task:task:read')"
          link
          type="success"
          :disabled="
            readOnly ||
            !!execution.busy ||
            !!execution.current ||
            row.status === 'COMPLETED' ||
            !!(row.archivedAt || row.goalArchivedAt) ||
            row.assignmentStatus === 'POOL'
          "
          @click="begin(row)"
          >开始执行</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:update') && canGrow('task:task:read') 成立时渲染；disabled 取自 readOnly || !!(row.archivedAt || row.goalArchivedAt)；启用 link 属性；类型设为 primary；click 事件触发时执行 $emit('edit', row)。 --><el-button
          v-if="canGrow('task:task:update') && canGrow('task:task:read')"
          :disabled="readOnly || !!(row.archivedAt || row.goalArchivedAt)"
          link
          type="primary"
          @click="$emit('edit', row)"
          >编辑</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:status') 成立时渲染；disabled 取自 readOnly || !!(row.archivedAt || row.goalArchivedAt)；启用 link 属性；类型设为 primary；click 事件触发时执行 $emit('status', row)。 --><el-button
          v-if="canGrow('task:task:status')"
          :disabled="readOnly || !!(row.archivedAt || row.goalArchivedAt)"
          link
          type="primary"
          @click="$emit('status', row)"
          >状态</el-button
        >
        <!-- 显示Element Plus 的 dropdown 组件；仅当 canGrow('planning:assignment:read') || canGrow('task:task:delete') 成立时渲染；trigger 固定为 click；command 事件触发时执行 (command) => $emit(command, row)。 -->
        <el-dropdown
          v-if="
            canGrow('planning:assignment:read') || canGrow('task:task:delete')
          "
          trigger="click"
          @command="(command) => $emit(command, row)"
        >
          <!-- 显示Element Plus 的 button 组件；启用 text 属性；aria-label 提供无障碍说明 更多任务操作。 -->
          <el-button text aria-label="更多任务操作"
            >更多<!-- 显示应用Icon（AppIcon）组件；name 固定为 more；size 取自 16。 --><AppIcon name="more" :size="16"
          /></el-button>
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
          <template #dropdown
            ><!-- 显示Element Plus 的 dropdown-menu 组件。 --><el-dropdown-menu>
              <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canGrow('planning:assignment:manage') && canGrow('planning:assignment:read') 成立时渲染；command 固定为 assign；disabled 取自 row.status === 'COMPLETED' || !!(row.archivedAt || row.goalArchivedAt)。 -->
              <el-dropdown-item
                v-if="
                  canGrow('planning:assignment:manage') &&
                  canGrow('planning:assignment:read')
                "
                command="assign"
                :disabled="
                  row.status === 'COMPLETED' ||
                  !!(row.archivedAt || row.goalArchivedAt)
                "
                >安排／改期</el-dropdown-item
              >
              <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canGrow('planning:assignment:remove') && canGrow('planning:assignment:read') 成立时渲染；command 固定为 unassign；disabled 取自 !!(row.archivedAt || row.goalArchivedAt) || row.assignmentStatus === 'POOL'。 -->
              <el-dropdown-item
                v-if="
                  canGrow('planning:assignment:remove') &&
                  canGrow('planning:assignment:read')
                "
                command="unassign"
                :disabled="
                  !!(row.archivedAt || row.goalArchivedAt) ||
                  row.assignmentStatus === 'POOL'
                "
                >移出当前安排</el-dropdown-item
              >
              <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canGrow('planning:assignment:read') 成立时渲染；command 固定为 history。 -->
              <el-dropdown-item
                v-if="canGrow('planning:assignment:read')"
                command="history"
                >安排历史</el-dropdown-item
              >
              <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canGrow('task:task:delete') 成立时渲染；command 固定为 remove；disabled 取自 readOnly || !!(row.archivedAt || row.goalArchivedAt)。 -->
              <el-dropdown-item
                v-if="canGrow('task:task:delete')"
                command="remove"
                :disabled="readOnly || !!(row.archivedAt || row.goalArchivedAt)"
                >删除任务</el-dropdown-item
              >
            </el-dropdown-menu></template
          >
        </el-dropdown></template
      ></el-table-column
    ></el-table
  >
</template>
