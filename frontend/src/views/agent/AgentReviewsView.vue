<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, timeText } from "../../api/agentWorkspace";
import AgentMarkdown from "../../components/agent/AgentMarkdown.vue";
import { useRouter } from "vue-router";
const router = useRouter();
function adjustInAssistant(row) { router.push({ path: '/agent', query: { reviewId: row.id } }); }
const rows = ref([]), total = ref(0), page = ref(1), busy = ref(false), creating = ref(false), error = ref("");
const range = ref([]), report = ref(null), drawer = ref(false), today = ref(""), retrying = ref(null);
let timer, disposed = false;
const labels = { PENDING: "等待分析", RUNNING: "分析中", READY: "已生成", FAILED: "生成失败" };
async function load() {
  busy.value = true;
  try { const data = await workspaceApi.get("reviews", { page: page.value, size: 20 }); if (disposed) return; rows.value = data.items; total.value = data.total; if (drawer.value && report.value) report.value = rows.value.find(row => row.id === report.value.id) || await workspaceApi.get(`reviews/${report.value.id}`); }
  catch (e) { if (!disposed) error.value = e.message; } finally { busy.value = false; }
}
async function retry(row) {
  retrying.value = row.id; error.value = "";
  try { const data = await workspaceApi.post(`reviews/${row.id}/retry`); if (drawer.value && report.value?.id === row.id) report.value = data; await load(); ElMessage.success("已重新开始生成，结果会自动更新"); }
  catch (e) { error.value = e.message; }
  finally { retrying.value = null; }
}
async function create() {
  if (range.value?.length !== 2) return ElMessage.warning("请选择复盘开始和结束日期");
  creating.value = true; error.value = "";
  try { report.value = await workspaceApi.post("reviews", { dateFrom: range.value[0], dateTo: range.value[1] }); drawer.value = true; await load(); ElMessage.success("已开始分析真实统计，结果会自动更新"); }
  catch (e) { error.value = e.message; } finally { creating.value = false; }
}
onMounted(async () => {
  try { const { api } = await import('../../api/http'); const board = await api('/dashboard/overview'); today.value = board.today; const start = new Date(`${board.today}T12:00:00Z`); start.setUTCDate(start.getUTCDate() - 6); range.value = [start.toISOString().slice(0, 10), board.today]; } catch (e) { error.value = e.message; }
  await load(); timer = setInterval(() => { if (!busy.value && !document.hidden && rows.value.some(row => ['PENDING', 'RUNNING'].includes(row.status))) load(); }, 4000);
});
onBeforeUnmount(() => { disposed = true; clearInterval(timer); });
</script>

<template>
  <section>
    <h2>成长复盘</h2><p class="intro">回顾这段时间的投入、习惯和进步，找到下一步的方向。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card shadow="never"><div class="review-form"><el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :disabled-date="date => today && date.getTime() > new Date(`${today}T23:59:59`).getTime()" /><el-button v-if="canGrow('agent:review:manage')" type="primary" :loading="creating" @click="create">生成复盘</el-button><el-button :loading="busy" @click="load">刷新</el-button></div>
      <el-skeleton v-if="busy && !rows.length" :rows="5" animated />
      <el-table v-else :data="rows" empty-text="暂无复盘，选择日期后生成第一份"><el-table-column label="日期区间" min-width="240"><template #default="{ row }">{{ row.dateFrom }} 至 {{ row.dateTo }}</template></el-table-column><el-table-column prop="timeZone" label="统计时区" min-width="160" /><el-table-column label="状态" width="128"><template #default="{ row }"><el-tag :type="row.status === 'READY' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'">{{ labels[row.status] }}</el-tag></template></el-table-column><el-table-column label="创建时间" min-width="184"><template #default="{ row }">{{ timeText(row.createdAt) }}</template></el-table-column><el-table-column label="操作" width="260"><template #default="{ row }"><el-button type="primary" link @click="report = row; drawer = true">查看</el-button><el-button v-if="row.status === 'READY' && canGrow('agent:workflow:manage')" type="primary" link @click="adjustInAssistant(row)">带到助手调整</el-button><el-button v-if="row.status === 'FAILED' && canGrow('agent:review:manage')" type="warning" link :loading="retrying === row.id" @click="retry(row)">重新生成</el-button></template></el-table-column></el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </el-card>
    <el-drawer v-model="drawer" title="成长复盘" size="min(800px, 96vw)"><template v-if="report"><p>{{ report.dateFrom }} 至 {{ report.dateTo }} · {{ report.timeZone }}</p><el-alert v-if="report.errorMessage" :title="report.errorMessage" type="error" :closable="false" /><div v-if="report.status === 'FAILED' && canGrow('agent:review:manage')" class="review-retry"><el-button type="warning" :loading="retrying === report.id" @click="retry(report)">重新生成复盘</el-button></div><el-skeleton v-if="['PENDING', 'RUNNING'].includes(report.status)" :rows="8" animated /><AgentMarkdown v-else-if="report.content" :content="report.content" /><el-empty v-else description="暂未生成内容，可重新生成" /><el-button v-if="report.status === 'READY' && canGrow('agent:workflow:manage')" type="primary" @click="adjustInAssistant(report)">带到成长助手调整计划</el-button><el-collapse><el-collapse-item title="查看本次分析依据的统计快照" name="facts"><pre class="facts">{{ JSON.stringify(report.facts, null, 2) }}</pre></el-collapse-item></el-collapse></template></el-drawer>
  </section>
</template>

<style scoped>
h2 { margin:0 0 8px; font-size:24px; line-height:32px; } .intro { color:var(--el-text-color-secondary); margin:0 0 24px; } .review-form { display:flex; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:24px; } .el-pagination { margin-top:24px; justify-content:flex-end; } .facts { overflow:auto; white-space:pre-wrap; overflow-wrap:anywhere; font-size:12px; line-height:20px; padding:16px; border-radius:8px; background:var(--el-fill-color-light); } .el-alert { margin-bottom:16px; } .review-retry { margin:0 0 16px; }
</style>
