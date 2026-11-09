<!-- 渲染成长助手复盘列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { onBeforeUnmount, onMounted, ref } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/agentWorkspace 提供的组件、状态函数或请求方法。 */
import { workspaceApi, timeText } from "../../api/agentWorkspace";
/* 引入 ../../components/agent/AgentMarkdown.vue 提供的组件、状态函数或请求方法。 */
import AgentMarkdown from "../../components/agent/AgentMarkdown.vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter();
/* adjustInAssistant：调用 adjustInAssistant 完成adjustInAssistant处理。 */
function adjustInAssistant(row) { /* 调用 router.push 完成push处理。 */ router.push({ /* 路径（path）字段使用固定文字 '/agent'。 */ path: '/agent', /* 查询条件（query）字段使用表达式 { reviewId: row.id } 的结果。 */ query: { /* 复盘编号（reviewId）字段使用表达式 row.id 的结果。 */ reviewId: row.id } }); }
const /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ rows = ref([]), /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ total = ref(0), /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ page = ref(1), /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ busy = ref(false), /* creating保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ creating = ref(false), /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ error = ref("");
const /* 区间（range）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ range = ref([]), /* 复盘报告（report）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ report = ref(null), /* 抽屉（drawer）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ drawer = ref(false), /* 今天（today）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ today = ref(""), /* retrying保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ retrying = ref(null);
let /* 声明 定时器（timer），稍后再填入处理结果。 */ timer, /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */ disposed = false;
const /* 名称对应表（labels）保存表达式 { PENDING: "等待分析", RUNNING: "分析中", READY: "已生成", FAILED: "生成失败" } 的结果。 */ labels = { /* 等待处理（PENDING）字段使用固定文字 '等待分析'。 */ PENDING: "等待分析", /* 运行中（RUNNING）字段使用固定文字 '分析中'。 */ RUNNING: "分析中", /* 已就绪（READY）字段使用固定文字 '已生成'。 */ READY: "已生成", /* 失败（FAILED）字段使用固定文字 '生成失败'。 */ FAILED: "生成失败" };
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { const /* 数据（data）保存等待 读取服务端数据；认证信息由公共请求层统一附带 完成后取得结果。 */ data = await workspaceApi.get("reviews", { /* 当前页（page）字段使用表达式 page.value 的结果。 */ page: page.value, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 }); /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */ if (disposed) /* 返回空内容，当前函数到此结束。 */ return; /* 把表达式 data.items 的结果写入 rows.value。 */ rows.value = data.items; /* 把表达式 data.total 的结果写入 total.value。 */ total.value = data.total; /* 检查条件 drawer.value && report.value 是否成立，成立时走这一分支。 */ if (drawer.value && report.value) /* 把优先使用前面的有效值，没有时采用后面的备用值：rows.value.find(row => row.id === report.value.id) || await workspaceApi.get(`reviews/${report.value.id}`)写入 report.value。 */ report.value = rows.value.find(row => row.id === report.value.id) || await workspaceApi.get(`reviews/${report.value.id}`); }
  /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */
  catch (e) { /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */ if (!disposed) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; } finally { /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false; }
}
/* 重试（retry）：调用 retry 完成重试处理。 */
async function retry(row) {
  /* 把表达式 row.id 的结果写入 retrying.value。 */
  retrying.value = row.id; /* 把固定文字 ''写入 error.value。 */ error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { const /* 数据（data）保存等待 提交一个业务动作或创建请求；认证信息由公共请求层统一附带 完成后取得结果。 */ data = await workspaceApi.post(`reviews/${row.id}/retry`); /* 检查条件 drawer.value && report.value?.id === row.id 是否成立，成立时走这一分支。 */ if (drawer.value && report.value?.id === row.id) /* 把数据（data）写入 report.value。 */ report.value = data; /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load(); /* 调用 ElMessage.success 完成成功处理。 */ ElMessage.success("已重新开始生成，结果会自动更新"); }
  /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */
  catch (e) { /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; }
  finally { /* 把空值，表示当前还没有对应内容写入 retrying.value。 */ retrying.value = null; }
}
/* 创建（create）：调用 create 完成创建处理。 */
async function create() {
  /* 检查条件 range.value?.length !== 2 是否成立，成立时走这一分支。 */
  if (range.value?.length !== 2) /* 返回调用 ElMessage.warning 完成warning处理，当前函数到此结束。 */ return ElMessage.warning("请选择复盘开始和结束日期");
  /* 把真，表示条件成立或开关打开写入 creating.value。 */
  creating.value = true; /* 把固定文字 ''写入 error.value。 */ error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { /* 把等待 提交一个业务动作或创建请求；认证信息由公共请求层统一附带 完成后取得结果写入 report.value。 */ report.value = await workspaceApi.post("reviews", { /* 日期来源（dateFrom）字段使用表达式 range.value[0] 的结果。 */ dateFrom: range.value[0], /* 日期To（dateTo）字段使用表达式 range.value[1] 的结果。 */ dateTo: range.value[1] }); /* 把真，表示条件成立或开关打开写入 drawer.value。 */ drawer.value = true; /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load(); /* 调用 ElMessage.success 完成成功处理。 */ ElMessage.success("已开始分析真实统计，结果会自动更新"); }
  /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */
  catch (e) { /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; } finally { /* 把假，表示条件不成立或开关关闭写入 creating.value。 */ creating.value = false; }
}
/* 设置 UTC日期（UTCDate） 的值。 */
onMounted(async () => {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { const /* 接口（{ api }）保存等待 调用 import 完成import处理 完成后取得结果。 */ { /* 直接使用已有的接口（api）作为同名字段。 */ api } = await import('../../api/http'); const /* board保存等待 调用 api 完成接口处理 完成后取得结果。 */ board = await api('/dashboard/overview'); /* 把表达式 board.today 的结果写入 today.value。 */ today.value = board.today; const /* 开始（start）保存新建 日期（Date） 实例。 */ start = new Date(`${board.today}T12:00:00Z`); /* 设置 UTC日期（UTCDate） 的值。 */ start.setUTCDate(start.getUTCDate() - 6); /* 把调用 [start.toISOString 完成toISO文字处理写入 range.value。 */ range.value = [start.toISOString().slice(0, 10), board.today]; } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; }
  /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  await load(); /* 把判断是否至少有一条数据符合条件写入 timer。 */ timer = setInterval(() => { /* 检查条件 !busy.value && !document.hidden && rows.value.some(row => ['PENDING', 'RUNNING'].includes(row.status)) 是否成立，成立时走这一分支。 */ if (!busy.value && !document.hidden && rows.value.some(row => ['PENDING', 'RUNNING'].includes(row.status))) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(); }, 4000);
});
/* 注册组件销毁前的清理，取消请求并释放定时器。 */
onBeforeUnmount(() => { /* 把真，表示条件成立或开关打开写入 disposed。 */ disposed = true; /* 停止重复执行的定时器。 */ clearInterval(timer); });
</script>

