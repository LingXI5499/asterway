<!-- 渲染任务安排历史界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 只读分页查看当前任务的安排历史，不提供修改历史入口。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query } from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { dateText } from "../../api/ui";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 任务编号（taskId）字段使用表达式 [String, Number] 的结果。 */ taskId: [String, Number] });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["close"]);
const /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ rows = ref([]),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
const /* reasons保存表达式 { RESCHEDULED: "改期", RETURNED_TO_POOL: "退回任务池", TASK_DELETED: "任务删除", GOAL_ARCHIVED: "目标归档", DAILY_PLAN_DELETED: "日计划删除"… 的结果。 */ reasons = {
  /* RESCHEDULED字段使用固定文字 '改期'。 */
  RESCHEDULED: "改期",
  /* 已返回TO连接池（RETURNED_TO_POOL）字段使用固定文字 '退回任务池'。 */
  RETURNED_TO_POOL: "退回任务池",
  /* 任务删除标记（TASK_DELETED）字段使用固定文字 '任务删除'。 */
  TASK_DELETED: "任务删除",
  /* 目标已归档（GOAL_ARCHIVED）字段使用固定文字 '目标归档'。 */
  GOAL_ARCHIVED: "目标归档",
  /* 每日方案删除标记（DAILY_PLAN_DELETED）字段使用固定文字 '日计划删除'。 */
  DAILY_PLAN_DELETED: "日计划删除",
  /* 每周方案删除标记（WEEKLY_PLAN_DELETED）字段使用固定文字 '周计划删除'。 */
  WEEKLY_PLAN_DELETED: "周计划删除",
};
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* d保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ d = await query(`/tasks/${props.taskId}/assignment-history`, {
      /* 当前页（page）字段使用表达式 page.value 的结果。 */
      page: page.value,
      /* 每页条数（size）字段使用固定数值 20。 */
      size: 20,
    });
    /* 把表达式 d.items 的结果写入 rows.value。 */
    rows.value = d.items;
    /* 把表达式 d.total 的结果写入 total.value。 */
    total.value = d.total;
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示Element Plus 的 drawer 组件；model-value 取自 true；close 事件触发时执行 emit('close')；title 固定为 任务安排历史；size 固定为 min(800px,100%)。 -->
  <el-dialog
    :model-value="true"
    @close="emit('close')"
    title="任务安排历史"
    width="min(800px,94vw)"
    align-center append-to-body class="plan-dialog"
    ><!-- 显示一段文字；用样式类 hint 控制外观。 --><p class="hint">
      改期保留原记录，任务本身不会复制。周／日编号对应当时的历史计划。
    </p>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false" /><!-- 显示Element Plus 的 table 组件；data 取自 rows；loading 指令使用 busy。 --><el-table :data="rows" v-loading="busy"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无记录，可调整筛选条件或从第一条记录开始。'。 --><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 weeklyPlanId；label 固定为 周计划。 --><el-table-column prop="weeklyPlanId" label="周计划" /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 dailyPlanId；label 固定为 日计划。 --><el-table-column
        prop="dailyPlanId"
        label="日计划"
      /><!-- 显示Element Plus 的 table-column 组件；label 固定为 安排时间；min-width 固定为 180。 --><el-table-column label="安排时间" min-width="180"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 dateText(row.assignedAt) 的当前值转成文字显示。 -->{{
          dateText(row.assignedAt)
        }}</template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 结束时间；min-width 固定为 180。 --><el-table-column label="结束时间" min-width="180"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 dateText(row.endedAt) 的当前值转成文字显示。 -->{{
          dateText(row.endedAt)
        }}</template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 原因；min-width 固定为 130。 --><el-table-column label="原因" min-width="130"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 tag 组件；type 取自 row.endedAt ? 'info' : 'success'。 --><el-tag :type="row.endedAt ? 'info' : 'success'"><!-- 把 row.endedAt ? reasons[row.endReason] || row.endReason : "当前安排" 的当前值转成文字显示。 -->{{
            row.endedAt ? reasons[row.endReason] || row.endReason : "当前安排"
          }}</el-tag></template
        ></el-table-column
      ></el-table
    ><!-- 显示Element Plus 的 pagination 组件；用样式类 pagination 控制外观；与 page 双向同步（current-page）；page-size 取自 20；total 取自 total；layout 固定为 total,prev,pager,next；current-change 事件触发时执行 load。 --><el-pagination
      class="pagination"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="load"
  /></el-dialog>
</template>
