<script setup>
import { computed, ref } from "vue";
import { useConversationPlan } from "../../composables/useConversationPlan";
import { canGrow } from "../../api/growth";
import AgentPlanCard from "./AgentPlanCard.vue";
import AgentReviewCard from "./AgentReviewCard.vue";
import AgentActualProgress from "./AgentActualProgress.vue";

const props = defineProps({ conversationId: String, event: Object, reviewEvent: Object, externalReviewId: String, busy: Boolean, open: Boolean, refreshKey: Number });
const emit = defineEmits(["close", "available", "reveal", "revise", "working"]);
const reviewActing = ref(false);
const { detail, run, plan, commit, progress, phase, succeeded, loading, acting, error, goals,
  progressError, tracking, canManage, canAccept, canRetry, waiting, load, accept, decline } = useConversationPlan(props, emit);
const updating = computed(() => plan.value?.snapshot?.taskAssignments?.some(item => item.taskId));
function reviewWorking(value) { reviewActing.value = value; emit('working', value); }
async function adjusted(data) { if (data?.run?.id) await load(data.run.id); emit('reveal'); }
const labels = { EMPTY: "从一个目标开始", DRAFT: "沟通需求", COLLECTING: "等待你补充", CONTEXT: "核对已有安排",
  GENERATING: "整理完整方案", WAITING_CONFIRM: "等待你接受", CONFIRMED: "方案已接受", EXECUTING: "正在创建并核对",
  SUCCEEDED: "方案已执行", FAILED: "提交未完成", ERROR: "整理暂未完成", INTERRUPTED: "整理已停止", CANCELLED: "暂未接受", EXPIRED: "方案已过期" };
const stepIndex = computed(() => succeeded.value ? 4 : ["CONFIRMED", "EXECUTING", "FAILED"].includes(phase.value) ? 3
  : waiting.value ? 2 : ["CONTEXT", "GENERATING"].includes(phase.value) || plan.value ? 1 : 0);
const steps = ["明确目标", "整理方案", "接受方案", "提交并核对"];
const history = computed(() => (progress.value.steps || []).slice(-8));
const historyLabel = phase => ({ COLLECTING: "补齐需求", CONTEXT: "核对现状", GENERATING: "生成方案", WAITING_CONFIRM: "方案就绪", ERROR: "整理失败", INTERRUPTED: "已停止" })[phase] || phase;
const canDecline = computed(() => canManage.value && ["DRAFT", "WAITING_CONFIRM", "CONFIRMED"].includes(run.value?.status));
</script>

