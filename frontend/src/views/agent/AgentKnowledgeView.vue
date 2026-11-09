<!-- 左侧选择个人知识库，右侧只读取当前库的文档；导入队列独立保留目标库。 -->
<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, timeText } from "../../api/agentWorkspace";
import { useKnowledgeImport, documentState, importLabels } from "../../composables/useKnowledgeImport";
import KnowledgeFormatHelp from "../../components/agent/KnowledgeFormatHelp.vue";
import "../../styles/knowledge.css";

const route = useRoute(), router = useRouter();
const bases = ref([]), baseId = ref(""), baseKeyword = ref(""), loadingBases = ref(false);
const rows = ref([]), total = ref(0), busy = ref(false), error = ref("");
const capabilities = ref(null), capabilityError = ref(""), input = ref(null), selection = ref([]);
const filters = reactive({ keyword: "", status: "", page: 1, size: 20 });
const current = computed(() => bases.value.find(b => b.id === baseId.value));
const visibleBases = computed(() => bases.value.filter(b => b.name.toLowerCase().includes(baseKeyword.value.trim().toLowerCase())));
const manageable = computed(() => canGrow("agent:knowledge:manage"));
const baseDialog = ref(false), editingBase = ref(null), baseForm = reactive({ name: "", description: "" });
const baseSaving = ref(false), baseError = ref("");
const moveDialog = ref(false), moving = ref([]), moveTarget = ref(""), moveSaving = ref(false), moveError = ref("");
const taskDialog = ref(false), tasks = ref([]), taskBusy = ref(false), taskPage = ref(1), taskTotal = ref(0), taskError = ref("");
let sequence = 0, disposed = false, controller, timer, refreshTimer;
function scheduleRefresh() { clearTimeout(refreshTimer); refreshTimer = setTimeout(() => { load(); loadBases(); }, 200); }
const imports = useKnowledgeImport(capabilities, scheduleRefresh);
const { items: importItems, open: importOpen } = imports;
const importCount = computed(() => importItems.value.length);
const processingKinds = { DOCUMENT_PARSE: "解析文档", DOCUMENT_EMBEDDING: "建立索引" };
const processingStatuses = { PENDING: "等待处理", RUNNING: "处理中", SUCCEEDED: "处理完成", FAILED: "失败", CANCELLED: "已取消" };

