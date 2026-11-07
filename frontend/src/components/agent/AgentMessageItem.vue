<script setup>
import { computed } from "vue";
import { ElMessage } from "element-plus";
import AgentMarkdown from "./AgentMarkdown.vue";

const props = defineProps({
  turn: { type: Object, required: true },
  busy: Boolean,
});
defineEmits(["retry"]);

const answer = computed(() => props.turn.answer || null);
const process = computed(() => [
  ...(answer.value?.activities || []).filter(item => item.id === 'context'),
  ...(answer.value?.tools || []),
  ...(answer.value?.activities || []).filter(item => item.id !== 'context'),
]);
const processing = computed(() => process.value.find(item => item.status === 'running'));
const statusText = computed(() => {
  const value = answer.value;
  if (!value) return "等待回复";
  if (value.pending) return "正在生成";
  if (value.stopped) return "已停止生成";
  if (value.complete) return "";
  if (value.finishReason === "length") return "达到输出限制";
  return "回复未完成";
});
const notice = computed(() => {
  const value = answer.value;
  if (!value || value.pending || value.complete || value.stopped) return "";
  if (value.finishReason === "length")
    return "回复达到输出限制，内容可能不完整，可以再次提问补充。";
  return "回复未完成：生成被中断或已停止，已收到的内容仅供参考。";
});
const retryable = computed(() => {
  const value = answer.value;
  return !!value && !value.pending && (!value.complete || !!value.error);
});
const toolIcon = (status) =>
  status === "running"
    ? "refresh"
    : status === "ok"
      ? "check"
      : status === "limited"
        ? "info"
      : status === "cancelled"
        ? "close"
        : "error";
const hasActions = computed(
  () =>
    !!answer.value?.content ||
    retryable.value ||
    !!answer.value?.traceId,
);

async function copy() {
  if (!answer.value?.content) return;
  try {
    await navigator.clipboard.writeText(answer.value.content);
    ElMessage.success("回复已复制");
  } catch {
    ElMessage.warning("无法访问剪贴板，请选中文字复制");
  }
}
</script>

<template>
  <article class="agent-turn">
    <div class="agent-question">
      <p>{{ turn.question }}</p>
    </div>
    <div class="agent-answer">
      <div class="agent-answer-label">
        <AppIcon name="sparkle" :size="20" /><strong>成长助手</strong
        ><span v-if="statusText" class="muted">{{ statusText }}</span>
      </div>
      <details v-if="process.length" class="agent-process"><summary><AppIcon :name="processing ? 'refresh' : 'check'" :size="14" /><span>{{ processing ? processing.label + '…' : '查看处理过程' }}</span><small>{{ process.length }} 个步骤</small></summary><ol><li v-for="(step, index) in process" :key="step.id || index" :class="step.status"><AppIcon :name="toolIcon(step.status)" :size="14" /><div><strong>{{ step.label }}</strong><p v-if="step.summary">{{ step.summary }}</p></div><small v-if="step.durationMs != null">{{ step.durationMs < 1000 ? step.durationMs + ' 毫秒' : (step.durationMs / 1000).toFixed(2) + ' 秒' }}</small><small v-else-if="step.status === 'cancelled'">已停止</small><small v-else-if="step.status === 'error'">未完成</small></li></ol></details>
      <div
        v-if="answer?.pending && !answer.content"
        class="agent-skeleton"
        aria-label="等待回复"
      >
        <span /><span /><span />
      </div>
      <AgentMarkdown v-if="answer?.content" :content="answer.content" />
      <div v-if="answer?.citations?.length" class="agent-citations" aria-label="引用来源">
        <strong>参考资料</strong>
        <RouterLink v-for="source in answer.citations" :key="source.chunkId"
          :to="`/agent/knowledge/${source.documentId}?revisionId=${source.revisionId}&chunkId=${source.chunkId}`">
          {{ source.label }} · {{ source.title }}<span v-if="source.pageNumber"> · 第 {{ source.pageNumber }} 页</span>
        </RouterLink>
      </div>
      <RouterLink v-if="answer?.usedMemoryIds?.length" class="muted" to="/agent/memories">本轮参考了 {{ answer.usedMemoryIds.length }} 条已确认记忆</RouterLink>
      <p v-if="answer?.stopped" class="muted agent-inline-note" role="status">
        生成已停止，已收到的内容已保留。
      </p>
      <el-alert
        v-if="answer?.error"
        :title="answer.error"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else-if="notice"
        :title="notice"
        type="warning"
        :closable="false"
        show-icon
      />
      <div v-if="hasActions" class="agent-answer-actions">
        <el-button v-if="answer?.content" text size="small" @click="copy"
          aria-label="复制回复" title="复制回复"><AppIcon name="copy" :size="16" /></el-button
        >
        <el-button
          v-if="retryable"
          text
          size="small"
          :disabled="busy"
          @click="$emit('retry', turn.question)"
          ><AppIcon name="refresh" :size="14" />再次提问</el-button
        >
        <span v-if="answer?.traceId" class="muted agent-trace"
          >追踪号：{{ answer.traceId }}</span
        >
      </div>
    </div>
  </article>
