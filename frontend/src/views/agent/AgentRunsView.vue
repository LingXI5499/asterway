<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { canGrow } from "../../api/growth";
import { workspaceApi, timeText } from "../../api/agentWorkspace";
import AgentPlanCard from "../../components/agent/AgentPlanCard.vue";
const workflows = ref([]), calls = ref([]), summary = ref({}), detail = ref(null), drawer = ref(false);
const tasks = ref([]), retrying = ref('');
const busy = ref(false), error = ref(""), tab = ref("workflows"), total = ref(0);
const filters = reactive({ status: "", page: 1, size: 20 });
const statuses = { WAITING_CONFIRM: "待确认", CONFIRMED: "已确认", EXECUTING: "执行中", SUCCEEDED: "已完成", FAILED: "失败", CANCELLED: "已取消", EXPIRED: "已过期" };
const taskStates = { PENDING: '等待处理', RUNNING: '处理中', SUCCEEDED: '已完成', FAILED: '处理失败', CANCELLED: '已取消' };
const kinds = { MEMORY_EXTRACT: '提取候选记忆', MEMORY_EMBEDDING: '记忆索引', DOCUMENT_PARSE: '解析资料', DOCUMENT_EMBEDDING: '资料索引', REVIEW_GENERATE: '成长复盘' };
let sequence = 0, disposed = false, timer;
function changeTab() { filters.status = ''; load(true); }
async function load(reset = false) {
  if (reset) filters.page = 1;
  busy.value = true; error.value = "";
  const ticket = ++sequence, mode = tab.value;
  clearTimeout(timer);
  try {
    const data = await workspaceApi.get(mode === 'tasks' ? 'runs/tasks' : mode === "calls" ? "runs/calls" : "planning/proposals", filters);
    if (disposed || ticket !== sequence) return;
    total.value = data.total;
    if (mode === 'tasks') tasks.value = data.items;
    else if (mode === "calls") { calls.value = data.items; summary.value = data.summary || {}; } else workflows.value = data.items;
    if (mode === 'tasks' && tasks.value.some(row => ['PENDING', 'RUNNING'].includes(row.status))) timer = setTimeout(() => load(), 4000);
  } catch (e) { if (ticket === sequence) error.value = e.message; } finally { if (ticket === sequence) busy.value = false; }
}
async function retryTask(row) {
  if (retrying.value) return;
  retrying.value = row.id; error.value = '';
  try { await workspaceApi.post(`runs/tasks/${row.id}/retry`); await load(); ElMessage.success('已重新排队，结果会自动更新'); }
  catch (e) { error.value = e.message; }
  finally { retrying.value = ''; }
}
async function show(row) { try { detail.value = await workspaceApi.get(`planning/proposals/${row.id}`); drawer.value = true; } catch (e) { error.value = e.message; } }
onMounted(load);
onBeforeUnmount(() => { disposed = true; sequence += 1; clearTimeout(timer); });
</script>

