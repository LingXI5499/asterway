<script setup>
import { computed } from "vue";
import AgentMarkdown from "./AgentMarkdown.vue";
import { useConversationReview } from "../../composables/useConversationReview";
const props = defineProps({ conversationId: String, sourceRunId: String, event: Object, externalId: String, busy: Boolean, open: Boolean });
const emit = defineEmits(["working", "adjusted", "reveal"]);
const { report, error, acting, action, canReview, canAdjust, start, retry, adjust, load } = useConversationReview(props, emit);
const pending = computed(() => ["PENDING", "RUNNING"].includes(report.value?.status));
</script>

<template>
  <section class="review-card" aria-label="执行反馈与复盘">
    <div class="review-heading"><AppIcon name="history" :size="16" /><h3>执行反馈与复盘</h3></div>
    <p v-if="!report">{{ conversationId ? '根据实际投入、任务完成和逾期情况，检查当前安排，再决定下一步。' : '开始一次对话后，即可在这里复盘执行并调整安排。' }}</p>
    <template v-if="report">
      <p class="review-range">{{ report.dateFrom }} 至 {{ report.dateTo }} · {{ report.timeZone }}</p>
      <el-skeleton v-if="pending" :rows="3" animated /><p v-if="pending" role="status">正在分析真实记录，完成后自动更新。</p>
      <el-alert v-if="report.status === 'FAILED'" :title="report.errorMessage || '复盘暂未完成，可重新生成'" type="warning" :closable="false" />
      <details v-if="report.status === 'READY'" class="review-analysis"><summary>查看复盘与调整依据</summary><AgentMarkdown :content="report.content" /></details>
      <p v-if="report.status === 'READY'">可据此整理后续安排。调整方案会先展示改期前后信息，接受后才更新。</p>
    </template>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="review-actions">
      <el-button v-if="canAdjust" type="primary" :loading="acting && action === 'adjust'" :disabled="busy || acting" @click="adjust">根据复盘调整计划</el-button>
      <el-button v-if="canReview && report?.status === 'FAILED'" :loading="acting" :disabled="busy || acting" @click="retry">重新生成复盘</el-button>
      <el-button v-else-if="canReview && !pending" :loading="acting && action === 'review'" :disabled="busy || acting || !conversationId" @click="start">{{ report ? '复盘最近七天' : '复盘执行情况' }}</el-button>
      <el-button v-if="pending" text :disabled="acting" @click="load(report.id)">刷新复盘</el-button>
    </div>
    <p v-if="acting && action === 'adjust'" role="status">正在核对当前安排并整理调整方案…</p>
  </section>
</template>

<style scoped>
.review-card { margin: 24px 0; padding: 16px; border: 1px solid var(--aw-border); border-radius: 12px; background: var(--aw-bg); }
.review-heading { display: flex; align-items: center; gap: 8px; }
h3 { margin: 0; font-size: 14px; line-height: 20px; font-weight: 600; }
p, summary { font-size: 12px; line-height: 16px; color: var(--aw-text-secondary); margin: 8px 0; }
.review-range { margin: 12px 0; }
.review-analysis summary { cursor: pointer; color: var(--aw-text); }
.review-analysis :deep(.agent-markdown) { font-size: 14px; line-height: 20px; }
.review-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.review-actions :deep(.el-button + .el-button) { margin-left: 0; }
.review-actions :deep(.el-button) { max-width: 100%; font-size: 14px; }
</style>