</template>

<style scoped>
.agent-process { margin: 0 0 16px; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.agent-process summary { display: flex; align-items: center; gap: 8px; cursor: pointer; width: fit-content; list-style: none; padding: 4px 0; }
.agent-process summary::-webkit-details-marker { display: none; }
.agent-process summary small { opacity: .7; }
.agent-process ol { list-style: none; padding: 12px; margin: 8px 0 0; background: var(--aw-surface-muted); border-radius: 8px; }
.agent-process li { display: flex; align-items: flex-start; gap: 8px; padding: 8px 0; }
.agent-process li > div { flex: 1; min-width: 0; }
.agent-process li p { margin: 4px 0 0; font-size: 12px; line-height: 16px; }
.agent-process li strong, .agent-process li small { font-size: 12px; line-height: 16px; }
.agent-process li.error { color: var(--aw-danger); }
.agent-process li.limited { color: var(--aw-text-secondary); }
.agent-citations { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 16px 0; padding: 12px; background: var(--el-fill-color-light); border-radius: 8px; font-size: 12px; line-height: 20px; }
.agent-citations a { color: var(--el-color-primary); }
.agent-turn + .agent-turn {
  margin-top: 32px;
}
.agent-question {
  margin-left: auto;
  width: fit-content;
  max-width: 85%;
  padding: 12px 16px;
  background: var(--aw-surface-muted);
  border-radius: 12px;
}
.agent-speaker {
  color: var(--el-color-primary);
  font-size: 12px;
  line-height: 16px;
}
.agent-question p {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 14px;
  line-height: 20px;
  margin: 0;
}
.agent-answer {
  margin-top: 24px;
}
.agent-answer-label {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 20px;
}
.agent-answer-label .aw-icon {
  color: var(--el-color-primary);
}
.agent-answer-label .muted {
  font-size: 12px;
  line-height: 16px;
}
.agent-answer :deep(.el-alert) {
  margin-top: 16px;
}
.agent-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 18px;
}
.agent-tool {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}
.agent-tool.running .aw-icon {
  animation: agent-tool-spin 1500ms var(--aw-motion) infinite;
}
.agent-tool.error {
  color: var(--el-color-danger);
}
@keyframes agent-tool-spin {
  to {
    transform: rotate(360deg);
  }
}
.agent-inline-note {
  font-size: 14px;
  line-height: 20px;
}
.agent-answer-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
  line-height: 16px;
}
.agent-answer-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.agent-answer-actions :deep(.el-button) {
  gap: 4px;
}
.agent-trace {
  overflow-wrap: anywhere;
}
.agent-skeleton {
  display: grid;
  gap: 8px;
}
.agent-skeleton span {
  height: 16px;
  background: var(--el-fill-color);
  border-radius: 4px;
  animation: agent-wait 1500ms cubic-bezier(0.22, 1, 0.36, 1) infinite alternate;
}
.agent-skeleton span:nth-child(2) {
  width: 85%;
}
.agent-skeleton span:nth-child(3) {
  width: 64%;
}
@keyframes agent-wait {
  from {
    opacity: 0.4;
  }
  to {
    opacity: 1;
  }
}
@media (prefers-reduced-motion: reduce) {
  .agent-skeleton span, .agent-tool.running .aw-icon {
    animation: none;
  }
}
@media (max-width: 767px) {
  .agent-question {
    max-width: 95%;
  }
}
</style>
