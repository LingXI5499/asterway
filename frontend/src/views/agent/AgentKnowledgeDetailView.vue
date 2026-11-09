<!-- 文档身份和历史引用不随移动知识库而变化，新版本复用统一的格式能力。 -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, query, timeText } from "../../api/agentWorkspace";
import { validateImportFile, documentState, importLabels } from "../../composables/useKnowledgeImport";
import AgentMarkdown from "../../components/agent/AgentMarkdown.vue";
import KnowledgeFormatHelp from "../../components/agent/KnowledgeFormatHelp.vue";
const route = useRoute();
const doc = ref(null), revisions = ref([]), chunks = ref([]), total = ref(0), page = ref(1);
const busy = ref(false), error = ref(""), uploading = ref(false), input = ref(null), source = ref(null);
const capabilities = ref(null), capabilityError = ref("");
const current = computed(() => revisions.value.find(row => row.id === doc.value?.currentRevisionId));
const labels = { UPLOADED: "等待解析", PARSING: "解析中", READY: "正文就绪", FAILED: "失败" };
const indexLabels = { PENDING: "等待索引", PROCESSING: "索引中", READY: "可检索", FAILED: "索引失败" };
let timer, disposed = false, sequence = 0, controller;
async function load() {
  const seq = ++sequence, id = route.params.id;
  const revisionId = route.query.revisionId, chunkId = route.query.chunkId;
  controller?.abort(); controller = new AbortController(); const signal = controller.signal;
  busy.value = true; error.value = "";
  try {
    const [detail, history, parts] = await Promise.all([
      workspaceApi.get(`knowledge/${id}`, {}, signal), workspaceApi.get(`knowledge/${id}/revisions`, {}, signal),
      workspaceApi.get(`knowledge/${id}/chunks`, { page: page.value, size: 10 }, signal),
    ]);
    if (disposed || seq !== sequence) return;
    doc.value = detail; revisions.value = history.items; chunks.value = parts.items; total.value = parts.total;
    if (revisionId && chunkId) {
      const cited = await workspaceApi.get(`knowledge/${id}/revisions/${revisionId}/chunks/${chunkId}`, {}, signal);
      if (!disposed && seq === sequence) source.value = cited;
    }
  } catch (e) { if (!disposed && seq === sequence && e.name !== "AbortError") error.value = e.message; }
  finally { if (seq === sequence) busy.value = false; }
}
async function readCapabilities() {
  try { capabilities.value = await workspaceApi.get("knowledge/capabilities"); capabilityError.value = ""; }
  catch (e) { capabilityError.value = e.message; }
}
async function replace(event) {
  const file = event.target.files?.[0]; event.target.value = "";
  if (!file || uploading.value) return;
  if (!capabilities.value) return ElMessage.warning("请等待格式说明加载完成");
  const validation = validateImportFile(file, capabilities.value);
  if (validation) return ElMessage.warning(validation);
  uploading.value = true;
  try { await workspaceApi.upload(file, { documentId: doc.value.id, version: doc.value.version }); ElMessage.success("新版本已上传，旧版本与引用继续保留"); await load(); }
  catch (e) { ElMessage.error(e.message); } finally { uploading.value = false; }
}
async function retry() {
  if (uploading.value) return;
  uploading.value = true;
  try { await workspaceApi.post(`knowledge/${doc.value.id}/retry${query({ version: doc.value.version })}`); ElMessage.success("已重新安排处理"); await load(); }
  catch (e) { ElMessage.error(e.message); } finally { uploading.value = false; }
}
watch(() => route.fullPath, () => { page.value = 1; source.value = null; load(); });
onMounted(async () => {
  await Promise.all([load(), readCapabilities()]);
  timer = setInterval(() => { if (!document.hidden && !busy.value && doc.value && ["WAIT_PARSE","PARSING","INDEXING"].includes(documentState(doc.value))) load(); }, 4000);
});
onBeforeUnmount(() => { disposed = true; sequence++; controller?.abort(); clearInterval(timer); });
</script>
<template>
  <section>
    <el-button text @click="$router.push({ path: '/agent/knowledge', query: { knowledgeBaseId: doc?.knowledgeBaseId || $route.query.knowledgeBaseId } })"><AppIcon name="back" :size="16" />返回知识库</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon /><el-button v-if="error" @click="load">重新加载</el-button>
    <el-skeleton v-if="!doc && busy" :rows="6" animated />
    <template v-if="doc">
      <div class="detail-heading"><div><h2>{{ doc.title }}</h2><p>{{ doc.knowledgeBaseName }} · {{ doc.fileName }} · 更新于 {{ timeText(doc.updatedAt) }}</p></div><div><el-button :loading="busy" @click="load">刷新</el-button><el-button v-if="canGrow('agent:knowledge:manage')" :loading="uploading" :disabled="!capabilities" @click="input?.click()">上传新版本</el-button><el-button v-if="canGrow('agent:knowledge:manage') && documentState(doc) === 'FAILED'" type="primary" :loading="uploading" @click="retry">重试处理</el-button></div></div>
      <KnowledgeFormatHelp :capabilities="capabilities" :error="capabilityError" /><el-button v-if="capabilityError" @click="readCapabilities">重新读取格式说明</el-button>
      <input ref="input" type="file" hidden :accept="capabilities?.accept || ''" @change="replace" />
      <el-alert v-if="doc.errorMessage || current?.errorMessage" :title="doc.errorMessage || current.errorMessage" type="warning" :closable="false" show-icon />
      <el-card v-if="source" class="source-preview" shadow="never"><template #header>对话引用原文 · 版本 {{ source.revision.revisionNo }} · 第 {{ source.chunkNo + 1 }} 段</template><AgentMarkdown :content="source.content" /></el-card>
      <el-tabs>
        <el-tab-pane label="当前正文">
          <div class="state-line"><el-tag>{{ importLabels[documentState(doc)] }}</el-tag><el-tag :type="current?.indexStatus === 'READY' ? 'success' : 'info'">语义索引：{{ indexLabels[current?.indexStatus] || '等待处理' }}</el-tag><span>{{ total }} 段</span></div>
          <el-empty v-if="!chunks.length && !busy" description="暂无正文，等待解析完成或重试处理" />
          <el-card v-for="chunk in chunks" :key="chunk.id" shadow="never" class="chunk-card"><template #header><strong>第 {{ chunk.chunkNo + 1 }} 段</strong><span v-if="chunk.pageNumber"> · 第 {{ chunk.pageNumber }} 页</span><span v-if="chunk.heading"> · {{ chunk.heading }}</span><el-tag size="small" :type="chunk.hasEmbedding ? 'success' : 'info'">{{ chunk.hasEmbedding ? '可语义检索' : '等待索引' }}</el-tag></template><AgentMarkdown :content="chunk.content" /></el-card>
          <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total,prev,pager,next" @current-change="load" />
        </el-tab-pane>
        <el-tab-pane label="版本历史"><el-table :data="revisions"><el-table-column prop="revisionNo" label="版本" width="80" /><el-table-column prop="fileName" label="文件" min-width="220" /><el-table-column label="解析"><template #default="{ row }">{{ labels[row.status] }}</template></el-table-column><el-table-column label="索引"><template #default="{ row }">{{ indexLabels[row.indexStatus] }}</template></el-table-column><el-table-column prop="chunkCount" label="段落" /><el-table-column label="上传时间" width="184"><template #default="{ row }">{{ timeText(row.createdAt) }}</template></el-table-column></el-table></el-tab-pane>
      </el-tabs>
    </template>
  </section>
</template>
<style scoped>
.detail-heading { display:flex; gap:16px; justify-content:space-between; align-items:center; margin:16px 0 24px; } h2 { margin:0 0 8px; font-size:24px; line-height:32px; overflow-wrap:anywhere; } p { margin:0; color:var(--el-text-color-secondary); } .state-line { display:flex; align-items:center; flex-wrap:wrap; gap:12px; margin:16px 0; } .chunk-card, .source-preview { margin-bottom:16px; } .chunk-card .el-tag { margin-left:12px; } .el-pagination { margin-top:24px; justify-content:flex-end; flex-wrap:wrap; } .el-alert { margin:16px 0; } @media(max-width:768px) { .detail-heading { align-items:flex-start; flex-direction:column; } }
</style>
