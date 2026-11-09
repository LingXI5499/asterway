<!-- 渲染成长助手长期记忆列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { onMounted, reactive, ref } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage, ElMessageBox } from "element-plus";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/agentWorkspace 提供的组件、状态函数或请求方法。 */
import { workspaceApi, timeText } from "../../api/agentWorkspace";

const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = reactive({ /* 状态（status）字段使用固定文字 ''。 */ status: "", /* 种类（kind）字段使用固定文字 ''。 */ kind: "", /* keyword字段使用固定文字 ''。 */ keyword: "", /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 });
const /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ rows = ref([]), /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ total = ref(0), /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ busy = ref(false), /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ error = ref("");
const /* 对话框（dialog）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ dialog = ref(false), /* saving保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ saving = ref(false), /* editing保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ editing = ref(null);
const /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ form = reactive({ /* 种类（kind）字段使用固定状态或编码 PREFERENCE（PREFERENCE）。 */ kind: "PREFERENCE", /* 内容（content）字段使用固定文字 ''。 */ content: "" });
const /* kinds保存表达式 { PREFERENCE: "偏好", FACT: "事实", CONSTRAINT: "约束", GOAL_CONTEXT: "目标背景" } 的结果。 */ kinds = { /* PREFERENCE字段使用固定文字 '偏好'。 */ PREFERENCE: "偏好", /* FACT字段使用固定文字 '事实'。 */ FACT: "事实", /* 约束（CONSTRAINT）字段使用固定文字 '约束'。 */ CONSTRAINT: "约束", /* 目标上下文（GOAL_CONTEXT）字段使用固定文字 '目标背景'。 */ GOAL_CONTEXT: "目标背景" };
const /* statuses保存表达式 { CANDIDATE: "待确认", CONFIRMED: "已确认", REJECTED: "已拒绝", ARCHIVED: "已归档" } 的结果。 */ statuses = { /* 候选（CANDIDATE）字段使用固定文字 '待确认'。 */ CANDIDATE: "待确认", /* 已确认（CONFIRMED）字段使用固定文字 '已确认'。 */ CONFIRMED: "已确认", /* REJECTED字段使用固定文字 '已拒绝'。 */ REJECTED: "已拒绝", /* 已归档（ARCHIVED）字段使用固定文字 '已归档'。 */ ARCHIVED: "已归档" };
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load(reset = false) {
  /* 检查条件 reset 是否成立，成立时走这一分支。 */
  if (reset) /* 把固定数值 1写入 filters.page。 */ filters.page = 1;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true; /* 把固定文字 ''写入 error.value。 */ error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try { const /* 数据（data）保存等待 读取服务端数据；认证信息由公共请求层统一附带 完成后取得结果。 */ data = await workspaceApi.get("memories", filters); /* 把表达式 data.items 的结果写入 rows.value。 */ rows.value = data.items; /* 把表达式 data.total 的结果写入 total.value。 */ total.value = data.total; }
  /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */
  catch (e) { /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; } finally { /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false; }
}
/* edit：调用 edit 完成edit处理。 */
function edit(row = null) { /* 把当前记录（row）写入 editing.value。 */ editing.value = row; /* 把沿 row?.kind || "PREFERENCE" 读取数据；中间对象为空时返回空值写入 form.kind。 */ form.kind = row?.kind || "PREFERENCE"; /* 把沿 row?.content || "" 读取数据；中间对象为空时返回空值写入 form.content。 */ form.content = row?.content || ""; /* 把真，表示条件成立或开关打开写入 dialog.value。 */ dialog.value = true; }
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 检查条件 !form.content.trim() 是否成立，成立时走这一分支。 */
  if (!form.content.trim()) /* 返回调用 ElMessage.warning 完成warning处理，当前函数到此结束。 */ return ElMessage.warning("请填写要保留的内容");
  /* 把真，表示条件成立或开关打开写入 saving.value。 */
  saving.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 editing.value 是否成立，成立时走这一分支。 */
    if (editing.value) /* 等待提交部分字段修改；认证信息由公共请求层统一附带。 */ await workspaceApi.patch(`memories/${editing.value.id}`, { /* 内容（content）字段使用去掉文字两端的空白。 */ content: form.content.trim(), /* 版本号（version）字段使用表达式 editing.value.version 的结果。 */ version: editing.value.version });
    else /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */ await workspaceApi.post("memories/candidates", { /* 种类（kind）字段使用表达式 form.kind 的结果。 */ kind: form.kind, /* 内容（content）字段使用去掉文字两端的空白。 */ content: form.content.trim(), /* 来源（source）字段使用固定状态或编码 USER_STATED（用户STATED）。 */ source: "USER_STATED", /* confidence字段使用固定数值 1。 */ confidence: 1 });
    /* 把假，表示条件不成立或开关关闭写入 dialog.value。 */
    dialog.value = false; /* 调用 ElMessage.success 完成成功处理。 */ ElMessage.success("已保存，待确认的内容需要确认后才会使用"); /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 调用 ElMessage.error 完成错误处理。 */ ElMessage.error(e.message); } finally { /* 把假，表示条件不成立或开关关闭写入 saving.value。 */ saving.value = false; }
}
/* 操作（action）：调用 action 完成操作处理。 */
async function action(row, kind) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 kind === "delete" 是否成立，成立时走这一分支。 */
    if (kind === "delete") { /* 等待调用 ElMessageBox.confirm 完成确认处理。 */ await ElMessageBox.confirm("删除后，此内容不再参与之后的对话。", "删除记忆", { /* 类型（type）字段使用固定文字 'warning'。 */ type: "warning" }); /* 等待调用 workspaceApi.remove 完成移除处理。 */ await workspaceApi.remove(`memories/${row.id}`, row.version); }
    else /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */ await workspaceApi.post(`memories/${row.id}/${kind}`, { /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version });
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("记忆状态已更新"); /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 检查条件 e?.message 是否成立，成立时走这一分支。 */ if (e?.message) /* 调用 ElMessage.error 完成错误处理。 */ ElMessage.error(e.message); }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>

