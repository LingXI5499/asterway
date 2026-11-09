<!-- 在聊天旁边持续展示方案、提交结果、复盘和实际进度。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed, ref } from "vue";
/* 引入 ../../composables/useConversationPlan 提供的组件、状态函数或请求方法。 */
import { useConversationPlan } from "../../composables/useConversationPlan";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ./AgentPlanCard.vue 提供的组件、状态函数或请求方法。 */
import AgentPlanCard from "./AgentPlanCard.vue";
/* 引入 ./AgentReviewCard.vue 提供的组件、状态函数或请求方法。 */
import AgentReviewCard from "./AgentReviewCard.vue";
/* 引入 ./AgentActualProgress.vue 提供的组件、状态函数或请求方法。 */
import AgentActualProgress from "./AgentActualProgress.vue";

const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 对话编号（conversationId）字段使用可为空的文字（String）。 */ conversationId: String, /* 事件（event）字段使用对象（Object）。 */ event: Object, /* 复盘事件（reviewEvent）字段使用对象（Object）。 */ reviewEvent: Object, /* external复盘编号（externalReviewId）字段使用可为空的文字（String）。 */ externalReviewId: String, /* 正在处理（busy）字段使用可为空的真假值（Boolean）。 */ busy: Boolean, /* 是否展开（open）字段使用可为空的真假值（Boolean）。 */ open: Boolean, /* 刷新键（refreshKey）字段使用数值（Number）。 */ refreshKey: Number });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["close", "available", "reveal", "revise", "working"]);
const /* 复盘操作进行中（reviewActing）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ reviewActing = ref(false);
const /* 详情方案运行方案提交回执进度当前步骤已成功正在加载操作进行中错误目标列表进度错误实际进度是否可以管理是否可以接受是否可以重试等待确认加载接受暂不接受（{ detail, run, plan, commit, progress, phase, succeeded, loading, acting, error, goals, progressError, tracking, canManage, canAccept, canRetry, waiting, load, accept, decline }）保存组合方案加载、确认、重试和真实进度跟踪状态。 */ { /* 直接使用已有的详情（detail）作为同名字段。 */ detail, /* 直接使用已有的方案运行（run）作为同名字段。 */ run, /* 直接使用已有的方案（plan）作为同名字段。 */ plan, /* 直接使用已有的提交回执（commit）作为同名字段。 */ commit, /* 直接使用已有的进度（progress）作为同名字段。 */ progress, /* 直接使用已有的当前步骤（phase）作为同名字段。 */ phase, /* 直接使用已有的已成功（succeeded）作为同名字段。 */ succeeded, /* 直接使用已有的正在加载（loading）作为同名字段。 */ loading, /* 直接使用已有的操作进行中（acting）作为同名字段。 */ acting, /* 直接使用已有的错误（error）作为同名字段。 */ error, /* 直接使用已有的目标列表（goals）作为同名字段。 */ goals,
  /* 直接使用已有的进度错误（progressError）作为同名字段。 */
  progressError, /* 直接使用已有的实际进度（tracking）作为同名字段。 */ tracking, /* 直接使用已有的是否有方案管理权限（canManage）作为同名字段。 */ canManage, /* 直接使用已有的是否允许接受当前方案（canAccept）作为同名字段。 */ canAccept, /* 直接使用已有的是否允许重试已确认的提交（canRetry）作为同名字段。 */ canRetry, /* 直接使用已有的等待确认（waiting）作为同名字段。 */ waiting, /* 直接使用已有的加载（load）作为同名字段。 */ load, /* 直接使用已有的接受（accept）作为同名字段。 */ accept, /* 直接使用已有的暂不接受（decline）作为同名字段。 */ decline } = useConversationPlan(props, emit);