async function loadBases() {
  loadingBases.value = true;
  try {
    const data = await workspaceApi.get("knowledge/bases");
    if (disposed) return;
    bases.value = data.items;
    if (!bases.value.some(b => b.id === baseId.value)) {
      const requested = String(route.query.knowledgeBaseId || "");
      await selectBase(bases.value.some(b => b.id === requested) ? requested : bases.value[0]?.id || "");
    }
  } catch (e) { if (!disposed) error.value = e.message; }
  finally { loadingBases.value = false; }
}
async function load(reset = false) {
  if (reset) filters.page = 1;
  const seq = ++sequence;
  controller?.abort(); controller = new AbortController();
  if (!baseId.value) { rows.value = []; total.value = 0; return; }
  busy.value = true; error.value = "";
  try {
    const data = await workspaceApi.get("knowledge", { ...filters, knowledgeBaseId: baseId.value }, controller.signal);
    if (disposed || seq !== sequence) return;
    const last = Math.max(1, Math.ceil(data.total / filters.size));
    if (filters.page > last) { filters.page = last; return load(); }
    rows.value = data.items; total.value = data.total;
  } catch (e) { if (!disposed && seq === sequence && e.name !== "AbortError") error.value = e.message; }
  finally { if (seq === sequence) busy.value = false; }
}
async function selectBase(id) {
  baseId.value = id; selection.value = []; rows.value = []; total.value = 0; filters.page = 1;
  if (String(route.query.knowledgeBaseId || "") !== id) router.replace({ path: route.path, query: { ...route.query, knowledgeBaseId: id || undefined } });
  await load();
}
async function readCapabilities() {
  capabilityError.value = "";
  try { capabilities.value = await workspaceApi.get("knowledge/capabilities"); }
  catch (e) { capabilityError.value = e.message; }
}
function addFiles(files) {
  try { imports.enqueue(Array.from(files || []), current.value); }
  catch (e) { ElMessage.warning(e.message); }
}
function upload(event) { addFiles(event.target.files); event.target.value = ""; }
function drop(event) { if (manageable.value) addFiles(event.dataTransfer.files); }
function editBase(row = null) {
  editingBase.value = row; baseForm.name = row?.name || ""; baseForm.description = row?.description || "";
  baseError.value = ""; baseDialog.value = true;
}
async function saveBase() {
  if (baseSaving.value) return;
  baseSaving.value = true; baseError.value = "";
  try {
    const body = { name: baseForm.name, description: baseForm.description };
    const row = editingBase.value
      ? await workspaceApi.patch(`knowledge/bases/${editingBase.value.id}`, { ...body, version: editingBase.value.version })
      : await workspaceApi.post("knowledge/bases", body);
    baseDialog.value = false; await loadBases(); await selectBase(row.id); ElMessage.success("知识库已保存");
  } catch (e) { baseError.value = e.message; }
  finally { baseSaving.value = false; }
}
async function removeBase() {
  if (!current.value) return;
  const row = current.value;
  try {
    await ElMessageBox.confirm(`删除空知识库“${row.name}”？已有消息和引用会保留。`, "删除知识库", { type: "warning" });
    await workspaceApi.remove(`knowledge/bases/${row.id}`, row.version); await loadBases();
    ElMessage.success("知识库已删除");
  } catch (e) { if (e?.message) ElMessage.error(e.message); }
}
function openMove(records) { moving.value = records; moveTarget.value = ""; moveError.value = ""; moveDialog.value = true; }
async function moveDocuments() {
  if (!moveTarget.value || moveSaving.value) return;
  moveSaving.value = true; moveError.value = "";
  try {
    await workspaceApi.post("knowledge/move", { knowledgeBaseId: moveTarget.value, documents: moving.value.map(r => ({ id: r.id, version: r.version })) });
    moveDialog.value = false; selection.value = []; await Promise.all([load(), loadBases()]); ElMessage.success("文档已移动，版本和索引已保留");
  } catch (e) { moveError.value = e.message; }
  finally { moveSaving.value = false; }
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除“${row.title}”后将停止检索，历史引用仍保留来源标识。`, "删除知识文档", { type: "warning" });
    await workspaceApi.remove(`knowledge/${row.id}`, row.version); selection.value = [];
    await Promise.all([load(), loadBases()]); ElMessage.success("文档已删除");
  } catch (e) { if (e?.message) ElMessage.error(e.message); }
}
async function retryDocument(row) {
  try {
    await workspaceApi.post(`knowledge/${row.id}/retry?version=${row.version}`);
    await load(); ElMessage.success("已重新安排处理");
  } catch (e) { ElMessage.error(e.message); }
}
async function showTasks(reset = true) {
  if (reset) taskPage.value = 1;
  taskDialog.value = true; taskBusy.value = true; taskError.value = "";
  try {
    const data = await workspaceApi.get("knowledge/tasks", { knowledgeBaseId: baseId.value, page: taskPage.value, size: 20 });
    tasks.value = data.items; taskTotal.value = data.total;
  } catch (e) { taskError.value = e.message; }
  finally { taskBusy.value = false; }
}
function viewDocument(row) { router.push({ path: `/agent/knowledge/${row.id}`, query: { knowledgeBaseId: row.knowledgeBaseId } }); }
onBeforeRouteLeave(async to => {
  if (!imports.pending.value || to.path === "/login") return true;
  try {
    await ElMessageBox.confirm("还有文件尚未上传，离开后需重新选择这些文件。已经上传的文档会继续在后台处理。", "离开导入页面", { confirmButtonText: "离开", cancelButtonText: "继续上传" });
    return true;
  } catch { return false; }
});
onMounted(async () => {
  await Promise.all([loadBases(), readCapabilities()]);
  timer = setInterval(() => { if (!document.hidden && !busy.value && rows.value.some(r => ["WAIT_PARSE","PARSING","INDEXING"].includes(documentState(r)))) load(); }, 4000);
});
onBeforeUnmount(() => { disposed = true; sequence++; controller?.abort(); clearInterval(timer); clearTimeout(refreshTimer); });
</script>

<template>
  <section class="knowledge-workspace">
    <div class="knowledge-heading"><div><h2>知识库</h2><p>按主题整理自己的资料，为每个会话选择要引用的知识库。</p></div><div class="knowledge-heading-actions">
      <el-button v-if="importCount" @click="importOpen = true">导入进度（{{ importCount }}）</el-button>
      <el-button v-if="manageable" type="primary" @click="editBase()">新建知识库</el-button>
    </div></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button v-if="error" @click="loadBases(); load()">重新加载</el-button>
    <div class="knowledge-layout">
      <aside class="knowledge-base-list" aria-label="我的知识库">
        <el-input v-model="baseKeyword" clearable placeholder="查找知识库" aria-label="查找知识库" />
        <el-skeleton v-if="loadingBases && !bases.length" :rows="4" animated />
        <button v-for="base in visibleBases" :key="base.id" type="button" :class="{ selected: base.id === baseId }" :aria-pressed="base.id === baseId" @click="selectBase(base.id)"><AppIcon name="book" :size="18" /><span>{{ base.name }}</span><small>{{ base.documentCount }}</small></button>
        <p v-if="!loadingBases && !visibleBases.length" class="muted">{{ bases.length ? '没有找到这个知识库' : '还没有知识库' }}</p>
      </aside>
      <main class="knowledge-documents">
        <el-select class="knowledge-mobile-select" :model-value="baseId" placeholder="选择知识库" aria-label="选择知识库" @update:model-value="selectBase"><el-option v-for="base in bases" :key="base.id" :value="base.id" :label="base.name + '（' + base.documentCount + '）'" /></el-select>
        <div v-if="current" class="knowledge-current-heading"><div><h3>{{ current.name }}</h3><p class="muted">{{ current.description || '可以在这里导入、整理和检索这个主题的资料。' }}</p></div><div class="knowledge-heading-actions">
          <el-button @click="showTasks()">处理记录</el-button><el-button v-if="manageable" @click="editBase(current)">编辑知识库</el-button>
          <el-button v-if="manageable" :disabled="current.documentCount > 0" @click="removeBase">删除空库</el-button>
        </div></div>
        <div class="knowledge-upload-zone" :class="{ disabled: !current || !manageable }" @dragover.prevent @drop.prevent="drop">
          <div><strong>{{ current ? '拖入多个文件，导入到“' + current.name + '”' : '先创建知识库，再开始整理资料' }}</strong><p>上传后会依次解析和建立索引，可从导入进度查看每个文件的结果。</p></div>
          <el-button v-if="manageable" type="primary" :disabled="!current || !capabilities" @click="input?.click()">选择多个文件</el-button>
        </div>
        <input ref="input" type="file" multiple hidden :accept="capabilities?.accept || ''" @change="upload" />
        <KnowledgeFormatHelp :capabilities="capabilities" :error="capabilityError" />
        <el-button v-if="capabilityError" @click="readCapabilities">重新读取格式说明</el-button>
        <template v-if="current">
          <form class="knowledge-filter-row" @submit.prevent="load(true)"><el-input v-model="filters.keyword" clearable placeholder="搜索当前库的文档" aria-label="搜索当前库的文档" />
            <el-select v-model="filters.status" clearable placeholder="全部解析状态" aria-label="解析状态"><el-option label="等待解析" value="UPLOADED" /><el-option label="解析中" value="PARSING" /><el-option label="正文就绪" value="READY" /><el-option label="解析失败" value="FAILED" /></el-select>
            <el-button native-type="submit">查询</el-button><el-button @click="filters.keyword = ''; filters.status = ''; load(true)">重置</el-button>
            <el-button v-if="manageable" :disabled="!selection.length || bases.length < 2" @click="openMove(selection)">移动所选（{{ selection.length }}）</el-button>
          </form>
          <el-table :data="rows" row-key="id" v-loading="busy" @selection-change="value => selection = value" empty-text="当前知识库还没有文档，可以导入第一篇资料。">
            <el-table-column v-if="manageable" type="selection" width="48" />
            <el-table-column label="文档" min-width="240"><template #default="{ row }"><button class="knowledge-document-title" @click="viewDocument(row)">{{ row.title }}</button><p class="muted">{{ row.fileName }} · {{ (row.sizeBytes / 1024).toFixed(1) }} KB</p><p class="muted">{{ timeText(row.updatedAt) }} · {{ row.chunkCount }} 个片段</p><p v-if="row.errorMessage || row.indexError" class="knowledge-row-error">{{ row.errorMessage || row.indexError }}</p></template></el-table-column>
            <el-table-column label="处理状态" width="112"><template #default="{ row }"><el-tag :type="documentState(row) === 'FAILED' ? 'danger' : documentState(row) === 'READY' ? 'success' : 'info'">{{ importLabels[documentState(row)] }}</el-tag></template></el-table-column>
            <el-table-column label="操作" min-width="200"><template #default="{ row }"><el-button link type="primary" @click="viewDocument(row)">查看</el-button><el-button v-if="manageable" link :disabled="bases.length < 2" @click="openMove([row])">移动</el-button><el-button v-if="manageable && documentState(row) === 'FAILED'" link type="primary" @click="retryDocument(row)">重试</el-button><el-button v-if="manageable" link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
          </el-table>
          <el-pagination class="knowledge-pagination" v-model:current-page="filters.page" :page-size="filters.size" :total="total" layout="total,prev,pager,next" @current-change="load()" />
        </template>
        <AppEmptyState v-else-if="!loadingBases" description="建立第一个知识库，比如英语、Java学习或古诗词。" />
      </main>
    </div>

    <el-dialog v-model="baseDialog" :title="editingBase ? '编辑知识库' : '新建知识库'" width="min(520px,94vw)" align-center append-to-body :close-on-click-modal="false" :show-close="!baseSaving" :close-on-press-escape="!baseSaving">
      <el-alert v-if="baseError" :title="baseError" type="error" :closable="false" /><el-form label-position="top" :disabled="baseSaving" @submit.prevent="saveBase"><el-form-item label="名称"><el-input v-model="baseForm.name" maxlength="100" show-word-limit placeholder="例如：英语知识库" /></el-form-item><el-form-item label="简介"><el-input v-model="baseForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="这个知识库主要存放什么资料" /></el-form-item></el-form>
      <template #footer><el-button :disabled="baseSaving" @click="baseDialog = false">取消</el-button><el-button type="primary" :loading="baseSaving" @click="saveBase">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="moveDialog" title="移动文档" width="min(520px,94vw)" align-center append-to-body :close-on-click-modal="false" :show-close="!moveSaving" :close-on-press-escape="!moveSaving">
      <p>将 {{ moving.length }} 篇文档移动到另一个知识库，版本和索引会保留。</p><el-alert v-if="moveError" :title="moveError" type="error" :closable="false" /><el-select v-model="moveTarget" :disabled="moveSaving" placeholder="选择目标知识库" aria-label="目标知识库"><el-option v-for="base in bases.filter(b => b.id !== baseId)" :key="base.id" :value="base.id" :label="base.name" /></el-select>
      <template #footer><el-button :disabled="moveSaving" @click="moveDialog = false">取消</el-button><el-button type="primary" :loading="moveSaving" :disabled="!moveTarget" @click="moveDocuments">确认移动</el-button></template>
    </el-dialog>
    <el-dialog v-model="importOpen" title="批量导入进度" width="min(1040px,96vw)" align-center append-to-body class="knowledge-dialog">
      <p class="muted">关闭弹窗后继续上传。已经上传的文档会在后台继续解析和建立索引。</p><el-alert v-if="imports.paused.value" title="登录状态已变化，待上传队列已暂停。请重新登录后重新选择未上传文件。" type="warning" :closable="false" />
      <el-table :data="importItems" empty-text="还没有导入文件"><el-table-column prop="name" label="文件" min-width="220" /><el-table-column prop="baseName" label="目标知识库" min-width="140" /><el-table-column label="状态" width="112"><template #default="{ row }"><el-tag :type="['FAILED','INVALID'].includes(row.state) ? 'danger' : ['READY','DUPLICATE'].includes(row.state) ? 'success' : 'info'">{{ importLabels[row.state] }}</el-tag></template></el-table-column><el-table-column prop="error" label="说明" min-width="200" /><el-table-column label="操作" width="130"><template #default="{ row }"><el-button v-if="row.state === 'WAITING'" link @click="imports.remove(row)">移除</el-button><el-button v-if="row.state === 'FAILED'" link type="primary" :loading="row.retrying" @click="imports.retry(row)">重试</el-button><el-button v-if="row.doc" link @click="viewDocument(row.doc)">查看</el-button></template></el-table-column></el-table>
      <template #footer><el-button @click="imports.clearFinished()">清除已结束记录</el-button><el-button type="primary" @click="importOpen = false">收起进度</el-button></template>
    </el-dialog>
    <el-dialog v-model="taskDialog" :title="(current?.name || '') + ' · 后台处理记录'" width="min(960px,94vw)" align-center append-to-body class="knowledge-dialog"><el-alert v-if="taskError" :title="taskError" type="error" :closable="false" /><el-button v-if="taskError" @click="showTasks(false)">重新加载</el-button><el-table :data="tasks" v-loading="taskBusy" empty-text="当前知识库暂无处理记录"><el-table-column label="文档" min-width="200"><template #default="{ row }"><button class="knowledge-document-title" @click="viewDocument({ id: row.refId, knowledgeBaseId: row.knowledgeBaseId })">{{ row.documentTitle || '文档 #' + row.refId }}</button><p class="muted">{{ row.fileName }}</p></template></el-table-column><el-table-column label="处理步骤" min-width="120"><template #default="{ row }">{{ processingKinds[row.kind] || row.kind }}</template></el-table-column><el-table-column label="状态" width="112"><template #default="{ row }">{{ processingStatuses[row.status] || row.status }}</template></el-table-column><el-table-column prop="attempts" label="尝试次数" width="96" /><el-table-column prop="errorMessage" label="处理说明" min-width="200" /><el-table-column label="开始时间" width="184"><template #default="{ row }">{{ timeText(row.startedAt) }}</template></el-table-column></el-table><el-pagination class="knowledge-pagination" v-model:current-page="taskPage" :page-size="20" :total="taskTotal" layout="total,prev,pager,next" @current-change="showTasks(false)" /></el-dialog>
  </section>
</template>