<template>
  <aside v-show="open" class="agent-plan-workspace" aria-label="方案与进度">
    <slot name="resize" />
    <header class="plan-workspace-head"><div><AppIcon name="goal" :size="18" /><h2>方案与进度</h2></div><button type="button" class="plan-close" aria-label="收起方案面板" @click="emit('close')"><AppIcon name="close" :size="18" /></button></header>
    <div class="plan-workspace-body">
      <div class="plan-phase" role="status"><span class="phase-dot" :class="{ running: ['CONTEXT', 'GENERATING', 'EXECUTING'].includes(phase) }"></span>{{ labels[phase] || run?.statusLabel || '沟通需求' }}<span v-if="plan" class="plan-version">第 {{ plan.versionNo }} 版</span></div>
      <ol class="plan-route" aria-label="方案处理进度"><li v-for="(step, index) in steps" :key="step" :class="{ done: stepIndex > index, current: stepIndex === index }"><span><AppIcon v-if="stepIndex > index" name="check" :size="12" /><template v-else>{{ index + 1 }}</template></span><small>{{ step }}</small></li></ol>
      <div v-if="!run && !event && !loading" class="plan-get-started"><AppIcon name="chat" :size="32" /><h3>我们边聊边完善</h3><p>告诉助手你想达到什么目标。时间、基础和安排会在对话中逐步补齐，完整方案会显示在这里。</p><p>核对之后，点击接受再创建。</p></div>
      <el-skeleton v-if="loading && !plan" :rows="5" animated />
      <div v-if="progress.objective && !plan" class="plan-understanding"><span>当前目标</span><h3>{{ progress.objective }}</h3><p>{{ progress.summary }}</p></div>
      <div v-if="progress.questions?.length" class="plan-questions"><h3>还需要了解</h3><ul><li v-for="question in progress.questions" :key="question">{{ question }}</li></ul><p>直接在对话里回答即可。</p></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-alert v-if="['ERROR', 'INTERRUPTED'].includes(phase)" :title="phase === 'INTERRUPTED' ? '本次整理已停止。继续对话可以重新整理。' : '本次方案尚未整理完成。补充或重发要求可以继续。'" type="warning" :closable="false" />
      <AgentReviewCard v-if="canGrow('agent:review:read')" :conversation-id="conversationId" :source-run-id="succeeded ? run?.id : undefined" :event="reviewEvent" :external-id="externalReviewId || run?.sourceReviewId" :busy="busy || acting" :open="open" @working="reviewWorking" @adjusted="adjusted" @reveal="emit('reveal')" />
      <AgentPlanCard v-if="plan" :plan="plan" />
      <div v-if="succeeded" class="plan-receipt"><AppIcon name="check" :size="18" /><div><strong>已执行，结果已核对</strong><p>执行进度来自实际业务记录，完成任务后会更新。</p></div></div>
      <AgentActualProgress :tracking="tracking" />
      <p v-if="progressError" class="plan-note">{{ progressError }}</p>
      <div v-if="succeeded && commit?.result" class="plan-result-links"><RouterLink v-if="commit.result.taskIds?.length" to="/growth/tasks">查看任务</RouterLink><RouterLink v-if="commit.result.weeklyPlanIds?.length" to="/planning/weekly-plans">查看计划</RouterLink><RouterLink v-if="commit.result.habitIds?.length" to="/habits">查看习惯</RouterLink></div>
      <details v-if="history.length" class="plan-process"><summary>查看处理记录</summary><ol><li v-for="(entry, index) in history" :key="index"><strong>{{ historyLabel(entry.phase) }}</strong><p>{{ entry.summary }}</p></li></ol></details>
      <p v-if="phase === 'EXPIRED' || phase === 'CANCELLED'" class="plan-note">{{ phase === 'EXPIRED' ? '方案已过期。继续对话可重新整理。' : '没有创建本方案的业务内容，可继续对话调整。' }}</p>
    </div>
    <footer class="plan-workspace-footer">
      <template v-if="waiting || canRetry || acting"><p>{{ waiting ? updating ? '接受后更新本版列出的任务安排，保留原任务及历史。' : `接受后将创建当前第 ${plan?.versionNo} 版中的内容。` : acting ? '正在处理，请稍候。' : '沿用已接受的版本重试，保留幂等提交。' }}</p><div><el-button v-if="waiting" :disabled="busy || acting || reviewActing" @click="emit('revise', '我想调整当前方案：')">继续修改</el-button><el-button type="primary" :loading="acting" :disabled="reviewActing || (waiting ? !canAccept : busy || acting)" @click="accept">{{ acting ? '正在提交' : waiting ? updating ? '接受并更新安排' : '接受并创建' : '重试提交' }}</el-button></div></template>
      <div class="plan-secondary-actions"><el-button v-if="canDecline" text size="small" :disabled="busy || acting" @click="decline">暂不接受</el-button><el-button v-if="run" text size="small" :disabled="acting" @click="load()"><AppIcon name="refresh" :size="14" />刷新进度</el-button></div>
    </footer>
  </aside>
</template>