<template>
  <!-- 显示一个有独立含义的内容区；用样式类 assistant-workspace 控制外观。 -->
  <section class="assistant-workspace">
    <!-- 显示一个布局容器；用样式类 assistant-heading 控制外观。 -->
    <div class="assistant-heading"><!-- 显示一个布局容器。 --><div><!-- 显示二级标题。 --><h2>长期记忆</h2><!-- 显示一段文字。 --><p>确认适合长期保留的偏好和约束，随时修改、归档或删除。</p></div><!-- 显示Element Plus 的 button 组件；仅当 canGrow('agent:memory:manage') 成立时渲染；类型设为 primary；click 事件触发时执行 edit()。 --><el-button v-if="canGrow('agent:memory:manage')" type="primary" @click="edit()">新增记忆</el-button></div>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；启用 show-icon 属性；closable 取自 false。 -->
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <!-- 显示Element Plus 的 card 组件；shadow 固定为 never。 -->
    <el-card shadow="never">
      <!-- 显示Element Plus 的 form 组件；启用 inline 属性；submit 事件触发时执行 load(true)。 -->
      <el-form inline @submit.prevent="load(true)">
        <!-- 显示Element Plus 的 form-item 组件。 -->
        <el-form-item><!-- 显示Element Plus 的 input 组件；与 filters.keyword 双向同步；未输入时提示 搜索记忆内容；启用 clearable 属性。 --><el-input v-model="filters.keyword" placeholder="搜索记忆内容" clearable /></el-form-item>
        <!-- 显示Element Plus 的 form-item 组件。 -->
        <el-form-item><!-- 显示Element Plus 的 select 组件；与 filters.status 双向同步；未输入时提示 全部状态；启用 clearable 属性；使用这里给定的行内样式。 --><el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 144px"><!-- 显示Element Plus 的 option 组件；按 (name, key) in statuses 为每条数据重复生成内容；key 取自 key；label 取自 name；value 取自 key。 --><el-option v-for="(name, key) in statuses" :key="key" :label="name" :value="key" /></el-select></el-form-item>
        <!-- 显示Element Plus 的 form-item 组件。 -->
        <el-form-item><!-- 显示Element Plus 的 select 组件；与 filters.kind 双向同步；未输入时提示 全部类型；启用 clearable 属性；使用这里给定的行内样式。 --><el-select v-model="filters.kind" placeholder="全部类型" clearable style="width: 144px"><!-- 显示Element Plus 的 option 组件；按 (name, key) in kinds 为每条数据重复生成内容；key 取自 key；label 取自 name；value 取自 key。 --><el-option v-for="(name, key) in kinds" :key="key" :label="name" :value="key" /></el-select></el-form-item>
        <!-- 显示Element Plus 的 form-item 组件。 -->
        <el-form-item><!-- 显示Element Plus 的 button 组件；native-type 固定为 submit；loading 取自 busy。 --><el-button native-type="submit" :loading="busy">查询</el-button><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 Object.assign(filters, { status: '', kind: '', keyword: '' }); load(true)。 --><el-button @click="Object.assign(filters, { status: '', kind: '', keyword: '' }); load(true)">重置</el-button></el-form-item>
      </el-form>
      <!-- 显示Element Plus 的 skeleton 组件；仅当 busy && !rows.length 成立时渲染；rows 取自 5；启用 animated 属性。 -->
      <el-skeleton v-if="busy && !rows.length" :rows="5" animated />
      <!-- 显示Element Plus 的 table 组件；前面条件均不成立时显示这个备用分支；data 取自 rows；empty-text 取自 error ? '加载失败，可重新查询' : '暂无记忆，可以新增或在对话后确认候选'。 -->
      <el-table v-else :data="rows" :empty-text="error ? '加载失败，可重新查询' : '暂无记忆，可以新增或在对话后确认候选'">
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 内容；min-width 固定为 300。 -->
        <el-table-column label="内容" min-width="300"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示一个布局容器；用样式类 memory-content 控制外观。 --><div class="memory-content"><!-- 把 row.content 的当前值转成文字显示。 -->{{ row.content }}</div><!-- 显示辅助说明文字。 --><small><!-- 把 kinds[row.kind] 的当前值转成文字显示。 -->{{ kinds[row.kind] }} · <!-- 把 row.source === 'INFERRED' ? '对话提取，等待你确认' : '由你提供' 的当前值转成文字显示。 -->{{ row.source === 'INFERRED' ? '对话提取，等待你确认' : '由你提供' }}</small></template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 112。 -->
        <el-table-column label="状态" width="112"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 显示Element Plus 的 tag 组件；type 取自 row.status === 'CONFIRMED' ? 'success' : 'info'。 --><el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'info'"><!-- 把 statuses[row.status] 的当前值转成文字显示。 -->{{ statuses[row.status] }}</el-tag></template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 更新时间；width 固定为 184。 -->
        <el-table-column label="更新时间" width="184"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 timeText(row.updatedAt) 的当前值转成文字显示。 -->{{ timeText(row.updatedAt) }}</template></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；仅当 canGrow('agent:memory:manage') 成立时渲染；label 固定为 操作；min-width 固定为 240。 -->
        <el-table-column v-if="canGrow('agent:memory:manage')" label="操作" min-width="240"><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }">
          <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'CANDIDATE' 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 action(row, 'confirm')。 -->
          <el-button v-if="row.status === 'CANDIDATE'" link type="primary" @click="action(row, 'confirm')">确认</el-button>
          <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'CANDIDATE' 成立时渲染；启用 link 属性；click 事件触发时执行 action(row, 'reject')。 -->
          <el-button v-if="row.status === 'CANDIDATE'" link @click="action(row, 'reject')">拒绝</el-button>
          <!-- 显示Element Plus 的 button 组件；仅当 ['CANDIDATE', 'CONFIRMED'].includes(row.status) 成立时渲染；启用 link 属性；click 事件触发时执行 edit(row)。 -->
          <el-button v-if="['CANDIDATE', 'CONFIRMED'].includes(row.status)" link @click="edit(row)">编辑</el-button>
          <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'CONFIRMED' 成立时渲染；启用 link 属性；click 事件触发时执行 action(row, 'archive')。 -->
          <el-button v-if="row.status === 'CONFIRMED'" link @click="action(row, 'archive')">归档</el-button>
          <!-- 显示Element Plus 的 button 组件；启用 link 属性；类型设为 danger；click 事件触发时执行 action(row, 'delete')。 -->
          <el-button link type="danger" @click="action(row, 'delete')">删除</el-button>
        </template></el-table-column>
      </el-table>
      <!-- 显示Element Plus 的 pagination 组件；与 filters.page 双向同步（current-page）；page-size 取自 filters.size；total 取自 total；layout 固定为 total, prev, pager, next；current-change 事件触发时执行 load()。 -->
      <el-pagination v-model:current-page="filters.page" :page-size="filters.size" :total="total" layout="total, prev, pager, next" @current-change="load()" />
    </el-card>
    <!-- 显示Element Plus 的 dialog 组件；与 dialog 双向同步；title 取自 editing ? '编辑记忆' : '新增记忆候选'；width 固定为 min(560px, 94vw)。 -->
    <el-dialog v-model="dialog" :title="editing ? '编辑记忆' : '新增记忆候选'" width="min(560px, 94vw)">
      <!-- 显示Element Plus 的 form 组件；label-position 固定为 top；submit 事件触发时执行 save。 -->
      <el-form label-position="top" @submit.prevent="save">
        <!-- 显示Element Plus 的 form-item 组件；label 固定为 记忆类型。 -->
        <el-form-item label="记忆类型"><!-- 显示Element Plus 的 select 组件；与 form.kind 双向同步；disabled 取自 !!editing。 --><el-select v-model="form.kind" :disabled="!!editing"><!-- 显示Element Plus 的 option 组件；按 (name, key) in kinds 为每条数据重复生成内容；key 取自 key；label 取自 name；value 取自 key。 --><el-option v-for="(name, key) in kinds" :key="key" :label="name" :value="key" /></el-select></el-form-item>
        <!-- 显示Element Plus 的 form-item 组件；label 固定为 记忆内容；启用 required 属性。 -->
        <el-form-item label="记忆内容" required><!-- 显示Element Plus 的 input 组件；与 form.content 双向同步；类型设为 textarea；rows 取自 5；maxlength 取自 4000；启用 show-word-limit 属性；未输入时提示 例如：我习惯在晚上安排阅读任务。 --><el-input v-model="form.content" type="textarea" :rows="5" :maxlength="4000" show-word-limit placeholder="例如：我习惯在晚上安排阅读任务" /></el-form-item>
      </el-form>
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 -->
      <template #footer><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 dialog = false。 --><el-button @click="dialog = false">取消</el-button><!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 saving；click 事件触发时执行 save。 --><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
