<!-- 渲染任务详情页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute, useRouter } from "vue-router";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../../api/http";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import {
  getResource,
  states,
  priorities,
  positions,
  canGrow,
} from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { dateText } from "../../api/ui";
/* 引入 ../../components/growth/GrowthActions.vue 提供的组件、状态函数或请求方法。 */
import GrowthActions from "../../components/growth/GrowthActions.vue";
/* 引入 ../../components/personal/GrowthEvidence.vue 提供的组件、状态函数或请求方法。 */
import GrowthEvidence from "../../components/personal/GrowthEvidence.vue";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { canStartTask } from "../../api/personal";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import { execution, startExecution } from "../../stores/personal";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* begin：开始一组需要一起提交或回滚的数据库操作。 */
async function begin() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 startExecution 完成开始执行计时处理。 */
    await startExecution(record.value);
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 调用 report 完成复盘报告处理。 */
    report(error);
  }
}
const /* route保存取得当前页面路径和查询参数。 */ route = useRoute(),
  /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */
  router = useRouter(),
  /* 记录（record）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  record = ref(null),
  /* 目标（goal）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  goal = ref(null),
  /* 任务安排（assignment）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  assignment = ref(null),
  /* assigned周（assignedWeek）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  assignedWeek = ref(null),
  /* assigned日（assignedDay）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  assignedDay = ref(null),
  /* 正在加载（loading）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  loading = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* actions保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  actions = ref(null);
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 loading.value。 */
  loading.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 读取 Resource 的值 完成后取得结果写入 record.value。 */
    record.value = await getResource("task", route.params.id);
    /* 把等待 读取 Resource 的值 完成后取得结果写入 goal.value。 */
    goal.value = await getResource("goal", record.value.goalId);
    /* 检查当前用户是否具有需要的权限：canGrow("planning:assignment:read")，成立时走这一分支。 */
    if (canGrow("planning:assignment:read")) {
      /* 把等待 调用 api 完成接口处理 完成后取得结果写入 assignment.value。 */
      assignment.value = await api(`/tasks/${record.value.id}/assignment`);
      /* 把根据前面的条件，从两个候选结果中选择一个：assignment.value ? await api(`/weekly-plans/${assignment.value.weeklyPlanId}`) : null写入 assignedWeek.value。 */
      assignedWeek.value = assignment.value
        ? await api(`/weekly-plans/${assignment.value.weeklyPlanId}`)
        : null;
      /* 把沿 assignment.value?.dailyPlanId ? await api(`/daily-plans/${assignment.value.dailyPlanId}`) : null 读取数据；中间对象为空时返回空值写入 assignedDay.value。 */
      assignedDay.value = assignment.value?.dailyPlanId
        ? await api(`/daily-plans/${assignment.value.dailyPlanId}`)
        : null;
    }
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 loading.value。 */
    loading.value = false;
  }
}
/* 已修改（changed）：调用 changed 完成已修改处理。 */
async function changed() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待读取 Resource 的值。 */
    await getResource("task", route.params.id);
    /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    /* 调用 router.push 完成push处理。 */
    router.push("/growth/tasks");
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
  <el-alert
    v-if="error"
    :title="error"
    type="error"
    :closable="false"
  /><!-- 显示Element Plus 的 skeleton 组件；仅当 loading && !record 成立时渲染；rows 取自 8。 --><el-skeleton v-if="loading && !record" :rows="8" /><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 record 成立时渲染。 --><template v-if="record"
    ><!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 --><section class="card">
      <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
      <div class="toolbar">
        <!-- 显示Element Plus 的 button 组件；启用 text 属性；click 事件触发时执行 router.push(`/growth/stages/${record.stageId}`)。 -->
        <el-button text @click="router.push(`/growth/stages/${record.stageId}`)"
          >返回所属阶段</el-button
        ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 record.status。 --><AppStatusTag :value="record.status" /><!-- 显示Element Plus 的 tag 组件；仅当 goal?.archivedAt 成立时渲染；类型设为 info。 --><el-tag
          v-if="goal?.archivedAt"
          type="info"
          >目标已归档</el-tag
        >
      </div>
      <!-- 显示二级标题。 -->
      <h2><!-- 把 record.name 的当前值转成文字显示。 -->{{ record.name }}</h2>
      <!-- 显示一段文字；用样式类 preserve-text 控制外观。 -->
      <p class="preserve-text"><!-- 把 record.description || "尚未填写描述" 的当前值转成文字显示。 -->{{ record.description || "尚未填写描述" }}</p>
      <!-- 显示Element Plus 的 descriptions 组件；column 取自 2；启用 border 属性。 -->
      <el-descriptions :column="2" border
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 来源目标。 --><el-descriptions-item label="来源目标"><!-- 把 record.goalName 的当前值转成文字显示。 -->{{
          record.goalName
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 来源阶段。 --><el-descriptions-item label="来源阶段"><!-- 把 record.stageName 的当前值转成文字显示。 -->{{
          record.stageName
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 完成条件；span 取自 2。 --><el-descriptions-item label="完成条件" :span="2"
          ><!-- 显示行内文字区域；用样式类 preserve-text 控制外观。 --><span class="preserve-text"><!-- 把 record.completionCriteria || "未填写" 的当前值转成文字显示。 -->{{
            record.completionCriteria || "未填写"
          }}</span></el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 优先级。 --><el-descriptions-item label="优先级"><!-- 把 priorities[record.priority] 的当前值转成文字显示。 -->{{
          priorities[record.priority]
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 权重。 --><el-descriptions-item label="权重"><!-- 把 record.weight 的当前值转成文字显示。 -->{{
          record.weight
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 预计耗时。 --><el-descriptions-item label="预计耗时"><!-- 把 record.estimatedMinutes ? record.estimatedMinutes + " 分钟" : "未指定" 的当前值转成文字显示。 -->{{
          record.estimatedMinutes ? record.estimatedMinutes + " 分钟" : "未指定"
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 完成时间。 --><el-descriptions-item label="完成时间"><!-- 把 dateText(record.completedAt) 的当前值转成文字显示。 -->{{
          dateText(record.completedAt)
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 开始日期。 --><el-descriptions-item label="开始日期"><!-- 把 record.startDate || "未指定" 的当前值转成文字显示。 -->{{
          record.startDate || "未指定"
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 截止日期。 --><el-descriptions-item label="截止日期"><!-- 把 record.dueDate || "未指定" 的当前值转成文字显示。 -->{{
          record.dueDate || "未指定"
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 当前位置。 --><el-descriptions-item label="当前位置"><!-- 把 positions[record.assignmentStatus] 的当前值转成文字显示。 -->{{
          positions[record.assignmentStatus]
        }}</el-descriptions-item
        ><!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 当前安排。 --><el-descriptions-item label="当前安排"
          ><!-- 把 assignedDay?.planDate || assignedWeek?.weekStartDate || "未安排" 的当前值转成文字显示。 -->{{ assignedDay?.planDate || assignedWeek?.weekStartDate || "未安排"
          }}<!-- 把 !assignedDay && assignedWeek ? " 所在周" : "" 的当前值转成文字显示。 -->{{
            !assignedDay && assignedWeek ? " 所在周" : ""
          }}</el-descriptions-item
        ></el-descriptions
      >
      <!-- 显示一个布局容器；用样式类 toolbar detail-toolbar 控制外观。 -->
      <div class="toolbar detail-toolbar">
        <!-- 显示Element Plus 的 button 组件；仅当 canStartTask() 成立时渲染；类型设为 success；disabled 取自 !!execution.current || !!execution.busy || !assignment || !!goal?.archivedAt || record.status === 'COMPLE…；click 事件触发时执行 begin。 -->
        <el-button
          v-if="canStartTask()"
          type="success"
          :disabled="
            !!execution.current ||
            !!execution.busy ||
            !assignment ||
            !!goal?.archivedAt ||
            record.status === 'COMPLETED'
          "
          @click="begin"
          >开始执行</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:update') 成立时渲染；disabled 取自 !!goal?.archivedAt；类型设为 primary；click 事件触发时执行 actions.edit('task', record)。 -->
        <el-button
          v-if="canGrow('task:task:update')"
          :disabled="!!goal?.archivedAt"
          type="primary"
          @click="actions.edit('task', record)"
          >编辑任务</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:status') 成立时渲染；disabled 取自 !!goal?.archivedAt；click 事件触发时执行 actions.status('task', record)。 --><el-button
          v-if="canGrow('task:task:status')"
          :disabled="!!goal?.archivedAt"
          @click="actions.status('task', record)"
          >修改状态</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('planning:assignment:manage') && canGrow('planning:assignment:read') 成立时渲染；disabled 取自 record.status === 'COMPLETED' || !!goal?.archivedAt；click 事件触发时执行 actions.assign(record)。 --><el-button
          v-if="
            canGrow('planning:assignment:manage') &&
            canGrow('planning:assignment:read')
          "
          :disabled="record.status === 'COMPLETED' || !!goal?.archivedAt"
          @click="actions.assign(record)"
          >安排／改期</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('planning:assignment:remove') && canGrow('planning:assignment:read') 成立时渲染；disabled 取自 !assignment || !!goal?.archivedAt；click 事件触发时执行 actions.unassign(record)。 --><el-button
          v-if="
            canGrow('planning:assignment:remove') &&
            canGrow('planning:assignment:read')
          "
          :disabled="!assignment || !!goal?.archivedAt"
          @click="actions.unassign(record)"
          >移出计划</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('planning:assignment:read') 成立时渲染；click 事件触发时执行 actions.history(record)。 --><el-button
          v-if="canGrow('planning:assignment:read')"
          @click="actions.history(record)"
          >安排历史</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:delete') 成立时渲染；disabled 取自 !!goal?.archivedAt；类型设为 danger；启用 plain 属性；click 事件触发时执行 actions.remove('task', record)。 --><el-button
          v-if="canGrow('task:task:delete')"
          :disabled="!!goal?.archivedAt"
          type="danger"
          plain
          @click="actions.remove('task', record)"
          >删除任务</el-button
        >
      </div>
    </section>
    <!-- 显示成长业务证明信息（GrowthEvidence）组件；kind 固定为 task；id 取自 record.id。 -->
    <GrowthEvidence kind="task" :id="record.id" /></template
  ><!-- 显示成长业务Actions（GrowthActions）组件；把组件或元素引用保存为 actions，供脚本读取；changed 事件触发时执行 changed。 --><GrowthActions ref="actions" @changed="changed" />
</template>
