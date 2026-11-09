<!-- 渲染成长助手运行记录列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/agentWorkspace 提供的组件、状态函数或请求方法。 */
import { workspaceApi, timeText } from "../../api/agentWorkspace";
/* 引入 ../../components/agent/AgentPlanCard.vue 提供的组件、状态函数或请求方法。 */
import AgentPlanCard from "../../components/agent/AgentPlanCard.vue";
const /* workflows保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ workflows = ref([]), /* 调用列表（calls）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ calls = ref([]), /* 汇总（summary）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ summary = ref({}), /* 详情（detail）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ detail = ref(null), /* 抽屉（drawer）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ drawer = ref(false);
const /* 任务列表（tasks）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ tasks = ref([]), /* retrying保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ retrying = ref('');
const /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ busy = ref(false), /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ error = ref(""), /* tab保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ tab = ref("workflows"), /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ total = ref(0);
const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = reactive({ /* 状态（status）字段使用固定文字 ''。 */ status: "", /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 });
const /* statuses保存表达式 { WAITING_CONFIRM: "待确认", CONFIRMED: "已确认", EXECUTING: "执行中", SUCCEEDED: "已完成", FAILED: "失败", CANCELLED: "已取消", EXPIRED:… 的结果。 */ statuses = { /* 等待确认确认（WAITING_CONFIRM）字段使用固定文字 '待确认'。 */ WAITING_CONFIRM: "待确认", /* 已确认（CONFIRMED）字段使用固定文字 '已确认'。 */ CONFIRMED: "已确认", /* 正在执行（EXECUTING）字段使用固定文字 '执行中'。 */ EXECUTING: "执行中", /* 已成功（SUCCEEDED）字段使用固定文字 '已完成'。 */ SUCCEEDED: "已完成", /* 失败（FAILED）字段使用固定文字 '失败'。 */ FAILED: "失败", /* 已取消（CANCELLED）字段使用固定文字 '已取消'。 */ CANCELLED: "已取消", /* 已过期（EXPIRED）字段使用固定文字 '已过期'。 */ EXPIRED: "已过期" };
const /* 任务States（taskStates）保存表达式 { PENDING: '等待处理', RUNNING: '处理中', SUCCEEDED: '已完成', FAILED: '处理失败', CANCELLED: '已取消' } 的结果。 */ taskStates = { /* 等待处理（PENDING）字段使用固定文字 '等待处理'。 */ PENDING: '等待处理', /* 运行中（RUNNING）字段使用固定文字 '处理中'。 */ RUNNING: '处理中', /* 已成功（SUCCEEDED）字段使用固定文字 '已完成'。 */ SUCCEEDED: '已完成', /* 失败（FAILED）字段使用固定文字 '处理失败'。 */ FAILED: '处理失败', /* 已取消（CANCELLED）字段使用固定文字 '已取消'。 */ CANCELLED: '已取消' };
const /* kinds保存表达式 { MEMORY_EXTRACT: '提取候选记忆', MEMORY_EMBEDDING: '记忆索引', DOCUMENT_PARSE: '解析资料', DOCUMENT_EMBEDDING: '资料索引', REVIEW_GENERAT… 的结果。 */ kinds = { /* 长期记忆提取（MEMORY_EXTRACT）字段使用固定文字 '提取候选记忆'。 */ MEMORY_EXTRACT: '提取候选记忆', /* 长期记忆向量（MEMORY_EMBEDDING）字段使用固定文字 '记忆索引'。 */ MEMORY_EMBEDDING: '记忆索引', /* 知识文档解析（DOCUMENT_PARSE）字段使用固定文字 '解析资料'。 */ DOCUMENT_PARSE: '解析资料', /* 知识文档向量（DOCUMENT_EMBEDDING）字段使用固定文字 '资料索引'。 */ DOCUMENT_EMBEDDING: '资料索引', /* 复盘GENERATE（REVIEW_GENERATE）字段使用固定文字 '成长复盘'。 */ REVIEW_GENERATE: '成长复盘' };
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0, /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */ disposed = false, /* 声明 定时器（timer），稍后再填入处理结果。 */ timer;
/* 修改Tab（changeTab）：调用 changeTab 完成修改Tab处理。 */
function changeTab() { /* 把固定文字 ''写入 filters.status。 */ filters.status = ''; /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(true); }
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load(reset = false) {
  /* 检查条件 reset 是否成立，成立时走这一分支。 */
  if (reset) /* 把固定数值 1写入 filters.page。 */ filters.page = 1;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true; /* 把固定文字 ''写入 error.value。 */ error.value = "";
  const /* 本次请求的序号（ticket）保存按表达式计算得到的值：++sequence。 */ ticket = ++sequence, /* mode保存表达式 tab.value 的结果。 */ mode = tab.value;
  /* 取消尚未触发的一次性定时器。 */
  clearTimeout(timer);
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 根据前面的条件，从两个候选结果中选择一个：workspaceApi.get(mode === 'tasks' ? 'runs/tasks' : mode === "calls" ? "runs/calls" : "planning/propo… 完成后取得结果。 */ data = await workspaceApi.get(mode === 'tasks' ? 'runs/tasks' : mode === "calls" ? "runs/calls" : "planning/proposals", filters);
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || ticket !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把表达式 data.total 的结果写入 total.value。 */
    total.value = data.total;
    /* 检查条件 mode === 'tasks' 是否成立，成立时走这一分支。 */
    if (mode === 'tasks') /* 把表达式 data.items 的结果写入 tasks.value。 */ tasks.value = data.items;
    else /* 检查条件 mode === "calls" 是否成立，成立时走这一分支。 */ if (mode === "calls") { /* 把表达式 data.items 的结果写入 calls.value。 */ calls.value = data.items; /* 把优先使用前面的有效值，没有时采用后面的备用值：data.summary || {}写入 summary.value。 */ summary.value = data.summary || {}; } else /* 把表达式 data.items 的结果写入 workflows.value。 */ workflows.value = data.items;
    /* 检查条件 mode === 'tasks' && tasks.value.some(row => ['PENDING', 'RUNNING'].includes(row.status)) 是否成立，成立时走这一分支。 */
    if (mode === 'tasks' && tasks.value.some(row => ['PENDING', 'RUNNING'].includes(row.status))) /* 把过指定毫秒后执行一次处理写入 timer。 */ timer = setTimeout(() => load(), 4000);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 检查当前结果仍属于最新请求，并且其余条件也都满足：ticket === sequence，成立时走这一分支。 */ if (ticket === sequence) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; } finally { /* 检查当前结果仍属于最新请求，并且其余条件也都满足：ticket === sequence，成立时走这一分支。 */ if (ticket === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false; }
}
/* 重试任务（retryTask）：重试 任务（Task）。 */
async function retryTask(row) {
  /* 检查条件 retrying.value 是否成立，成立时走这一分支。 */
  if (retrying.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把表达式 row.id 的结果写入 retrying.value。 */
  retrying.value = row.id; /* 把固定文字 ''写入 error.value。 */ error.value = '';
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */ await workspaceApi.post(`runs/tasks/${row.id}/retry`); /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load(); /* 调用 ElMessage.success 完成成功处理。 */ ElMessage.success('已重新排队，结果会自动更新'); }
  /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */
  catch (e) { /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; }
  finally { /* 把固定文字 ''写入 retrying.value。 */ retrying.value = ''; }
}
/* show：调用 show 完成show处理。 */
async function show(row) { /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */ try { /* 把等待 读取服务端数据；认证信息由公共请求层统一附带 完成后取得结果写入 detail.value。 */ detail.value = await workspaceApi.get(`planning/proposals/${row.id}`); /* 把真，表示条件成立或开关打开写入 drawer.value。 */ drawer.value = true; } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; } }
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
/* 注册组件销毁前的清理，取消请求并释放定时器。 */
onBeforeUnmount(() => { /* 把真，表示条件成立或开关打开写入 disposed。 */ disposed = true; /* 更新本次处理状态：sequence += 1。 */ sequence += 1; /* 取消尚未触发的一次性定时器。 */ clearTimeout(timer); });
</script>