<template>
  <!-- 显示一个有独立含义的内容区。 -->
  <section>
    <!-- 显示二级标题。 -->
    <h2>成长复盘</h2><!-- 显示一段文字；用样式类 intro 控制外观。 --><p class="intro">回顾这段时间的投入、习惯和进步，找到下一步的方向。</p>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false；启用 show-icon 属性。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <!-- 显示Element Plus 的 card 组件；shadow 固定为 never。 -->
    <el-card shadow="never"><!-- 显示一个布局容器；用样式类 review-form 控制外观。 --><div class="review-form"><!-- 显示Element Plus 的 date-picker 组件；与 range 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；start-placeholder 固定为 开始日期；end-placeholder 固定为 结束日期；disabled-date 取自 date => today && date.getTime() > new Date(`${today}T23:59:59`).getTime()。 --><el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :disabled-date="date => today && date.getTime() > new Date(`${today}T23:59:59`).getTime()" /><!-- 显示Element Plus 的 button 组件；仅当 canGrow('agent:review:manage') 成立时渲染；类型设为 primary；loading 取自 creating；click 事件触发时执行 create。 --><el-button v-if="canGrow('agent:review:manage')" type="primary" :loading="creating" @click="create">生成复盘</el-button><!-- 显示Element Plus 的 button 组件；loading 取自 busy；click 事件触发时执行 load。 --><el-button :loading="busy" @click="load">刷新</el-button></div>
      <!-- 显示Element Plus 的 skeleton 组件；仅当 busy && !rows.length 成立时渲染；rows 取自 5；启用 animated 属性。 -->
      <el-skeleton v-if="busy && !rows.length" :rows="5" animated />
      <!-- 显示Element Plus 的 table 组件；前面条件均不成立时显示这个备用分支；data 取自 rows；empty-text 固定为 暂无复盘，选择日期后生成第一份。 -->
      <el-table v-else :data="rows" empty-text="暂无复盘，选择日期后生成第一份"><!-- 显示Element Plus 的 table-column 组件；label 固定为 日期区间；min-width 固定为 240。 --><el-table-column label="日期区间" min-width="240"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 row.dateFrom 的当前值转成文字显示。 -->{{ row.dateFrom }} 至 <!-- 把 row.dateTo 的当前值转成文字显示。 -->{{ row.dateTo }}</template></el-table-column><!-- 显示Element Plus 的 table-column 组件；prop 固定为 timeZone；label 固定为 统计时区；min-width 固定为 160。 --><el-table-column prop="timeZone" label="统计时区" min-width="160" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 128。 --><el-table-column label="状态" width="128"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 tag 组件；type 取自 row.status === 'READY' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'。 --><el-tag :type="row.status === 'READY' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'"><!-- 把 labels[row.status] 的当前值转成文字显示。 -->{{ labels[row.status] }}</el-tag></template></el-table-column><!-- 显示Element Plus 的 table-column 组件；label 固定为 创建时间；min-width 固定为 184。 --><el-table-column label="创建时间" min-width="184"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 timeText(row.createdAt) 的当前值转成文字显示。 -->{{ timeText(row.createdAt) }}</template></el-table-column><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 260。 --><el-table-column label="操作" width="260"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 button 组件；类型设为 primary；启用 link 属性；click 事件触发时执行 report = row; drawer = true。 --><el-button type="primary" link @click="report = row; drawer = true">查看</el-button><!-- 显示Element Plus 的 button 组件；仅当 row.status === 'READY' && canGrow('agent:workflow:manage') 成立时渲染；类型设为 primary；启用 link 属性；click 事件触发时执行 adjustInAssistant(row)。 --><el-button v-if="row.status === 'READY' && canGrow('agent:workflow:manage')" type="primary" link @click="adjustInAssistant(row)">带到助手调整</el-button><!-- 显示Element Plus 的 button 组件；仅当 row.status === 'FAILED' && canGrow('agent:review:manage') 成立时渲染；类型设为 warning；启用 link 属性；loading 取自 retrying === row.id；click 事件触发时执行 retry(row)。 --><el-button v-if="row.status === 'FAILED' && canGrow('agent:review:manage')" type="warning" link :loading="retrying === row.id" @click="retry(row)">重新生成</el-button></template></el-table-column></el-table>
      <!-- 显示Element Plus 的 pagination 组件；与 page 双向同步（current-page）；page-size 取自 20；total 取自 total；layout 固定为 total, prev, pager, next；current-change 事件触发时执行 load。 -->
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </el-card>
    <!-- 显示Element Plus 的 drawer 组件；与 drawer 双向同步；title 固定为 成长复盘；size 固定为 min(800px, 96vw)。 -->
    <el-drawer v-model="drawer" title="成长复盘" size="min(800px, 96vw)"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 report 成立时渲染。 --><template v-if="report"><!-- 显示一段文字。 --><p><!-- 把 report.dateFrom 的当前值转成文字显示。 -->{{ report.dateFrom }} 至 <!-- 把 report.dateTo 的当前值转成文字显示。 -->{{ report.dateTo }} · <!-- 把 report.timeZone 的当前值转成文字显示。 -->{{ report.timeZone }}</p><!-- 显示Element Plus 的 alert 组件；仅当 report.errorMessage 成立时渲染；title 取自 report.errorMessage；类型设为 error；closable 取自 false。 --><el-alert v-if="report.errorMessage" :title="report.errorMessage" type="error" :closable="false" /><!-- 显示一个布局容器；仅当 report.status === 'FAILED' && canGrow('agent:review:manage') 成立时渲染；用样式类 review-retry 控制外观。 --><div v-if="report.status === 'FAILED' && canGrow('agent:review:manage')" class="review-retry"><!-- 显示Element Plus 的 button 组件；类型设为 warning；loading 取自 retrying === report.id；click 事件触发时执行 retry(report)。 --><el-button type="warning" :loading="retrying === report.id" @click="retry(report)">重新生成复盘</el-button></div><!-- 显示Element Plus 的 skeleton 组件；仅当 ['PENDING', 'RUNNING'].includes(report.status) 成立时渲染；rows 取自 8；启用 animated 属性。 --><el-skeleton v-if="['PENDING', 'RUNNING'].includes(report.status)" :rows="8" animated /><!-- 显示成长助手Markdown（AgentMarkdown）组件；前一条件不成立且 report.content 成立时渲染；content 取自 report.content。 --><AgentMarkdown v-else-if="report.content" :content="report.content" /><!-- 显示Element Plus 的 empty 组件；前面条件均不成立时显示这个备用分支；description 固定为 暂未生成内容，可重新生成。 --><el-empty v-else description="暂未生成内容，可重新生成" /><!-- 显示Element Plus 的 button 组件；仅当 report.status === 'READY' && canGrow('agent:workflow:manage') 成立时渲染；类型设为 primary；click 事件触发时执行 adjustInAssistant(report)。 --><el-button v-if="report.status === 'READY' && canGrow('agent:workflow:manage')" type="primary" @click="adjustInAssistant(report)">带到成长助手调整计划</el-button><!-- 显示Element Plus 的 collapse 组件。 --><el-collapse><!-- 显示Element Plus 的 collapse-item 组件；title 固定为 查看本次分析依据的统计快照；name 固定为 facts。 --><el-collapse-item title="查看本次分析依据的统计快照" name="facts"><!-- 显示提前（pre）组件；用样式类 facts 控制外观。 --><pre class="facts"><!-- 把 JSON.stringify(report.facts, null, 2) 的当前值转成文字显示。 -->{{ JSON.stringify(report.facts, null, 2) }}</pre></el-collapse-item></el-collapse></template></el-drawer>
  </section>
