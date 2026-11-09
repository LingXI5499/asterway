<!-- 渲染成长业务Actions界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 业务操作协调器：复用表单与确认框，成功后仅通知当前页面刷新数据。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, computed } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../../api/http";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import {
  getResource,
  requireActive,
  resources,
  detailPath,
  states,
  canGrow,
  permission,
} from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { signalChange } from "../../api/personal";
/* 引入 ./GrowthEditor.vue 提供的组件、状态函数或请求方法。 */
import GrowthEditor from "./GrowthEditor.vue";
import TaskDetailsDialog from "./TaskDetailsDialog.vue";
/* 引入 ./TaskAssignmentDialog.vue 提供的组件、状态函数或请求方法。 */
import TaskAssignmentDialog from "./TaskAssignmentDialog.vue";
/* 引入 ./AssignmentHistory.vue 提供的组件、状态函数或请求方法。 */
import AssignmentHistory from "./AssignmentHistory.vue";
/* 引入 ./OrderDialog.vue 提供的组件、状态函数或请求方法。 */
import OrderDialog from "./OrderDialog.vue";
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["changed", "detail-open", "detail-close"]);
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* editor保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  editor = ref(null),
  /* 任务安排（assignment）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  assignment = ref(null),
  /* 历史（history）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  history = ref(null),
  /* 顺序（order）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  order = ref(null),
  /* 弹出层（modal）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  modal = ref(null),
  /* 值（value）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  value = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
const /* allowedStates保存随依赖自动重算的筛选结果或数量。 */ allowedStates = computed(() =>
  Object.fromEntries(
    Object.entries(states).filter(
      ([code]) => modal.value?.kind === "task" || code !== "PAUSED",
    ),
  ),
);
const /* 编号Of（idOf）保存优先使用前面的有效值，没有时采用后面的备用值：(row) => row.taskId || row.id。 */ idOf = (row) => row.taskId || row.id;
/* 已修改（changed）：调用 changed 完成已修改处理。 */
// 详情保持打开，其他操作保存成功后刷新其内容。
const detail = ref(null), detailPanel = ref(null);
function created() { signalChange("growth"); emit("changed"); }
function closeDetails() { if (detail.value) { detail.value = null; emit("detail-close"); } }
function changed() {
  /* 调用 signalChange 完成取消信号修改处理。 */
  signalChange("growth");
  /* 把空值，表示当前还没有对应内容写入 editor.value。 */
  editor.value = null;
  /* 把空值，表示当前还没有对应内容写入 assignment.value。 */
  assignment.value = null;
  /* 把空值，表示当前还没有对应内容写入 order.value。 */
  order.value = null;
  /* 把空值，表示当前还没有对应内容写入 modal.value。 */
  modal.value = null;
  /* 调用 ElMessage.success 完成成功处理。 */
  ElMessage.success("操作成功");
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("changed");
  detailPanel.value?.load();
}
/* edit：调用 edit 完成edit处理。 */
async function edit(kind, row, parentId, options = {}) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 kind === "task" && row?.taskId 是否成立，成立时走这一分支。 */
    if (kind === "task" && row?.taskId)
      /* 把等待 读取 Resource 的值 完成后取得结果写入 row。 */
      row = await getResource("task", row.taskId);
    /* 检查条件 row 是否成立，成立时走这一分支。 */
    if (row) /* 等待要求满足 页面是否仍处于活动状态（Active）。 */ await requireActive(kind, row);
    else /* 检查条件 parentId 是否成立，成立时走这一分支。 */ if (parentId)
      /* 等待要求满足 页面是否仍处于活动状态（Active）。 */
      await requireActive(
        kind === "stage" ? "goal" : "stage",
        await getResource(kind === "stage" ? "goal" : "stage", parentId),
      );
    /* 把沿 { kind, id: row?.id, parentId } 读取数据；中间对象为空时返回空值写入 editor.value。 */
    editor.value = { /* 直接使用已有的种类（kind）作为同名字段。 */ kind, /* 编号（id）字段使用沿 row?.id 读取数据；中间对象为空时返回空值。 */ id: row?.id, /* 直接使用已有的上级编号（parentId）作为同名字段。 */ parentId, planDate: options.planDate };
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 状态（status）：调用 status 完成状态处理。 */
async function status(kind, row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 记录（record）保存根据前面的条件，从两个候选结果中选择一个：kind === "task" && row.taskId ? await getResource("task", row.taskId) : row。 */ record =
      kind === "task" && row.taskId
        ? await getResource("task", row.taskId)
        : row;
    /* 等待要求满足 页面是否仍处于活动状态（Active）。 */
    await requireActive(kind, record);
    /* 把表达式 { type: "status", kind, row: record } 的结果写入 modal.value。 */
    modal.value = { /* 类型（type）字段使用固定文字 'status'。 */ type: "status", /* 直接使用已有的种类（kind）作为同名字段。 */ kind, /* 当前记录（row）字段使用记录（record）。 */ row: record };
    /* 把表达式 record.status 的结果写入 value.value。 */
    value.value = record.status;
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 归档（archive）：调用 archive 完成归档处理。 */
function archive(row) {
  /* 把表达式 { type: "archive", kind: "goal", row } 的结果写入 modal.value。 */
  modal.value = { /* 类型（type）字段使用固定文字 'archive'。 */ type: "archive", /* 种类（kind）字段使用固定文字 'goal'。 */ kind: "goal", /* 直接使用已有的当前记录（row）作为同名字段。 */ row };
  /* 把假，表示条件不成立或开关关闭写入 value.value。 */
  value.value = false;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
}
/* 提交（submit）：调用 submit 完成提交处理。 */
async function submit() {
  if (busy.value) return;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* m保存表达式 modal.value 的结果。 */ m = modal.value;
    /* 等待调用 api 完成接口处理。 */
    await api(
      `/${resources[m.kind]}/${m.row.id}/${m.type === "status" ? "status" : "archive"}`,
      {
        /* 方法（method）字段使用根据前面的条件，从两个候选结果中选择一个：m.type === "status" ? "PATCH" : "POST"。 */
        method: m.type === "status" ? "PATCH" : "POST",
        /* 请求正文（body）字段使用根据前面的条件，从两个候选结果中选择一个：m.type === "status" ? { status: value.value, version: m.row.version } : { version: m.row.version, co…。 */
        body:
          m.type === "status"
            ? { /* 状态（status）字段使用表达式 value.value 的结果。 */ status: value.value, /* 版本号（version）字段使用表达式 m.row.version 的结果。 */ version: m.row.version }
            : { /* 版本号（version）字段使用表达式 m.row.version 的结果。 */ version: m.row.version, /* 已确认（confirmed）字段使用条件判断的结果：value.value === true。 */ confirmed: value.value === true },
      },
    );
    /* 调用 changed 完成已修改处理。 */
    changed();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 移除（remove）：调用 remove 完成移除处理。 */
async function remove(kind, row) {
  /* 检查条件 busy.value 是否成立，成立时走这一分支。 */
  if (busy.value) /* 返回空内容，当前函数到此结束。 */ return;
  // 目标只归档；遗留调用也必须进入明确确认流程，不再请求删除接口。
  /* 检查条件 kind === "goal" 是否成立，成立时走这一分支。 */
  if (kind === "goal") /* 返回调用 archive 完成归档处理，当前函数到此结束。 */ return archive(row);
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 kind === "task" && row?.taskId 是否成立，成立时走这一分支。 */
    if (kind === "task" && row?.taskId)
      /* 把等待 读取 Resource 的值 完成后取得结果写入 row。 */
      row = await getResource("task", row.taskId);
    /* 等待确认 操作（Action）。 */
    await confirmAction(
      kind === "task"
        ? "删除任务会结束尚未结束的执行和当前安排，并保留历史，是否继续？"
        : "确认删除阶段？有效子项必须先处理。",
    );
    /* 把真，表示条件成立或开关打开写入 busy.value。 */
    busy.value = true;
    /* 等待调用 api 完成接口处理。 */
    await api(`/${resources[kind]}/${row.id}?version=${row.version}`, {
      /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */
      method: "DELETE",
    });
    /* 调用 changed 完成已修改处理。 */
    if (kind === "task" && String(detail.value?.id) === String(row.id)) closeDetails();
    changed();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 恢复（restore）：调用 restore 完成恢复处理。 */
async function restore(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction("恢复目标不会自动恢复执行或此前结束的安排，是否继续？");
    /* 等待调用 api 完成接口处理。 */
    await api(`/goals/${row.id}/restore`, {
      /* 方法（method）字段使用固定状态或编码 POST（POST）。 */
      method: "POST",
      /* 请求正文（body）字段使用表达式 { version: row.version } 的结果。 */
      body: { /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version },
    });
    /* 调用 changed 完成已修改处理。 */
    changed();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 安排（assign）：调用 assign 完成安排处理。 */
async function assign(row, preset = {}) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 任务（task）保存根据前面的条件，从两个候选结果中选择一个：row.taskId ? await getResource("task", row.taskId) : row。 */ task = row.taskId ? await getResource("task", row.taskId) : row;
    /* 等待要求满足 页面是否仍处于活动状态（Active）。 */
    await requireActive("task", task);
    /* 检查条件 task.status === "COMPLETED" 是否成立，成立时走这一分支。 */
    if (task.status === "COMPLETED") /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("已完成任务不能重新安排");
    /* 把表达式 { taskId: idOf(row), ...preset } 的结果写入 assignment.value。 */
    assignment.value = { /* 任务编号（taskId）字段使用调用 idOf 完成编号Of处理。 */ taskId: idOf(row), ...preset };
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* unassign：调用 unassign 完成unassign处理。 */
async function unassign(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 任务编号（taskId）保存调用 idOf 完成编号Of处理。 */ taskId = idOf(row);
    const /* a保存等待 调用 api 完成接口处理 完成后取得结果。 */ a = await api(`/tasks/${taskId}/assignment`);
    /* 检查条件 !a 是否成立，成立时走这一分支。 */
    if (!a) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("该任务没有当前安排");
    /* 等待确认 操作（Action）。 */
    await confirmAction("结束当前安排并退回任务池？安排历史会保留。");
    /* 等待调用 api 完成接口处理。 */
    await api(
      `/tasks/${taskId}/assignment?` +
        new URLSearchParams({ /* 任务安排编号（assignmentId）字段使用表达式 a.id 的结果。 */ assignmentId: a.id, /* 版本号（version）字段使用表达式 a.version 的结果。 */ version: a.version }),
      { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" },
    );
    /* 调用 changed 完成已修改处理。 */
    changed();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 详情（details）：调用 details 完成详情处理。 */
function details(kind, row) {
  if (kind === "task") {
    detail.value = { id: idOf(row) };
    emit("detail-open", String(idOf(row)));
  } else router.push(detailPath(kind, row.id));
}
/* 声明父组件通过引用可以调用的功能。 */
defineExpose({
  /* 直接使用已有的edit作为同名字段。 */
  edit,
  /* 直接使用已有的状态（status）作为同名字段。 */
  status,
  /* 直接使用已有的归档（archive）作为同名字段。 */
  archive,
  /* 直接使用已有的移除（remove）作为同名字段。 */
  remove,
  /* 直接使用已有的恢复（restore）作为同名字段。 */
  restore,
  /* 直接使用已有的安排（assign）作为同名字段。 */
  assign,
  /* 直接使用已有的unassign作为同名字段。 */
  unassign,
  /* 直接使用已有的详情（details）作为同名字段。 */
  details,
  closeDetails,
  /* 历史（history）字段使用表达式 (row) => (history.value = { taskId: idOf(row) }) 的结果。 */
  history: (row) => (history.value = { /* 任务编号（taskId）字段使用调用 idOf 完成编号Of处理。 */ taskId: idOf(row) }),
  /* 排序（sort）字段使用表达式 (kind, parentId) => (order.value = { kind, parentId }) 的结果。 */
  sort: (kind, parentId) => (order.value = { /* 直接使用已有的种类（kind）作为同名字段。 */ kind, /* 直接使用已有的上级编号（parentId）作为同名字段。 */ parentId }),
});
</script>
<template>
  <TaskDetailsDialog v-if="detail" ref="detailPanel" :id="detail.id" @close="closeDetails"
    @edit="r => edit('task', r)" @status="r => status('task', r)" @remove="r => remove('task', r)"
    @assign="r => assign(r)" @unassign="unassign" @history="r => history = { taskId: idOf(r) }" @changed="created" />
  <!-- 显示成长业务Editor（GrowthEditor）组件；仅当 editor 成立时渲染；属性集合 取自 editor；close 事件触发时执行 editor = null；saved 事件触发时执行 changed。 -->
  <GrowthEditor
    v-if="editor"
    v-bind="editor"
    @close="editor = null"
    @saved="changed"
    @created="created"
  /><!-- 显示任务任务安排对话框（TaskAssignmentDialog）组件；仅当 assignment 成立时渲染；属性集合 取自 assignment；close 事件触发时执行 assignment = null；saved 事件触发时执行 changed。 --><TaskAssignmentDialog
    v-if="assignment"
    v-bind="assignment"
    @close="assignment = null"
    @saved="changed"
  /><!-- 显示任务安排历史（AssignmentHistory）组件；仅当 history 成立时渲染；属性集合 取自 history；close 事件触发时执行 history = null。 --><AssignmentHistory
    v-if="history"
    v-bind="history"
    @close="history = null"
  /><!-- 显示顺序对话框（OrderDialog）组件；仅当 order 成立时渲染；属性集合 取自 order；close 事件触发时执行 order = null；saved 事件触发时执行 changed。 --><OrderDialog
    v-if="order"
    v-bind="order"
    @close="order = null"
    @saved="changed"
  /><!-- 显示Element Plus 的 dialog 组件；仅当 modal 成立时渲染；model-value 取自 true；close 事件触发时执行 modal = null；title 取自 modal.type === 'status' ? '修改状态' : '归档目标'；width 固定为 min(480px,94vw)；close-on-click-modal 取自 false。 --><el-dialog
    v-if="modal"
    :model-value="true"
    @close="modal = null"
    :title="modal.type === 'status' ? '修改状态' : '归档目标'"
    width="min(480px,94vw)"
    align-center append-to-body
    :show-close="!busy" :close-on-press-escape="!busy"
    :close-on-click-modal="false"
    ><!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 --><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 select 组件；仅当 modal.type === 'status' 成立时渲染；与 value 双向同步。 --><el-select v-if="modal.type === 'status'" v-model="value"
      ><!-- 显示Element Plus 的 option 组件；按 (label, code) in allowedStates 为每条数据重复生成内容；key 取自 code；value 取自 code；label 取自 label。 --><el-option
        v-for="(label, code) in allowedStates"
        :key="code"
        :value="code"
        :label="label" /></el-select
    ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 --><template v-else
      ><!-- 显示一段文字。 --><p>归档将结束目标下全部尚未结束的执行，并移出全部有效任务安排。</p>
      <!-- 显示一段文字。 -->
      <p>任务与执行历史会保留，恢复目标后需要重新安排，不会自动恢复计时。</p>
      <!-- 显示Element Plus 的 checkbox 组件；与 value 双向同步。 -->
      <el-checkbox v-model="value">我已了解并确认归档</el-checkbox></template
    ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 --><template #footer
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 modal = null。 --><el-button :disabled="busy" @click="modal = null">取消</el-button
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 busy；disabled 取自 !value；click 事件触发时执行 submit。 --><el-button
        type="primary"
        :loading="busy"
        :disabled="!value"
        @click="submit"
        >确认</el-button
      ></template
    ></el-dialog
  >
</template>