const /* updating保存随相关状态变化自动重算的值。 */ updating = computed(() => plan.value?.snapshot?.taskAssignments?.some(item => item.taskId));
/* 复盘Working（reviewWorking）：调用 reviewWorking 完成复盘Working处理。 */
function reviewWorking(value) { /* 把值（value）写入 reviewActing.value。 */ reviewActing.value = value; /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit('working', value); }
/* adjusted：调用 adjusted 完成adjusted处理。 */
async function adjusted(data) { /* 检查条件 data?.run?.id 是否成立，成立时走这一分支。 */ if (data?.run?.id) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load(data.run.id); /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit('reveal'); }
const /* 名称对应表（labels）保存表达式 { EMPTY: "从一个目标开始", DRAFT: "沟通需求", COLLECTING: "等待你补充", CONTEXT: "核对已有安排", GENERATING: "整理完整方案", WAITING_CONFIRM: "等待你接受… 的结果。 */ labels = { /* 空状态（EMPTY）字段使用固定文字 '从一个目标开始'。 */ EMPTY: "从一个目标开始", /* 草案（DRAFT）字段使用固定文字 '沟通需求'。 */ DRAFT: "沟通需求", /* COLLECTING字段使用固定文字 '等待你补充'。 */ COLLECTING: "等待你补充", /* 上下文（CONTEXT）字段使用固定文字 '核对已有安排'。 */ CONTEXT: "核对已有安排",
  /* GENERATING字段使用固定文字 '整理完整方案'。 */
  GENERATING: "整理完整方案", /* 等待确认确认（WAITING_CONFIRM）字段使用固定文字 '等待你接受'。 */ WAITING_CONFIRM: "等待你接受", /* 已确认（CONFIRMED）字段使用固定文字 '方案已接受'。 */ CONFIRMED: "方案已接受", /* 正在执行（EXECUTING）字段使用固定文字 '正在创建并核对'。 */ EXECUTING: "正在创建并核对",
  /* 已成功（SUCCEEDED）字段使用固定文字 '方案已执行'。 */
  SUCCEEDED: "方案已执行", /* 失败（FAILED）字段使用固定文字 '提交未完成'。 */ FAILED: "提交未完成", /* 错误（ERROR）字段使用固定文字 '整理暂未完成'。 */ ERROR: "整理暂未完成", /* INTERRUPTED字段使用固定文字 '整理已停止'。 */ INTERRUPTED: "整理已停止", /* 已取消（CANCELLED）字段使用固定文字 '暂未接受'。 */ CANCELLED: "暂未接受", /* 已过期（EXPIRED）字段使用固定文字 '方案已过期'。 */ EXPIRED: "方案已过期" };
const /* step索引（stepIndex）保存随相关状态变化自动重算的值。 */ stepIndex = computed(() => succeeded.value ? 4 : ["CONFIRMED", "EXECUTING", "FAILED"].includes(phase.value) ? 3
  : waiting.value ? 2 : ["CONTEXT", "GENERATING"].includes(phase.value) || plan.value ? 1 : 0);
const /* steps保存表达式 ["明确目标", "整理方案", "接受方案", "提交并核对"] 的结果。 */ steps = ["明确目标", "整理方案", "接受方案", "提交并核对"];
const /* 历史（history）保存随相关状态变化自动重算的值。 */ history = computed(() => (progress.value.steps || []).slice(-8));
const /* 历史显示名称（historyLabel）保存优先使用前面的有效值，没有时采用后面的备用值：phase => ({ COLLECTING: "补齐需求", CONTEXT: "核对现状", GENERATING: "生成方案", WAITING_CONFIRM: "方案就绪", ERROR: "整理失败", I…。 */ historyLabel = phase => ({ /* COLLECTING字段使用固定文字 '补齐需求'。 */ COLLECTING: "补齐需求", /* 上下文（CONTEXT）字段使用固定文字 '核对现状'。 */ CONTEXT: "核对现状", /* GENERATING字段使用固定文字 '生成方案'。 */ GENERATING: "生成方案", /* 等待确认确认（WAITING_CONFIRM）字段使用固定文字 '方案就绪'。 */ WAITING_CONFIRM: "方案就绪", /* 错误（ERROR）字段使用固定文字 '整理失败'。 */ ERROR: "整理失败", /* INTERRUPTED字段使用固定文字 '已停止'。 */ INTERRUPTED: "已停止" })[phase] || phase;
const /* 是否可以暂不接受（canDecline）保存随相关状态变化自动重算的值。 */ canDecline = computed(() => canManage.value && ["DRAFT", "WAITING_CONFIRM", "CONFIRMED"].includes(run.value?.status));
</script>