</template>

<style scoped>
/* 以下规则应用于 h2 选中的元素。 */
h2 { /* 设置元素外侧留白；取值 0 0 8px。 */ margin:0 0 8px; /* 设置字号；取值 24px。 */ font-size:24px; /* 设置每行文字的高度；取值 32px。 */ line-height:32px; } /* 以下规则应用于 .intro 选中的元素。 */ .intro { /* 设置文字或继承该颜色的图形颜色；取值 var(—el-text-color-secondary)。 */ color:var(--el-text-color-secondary); /* 设置元素外侧留白；取值 0 0 24px。 */ margin:0 0 24px; } /* 以下规则应用于 .review-form 选中的元素。 */ .review-form { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display:flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items:center; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap:wrap; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap:12px; /* 设置下方外侧留白；取值 24px。 */ margin-bottom:24px; } /* 以下规则应用于 .el-pagination 选中的元素。 */ .el-pagination { /* 设置上方外侧留白；取值 24px。 */ margin-top:24px; /* 设置条目在主方向上的对齐与空余空间分配；取值 flex-end。 */ justify-content:flex-end; } /* 以下规则应用于 .facts 选中的元素。 */ .facts { /* 决定内容超出边界时显示、裁切还是滚动；取值 auto。 */ overflow:auto; /* 决定空白和换行如何保留；取值 pre-wrap。 */ white-space:pre-wrap; /* 设置 CSS 属性 overflow-wrap；取值 anywhere。 */ overflow-wrap:anywhere; /* 设置字号；取值 12px。 */ font-size:12px; /* 设置每行文字的高度；取值 20px。 */ line-height:20px; /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding:16px; /* 设置边角圆弧大小；取值 8px。 */ border-radius:8px; /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color-light)。 */ background:var(--el-fill-color-light); } /* 以下规则应用于 .el-alert 选中的元素。 */ .el-alert { /* 设置下方外侧留白；取值 16px。 */ margin-bottom:16px; } /* 以下规则应用于 .review-retry 选中的元素。 */ .review-retry { /* 设置元素外侧留白；取值 0 0 16px。 */ margin:0 0 16px; }
</style>