<template>
  <!-- 显示一个有独立含义的内容区。 -->
  <section>
    <!-- 显示二级标题。 -->
    <h2>运行记录</h2><!-- 显示一段文字；用样式类 intro 控制外观。 --><p class="intro">查看方案进度、提交结果和使用记录。</p>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false；启用 show-icon 属性。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <!-- 显示Element Plus 的 card 组件；shadow 固定为 never。 -->
    <el-card shadow="never"><!-- 显示Element Plus 的 tabs 组件；与 tab 双向同步；tab-change 事件触发时执行 changeTab。 --><el-tabs v-model="tab" @tab-change="changeTab"><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 规划流程；name 固定为 workflows。 --><el-tab-pane label="规划流程" name="workflows" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 调用与用量；name 固定为 calls。 --><el-tab-pane label="调用与用量" name="calls" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 后台任务；name 固定为 tasks。 --><el-tab-pane label="后台任务" name="tasks" /></el-tabs>
      <!-- 显示Element Plus 的 form 组件；启用 inline 属性；submit 事件触发时执行 load(true)。 -->
      <el-form inline @submit.prevent="load(true)"><!-- 显示Element Plus 的 form-item 组件；仅当 tab !== 'calls' 成立时渲染。 --><el-form-item v-if="tab !== 'calls'"><!-- 显示Element Plus 的 select 组件；与 filters.status 双向同步；启用 clearable 属性；未输入时提示 全部状态；使用这里给定的行内样式。 --><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px"><!-- 显示Element Plus 的 option 组件；按 (label, value) in (tab === 'tasks' ? taskStates : statuses) 为每条数据重复生成内容；key 取自 value；label 取自 label；value 取自 value。 --><el-option v-for="(label, value) in (tab === 'tasks' ? taskStates : statuses)" :key="value" :label="label" :value="value" /></el-select></el-form-item><!-- 显示Element Plus 的 form-item 组件。 --><el-form-item><!-- 显示Element Plus 的 button 组件；native-type 固定为 submit；loading 取自 busy。 --><el-button native-type="submit" :loading="busy">刷新记录</el-button></el-form-item></el-form>
      <!-- 显示Element Plus 的 skeleton 组件；仅当 busy && !(tab === 'calls' ? calls.length : tab === 'tasks' ? tasks.length : workflows.length) 成立时渲染；rows 取自 6；启用 animated 属性。 -->
      <el-skeleton v-if="busy && !(tab === 'calls' ? calls.length : tab === 'tasks' ? tasks.length : workflows.length)" :rows="6" animated />
      <!-- 显示Element Plus 的 table 组件；前一条件不成立且 tab === 'workflows' 成立时渲染；data 取自 workflows；empty-text 固定为 暂无规划流程，在对话中说明目标即可开始。 -->
      <el-table v-else-if="tab === 'workflows'" :data="workflows" empty-text="暂无规划流程，在对话中说明目标即可开始"><!-- 显示Element Plus 的 table-column 组件；label 固定为 类型；min-width 固定为 160；prop 固定为 intentLabel。 --><el-table-column label="类型" min-width="160" prop="intentLabel" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 128；prop 固定为 statusLabel。 --><el-table-column label="状态" width="128" prop="statusLabel" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 创建时间；min-width 固定为 184。 --><el-table-column label="创建时间" min-width="184"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 timeText(row.createdAt) 的当前值转成文字显示。 -->{{ timeText(row.createdAt) }}</template></el-table-column><!-- 显示Element Plus 的 table-column 组件；label 固定为 结束时间；min-width 固定为 184。 --><el-table-column label="结束时间" min-width="184"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 timeText(row.finishedAt) 的当前值转成文字显示。 -->{{ timeText(row.finishedAt) }}</template></el-table-column><!-- 显示Element Plus 的 table-column 组件；prop 固定为 errorCode；label 固定为 错误码；min-width 固定为 160。 --><el-table-column prop="errorCode" label="错误码" min-width="160" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 100。 --><el-table-column label="操作" width="100"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 button 组件；类型设为 primary；启用 link 属性；click 事件触发时执行 show(row)。 --><el-button type="primary" link @click="show(row)">查看过程</el-button></template></el-table-column></el-table>
      <!-- 显示Element Plus 的 table 组件；前一条件不成立且 tab === 'tasks' 成立时渲染；data 取自 tasks；empty-text 固定为 暂无后台任务。 -->
      <el-table v-else-if="tab === 'tasks'" :data="tasks" empty-text="暂无后台任务">
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 任务；min-width 固定为 160。 -->
        <el-table-column label="任务" min-width="160"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 kinds[row.kind] || '后台处理' 的当前值转成文字显示。 -->{{ kinds[row.kind] || '后台处理' }}</template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 120。 -->
        <el-table-column label="状态" width="120"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 tag 组件；type 取自 row.status === 'FAILED' ? 'warning' : row.status === 'SUCCEEDED' ? 'success' : 'info'。 --><el-tag :type="row.status === 'FAILED' ? 'warning' : row.status === 'SUCCEEDED' ? 'success' : 'info'"><!-- 把 taskStates[row.status] 的当前值转成文字显示。 -->{{ taskStates[row.status] }}</el-tag></template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 attempts；label 固定为 尝试次数；width 固定为 100。 -->
        <el-table-column prop="attempts" label="尝试次数" width="100" />
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 处理结果；min-width 固定为 240。 -->
        <el-table-column label="处理结果" min-width="240"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 row.errorMessage || (row.status === 'SUCCEEDED' ? '处理已完成，结果已保存' : '等待处理结果') 的当前值转成文字显示。 -->{{ row.errorMessage || (row.status === 'SUCCEEDED' ? '处理已完成，结果已保存' : '等待处理结果') }}<!-- 显示辅助说明文字；仅当 row.errorCode 成立时渲染；用样式类 task-error-code 控制外观。 --><small v-if="row.errorCode" class="task-error-code"><!-- 把 row.errorCode 的当前值转成文字显示。 -->{{ row.errorCode }}</small></template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 更新时间；min-width 固定为 184。 -->
        <el-table-column label="更新时间" min-width="184"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 timeText(row.updatedAt || row.createdAt) 的当前值转成文字显示。 -->{{ timeText(row.updatedAt || row.createdAt) }}</template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 160。 -->
        <el-table-column label="操作" width="160"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 button 组件；仅当 row.status === 'FAILED' && ['MEMORY_EXTRACT', 'MEMORY_EMBEDDING'].includes(row.kind) && canGrow('agent:me… 成立时渲染；类型设为 primary；启用 link 属性；loading 取自 retrying === row.id；disabled 取自 !!retrying && retrying !== row.id；click 事件触发时执行 retryTask(row)。 --><el-button v-if="row.status === 'FAILED' && ['MEMORY_EXTRACT', 'MEMORY_EMBEDDING'].includes(row.kind) && canGrow('agent:memory:manage')" type="primary" link :loading="retrying === row.id" :disabled="!!retrying && retrying !== row.id" @click="retryTask(row)">重新处理</el-button><!-- 显示应用内部页面链接；前一条件不成立且 row.kind === 'REVIEW_GENERATE' 成立时渲染；to 固定为 /agent/reviews。 --><RouterLink v-else-if="row.kind === 'REVIEW_GENERATE'" to="/agent/reviews">查看复盘</RouterLink><!-- 显示应用内部页面链接；前一条件不成立且 row.kind.startsWith('DOCUMENT_') 成立时渲染；to 固定为 /agent/knowledge。 --><RouterLink v-else-if="row.kind.startsWith('DOCUMENT_')" to="/agent/knowledge">查看资料</RouterLink></template></el-table-column>
      </el-table>
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 -->
      <template v-else><!-- 显示一个布局容器；用样式类 usage-summary 控制外观。 --><div class="usage-summary"><!-- 显示行内文字区域。 --><span>累计调用 <!-- 显示重点文字。 --><strong><!-- 把 summary.calls || 0 的当前值转成文字显示。 -->{{ summary.calls || 0 }}</strong></span><!-- 显示行内文字区域。 --><span>输入 Token <!-- 显示重点文字。 --><strong><!-- 把 summary.promptTokens ?? '未知' 的当前值转成文字显示。 -->{{ summary.promptTokens ?? '未知' }}</strong></span><!-- 显示行内文字区域。 --><span>输出 Token <!-- 显示重点文字。 --><strong><!-- 把 summary.completionTokens ?? '未知' 的当前值转成文字显示。 -->{{ summary.completionTokens ?? '未知' }}</strong></span><!-- 显示行内文字区域。 --><span>平均耗时 <!-- 显示重点文字。 --><strong><!-- 把 ((summary.averageDurationMs || 0) / 1000).toFixed(2) 的当前值转成文字显示。 -->{{ ((summary.averageDurationMs || 0) / 1000).toFixed(2) }} 秒</strong></span></div><!-- 显示Element Plus 的 table 组件；data 取自 calls；empty-text 固定为 暂无调用记录。 --><el-table :data="calls" empty-text="暂无调用记录"><!-- 显示Element Plus 的 table-column 组件；prop 固定为 kind；label 固定为 类型；width 固定为 112。 --><el-table-column prop="kind" label="类型" width="112" /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 调用；min-width 固定为 200。 --><el-table-column prop="name" label="调用" min-width="200" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 结果；width 固定为 112。 --><el-table-column label="结果" width="112"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 tag 组件；type 取自 row.status === 'SUCCEEDED' ? 'success' : 'danger'。 --><el-tag :type="row.status === 'SUCCEEDED' ? 'success' : 'danger'"><!-- 把 row.status === 'SUCCEEDED' ? '成功' : row.status === 'CANCELLED' ? '已停止' : '失败' 的当前值转成文字显示。 -->{{ row.status === 'SUCCEEDED' ? '成功' : row.status === 'CANCELLED' ? '已停止' : '失败' }}</el-tag></template></el-table-column><!-- 显示Element Plus 的 table-column 组件；label 固定为 耗时；width 固定为 112。 --><el-table-column label="耗时" width="112"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 (row.durationMs / 1000).toFixed(2) 的当前值转成文字显示。 -->{{ (row.durationMs / 1000).toFixed(2) }} 秒</template></el-table-column><!-- 显示Element Plus 的 table-column 组件；label 固定为 首字耗时；width 固定为 112。 --><el-table-column label="首字耗时" width="112"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 row.firstTokenMs == null ? '—' : `${(row.firstTokenMs / 1000).toFixed(2)} 秒` 的当前值转成文字显示。 -->{{ row.firstTokenMs == null ? '—' : `${(row.firstTokenMs / 1000).toFixed(2)} 秒` }}</template></el-table-column><!-- 显示Element Plus 的 table-column 组件；prop 固定为 traceId；label 固定为 追踪编号；min-width 固定为 220；启用 show-overflow-tooltip 属性。 --><el-table-column prop="traceId" label="追踪编号" min-width="220" show-overflow-tooltip /><!-- 显示Element Plus 的 table-column 组件；label 固定为 时间；width 固定为 184。 --><el-table-column label="时间" width="184"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 timeText(row.createdAt) 的当前值转成文字显示。 -->{{ timeText(row.createdAt) }}</template></el-table-column></el-table></template>
      <!-- 显示Element Plus 的 pagination 组件；与 filters.page 双向同步（current-page）；page-size 取自 filters.size；total 取自 total；layout 固定为 total, prev, pager, next；current-change 事件触发时执行 load()。 -->
      <el-pagination v-model:current-page="filters.page" :page-size="filters.size" :total="total" layout="total, prev, pager, next" @current-change="load()" />
    </el-card>
    <!-- 显示Element Plus 的 drawer 组件；与 drawer 双向同步；title 固定为 规划过程；size 固定为 min(760px, 96vw)。 -->
    <el-drawer v-model="drawer" title="规划过程" size="min(760px, 96vw)"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 detail 成立时渲染。 --><template v-if="detail"><!-- 显示Element Plus 的 alert 组件；title 取自 detail.run.statusLabel；type 取自 detail.run.status === 'SUCCEEDED' ? 'success' : 'info'；closable 取自 false。 --><el-alert :title="detail.run.statusLabel" :type="detail.run.status === 'SUCCEEDED' ? 'success' : 'info'" :closable="false" /><!-- 显示成长助手方案Card（AgentPlanCard）组件；按 plan in [...detail.plans].reverse() 为每条数据重复生成内容；key 取自 plan.id；plan 取自 plan；用样式类 plan-history 控制外观。 --><AgentPlanCard v-for="plan in [...detail.plans].reverse()" :key="plan.id" :plan="plan" class="plan-history" /><!-- 显示Element Plus 的 timeline 组件。 --><el-timeline><!-- 显示Element Plus 的 timeline-item 组件；按 event in detail.events 为每条数据重复生成内容；key 取自 event.id；timestamp 取自 timeText(event.createdAt)。 --><el-timeline-item v-for="event in detail.events" :key="event.id" :timestamp="timeText(event.createdAt)"><!-- 把 event.eventType 的当前值转成文字显示。 -->{{ event.eventType }}</el-timeline-item></el-timeline><!-- 显示Element Plus 的 alert 组件；仅当 detail.commit?.errorMessage 成立时渲染；title 取自 detail.commit.errorMessage；类型设为 error；closable 取自 false。 --><el-alert v-if="detail.commit?.errorMessage" :title="detail.commit.errorMessage" type="error" :closable="false" /><!-- 显示应用内部页面链接；to 取自 '/agent?conversationId=' + detail.run.conversationId。 --><RouterLink :to="'/agent?conversationId=' + detail.run.conversationId">返回对话调整或确认方案</RouterLink></template></el-drawer>
  </section>
</template>

<style scoped>
/* 以下规则应用于 h2 选中的元素。 */
h2 { /* 设置元素外侧留白；取值 0 0 8px。 */ margin:0 0 8px; /* 设置字号；取值 24px。 */ font-size:24px; /* 设置每行文字的高度；取值 32px。 */ line-height:32px; } /* 以下规则应用于 .intro 选中的元素。 */ .intro { /* 设置文字或继承该颜色的图形颜色；取值 var(—el-text-color-secondary)。 */ color:var(--el-text-color-secondary); /* 设置元素外侧留白；取值 0 0 24px。 */ margin:0 0 24px; } /* 以下规则应用于 .usage-summary 选中的元素。 */ .usage-summary { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display:flex; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap:wrap; /* 设置相邻布局条目之间的间隔；取值 24px。 */ gap:24px; /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding:16px; /* 设置下方外侧留白；取值 16px。 */ margin-bottom:16px; /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color-light)。 */ background:var(--el-fill-color-light); /* 设置边角圆弧大小；取值 8px。 */ border-radius:8px; } /* 以下规则应用于 .usage-summary strong 选中的元素。 */ .usage-summary strong { /* 设置左侧外侧留白；取值 8px。 */ margin-left:8px; } /* 以下规则应用于 .el-pagination 选中的元素。 */ .el-pagination { /* 设置上方外侧留白；取值 24px。 */ margin-top:24px; /* 设置条目在主方向上的对齐与空余空间分配；取值 flex-end。 */ justify-content:flex-end; } /* 以下规则应用于 .plan-history 选中的元素。 */ .plan-history { /* 设置元素外侧留白；取值 16px 0。 */ margin:16px 0; } /* 以下规则应用于 .el-alert 选中的元素。 */ .el-alert { /* 设置下方外侧留白；取值 16px。 */ margin-bottom:16px; }
/* 以下规则应用于 .task-error-code 选中的元素。 */
.task-error-code { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); /* 设置上方外侧留白；取值 4px。 */ margin-top: 4px; }
</style>