<template>
  <!-- 显示侧边区域；由 open 决定显示或隐藏，组件仍保留；用样式类 agent-plan-workspace 控制外观；aria-label 提供无障碍说明 方案与进度。 -->
  <aside v-show="open" class="agent-plan-workspace" aria-label="方案与进度">
    <!-- 显示由父组件补充内容的插槽；name 固定为 resize。 -->
    <slot name="resize" />
    <!-- 显示顶部区域；用样式类 plan-workspace-head 控制外观。 -->
    <header class="plan-workspace-head"><!-- 显示一个布局容器。 --><div><!-- 显示应用Icon（AppIcon）组件；name 固定为 goal；size 取自 18。 --><AppIcon name="goal" :size="18" /><!-- 显示二级标题。 --><h2>方案与进度</h2></div><!-- 显示可点击按钮；类型设为 button；用样式类 plan-close 控制外观；aria-label 提供无障碍说明 收起方案面板；click 事件触发时执行 emit('close')。 --><button type="button" class="plan-close" aria-label="收起方案面板" @click="emit('close')"><!-- 显示应用Icon（AppIcon）组件；name 固定为 close；size 取自 18。 --><AppIcon name="close" :size="18" /></button></header>
    <!-- 显示一个布局容器；用样式类 plan-workspace-body 控制外观。 -->
    <div class="plan-workspace-body">
      <!-- 显示一个布局容器；用样式类 plan-phase 控制外观；role 固定为 status。 -->
      <div class="plan-phase" role="status"><!-- 显示行内文字区域；用样式类 phase-dot 控制外观；class 取自 { running: ['CONTEXT', 'GENERATING', 'EXECUTING'].includes(phase) }。 --><span class="phase-dot" :class="{ running: ['CONTEXT', 'GENERATING', 'EXECUTING'].includes(phase) }"></span><!-- 把 labels[phase] || run?.statusLabel || '沟通需求' 的当前值转成文字显示。 -->{{ labels[phase] || run?.statusLabel || '沟通需求' }}<!-- 显示行内文字区域；仅当 plan 成立时渲染；用样式类 plan-version 控制外观。 --><span v-if="plan" class="plan-version">第 <!-- 把 plan.versionNo 的当前值转成文字显示。 -->{{ plan.versionNo }} 版</span></div>
      <!-- 显示有序列表；用样式类 plan-route 控制外观；aria-label 提供无障碍说明 方案处理进度。 -->
      <ol class="plan-route" aria-label="方案处理进度"><!-- 显示一项列表内容；按 (step, index) in steps 为每条数据重复生成内容；key 取自 step；class 取自 { done: stepIndex > index, current: stepIndex === index }。 --><li v-for="(step, index) in steps" :key="step" :class="{ done: stepIndex > index, current: stepIndex === index }"><!-- 显示行内文字区域。 --><span><!-- 显示应用Icon（AppIcon）组件；仅当 stepIndex > index 成立时渲染；name 固定为 check；size 取自 12。 --><AppIcon v-if="stepIndex > index" name="check" :size="12" /><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 --><template v-else><!-- 把 index + 1 的当前值转成文字显示。 -->{{ index + 1 }}</template></span><!-- 显示辅助说明文字。 --><small><!-- 把 step 的当前值转成文字显示。 -->{{ step }}</small></li></ol>
      <!-- 显示一个布局容器；仅当 !run && !event && !loading 成立时渲染；用样式类 plan-get-started 控制外观。 -->
      <div v-if="!run && !event && !loading" class="plan-get-started"><!-- 显示应用Icon（AppIcon）组件；name 固定为 chat；size 取自 32。 --><AppIcon name="chat" :size="32" /><!-- 显示三级标题。 --><h3>我们边聊边完善</h3><!-- 显示一段文字。 --><p>告诉助手你想达到什么目标。时间、基础和安排会在对话中逐步补齐，完整方案会显示在这里。</p><!-- 显示一段文字。 --><p>核对之后，点击接受再创建。</p></div>
      <!-- 显示Element Plus 的 skeleton 组件；仅当 loading && !plan 成立时渲染；rows 取自 5；启用 animated 属性。 -->
      <el-skeleton v-if="loading && !plan" :rows="5" animated />
      <!-- 显示一个布局容器；仅当 progress.objective && !plan 成立时渲染；用样式类 plan-understanding 控制外观。 -->
      <div v-if="progress.objective && !plan" class="plan-understanding"><!-- 显示行内文字区域。 --><span>当前目标</span><!-- 显示三级标题。 --><h3><!-- 把 progress.objective 的当前值转成文字显示。 -->{{ progress.objective }}</h3><!-- 显示一段文字。 --><p><!-- 把 progress.summary 的当前值转成文字显示。 -->{{ progress.summary }}</p></div>
      <!-- 显示一个布局容器；仅当 progress.questions?.length 成立时渲染；用样式类 plan-questions 控制外观。 -->
      <div v-if="progress.questions?.length" class="plan-questions"><!-- 显示三级标题。 --><h3>还需要了解</h3><!-- 显示无序列表。 --><ul><!-- 显示一项列表内容；按 question in progress.questions 为每条数据重复生成内容；key 取自 question。 --><li v-for="question in progress.questions" :key="question"><!-- 把 question 的当前值转成文字显示。 -->{{ question }}</li></ul><!-- 显示一段文字。 --><p>直接在对话里回答即可。</p></div>
      <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false；启用 show-icon 属性。 -->
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <!-- 显示Element Plus 的 alert 组件；仅当 ['ERROR', 'INTERRUPTED'].includes(phase) 成立时渲染；title 取自 phase === 'INTERRUPTED' ? '本次整理已停止。继续对话可以重新整理。' : '本次方案尚未整理完成。补充或重发要求可以继续。'；类型设为 warning；closable 取自 false。 -->
      <el-alert v-if="['ERROR', 'INTERRUPTED'].includes(phase)" :title="phase === 'INTERRUPTED' ? '本次整理已停止。继续对话可以重新整理。' : '本次方案尚未整理完成。补充或重发要求可以继续。'" type="warning" :closable="false" />
      <!-- 显示成长助手复盘Card（AgentReviewCard）组件；仅当 canGrow('agent:review:read') 成立时渲染；conversation-id 取自 conversationId；source-run-id 取自 succeeded ? run?.id : undefined；event 取自 reviewEvent；external-id 取自 externalReviewId || run?.sourceReviewId；busy 取自 busy || acting；open 取自 open；working 事件触发时执行 reviewWorking；adjusted 事件触发时执行 adjusted；reveal 事件触发时执行 emit('reveal')。 -->
      <AgentReviewCard v-if="canGrow('agent:review:read')" :conversation-id="conversationId" :source-run-id="succeeded ? run?.id : undefined" :event="reviewEvent" :external-id="externalReviewId || run?.sourceReviewId" :busy="busy || acting" :open="open" @working="reviewWorking" @adjusted="adjusted" @reveal="emit('reveal')" />
      <!-- 显示成长助手方案Card（AgentPlanCard）组件；仅当 plan 成立时渲染；plan 取自 plan。 -->
      <AgentPlanCard v-if="plan" :plan="plan" />
      <!-- 显示一个布局容器；仅当 succeeded 成立时渲染；用样式类 plan-receipt 控制外观。 -->
      <div v-if="succeeded" class="plan-receipt"><!-- 显示应用Icon（AppIcon）组件；name 固定为 check；size 取自 18。 --><AppIcon name="check" :size="18" /><!-- 显示一个布局容器。 --><div><!-- 显示重点文字。 --><strong>已执行，结果已核对</strong><!-- 显示一段文字。 --><p>执行进度来自实际业务记录，完成任务后会更新。</p></div></div>
      <!-- 显示成长助手实际进度（AgentActualProgress）组件；tracking 取自 tracking。 -->
      <AgentActualProgress :tracking="tracking" />
      <!-- 显示一段文字；仅当 progressError 成立时渲染；用样式类 plan-note 控制外观。 -->
      <p v-if="progressError" class="plan-note"><!-- 把 progressError 的当前值转成文字显示。 -->{{ progressError }}</p>
      <!-- 显示一个布局容器；仅当 succeeded && commit?.result 成立时渲染；用样式类 plan-result-links 控制外观。 -->
      <div v-if="succeeded && commit?.result" class="plan-result-links"><!-- 显示应用内部页面链接；仅当 commit.result.taskIds?.length 成立时渲染；to 固定为 /growth/tasks。 --><RouterLink v-if="commit.result.taskIds?.length" to="/growth/tasks">查看任务</RouterLink><!-- 显示应用内部页面链接；仅当 commit.result.weeklyPlanIds?.length 成立时渲染；to 固定为 /planning/weekly-plans。 --><RouterLink v-if="commit.result.weeklyPlanIds?.length" to="/planning/weekly-plans">查看计划</RouterLink><!-- 显示应用内部页面链接；仅当 commit.result.habitIds?.length 成立时渲染；to 固定为 /habits。 --><RouterLink v-if="commit.result.habitIds?.length" to="/habits">查看习惯</RouterLink></div>
      <!-- 显示可展开的详情区域；仅当 history.length 成立时渲染；用样式类 plan-process 控制外观。 -->
      <details v-if="history.length" class="plan-process"><!-- 显示详情区域的展开入口。 --><summary>查看处理记录</summary><!-- 显示有序列表。 --><ol><!-- 显示一项列表内容；按 (entry, index) in history 为每条数据重复生成内容；key 取自 index。 --><li v-for="(entry, index) in history" :key="index"><!-- 显示重点文字。 --><strong><!-- 把 historyLabel(entry.phase) 的当前值转成文字显示。 -->{{ historyLabel(entry.phase) }}</strong><!-- 显示一段文字。 --><p><!-- 把 entry.summary 的当前值转成文字显示。 -->{{ entry.summary }}</p></li></ol></details>
      <!-- 显示一段文字；仅当 phase === 'EXPIRED' || phase === 'CANCELLED' 成立时渲染；用样式类 plan-note 控制外观。 -->
      <p v-if="phase === 'EXPIRED' || phase === 'CANCELLED'" class="plan-note"><!-- 把 phase === 'EXPIRED' ? '方案已过期。继续对话可重新整理。' : '没有创建本方案的业务内容，可继续对话调整。' 的当前值转成文字显示。 -->{{ phase === 'EXPIRED' ? '方案已过期。继续对话可重新整理。' : '没有创建本方案的业务内容，可继续对话调整。' }}</p>
    </div>
    <!-- 显示底部区域；用样式类 plan-workspace-footer 控制外观。 -->
    <footer class="plan-workspace-footer">
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 waiting || canRetry || acting 成立时渲染。 -->
      <template v-if="waiting || canRetry || acting"><!-- 显示一段文字。 --><p><!-- 把 waiting ? updating ? '接受后更新本版列出的任务安排，保留原任务及历史。' : `接受后将创建当前第 ${plan?.versionNo} 版中的内容。` : acting ? '正在处理，请稍候。' : '沿用已接受的版本重试，保留幂等提交。' 的当前值转成文字显示。 -->{{ waiting ? updating ? '接受后更新本版列出的任务安排，保留原任务及历史。' : `接受后将创建当前第 ${plan?.versionNo} 版中的内容。` : acting ? '正在处理，请稍候。' : '沿用已接受的版本重试，保留幂等提交。' }}</p><!-- 显示一个布局容器。 --><div><!-- 显示Element Plus 的 button 组件；仅当 waiting 成立时渲染；disabled 取自 busy || acting || reviewActing；click 事件触发时执行 emit('revise', '我想调整当前方案：')。 --><el-button v-if="waiting" :disabled="busy || acting || reviewActing" @click="emit('revise', '我想调整当前方案：')">继续修改</el-button><!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 acting；disabled 取自 reviewActing || (waiting ? !canAccept : busy || acting)；click 事件触发时执行 accept。 --><el-button type="primary" :loading="acting" :disabled="reviewActing || (waiting ? !canAccept : busy || acting)" @click="accept"><!-- 把 acting ? '正在提交' : waiting ? updating ? '接受并更新安排' : '接受并创建' : '重试提交' 的当前值转成文字显示。 -->{{ acting ? '正在提交' : waiting ? updating ? '接受并更新安排' : '接受并创建' : '重试提交' }}</el-button></div></template>
      <!-- 显示一个布局容器；用样式类 plan-secondary-actions 控制外观。 -->
      <div class="plan-secondary-actions"><!-- 显示Element Plus 的 button 组件；仅当 canDecline 成立时渲染；启用 text 属性；size 固定为 small；disabled 取自 busy || acting；click 事件触发时执行 decline。 --><el-button v-if="canDecline" text size="small" :disabled="busy || acting" @click="decline">暂不接受</el-button><!-- 显示Element Plus 的 button 组件；仅当 run 成立时渲染；启用 text 属性；size 固定为 small；disabled 取自 acting；click 事件触发时执行 load()。 --><el-button v-if="run" text size="small" :disabled="acting" @click="load()"><!-- 显示应用Icon（AppIcon）组件；name 固定为 refresh；size 取自 14。 --><AppIcon name="refresh" :size="14" />刷新进度</el-button></div>
    </footer>
  </aside>
