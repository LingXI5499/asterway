<script setup>
// 业务操作协调器：复用表单与确认框，成功后仅通知当前页面刷新数据。
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api/http";
import {
  getResource,
  requireActive,
  resources,
  detailPath,
  states,
  canGrow,
  permission,
} from "../../api/growth";
import { confirmAction, report } from "../../api/ui";
import { signalChange } from "../../api/personal";
import GrowthEditor from "./GrowthEditor.vue";
import TaskAssignmentDialog from "./TaskAssignmentDialog.vue";
import AssignmentHistory from "./AssignmentHistory.vue";
import OrderDialog from "./OrderDialog.vue";
const emit = defineEmits(["changed"]);
const router = useRouter(),
  editor = ref(null),
  assignment = ref(null),
  history = ref(null),
  order = ref(null),
  modal = ref(null),
  value = ref(""),
  busy = ref(false),
  error = ref("");
const allowedStates = computed(() =>
  Object.fromEntries(
    Object.entries(states).filter(
      ([code]) => modal.value?.kind === "task" || code !== "PAUSED",
    ),
  ),
);
const idOf = (row) => row.taskId || row.id;
function changed() {
  signalChange("growth");
  editor.value = null;
  assignment.value = null;
  order.value = null;
  modal.value = null;
  ElMessage.success("操作成功");
  emit("changed");
}
async function edit(kind, row, parentId) {
  try {
    if (kind === "task" && row?.taskId)
      row = await getResource("task", row.taskId);
    if (row) await requireActive(kind, row);
    else if (parentId)
      await requireActive(
        kind === "stage" ? "goal" : "stage",
        await getResource(kind === "stage" ? "goal" : "stage", parentId),
      );
    editor.value = { kind, id: row?.id, parentId };
  } catch (e) {
    report(e);
  }
}
async function status(kind, row) {
  try {
    const record =
      kind === "task" && row.taskId
        ? await getResource("task", row.taskId)
        : row;
    await requireActive(kind, record);
    modal.value = { type: "status", kind, row: record };
    value.value = record.status;
    error.value = "";
  } catch (e) {
    report(e);
  }
}
function archive(row) {
  modal.value = { type: "archive", kind: "goal", row };
  value.value = false;
  error.value = "";
}
async function submit() {
  busy.value = true;
  try {
    const m = modal.value;
    await api(
      `/${resources[m.kind]}/${m.row.id}/${m.type === "status" ? "status" : "archive"}`,
      {
        method: m.type === "status" ? "PATCH" : "POST",
        body:
          m.type === "status"
            ? { status: value.value, version: m.row.version }
            : { version: m.row.version, confirmed: value.value === true },
      },
    );
    changed();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function remove(kind, row) {
  if (busy.value) return;
  // 目标只归档；遗留调用也必须进入明确确认流程，不再请求删除接口。
  if (kind === "goal") return archive(row);
  try {
    if (kind === "task" && row?.taskId)
      row = await getResource("task", row.taskId);
    await confirmAction(
      kind === "task"
        ? "删除任务会结束尚未结束的执行和当前安排，并保留历史，是否继续？"
        : "确认删除阶段？有效子项必须先处理。",
    );
    busy.value = true;
    await api(`/${resources[kind]}/${row.id}?version=${row.version}`, {
      method: "DELETE",
    });
    changed();
  } catch (e) {
    report(e);
  } finally {
    busy.value = false;
  }
}
async function restore(row) {
  try {
    await confirmAction("恢复目标不会自动恢复执行或此前结束的安排，是否继续？");
    await api(`/goals/${row.id}/restore`, {
      method: "POST",
      body: { version: row.version },
    });
    changed();
  } catch (e) {
    report(e);
  }
}
async function assign(row, preset = {}) {
  try {
    const task = row.taskId ? await getResource("task", row.taskId) : row;
    await requireActive("task", task);
    if (task.status === "COMPLETED") throw new Error("已完成任务不能重新安排");
    assignment.value = { taskId: idOf(row), ...preset };
  } catch (e) {
    report(e);
  }
}
async function unassign(row) {
  try {
    const taskId = idOf(row);
    const a = await api(`/tasks/${taskId}/assignment`);
    if (!a) throw new Error("该任务没有当前安排");
    await confirmAction("结束当前安排并退回任务池？安排历史会保留。");
    await api(
      `/tasks/${taskId}/assignment?` +
        new URLSearchParams({ assignmentId: a.id, version: a.version }),
      { method: "DELETE" },
    );
    changed();
  } catch (e) {
    report(e);
  }
}
function details(kind, row) {
  router.push(detailPath(kind, kind === "task" ? idOf(row) : row.id));
}
defineExpose({
  edit,
  status,
  archive,
  remove,
  restore,
  assign,
  unassign,
  details,
  history: (row) => (history.value = { taskId: idOf(row) }),
  sort: (kind, parentId) => (order.value = { kind, parentId }),
});
</script>
<template>
  <GrowthEditor
    v-if="editor"
    v-bind="editor"
    @close="editor = null"
    @saved="changed"
  /><TaskAssignmentDialog
    v-if="assignment"
    v-bind="assignment"
    @close="assignment = null"
    @saved="changed"
  /><AssignmentHistory
    v-if="history"
    v-bind="history"
    @close="history = null"
  /><OrderDialog
    v-if="order"
    v-bind="order"
    @close="order = null"
    @saved="changed"
  /><el-dialog
    v-if="modal"
    :model-value="true"
    @close="modal = null"
    :title="modal.type === 'status' ? '修改状态' : '归档目标'"
    width="min(480px,94vw)"
    :close-on-click-modal="false"
    ><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-select v-if="modal.type === 'status'" v-model="value"
      ><el-option
        v-for="(label, code) in allowedStates"
        :key="code"
        :value="code"
        :label="label" /></el-select
    ><template v-else
      ><p>归档将结束目标下全部尚未结束的执行，并移出全部有效任务安排。</p>
      <p>任务与执行历史会保留，恢复目标后需要重新安排，不会自动恢复计时。</p>
      <el-checkbox v-model="value">我已了解并确认归档</el-checkbox></template
    ><template #footer
      ><el-button @click="modal = null">取消</el-button
      ><el-button
        type="primary"
        :loading="busy"
        :disabled="!value"
        @click="submit"
        >确认</el-button
      ></template
    ></el-dialog
  >
</template>
