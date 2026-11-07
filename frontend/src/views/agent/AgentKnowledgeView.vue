<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, timeText } from "../../api/agentWorkspace";
const rows = ref([]), total = ref(0), busy = ref(false), uploading = ref(false), error = ref("");
const input = ref(null), tasks = ref([]), taskDialog = ref(false);
const filters = reactive({ keyword: "", status: "", page: 1, size: 20 });
const labels = { UPLOADED: "等待解析", PARSING: "解析中", READY: "正文就绪", FAILED: "处理失败" };
let timer, disposed = false;
async function load(reset = false) {
  if (reset) filters.page = 1;
  busy.value = true; error.value = "";
  try { const data = await workspaceApi.get("knowledge", filters); if (!disposed) { rows.value = data.items; total.value = data.total; } }
  catch (e) { if (!disposed) error.value = e.message; } finally { busy.value = false; }
}
async function upload(event) {
  const file = event.target.files?.[0]; event.target.value = "";
  if (!file) return;
  if (file.size > 25 * 1024 * 1024 || !/\.(txt|md|pdf|docx)$/i.test(file.name)) return ElMessage.warning("请选择 25 MB 以内的 TXT、Markdown、PDF 或 DOCX 文件");
  uploading.value = true;
  try { const data = await workspaceApi.upload(file); ElMessage.success(data.created ? "文档已上传，正在后台处理" : "此文档已经存在"); await load(true); }
  catch (e) { ElMessage.error(e.message); } finally { uploading.value = false; }
}
async function remove(row) {
  try { await ElMessageBox.confirm(`删除“${row.title}”后将停止检索，历史引用仍保留来源标识。`, "删除知识文档", { type: "warning" }); await workspaceApi.remove(`knowledge/${row.id}`, row.version); ElMessage.success("文档已删除"); await load(); }
  catch (e) { if (e?.message) ElMessage.error(e.message); }
}
async function showTasks() {
  try { tasks.value = (await workspaceApi.get("knowledge/tasks", { size: 100 })).items; taskDialog.value = true; }
  catch (e) { ElMessage.error(e.message); }
}
onMounted(async () => { await load(); timer = setInterval(() => { if (!document.hidden && !busy.value && rows.value.some(row => ['UPLOADED', 'PARSING'].includes(row.status))) load(); }, 4000); });
onBeforeUnmount(() => { disposed = true; clearInterval(timer); });
</script>

<template>
  <section>
    <div class="workspace-heading"><div><h2>知识资料</h2><p>上传自己的资料，回答时可查看所引用的具体版本和原文。</p></div><div><el-button @click="showTasks">处理记录</el-button><el-button v-if="canGrow('agent:knowledge:manage')" type="primary" :loading="uploading" @click="input?.click()">上传文档</el-button></div></div>
    <input ref="input" type="file" hidden accept=".txt,.md,.pdf,.docx" @change="upload" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card shadow="never">
      <el-form inline @submit.prevent="load(true)"><el-form-item><el-input v-model="filters.keyword" clearable placeholder="搜索文档标题" /></el-form-item><el-form-item><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 144px"><el-option v-for="(name, key) in labels" :key="key" :label="name" :value="key" /></el-select></el-form-item><el-form-item><el-button native-type="submit" :loading="busy">查询</el-button><el-button @click="Object.assign(filters, { keyword: '', status: '' }); load(true)">重置</el-button></el-form-item></el-form>
      <el-skeleton v-if="busy && !rows.length" :rows="5" animated />
      <el-table v-else :data="rows" empty-text="还没有资料，上传第一篇文档即可开始检索">
        <el-table-column label="文档" min-width="260"><template #default="{ row }"><RouterLink :to="`/agent/knowledge/${row.id}`" class="document-title">{{ row.title }}</RouterLink><div class="muted">{{ row.fileName }} · {{ (row.sizeBytes / 1024).toFixed(1) }} KB</div></template></el-table-column>
        <el-table-column label="解析状态" width="128"><template #default="{ row }"><el-tag :type="row.status === 'FAILED' ? 'danger' : row.status === 'READY' ? 'success' : 'info'">{{ labels[row.status] || row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="chunkCount" label="段落" width="80" />
        <el-table-column label="更新时间" width="184"><template #default="{ row }">{{ timeText(row.updatedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="144"><template #default="{ row }"><el-button link type="primary" @click="$router.push(`/agent/knowledge/${row.id}`)">查看</el-button><el-button v-if="canGrow('agent:knowledge:manage')" link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="filters.page" :page-size="filters.size" :total="total" layout="total, prev, pager, next" @current-change="load()" />
    </el-card>
    <el-dialog v-model="taskDialog" title="后台处理记录" width="min(960px, 94vw)"><el-table :data="tasks" empty-text="暂无处理记录"><el-table-column prop="kind" label="类型" min-width="180" /><el-table-column prop="status" label="状态" width="112" /><el-table-column prop="attempts" label="尝试次数" width="96" /><el-table-column prop="errorMessage" label="处理说明" min-width="220" /><el-table-column label="开始时间" width="184"><template #default="{ row }">{{ timeText(row.startedAt) }}</template></el-table-column></el-table></el-dialog>
  </section>
</template>

<style scoped>
.workspace-heading { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:24px; } h2 { margin:0 0 8px; font-size:24px; line-height:32px; } p { margin:0; color:var(--el-text-color-secondary); } .document-title { color:var(--el-color-primary); text-decoration:none; } .muted { margin-top:4px; color:var(--el-text-color-secondary); font-size:12px; line-height:16px; } .el-pagination { margin-top:24px; justify-content:flex-end; } .el-alert { margin-bottom:16px; } @media(max-width:640px) { .workspace-heading { align-items:flex-start; flex-direction:column; } }
</style>