</template>

<style scoped>
/* 以下规则应用于 .agent-plan-workspace 选中的元素。 */
.agent-plan-workspace { /* 决定元素是否参与普通排版，以及按谁定位；取值 relative。 */ position: relative; /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 条目的排列方向；取值 column。 */ flex-direction: column; /* 设置高度；取值 100%。 */ height: 100%; /* 限制最小高度；取值 0。 */ min-height: 0; /* 限制最小宽度；取值 0。 */ min-width: 0; /* 设置背景颜色、图片或渐变；取值 var(—aw-surface)。 */ background: var(--aw-surface); /* 设置左侧边框；取值 1px solid var(—aw-border)。 */ border-left: 1px solid var(--aw-border); }
/* 以下规则应用于 .plan-workspace-head 选中的元素。 */
.plan-workspace-head { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置内容与边框之间的内侧留白；取值 16px 24px。 */ padding: 16px 24px; /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; }
/* 以下规则应用于 .plan-workspace-head > div 选中的元素。 */
.plan-workspace-head > div { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 .plan-workspace-head h2 选中的元素。 */
.plan-workspace-head h2 { /* 设置元素外侧留白；取值 0。 */ margin: 0; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .plan-close 选中的元素。 */
.plan-close { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置 CSS 属性 place-items；取值 center。 */ place-items: center; /* 设置内容与边框之间的内侧留白；取值 4px。 */ padding: 4px; /* 设置边框宽度、线型与颜色；取值 0。 */ border: 0; /* 设置边角圆弧大小；取值 4px。 */ border-radius: 4px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; }
/* 以下规则应用于 .plan-close:hover 选中的元素。 */
.plan-close:hover { /* 设置背景颜色、图片或渐变；取值 var(—aw-surface-muted)。 */ background: var(--aw-surface-muted); }
/* 以下规则应用于 .plan-workspace-body 选中的元素。 */
.plan-workspace-body { /* 设置条目如何占用、伸展或压缩可用空间；取值 1。 */ flex: 1; /* 限制最小高度；取值 0。 */ min-height: 0; /* 决定纵向超出时的处理；取值 auto。 */ overflow-y: auto; /* 决定横向超出时的处理；取值 hidden。 */ overflow-x: hidden; /* 设置内容与边框之间的内侧留白；取值 8px 24px 24px。 */ padding: 8px 24px 24px; /* 设置 CSS 属性 scrollbar-width；取值 thin。 */ scrollbar-width: thin; }
/* 以下规则应用于 .plan-phase 选中的元素。 */
.plan-phase { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .phase-dot 选中的元素。 */
.phase-dot { /* 设置宽度；取值 8px。 */ width: 8px; /* 设置高度；取值 8px。 */ height: 8px; /* 设置背景颜色、图片或渐变；取值 var(—aw-brand)。 */ background: var(--aw-brand); /* 设置边角圆弧大小；取值 50%。 */ border-radius: 50%; }
/* 以下规则应用于 .phase-dot.running 选中的元素。 */
.phase-dot.running { /* 设置使用的动画及其时间规则；取值 plan-pulse 1600ms var(—aw-motion) infinite alternate。 */ animation: plan-pulse 1600ms var(--aw-motion) infinite alternate; }
/* 以下规则应用于 .plan-version 选中的元素。 */
.plan-version { /* 设置左侧外侧留白；取值 auto。 */ margin-left: auto; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-route 选中的元素。 */
.plan-route { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置网格列数和各列宽度；取值 repeat(4, minmax(0, 1fr))。 */ grid-template-columns: repeat(4, minmax(0, 1fr)); /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; /* 设置元素外侧留白；取值 24px 0 32px。 */ margin: 24px 0 32px; /* 设置 CSS 属性 list-style；取值 none。 */ list-style: none; /* 设置相邻布局条目之间的间隔；取值 4px。 */ gap: 4px; }
/* 以下规则应用于 .plan-route li 选中的元素。 */
.plan-route li { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 条目的排列方向；取值 column。 */ flex-direction: column; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 flex-start。 */ align-items: flex-start; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); /* 决定元素是否参与普通排版，以及按谁定位；取值 relative。 */ position: relative; }
/* 以下规则应用于 .plan-route li::after 选中的元素。 */
.plan-route li::after { /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 为伪元素提供要显示的内容；取值 ''。 */ content: ''; /* 设置上侧定位距离；取值 12px。 */ top: 12px; /* 设置左侧定位距离；取值 32px。 */ left: 32px; /* 设置右侧定位距离；取值 4px。 */ right: 4px; /* 设置高度；取值 1px。 */ height: 1px; /* 设置背景颜色、图片或渐变；取值 var(—aw-border)。 */ background: var(--aw-border); }
/* 以下规则应用于 .plan-route li:last-child::after 选中的元素。 */
.plan-route li:last-child::after { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 none。 */ display: none; }
/* 以下规则应用于 .plan-route li > span 选中的元素。 */
.plan-route li > span { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置 CSS 属性 place-items；取值 center。 */ place-items: center; /* 设置宽度；取值 24px。 */ width: 24px; /* 设置高度；取值 24px。 */ height: 24px; /* 设置边框宽度、线型与颜色；取值 1px solid var(—aw-border)。 */ border: 1px solid var(--aw-border); /* 设置边角圆弧大小；取值 50%。 */ border-radius: 50%; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-route small 选中的元素。 */
.plan-route small { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-route .done, .plan-route .current 选中的元素。 */
.plan-route .done, .plan-route .current { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); }
/* 以下规则应用于 .plan-route .done > span, .plan-route .current > span 选中的元素。 */
.plan-route .done > span, .plan-route .current > span { /* 设置背景颜色、图片或渐变；取值 var(—aw-brand-soft)。 */ background: var(--aw-brand-soft); /* 设置边框颜色；取值 transparent。 */ border-color: transparent; }
/* 以下规则应用于 .plan-get-started 选中的元素。 */
.plan-get-started { /* 设置内容与边框之间的内侧留白；取值 24px 0。 */ padding: 24px 0; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 h3 选中的元素。 */
h3 { /* 设置字号；取值 16px。 */ font-size: 16px; /* 设置每行文字的高度；取值 24px。 */ line-height: 24px; /* 设置元素外侧留白；取值 0 0 8px。 */ margin: 0 0 8px; /* 设置字重；取值 600。 */ font-weight: 600; }
/* 以下规则应用于 p, li 选中的元素。 */
p, li { /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 p 选中的元素。 */
p { /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .plan-understanding 选中的元素。 */
.plan-understanding { /* 设置元素外侧留白；取值 24px 0。 */ margin: 24px 0; }
/* 以下规则应用于 .plan-understanding > span 选中的元素。 */
.plan-understanding > span { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-understanding h3 选中的元素。 */
.plan-understanding h3 { /* 设置上方外侧留白；取值 8px。 */ margin-top: 8px; }
/* 以下规则应用于 .plan-questions 选中的元素。 */
.plan-questions { /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding: 16px; /* 设置背景颜色、图片或渐变；取值 var(—aw-bg)。 */ background: var(--aw-bg); /* 设置边角圆弧大小；取值 12px。 */ border-radius: 12px; /* 设置下方外侧留白；取值 24px。 */ margin-bottom: 24px; }
/* 以下规则应用于 .plan-questions ul 选中的元素。 */
.plan-questions ul { /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; /* 设置左侧内侧留白；取值 16px。 */ padding-left: 16px; }
/* 以下规则应用于 .plan-questions li + li 选中的元素。 */
.plan-questions li + li { /* 设置上方外侧留白；取值 8px。 */ margin-top: 8px; }
/* 以下规则应用于 .plan-receipt 选中的元素。 */
.plan-receipt { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 flex-start。 */ align-items: flex-start; /* 设置内容与边框之间的内侧留白；取值 16px 0。 */ padding: 16px 0; /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-success)。 */ color: var(--aw-success); }
/* 以下规则应用于 .plan-receipt p 选中的元素。 */
.plan-receipt p { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .actual-goal-progress 选中的元素。 */
.actual-goal-progress { /* 设置上方外侧留白；取值 24px。 */ margin-top: 24px; }
/* 以下规则应用于 .actual-goal-progress article 选中的元素。 */
.actual-goal-progress article { /* 设置元素外侧留白；取值 16px 0。 */ margin: 16px 0; }
/* 以下规则应用于 .actual-goal-progress a 选中的元素。 */
.actual-goal-progress a { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .goal-progress-label 选中的元素。 */
.goal-progress-label { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; /* 设置元素外侧留白；取值 12px 0 8px。 */ margin: 12px 0 8px; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .actual-goal-progress ul 选中的元素。 */
.actual-goal-progress ul { /* 设置 CSS 属性 list-style；取值 none。 */ list-style: none; /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; /* 设置元素外侧留白；取值 16px 0 0。 */ margin: 16px 0 0; }
/* 以下规则应用于 .actual-goal-progress li 选中的元素。 */
.actual-goal-progress li { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; }
/* 以下规则应用于 .actual-goal-progress small 选中的元素。 */
.actual-goal-progress small { /* 决定空白和换行如何保留；取值 nowrap。 */ white-space: nowrap; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .plan-result-links 选中的元素。 */
.plan-result-links { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置相邻布局条目之间的间隔；取值 16px。 */ gap: 16px; /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .plan-result-links a 选中的元素。 */
.plan-result-links a { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); }
/* 以下规则应用于 .plan-process 选中的元素。 */
.plan-process { /* 设置上方外侧留白；取值 24px。 */ margin-top: 24px; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .plan-process summary 选中的元素。 */
.plan-process summary { /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; }
/* 以下规则应用于 .plan-process ol 选中的元素。 */
.plan-process ol { /* 设置左侧内侧留白；取值 16px。 */ padding-left: 16px; /* 设置下方外侧留白；取值 0。 */ margin-bottom: 0; }
/* 以下规则应用于 .plan-process li 选中的元素。 */
.plan-process li { /* 设置上方外侧留白；取值 12px。 */ margin-top: 12px; }
/* 以下规则应用于 .plan-process strong, .plan-process p 选中的元素。 */
.plan-process strong, .plan-process p { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-note 选中的元素。 */
.plan-note { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-workspace-footer 选中的元素。 */
.plan-workspace-footer { /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置内容与边框之间的内侧留白；取值 12px 24px 16px。 */ padding: 12px 24px 16px; /* 设置顶部边框；取值 1px solid var(—aw-border)。 */ border-top: 1px solid var(--aw-border); }
/* 以下规则应用于 .plan-workspace-footer p 选中的元素。 */
.plan-workspace-footer p { /* 设置元素外侧留白；取值 0 0 12px。 */ margin: 0 0 12px; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-workspace-footer > div 选中的元素。 */
.plan-workspace-footer > div { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置条目在主方向上的对齐与空余空间分配；取值 flex-end。 */ justify-content: flex-end; }
/* 以下规则应用于 .plan-workspace-footer .plan-secondary-actions 选中的元素。 */
.plan-workspace-footer .plan-secondary-actions { /* 设置上方外侧留白；取值 8px。 */ margin-top: 8px; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; }
/* 以下规则应用于 .plan-workspace-footer :deep(.el-button + .el-button) 选中的元素。 */
.plan-workspace-footer :deep(.el-button + .el-button) { /* 设置左侧外侧留白；取值 0。 */ margin-left: 0; }
/* 以下规则应用于 .plan-workspace-footer :deep(.el-button > span) 选中的元素。 */
.plan-workspace-footer :deep(.el-button > span) { /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 定义动画在不同进度时的外观：plan-pulse。 */
@keyframes plan-pulse { /* 以下规则应用于 from 选中的元素。 */ from { /* 设置整体透明度；取值 .35。 */ opacity: .35; } /* 以下规则应用于 to 选中的元素。 */ to { /* 设置整体透明度；取值 1。 */ opacity: 1; } }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width: 1199px)。 */
@media (max-width: 1199px) { /* 以下规则应用于 .agent-plan-workspace 选中的元素。 */ .agent-plan-workspace { /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 设置上侧定位距离；取值 0。 */ top: 0; /* 设置右侧定位距离；取值 0。 */ right: 0; /* 设置下侧定位距离；取值 0。 */ bottom: 0; /* 设置宽度；取值 min(480px, 100%)。 */ width: min(480px, 100%); /* 设置重叠时的前后层级；取值 520。 */ z-index: 520; /* 设置元素投影；取值 var(—aw-shadow-float)。 */ box-shadow: var(--aw-shadow-float); } }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width: 639px)。 */
@media (max-width: 639px) { /* 以下规则应用于 .plan-workspace-body 选中的元素。 */ .plan-workspace-body { /* 设置内容与边框之间的内侧留白；取值 8px 16px 24px。 */ padding: 8px 16px 24px; } /* 以下规则应用于 .plan-workspace-head, .plan-workspace-footer 选中的元素。 */ .plan-workspace-head, .plan-workspace-footer { /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding: 16px; } }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(prefers-reduced-motion: reduce)。 */
@media (prefers-reduced-motion: reduce) { /* 以下规则应用于 .phase-dot.running 选中的元素。 */ .phase-dot.running { /* 设置使用的动画及其时间规则；取值 none。 */ animation: none; } }
</style>