<style scoped>
.agent-plan-workspace { position: relative; display: flex; flex-direction: column; height: 100%; min-height: 0; min-width: 0; background: var(--aw-surface); border-left: 1px solid var(--aw-border); }
.plan-workspace-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 24px; flex-shrink: 0; }
.plan-workspace-head > div { display: flex; align-items: center; gap: 8px; }
.plan-workspace-head h2 { margin: 0; font-size: 14px; line-height: 20px; }
.plan-close { display: grid; place-items: center; padding: 4px; border: 0; border-radius: 4px; color: var(--aw-text-secondary); background: transparent; cursor: pointer; }
.plan-close:hover { background: var(--aw-surface-muted); }
.plan-workspace-body { flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; padding: 8px 24px 24px; scrollbar-width: thin; }
.plan-phase { display: flex; align-items: center; gap: 8px; font-size: 14px; line-height: 20px; }
.phase-dot { width: 8px; height: 8px; background: var(--aw-brand); border-radius: 50%; }
.phase-dot.running { animation: plan-pulse 1600ms var(--aw-motion) infinite alternate; }
.plan-version { margin-left: auto; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.plan-route { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); padding: 0; margin: 24px 0 32px; list-style: none; gap: 4px; }
.plan-route li { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; color: var(--aw-text-tertiary); position: relative; }
.plan-route li::after { position: absolute; content: ''; top: 12px; left: 32px; right: 4px; height: 1px; background: var(--aw-border); }
.plan-route li:last-child::after { display: none; }
.plan-route li > span { display: grid; place-items: center; width: 24px; height: 24px; border: 1px solid var(--aw-border); border-radius: 50%; font-size: 12px; line-height: 16px; }
.plan-route small { font-size: 12px; line-height: 16px; }
.plan-route .done, .plan-route .current { color: var(--aw-brand); }
.plan-route .done > span, .plan-route .current > span { background: var(--aw-brand-soft); border-color: transparent; }
.plan-get-started { padding: 24px 0; color: var(--aw-text-secondary); }
h3 { font-size: 16px; line-height: 24px; margin: 0 0 8px; font-weight: 600; }
p, li { font-size: 14px; line-height: 20px; }
p { margin: 8px 0; color: var(--aw-text-secondary); }
.plan-understanding { margin: 24px 0; }
.plan-understanding > span { color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.plan-understanding h3 { margin-top: 8px; }
.plan-questions { padding: 16px; background: var(--aw-bg); border-radius: 12px; margin-bottom: 24px; }
.plan-questions ul { margin: 8px 0; padding-left: 16px; }
.plan-questions li + li { margin-top: 8px; }
.plan-receipt { display: flex; gap: 12px; align-items: flex-start; padding: 16px 0; margin-top: 16px; color: var(--aw-success); }
.plan-receipt p { font-size: 12px; line-height: 16px; }
.actual-goal-progress { margin-top: 24px; }
.actual-goal-progress article { margin: 16px 0; }
.actual-goal-progress a { display: flex; justify-content: space-between; gap: 8px; font-size: 14px; line-height: 20px; }
.goal-progress-label { display: flex; justify-content: space-between; margin: 12px 0 8px; font-size: 12px; line-height: 16px; color: var(--aw-text-secondary); }
.actual-goal-progress ul { list-style: none; padding: 0; margin: 16px 0 0; }
.actual-goal-progress li { display: flex; justify-content: space-between; gap: 12px; margin: 8px 0; }
.actual-goal-progress small { white-space: nowrap; color: var(--aw-text-secondary); }
.plan-result-links { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 16px; font-size: 14px; line-height: 20px; }
.plan-result-links a { color: var(--aw-brand); }
.plan-process { margin-top: 24px; font-size: 12px; line-height: 16px; color: var(--aw-text-secondary); }
.plan-process summary { cursor: pointer; }
.plan-process ol { padding-left: 16px; margin-bottom: 0; }
.plan-process li { margin-top: 12px; }
.plan-process strong, .plan-process p { font-size: 12px; line-height: 16px; }
.plan-note { font-size: 12px; line-height: 16px; }
.plan-workspace-footer { flex-shrink: 0; padding: 12px 24px 16px; border-top: 1px solid var(--aw-border); }
.plan-workspace-footer p { margin: 0 0 12px; font-size: 12px; line-height: 16px; }
.plan-workspace-footer > div { display: flex; gap: 8px; justify-content: flex-end; }
.plan-workspace-footer .plan-secondary-actions { margin-top: 8px; justify-content: space-between; }
.plan-workspace-footer :deep(.el-button + .el-button) { margin-left: 0; }
.plan-workspace-footer :deep(.el-button > span) { gap: 8px; }
@keyframes plan-pulse { from { opacity: .35; } to { opacity: 1; } }
@media (max-width: 1199px) { .agent-plan-workspace { position: absolute; top: 0; right: 0; bottom: 0; width: min(480px, 100%); z-index: 520; box-shadow: var(--aw-shadow-float); } }
@media (max-width: 639px) { .plan-workspace-body { padding: 8px 16px 24px; } .plan-workspace-head, .plan-workspace-footer { padding: 16px; } }
@media (prefers-reduced-motion: reduce) { .phase-dot.running { animation: none; } }
</style>
