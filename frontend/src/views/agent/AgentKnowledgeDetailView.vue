<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, query, timeText } from "../../api/agentWorkspace";
import AgentMarkdown from "../../components/agent/AgentMarkdown.vue";
const route = useRoute();
const doc = ref(null), revisions = ref([]), chunks = ref([]), total = ref(0), page = ref(1);
const busy = ref(false), error = ref(""), uploading = ref(false), input = ref(null), source = ref(null);
const current = computed(() => revisions.value.find(row => row.id === doc.value?.currentRevisionId));
const labels = { UPLOADED: "等待解析", PARSING: "解析中", READY: "就绪", FAILED: "失败", PENDING: "等待向量化" };
let timer, disposed = false;
async function load() {
  busy.value = true; error.value = "";
  try {
    const id = route.params.id;
    const [detail, history, parts] = await Promise.all([workspaceApi.get(`knowledge/${id}`), workspaceApi.get(`knowledge/${id}/revisions`), workspaceApi.get(`knowledge/${id}/chunks`, { page: page.value, size: 10 })]);
    if (disposed) return;
    doc.value = detail; revisions.value = history.items; chunks.value = parts.items; total.value = parts.total;
    if (route.query.revisionId && route.query.chunkId) source.value = await workspaceApi.get(`knowledge/${id}/revisions/${route.query.revisionId}/chunks/${route.query.chunkId}`);
  } catch (e) { if (!disposed) error.value = e.message; } finally { busy.value = false; }
}
async function replace(event) {
  const file = event.target.files?.[0]; event.target.value = ""; if (!file) return;
  if (file.size > 25 * 1024 * 1024) return ElMessage.warning("文件不能超过 25 MB");
  uploading.value = true;
  try { await workspaceApi.upload(file, { documentId: doc.value.id, version: doc.value.version }); ElMessage.success("新版本已上传，旧版本与引用继续保留"); await load(); }
  catch (e) { ElMessage.error(e.message); } finally { uploading.value = false; }
}
async function retry() {
  uploading.value = true;
  try { await workspaceApi.post(`knowledge/${doc.value.id}/retry${query({ version: doc.value.version })}`); ElMessage.success("已重新安排处理"); await load(); }
  catch (e) { ElMessage.error(e.message); } finally { uploading.value = false; }
}
watch(() => route.fullPath, () => { page.value = 1; source.value = null; load(); });
onMounted(async () => { await load(); timer = setInterval(() => { if (!document.hidden && !busy.value && doc.value && (['UPLOADED', 'PARSING'].includes(doc.value.status) || current.value?.indexStatus === 'PENDING')) load(); }, 4000); });
onBeforeUnmount(() => { disposed = true; clearInterval(timer); });
</script>

<template>
  <section>
    <el-button text @click="$router.push('/agent/knowledge')"><AppIcon name="back" :size="16" />返回知识资料</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-skeleton v-if="!doc && busy" :rows="6" animated />
    <template v-if="doc">
      <div class="detail-heading"><div><h2>{{ doc.title }}</h2><p>{{ doc.fileName }} · 更新于 {{ timeText(doc.updatedAt) }}</p></div><div><el-button :loading="busy" @click="load">刷新</el-button><el-button v-if="canGrow('agent:knowledge:manage')" :loading="uploading" @click="input?.click()">上传新版本</el-button><el-button v-if="canGrow('agent:knowledge:manage') && (doc.status === 'FAILED' || current?.indexStatus === 'FAILED')" type="primary" :loading="uploading" @click="retry">重试处理</el-button></div></div>
      <input ref="input" type="file" hidden accept=".txt,.md,.pdf,.docx" @change="replace" />
      <el-alert v-if="doc.errorMessage || current?.errorMessage" :title="doc.errorMessage || current.errorMessage" type="warning" :closable="false" show-icon />
      <el-card v-if="source" class="source-preview" shadow="never"><template #header>对话引用原文 · 版本 {{ source.revision.revisionNo }} · 第 {{ source.chunkNo + 1 }} 段</template><AgentMarkdown :content="source.content" /></el-card>
      <el-tabs>
        <el-tab-pane label="当前正文">
          <div class="state-line"><el-tag>{{ labels[doc.status] }}</el-tag><el-tag :type="current?.indexStatus === 'READY' ? 'success' : 'info'">语义索引：{{ labels[current?.indexStatus] || '等待处理' }}</el-tag><span>{{ total }} 段</span></div>
          <el-empty v-if="!chunks.length && !busy" description="暂无正文，等待解析完成或重试处理" />
          <el-card v-for="chunk in chunks" :key="chunk.id" shadow="never" class="chunk-card"><template #header><strong>第 {{ chunk.chunkNo + 1 }} 段</strong><span v-if="chunk.pageNumber"> · 第 {{ chunk.pageNumber }} 页</span><span v-if="chunk.heading"> · {{ chunk.heading }}</span><el-tag size="small" :type="chunk.hasEmbedding ? 'success' : 'info'">{{ chunk.hasEmbedding ? '可语义检索' : '等待索引' }}</el-tag></template><AgentMarkdown :content="chunk.content" /></el-card>
          <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
        </el-tab-pane>
        <el-tab-pane label="版本历史"><el-table :data="revisions"><el-table-column prop="revisionNo" label="版本" width="80" /><el-table-column prop="fileName" label="文件" min-width="220" /><el-table-column label="解析"><template #default="{ row }">{{ labels[row.status] }}</template></el-table-column><el-table-column label="索引"><template #default="{ row }">{{ labels[row.indexStatus] }}</template></el-table-column><el-table-column prop="chunkCount" label="段落" /><el-table-column label="上传时间" width="184"><template #default="{ row }">{{ timeText(row.createdAt) }}</template></el-table-column></el-table></el-tab-pane>
      </el-tabs>
    </template>
  </section>
</template>

<style scoped>
.detail-heading { display:flex; gap:16px; justify-content:space-between; align-items:center; margin:16px 0 24px; } h2 { margin:0 0 8px; font-size:24px; line-height:32px; overflow-wrap:anywhere; } p { margin:0; color:var(--el-text-color-secondary); } .state-line { display:flex; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:16px; } .chunk-card, .source-preview { margin-bottom:16px; } .chunk-card .el-tag { margin-left:12px; } .el-pagination { margin-top:24px; justify-content:flex-end; } .el-alert { margin-bottom:16px; } @media(max-width:768px) { .detail-heading { align-items:flex-start; flex-direction:column; } }
</style>
