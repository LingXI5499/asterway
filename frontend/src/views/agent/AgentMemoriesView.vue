<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, timeText } from "../../api/agentWorkspace";

const filters = reactive({ status: "", kind: "", keyword: "", page: 1, size: 20 });
const rows = ref([]), total = ref(0), busy = ref(false), error = ref("");
const dialog = ref(false), saving = ref(false), editing = ref(null);
const form = reactive({ kind: "PREFERENCE", content: "" });
const kinds = { PREFERENCE: "偏好", FACT: "事实", CONSTRAINT: "约束", GOAL_CONTEXT: "目标背景" };
const statuses = { CANDIDATE: "待确认", CONFIRMED: "已确认", REJECTED: "已拒绝", ARCHIVED: "已归档" };
async function load(reset = false) {
  if (reset) filters.page = 1;
  busy.value = true; error.value = "";
  try { const data = await workspaceApi.get("memories", filters); rows.value = data.items; total.value = data.total; }
  catch (e) { error.value = e.message; } finally { busy.value = false; }
}
function edit(row = null) { editing.value = row; form.kind = row?.kind || "PREFERENCE"; form.content = row?.content || ""; dialog.value = true; }
async function save() {
  if (!form.content.trim()) return ElMessage.warning("请填写要保留的内容");
  saving.value = true;
  try {
    if (editing.value) await workspaceApi.patch(`memories/${editing.value.id}`, { content: form.content.trim(), version: editing.value.version });
    else await workspaceApi.post("memories/candidates", { kind: form.kind, content: form.content.trim(), source: "USER_STATED", confidence: 1 });
    dialog.value = false; ElMessage.success("已保存，待确认的内容需要确认后才会使用"); await load();
  } catch (e) { ElMessage.error(e.message); } finally { saving.value = false; }
}
async function action(row, kind) {
  try {
    if (kind === "delete") { await ElMessageBox.confirm("删除后，此内容不再参与之后的对话。", "删除记忆", { type: "warning" }); await workspaceApi.remove(`memories/${row.id}`, row.version); }
    else await workspaceApi.post(`memories/${row.id}/${kind}`, { version: row.version });
    ElMessage.success("记忆状态已更新"); await load();
  } catch (e) { if (e?.message) ElMessage.error(e.message); }
}
onMounted(load);
</script>

<template>
  <section class="assistant-workspace">
    <div class="assistant-heading"><div><h2>长期记忆</h2><p>确认适合长期保留的偏好和约束，随时修改、归档或删除。</p></div><el-button v-if="canGrow('agent:memory:manage')" type="primary" @click="edit()">新增记忆</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <el-card shadow="never">
      <el-form inline @submit.prevent="load(true)">
        <el-form-item><el-input v-model="filters.keyword" placeholder="搜索记忆内容" clearable /></el-form-item>
        <el-form-item><el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 144px"><el-option v-for="(name, key) in statuses" :key="key" :label="name" :value="key" /></el-select></el-form-item>
        <el-form-item><el-select v-model="filters.kind" placeholder="全部类型" clearable style="width: 144px"><el-option v-for="(name, key) in kinds" :key="key" :label="name" :value="key" /></el-select></el-form-item>
        <el-form-item><el-button native-type="submit" :loading="busy">查询</el-button><el-button @click="Object.assign(filters, { status: '', kind: '', keyword: '' }); load(true)">重置</el-button></el-form-item>
      </el-form>
      <el-skeleton v-if="busy && !rows.length" :rows="5" animated />
      <el-table v-else :data="rows" :empty-text="error ? '加载失败，可重新查询' : '暂无记忆，可以新增或在对话后确认候选'">
        <el-table-column label="内容" min-width="300"><template #default="{ row }"><div class="memory-content">{{ row.content }}</div><small>{{ kinds[row.kind] }} · {{ row.source === 'INFERRED' ? '对话提取，等待你确认' : '由你提供' }}</small></template></el-table-column>
        <el-table-column label="状态" width="112"><template #default="{ row }"><el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'info'">{{ statuses[row.status] }}</el-tag></template></el-table-column>
        <el-table-column label="更新时间" width="184"><template #default="{ row }">{{ timeText(row.updatedAt) }}</template></el-table-column>
        <el-table-column v-if="canGrow('agent:memory:manage')" label="操作" min-width="240"><template #default="{ row }">
          <el-button v-if="row.status === 'CANDIDATE'" link type="primary" @click="action(row, 'confirm')">确认</el-button>
          <el-button v-if="row.status === 'CANDIDATE'" link @click="action(row, 'reject')">拒绝</el-button>
          <el-button v-if="['CANDIDATE', 'CONFIRMED'].includes(row.status)" link @click="edit(row)">编辑</el-button>
          <el-button v-if="row.status === 'CONFIRMED'" link @click="action(row, 'archive')">归档</el-button>
          <el-button link type="danger" @click="action(row, 'delete')">删除</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="filters.page" :page-size="filters.size" :total="total" layout="total, prev, pager, next" @current-change="load()" />
    </el-card>
    <el-dialog v-model="dialog" :title="editing ? '编辑记忆' : '新增记忆候选'" width="min(560px, 94vw)">
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="记忆类型"><el-select v-model="form.kind" :disabled="!!editing"><el-option v-for="(name, key) in kinds" :key="key" :label="name" :value="key" /></el-select></el-form-item>
        <el-form-item label="记忆内容" required><el-input v-model="form.content" type="textarea" :rows="5" :maxlength="4000" show-word-limit placeholder="例如：我习惯在晚上安排阅读任务" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.assistant-heading { display:flex; gap:16px; justify-content:space-between; align-items:center; margin-bottom:24px; }
h2 { margin:0 0 8px; font-size:24px; line-height:32px; } p, small { color:var(--el-text-color-secondary); } p { margin:0; } .memory-content { white-space:pre-wrap; line-height:24px; margin-bottom:4px; }
.el-alert { margin-bottom:16px; } .el-pagination { margin-top:24px; justify-content:flex-end; }
@media(max-width:640px) { .assistant-heading { align-items:flex-start; flex-direction:column; } }
</style>