/* 以下规则应用于 .assistant-heading 选中的元素。 */
.assistant-heading { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display:flex; /* 设置相邻布局条目之间的间隔；取值 16px。 */ gap:16px; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content:space-between; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items:center; /* 设置下方外侧留白；取值 24px。 */ margin-bottom:24px; }
/* 以下规则应用于 h2 选中的元素。 */
h2 { /* 设置元素外侧留白；取值 0 0 8px。 */ margin:0 0 8px; /* 设置字号；取值 24px。 */ font-size:24px; /* 设置每行文字的高度；取值 32px。 */ line-height:32px; } /* 以下规则应用于 p, small 选中的元素。 */ p, small { /* 设置文字或继承该颜色的图形颜色；取值 var(—el-text-color-secondary)。 */ color:var(--el-text-color-secondary); } /* 以下规则应用于 p 选中的元素。 */ p { /* 设置元素外侧留白；取值 0。 */ margin:0; } /* 以下规则应用于 .memory-content 选中的元素。 */ .memory-content { /* 决定空白和换行如何保留；取值 pre-wrap。 */ white-space:pre-wrap; /* 设置每行文字的高度；取值 24px。 */ line-height:24px; /* 设置下方外侧留白；取值 4px。 */ margin-bottom:4px; }
/* 以下规则应用于 .el-alert 选中的元素。 */
.el-alert { /* 设置下方外侧留白；取值 16px。 */ margin-bottom:16px; } /* 以下规则应用于 .el-pagination 选中的元素。 */ .el-pagination { /* 设置上方外侧留白；取值 24px。 */ margin-top:24px; /* 设置条目在主方向上的对齐与空余空间分配；取值 flex-end。 */ justify-content:flex-end; }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width:640px)。 */
@media(max-width:640px) { /* 以下规则应用于 .assistant-heading 选中的元素。 */ .assistant-heading { /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 flex-start。 */ align-items:flex-start; /* 设置 Flex 条目的排列方向；取值 column。 */ flex-direction:column; } }
</style>