<template>
  <section>
    <h2>运行记录</h2><p class="intro">查看方案进度、提交结果和使用记录。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card shadow="never"><el-tabs v-model="tab" @tab-change="changeTab"><el-tab-pane label="规划流程" name="workflows" /><el-tab-pane label="调用与用量" name="calls" /><el-tab-pane label="后台任务" name="tasks" /></el-tabs>
      <el-form inline @submit.prevent="load(true)"><el-form-item v-if="tab !== 'calls'"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px"><el-option v-for="(label, value) in (tab === 'tasks' ? taskStates : statuses)" :key="value" :label="label" :value="value" /></el-select></el-form-item><el-form-item><el-button native-type="submit" :loading="busy">刷新记录</el-button></el-form-item></el-form>
      <el-skeleton v-if="busy && !(tab === 'calls' ? calls.length : tab === 'tasks' ? tasks.length : workflows.length)" :rows="6" animated />
      <el-table v-else-if="tab === 'workflows'" :data="workflows" empty-text="暂无规划流程，在对话中说明目标即可开始"><el-table-column label="类型" min-width="160" prop="intentLabel" /><el-table-column label="状态" width="128" prop="statusLabel" /><el-table-column label="创建时间" min-width="184"><template #default="{ row }">{{ timeText(row.createdAt) }}</template></el-table-column><el-table-column label="结束时间" min-width="184"><template #default="{ row }">{{ timeText(row.finishedAt) }}</template></el-table-column><el-table-column prop="errorCode" label="错误码" min-width="160" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button type="primary" link @click="show(row)">查看过程</el-button></template></el-table-column></el-table>
      <el-table v-else-if="tab === 'tasks'" :data="tasks" empty-text="暂无后台任务">
        <el-table-column label="任务" min-width="160"><template #default="{ row }">{{ kinds[row.kind] || '后台处理' }}</template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 'FAILED' ? 'warning' : row.status === 'SUCCEEDED' ? 'success' : 'info'">{{ taskStates[row.status] }}</el-tag></template></el-table-column>
        <el-table-column prop="attempts" label="尝试次数" width="100" />
        <el-table-column label="处理结果" min-width="240"><template #default="{ row }">{{ row.errorMessage || (row.status === 'SUCCEEDED' ? '处理已完成，结果已保存' : '等待处理结果') }}<small v-if="row.errorCode" class="task-error-code">{{ row.errorCode }}</small></template></el-table-column>
        <el-table-column label="更新时间" min-width="184"><template #default="{ row }">{{ timeText(row.updatedAt || row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="160"><template #default="{ row }"><el-button v-if="row.status === 'FAILED' && ['MEMORY_EXTRACT', 'MEMORY_EMBEDDING'].includes(row.kind) && canGrow('agent:memory:manage')" type="primary" link :loading="retrying === row.id" :disabled="!!retrying && retrying !== row.id" @click="retryTask(row)">重新处理</el-button><RouterLink v-else-if="row.kind === 'REVIEW_GENERATE'" to="/agent/reviews">查看复盘</RouterLink><RouterLink v-else-if="row.kind.startsWith('DOCUMENT_')" to="/agent/knowledge">查看资料</RouterLink></template></el-table-column>
      </el-table>
      <template v-else><div class="usage-summary"><span>累计调用 <strong>{{ summary.calls || 0 }}</strong></span><span>输入 Token <strong>{{ summary.promptTokens ?? '未知' }}</strong></span><span>输出 Token <strong>{{ summary.completionTokens ?? '未知' }}</strong></span><span>平均耗时 <strong>{{ ((summary.averageDurationMs || 0) / 1000).toFixed(2) }} 秒</strong></span></div><el-table :data="calls" empty-text="暂无调用记录"><el-table-column prop="kind" label="类型" width="112" /><el-table-column prop="name" label="调用" min-width="200" /><el-table-column label="结果" width="112"><template #default="{ row }"><el-tag :type="row.status === 'SUCCEEDED' ? 'success' : 'danger'">{{ row.status === 'SUCCEEDED' ? '成功' : row.status === 'CANCELLED' ? '已停止' : '失败' }}</el-tag></template></el-table-column><el-table-column label="耗时" width="112"><template #default="{ row }">{{ (row.durationMs / 1000).toFixed(2) }} 秒</template></el-table-column><el-table-column label="首字耗时" width="112"><template #default="{ row }">{{ row.firstTokenMs == null ? '—' : `${(row.firstTokenMs / 1000).toFixed(2)} 秒` }}</template></el-table-column><el-table-column prop="traceId" label="追踪编号" min-width="220" show-overflow-tooltip /><el-table-column label="时间" width="184"><template #default="{ row }">{{ timeText(row.createdAt) }}</template></el-table-column></el-table></template>
      <el-pagination v-model:current-page="filters.page" :page-size="filters.size" :total="total" layout="total, prev, pager, next" @current-change="load()" />
    </el-card>
    <el-drawer v-model="drawer" title="规划过程" size="min(760px, 96vw)"><template v-if="detail"><el-alert :title="detail.run.statusLabel" :type="detail.run.status === 'SUCCEEDED' ? 'success' : 'info'" :closable="false" /><AgentPlanCard v-for="plan in [...detail.plans].reverse()" :key="plan.id" :plan="plan" class="plan-history" /><el-timeline><el-timeline-item v-for="event in detail.events" :key="event.id" :timestamp="timeText(event.createdAt)">{{ event.eventType }}</el-timeline-item></el-timeline><el-alert v-if="detail.commit?.errorMessage" :title="detail.commit.errorMessage" type="error" :closable="false" /><RouterLink :to="'/agent?conversationId=' + detail.run.conversationId">返回对话调整或确认方案</RouterLink></template></el-drawer>
  </section>
</template>

<style scoped>
h2 { margin:0 0 8px; font-size:24px; line-height:32px; } .intro { color:var(--el-text-color-secondary); margin:0 0 24px; } .usage-summary { display:flex; flex-wrap:wrap; gap:24px; padding:16px; margin-bottom:16px; background:var(--el-fill-color-light); border-radius:8px; } .usage-summary strong { margin-left:8px; } .el-pagination { margin-top:24px; justify-content:flex-end; } .plan-history { margin:16px 0; } .el-alert { margin-bottom:16px; }
.task-error-code { display: block; font-size: 12px; color: var(--aw-text-tertiary); margin-top: 4px; }
</style>
