<!-- 渲染执行计时表界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { duration, instantText, executionStates } from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import { execution, controlExecution } from "../../stores/personal";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({ /* 记录列表（rows）字段使用Array。 */ rows: Array, /* 正在加载（loading）字段使用可为空的真假值（Boolean）。 */ loading: Boolean, /* controls字段使用可为空的真假值（Boolean）。 */ controls: Boolean });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["detail", "changed"]);
/* control：调用 control 完成control处理。 */
async function control(action, row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 controlExecution 完成control执行计时处理。 */
    await controlExecution(action, row);
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("changed");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("changed");
  }
}
</script>
<template>
  <!-- 显示Element Plus 的 table 组件；data 取自 rows；loading 指令使用 loading；empty-text 固定为 暂无执行记录。 -->
  <el-table :data="rows" v-loading="loading" empty-text="暂无执行记录">
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
    <template #empty><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 '暂无执行记录'。 --><AppEmptyState :description="'暂无执行记录'" /></template
    ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 taskName；label 固定为 任务；min-width 固定为 180；启用 show-overflow-tooltip 属性。 --><el-table-column
      prop="taskName"
      label="任务"
      min-width="180"
      show-overflow-tooltip
    />
    <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 104。 -->
    <el-table-column label="状态" width="104"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
        ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template
    ></el-table-column>
    <!-- 显示Element Plus 的 table-column 组件；label 固定为 首次开始；min-width 固定为 180。 -->
    <el-table-column label="首次开始" min-width="180"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 instantText(row.startedAt) 的当前值转成文字显示。 -->{{
        instantText(row.startedAt)
      }}</template></el-table-column
    >
    <!-- 显示Element Plus 的 table-column 组件；label 固定为 实际投入；min-width 固定为 120。 -->
    <el-table-column label="实际投入" min-width="120"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 duration(row.actualSeconds) 的当前值转成文字显示。 -->{{
        duration(row.actualSeconds)
      }}</template></el-table-column
    >
    <!-- 显示Element Plus 的 table-column 组件；label 固定为 结束时间；min-width 固定为 180。 -->
    <el-table-column label="结束时间" min-width="180"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 instantText(row.finishedAt) 的当前值转成文字显示。 -->{{
        instantText(row.finishedAt)
      }}</template></el-table-column
    >
    <!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；min-width 取自 controls ? 240 : 110；fixed 固定为 right。 -->
    <el-table-column
      label="操作"
      :min-width="controls ? 240 : 110"
      fixed="right"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }">
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('execution:session:read') 成立时渲染；类型设为 primary；启用 link 属性；click 事件触发时执行 emit('detail', row.id)。 -->
        <el-button
          v-if="canGrow('execution:session:read')"
          type="primary"
          link
          @click="emit('detail', row.id)"
          >详情</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 controls && row.status === 'PAUSED' && canGrow('execution:session:resume') 成立时渲染；类型设为 primary；启用 link 属性；disabled 取自 !!execution.busy；click 事件触发时执行 control('resume', row)。 -->
        <el-button
          v-if="
            controls &&
            row.status === 'PAUSED' &&
            canGrow('execution:session:resume')
          "
          type="primary"
          link
          :disabled="!!execution.busy"
          @click="control('resume', row)"
          >继续</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 controls && row.status === 'RUNNING' && canGrow('execution:session:pause') 成立时渲染；类型设为 warning；启用 link 属性；disabled 取自 !!execution.busy；click 事件触发时执行 control('pause', row)。 -->
        <el-button
          v-if="
            controls &&
            row.status === 'RUNNING' &&
            canGrow('execution:session:pause')
          "
          type="warning"
          link
          :disabled="!!execution.busy"
          @click="control('pause', row)"
          >暂停</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 controls && row.status !== 'FINISHED' && canGrow('execution:session:finish') 成立时渲染；类型设为 danger；启用 link 属性；disabled 取自 !!execution.busy；click 事件触发时执行 control('finish', row)。 -->
        <el-button
          v-if="
            controls &&
            row.status !== 'FINISHED' &&
            canGrow('execution:session:finish')
          "
          type="danger"
          link
          :disabled="!!execution.busy"
          @click="control('finish', row)"
          >结束</el-button
        >
      </template></el-table-column
    >
  </el-table>
</template>
