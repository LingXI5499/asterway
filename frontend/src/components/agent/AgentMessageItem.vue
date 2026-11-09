<!-- 渲染成长助手消息单条数据界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ./AgentMarkdown.vue 提供的组件、状态函数或请求方法。 */
import AgentMarkdown from "./AgentMarkdown.vue";

const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 本轮对话（turn）字段使用表达式 { type: Object, required: true } 的结果。 */
  turn: { /* 类型（type）字段使用对象（Object）。 */ type: Object, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true },
  /* 正在处理（busy）字段使用可为空的真假值（Boolean）。 */
  busy: Boolean,
});
/* 声明当前组件可以通知父组件的事件。 */
defineEmits(["retry"]);

const /* answer保存随相关状态变化自动重算的值。 */ answer = computed(() => props.turn.answer || null);
const /* 处理过程（process）保存随依赖自动重算的筛选结果或数量。 */ process = computed(() => [
  ...(answer.value?.activities || []).filter(item => item.id === 'context'),
  ...(answer.value?.tools || []),
  ...(answer.value?.activities || []).filter(item => item.id !== 'context'),
]);
const /* processing保存随依赖自动重算、从列表中找到的匹配对象。 */ processing = computed(() => process.value.find(item => item.status === 'running'));
const /* 状态文字（statusText）保存随相关状态变化自动重算的值。 */ statusText = computed(() => {
  const /* 值（value）保存表达式 answer.value 的结果。 */ value = answer.value;
  /* 检查条件 !value 是否成立，成立时走这一分支。 */
  if (!value) /* 返回固定文字 '等待回复'，当前函数到此结束。 */ return "等待回复";
  /* 检查条件 value.pending 是否成立，成立时走这一分支。 */
  if (value.pending) /* 返回固定文字 '正在生成'，当前函数到此结束。 */ return "正在生成";
  /* 检查条件 value.stopped 是否成立，成立时走这一分支。 */
  if (value.stopped) /* 返回固定文字 '已停止生成'，当前函数到此结束。 */ return "已停止生成";
  /* 检查条件 value.complete 是否成立，成立时走这一分支。 */
  if (value.complete) /* 返回固定文字 ''，当前函数到此结束。 */ return "";
  /* 检查条件 value.finishReason === "length" 是否成立，成立时走这一分支。 */
  if (value.finishReason === "length") /* 返回固定文字 '达到输出限制'，当前函数到此结束。 */ return "达到输出限制";
  /* 返回固定文字 '回复未完成'，当前函数到此结束。 */
  return "回复未完成";
});
const /* 达标提醒（notice）保存随相关状态变化自动重算的值。 */ notice = computed(() => {
  const /* 值（value）保存表达式 answer.value 的结果。 */ value = answer.value;
  /* 检查条件 !value || value.pending || value.complete || value.stopped 是否成立，成立时走这一分支。 */
  if (!value || value.pending || value.complete || value.stopped) /* 返回固定文字 ''，当前函数到此结束。 */ return "";
  /* 检查条件 value.finishReason === "length" 是否成立，成立时走这一分支。 */
  if (value.finishReason === "length")
    /* 返回固定文字 '回复达到输出限制，内容可能不完整，可以再次提问补充。'，当前函数到此结束。 */
    return "回复达到输出限制，内容可能不完整，可以再次提问补充。";
  /* 返回固定文字 '回复未完成：生成被中断或已停止，已收到的内容仅供参考。'，当前函数到此结束。 */
  return "回复未完成：生成被中断或已停止，已收到的内容仅供参考。";
});
const /* retryable保存随相关状态变化自动重算的值。 */ retryable = computed(() => {
  const /* 值（value）保存表达式 answer.value 的结果。 */ value = answer.value;
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：!!value && !value.pending && (!value.complete || !!value.error)，当前函数到此结束。 */
  return !!value && !value.pending && (!value.complete || !!value.error);
});
const /* 业务工具Icon（toolIcon）保存根据前面的条件，从两个候选结果中选择一个：(status) => status === "running" ? "refresh" : status === "ok" ? "check" : status === "limited" ? "i…。 */ toolIcon = (status) =>
  status === "running"
    ? "refresh"
    : status === "ok"
      ? "check"
      : status === "limited"
        ? "info"
      : status === "cancelled"
        ? "close"
        : "error";
const /* 是否存在Actions（hasActions）保存随相关状态变化自动重算的值。 */ hasActions = computed(
  () =>
    !!answer.value?.content ||
    retryable.value ||
    !!answer.value?.traceId,
);

/* copy：调用 copy 完成copy处理。 */
async function copy() {
  /* 检查条件 !answer.value?.content 是否成立，成立时走这一分支。 */
  if (!answer.value?.content) /* 返回空内容，当前函数到此结束。 */ return;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待写入 文字（Text）。 */
    await navigator.clipboard.writeText(answer.value.content);
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("回复已复制");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    /* 调用 ElMessage.warning 完成warning处理。 */
    ElMessage.warning("无法访问剪贴板，请选中文字复制");
  }
}
</script>

<template>
  <!-- 显示一条独立内容；用样式类 agent-turn 控制外观。 -->
  <article class="agent-turn">
    <!-- 显示一个布局容器；用样式类 agent-question 控制外观。 -->
    <div class="agent-question">
      <!-- 显示一段文字。 -->
      <p><!-- 把 turn.question 的当前值转成文字显示。 -->{{ turn.question }}</p>
    </div>
    <!-- 显示一个布局容器；用样式类 agent-answer 控制外观。 -->
    <div class="agent-answer">
      <!-- 显示一个布局容器；用样式类 agent-answer-label 控制外观。 -->
      <div class="agent-answer-label">
        <!-- 显示应用Icon（AppIcon）组件；name 固定为 sparkle；size 取自 20。 -->
        <AppIcon name="sparkle" :size="20" /><!-- 显示重点文字。 --><strong>成长助手</strong
        ><!-- 显示行内文字区域；仅当 statusText 成立时渲染；用样式类 muted 控制外观。 --><span v-if="statusText" class="muted"><!-- 把 statusText 的当前值转成文字显示。 -->{{ statusText }}</span>
      </div>
      <!-- 显示可展开的详情区域；仅当 process.length 成立时渲染；用样式类 agent-process 控制外观。 -->
      <details v-if="process.length" class="agent-process"><!-- 显示详情区域的展开入口。 --><summary><!-- 显示应用Icon（AppIcon）组件；name 取自 processing ? 'refresh' : 'check'；size 取自 14。 --><AppIcon :name="processing ? 'refresh' : 'check'" :size="14" /><!-- 显示行内文字区域。 --><span><!-- 把 processing ? processing.label + '…' : '查看处理过程' 的当前值转成文字显示。 -->{{ processing ? processing.label + '…' : '查看处理过程' }}</span><!-- 显示辅助说明文字。 --><small><!-- 把 process.length 的当前值转成文字显示。 -->{{ process.length }} 个步骤</small></summary><!-- 显示有序列表。 --><ol><!-- 显示一项列表内容；按 (step, index) in process 为每条数据重复生成内容；key 取自 step.id || index；class 取自 step.status。 --><li v-for="(step, index) in process" :key="step.id || index" :class="step.status"><!-- 显示应用Icon（AppIcon）组件；name 取自 toolIcon(step.status)；size 取自 14。 --><AppIcon :name="toolIcon(step.status)" :size="14" /><!-- 显示一个布局容器。 --><div><!-- 显示重点文字。 --><strong><!-- 把 step.label 的当前值转成文字显示。 -->{{ step.label }}</strong><!-- 显示一段文字；仅当 step.summary 成立时渲染。 --><p v-if="step.summary"><!-- 把 step.summary 的当前值转成文字显示。 -->{{ step.summary }}</p></div><!-- 显示辅助说明文字；仅当 step.durationMs != null 成立时渲染。 --><small v-if="step.durationMs != null"><!-- 把 step.durationMs < 1000 ? step.durationMs + ' 毫秒' : (step.durationMs / 1000).toFixed(2) + ' 秒' 的当前值转成文字显示。 -->{{ step.durationMs < 1000 ? step.durationMs + ' 毫秒' : (step.durationMs / 1000).toFixed(2) + ' 秒' }}</small><!-- 显示辅助说明文字；前一条件不成立且 step.status === 'cancelled' 成立时渲染。 --><small v-else-if="step.status === 'cancelled'">已停止</small><!-- 显示辅助说明文字；前一条件不成立且 step.status === 'error' 成立时渲染。 --><small v-else-if="step.status === 'error'">未完成</small></li></ol></details>
      <!-- 显示一个布局容器；仅当 answer?.pending && !answer.content 成立时渲染；用样式类 agent-skeleton 控制外观；aria-label 提供无障碍说明 等待回复。 -->
      <div
        v-if="answer?.pending && !answer.content"
        class="agent-skeleton"
        aria-label="等待回复"
      >
        <!-- 显示行内文字区域。 -->
        <span /><!-- 显示行内文字区域。 --><span /><!-- 显示行内文字区域。 --><span />
      </div>
      <!-- 显示成长助手Markdown（AgentMarkdown）组件；仅当 answer?.content 成立时渲染；content 取自 answer.content。 -->
      <AgentMarkdown v-if="answer?.content" :content="answer.content" />
      <!-- 显示一个布局容器；仅当 answer?.citations?.length 成立时渲染；用样式类 agent-citations 控制外观；aria-label 提供无障碍说明 引用来源。 -->
      <div v-if="answer?.citations?.length" class="agent-citations" aria-label="引用来源">
        <!-- 显示重点文字。 -->
        <strong>参考资料</strong>
        <!-- 显示应用内部页面链接；按 source in answer.citations 为每条数据重复生成内容；key 取自 source.chunkId；to 取自 `/agent/knowledge/${source.documentId}?revisionId=${source.revisionId}&chunkId=${source.chunkId}`。 -->
        <RouterLink v-for="source in answer.citations" :key="source.chunkId"
          :to="`/agent/knowledge/${source.documentId}?revisionId=${source.revisionId}&chunkId=${source.chunkId}`">
          <!-- 把 source.label 的当前值转成文字显示。 -->
          {{ source.label }} · <!-- 把 source.title 的当前值转成文字显示。 --><span v-if="source.knowledgeBaseName">{{ source.knowledgeBaseName }} / </span>{{ source.title }}<!-- 显示行内文字区域；仅当 source.pageNumber 成立时渲染。 --><span v-if="source.pageNumber"> · 第 <!-- 把 source.pageNumber 的当前值转成文字显示。 -->{{ source.pageNumber }} 页</span>
        </RouterLink>
      </div>
      <!-- 显示应用内部页面链接；仅当 answer?.usedMemoryIds?.length 成立时渲染；用样式类 muted 控制外观；to 固定为 /agent/memories。 -->
      <RouterLink v-if="answer?.usedMemoryIds?.length" class="muted" to="/agent/memories">本轮参考了 <!-- 把 answer.usedMemoryIds.length 的当前值转成文字显示。 -->{{ answer.usedMemoryIds.length }} 条已确认记忆</RouterLink>
      <!-- 显示一段文字；仅当 answer?.stopped 成立时渲染；用样式类 muted agent-inline-note 控制外观；role 固定为 status。 -->
      <p v-if="answer?.stopped" class="muted agent-inline-note" role="status">
        生成已停止，已收到的内容已保留。
      </p>
      <!-- 显示Element Plus 的 alert 组件；仅当 answer?.error 成立时渲染；title 取自 answer.error；类型设为 error；closable 取自 false；启用 show-icon 属性。 -->
      <el-alert
        v-if="answer?.error"
        :title="answer.error"
        type="error"
        :closable="false"
        show-icon
      />
      <!-- 显示Element Plus 的 alert 组件；前一条件不成立且 notice 成立时渲染；title 取自 notice；类型设为 warning；closable 取自 false；启用 show-icon 属性。 -->
      <el-alert
        v-else-if="notice"
        :title="notice"
        type="warning"
        :closable="false"
        show-icon
      />
      <!-- 显示一个布局容器；仅当 hasActions 成立时渲染；用样式类 agent-answer-actions 控制外观。 -->
      <div v-if="hasActions" class="agent-answer-actions">
        <!-- 显示Element Plus 的 button 组件；仅当 answer?.content 成立时渲染；启用 text 属性；size 固定为 small；click 事件触发时执行 copy；aria-label 提供无障碍说明 复制回复；title 固定为 复制回复。 -->
        <el-button v-if="answer?.content" text size="small" @click="copy"
          aria-label="复制回复" title="复制回复"><!-- 显示应用Icon（AppIcon）组件；name 固定为 copy；size 取自 16。 --><AppIcon name="copy" :size="16" /></el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 retryable 成立时渲染；启用 text 属性；size 固定为 small；disabled 取自 busy；click 事件触发时执行 $emit('retry', turn.question)。 -->
        <el-button
          v-if="retryable"
          text
          size="small"
          :disabled="busy"
          @click="$emit('retry', turn.question)"
          ><!-- 显示应用Icon（AppIcon）组件；name 固定为 refresh；size 取自 14。 --><AppIcon name="refresh" :size="14" />再次提问</el-button
        >
        <!-- 显示行内文字区域；仅当 answer?.traceId 成立时渲染；用样式类 muted agent-trace 控制外观。 -->
        <span v-if="answer?.traceId" class="muted agent-trace"
          >追踪号：<!-- 把 answer.traceId 的当前值转成文字显示。 -->{{ answer.traceId }}</span
        >
      </div>
    </div>
  </article>
</template>

<style scoped>
/* 以下规则应用于 .agent-process 选中的元素。 */
.agent-process { /* 设置元素外侧留白；取值 0 0 16px。 */ margin: 0 0 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-process summary 选中的元素。 */
.agent-process summary { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; /* 设置宽度；取值 fit-content。 */ width: fit-content; /* 设置 CSS 属性 list-style；取值 none。 */ list-style: none; /* 设置内容与边框之间的内侧留白；取值 4px 0。 */ padding: 4px 0; }
/* 以下规则应用于 .agent-process summary::-webkit-details-marker 选中的元素。 */
.agent-process summary::-webkit-details-marker { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 none。 */ display: none; }
/* 以下规则应用于 .agent-process summary small 选中的元素。 */
.agent-process summary small { /* 设置整体透明度；取值 .7。 */ opacity: .7; }
/* 以下规则应用于 .agent-process ol 选中的元素。 */
.agent-process ol { /* 设置 CSS 属性 list-style；取值 none。 */ list-style: none; /* 设置内容与边框之间的内侧留白；取值 12px。 */ padding: 12px; /* 设置元素外侧留白；取值 8px 0 0。 */ margin: 8px 0 0; /* 设置背景颜色、图片或渐变；取值 var(—aw-surface-muted)。 */ background: var(--aw-surface-muted); /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; }
/* 以下规则应用于 .agent-process li 选中的元素。 */
.agent-process li { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 flex-start。 */ align-items: flex-start; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置内容与边框之间的内侧留白；取值 8px 0。 */ padding: 8px 0; }
/* 以下规则应用于 .agent-process li > div 选中的元素。 */
.agent-process li > div { /* 设置条目如何占用、伸展或压缩可用空间；取值 1。 */ flex: 1; /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-process li p 选中的元素。 */
.agent-process li p { /* 设置元素外侧留白；取值 4px 0 0。 */ margin: 4px 0 0; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-process li strong, .agent-process li small 选中的元素。 */
.agent-process li strong, .agent-process li small { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-process li.error 选中的元素。 */
.agent-process li.error { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-danger)。 */ color: var(--aw-danger); }
/* 以下规则应用于 .agent-process li.limited 选中的元素。 */
.agent-process li.limited { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .agent-citations 选中的元素。 */
.agent-citations { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置元素外侧留白；取值 16px 0。 */ margin: 16px 0; /* 设置内容与边框之间的内侧留白；取值 12px。 */ padding: 12px; /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color-light)。 */ background: var(--el-fill-color-light); /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .agent-citations a 选中的元素。 */
.agent-citations a { /* 设置文字或继承该颜色的图形颜色；取值 var(—el-color-primary)。 */ color: var(--el-color-primary); }
/* 以下规则应用于 .agent-turn + .agent-turn 选中的元素。 */
.agent-turn + .agent-turn {
  /* 设置上方外侧留白；取值 32px。 */
  margin-top: 32px;
}
/* 以下规则应用于 .agent-question 选中的元素。 */
.agent-question {
  /* 设置左侧外侧留白；取值 auto。 */
  margin-left: auto;
  /* 设置宽度；取值 fit-content。 */
  width: fit-content;
  /* 限制最大宽度；取值 85%。 */
  max-width: 85%;
  /* 设置内容与边框之间的内侧留白；取值 12px 16px。 */
  padding: 12px 16px;
  /* 设置背景颜色、图片或渐变；取值 var(—aw-surface-muted)。 */
  background: var(--aw-surface-muted);
  /* 设置边角圆弧大小；取值 12px。 */
  border-radius: 12px;
}
/* 以下规则应用于 .agent-speaker 选中的元素。 */
.agent-speaker {
  /* 设置文字或继承该颜色的图形颜色；取值 var(—el-color-primary)。 */
  color: var(--el-color-primary);
  /* 设置字号；取值 12px。 */
  font-size: 12px;
  /* 设置每行文字的高度；取值 16px。 */
  line-height: 16px;
}
/* 以下规则应用于 .agent-question p 选中的元素。 */
.agent-question p {
  /* 决定空白和换行如何保留；取值 pre-wrap。 */
  white-space: pre-wrap;
  /* 设置 CSS 属性 overflow-wrap；取值 anywhere。 */
  overflow-wrap: anywhere;
  /* 设置字号；取值 14px。 */
  font-size: 14px;
  /* 设置每行文字的高度；取值 20px。 */
  line-height: 20px;
  /* 设置元素外侧留白；取值 0。 */
  margin: 0;
}
/* 以下规则应用于 .agent-answer 选中的元素。 */
.agent-answer {
  /* 设置上方外侧留白；取值 24px。 */
  margin-top: 24px;
}
/* 以下规则应用于 .agent-answer-label 选中的元素。 */
.agent-answer-label {
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */
  display: flex;
  /* 决定一行放不下时是否换行；取值 wrap。 */
  flex-wrap: wrap;
  /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */
  align-items: center;
  /* 设置相邻布局条目之间的间隔；取值 8px。 */
  gap: 8px;
  /* 设置下方外侧留白；取值 12px。 */
  margin-bottom: 12px;
  /* 设置字号；取值 14px。 */
  font-size: 14px;
  /* 设置每行文字的高度；取值 20px。 */
  line-height: 20px;
}
/* 以下规则应用于 .agent-answer-label .aw-icon 选中的元素。 */
.agent-answer-label .aw-icon {
  /* 设置文字或继承该颜色的图形颜色；取值 var(—el-color-primary)。 */
  color: var(--el-color-primary);
}
/* 以下规则应用于 .agent-answer-label .muted 选中的元素。 */
.agent-answer-label .muted {
  /* 设置字号；取值 12px。 */
  font-size: 12px;
  /* 设置每行文字的高度；取值 16px。 */
  line-height: 16px;
}
/* 以下规则应用于 .agent-answer :deep(.el-alert) 选中的元素。 */
.agent-answer :deep(.el-alert) {
  /* 设置上方外侧留白；取值 16px。 */
  margin-top: 16px;
}
/* 以下规则应用于 .agent-tools 选中的元素。 */
.agent-tools {
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */
  display: flex;
  /* 决定一行放不下时是否换行；取值 wrap。 */
  flex-wrap: wrap;
  /* 设置相邻布局条目之间的间隔；取值 8px。 */
  gap: 8px;
  /* 设置元素外侧留白；取值 0 0 12px。 */
  margin: 0 0 12px;
  /* 设置字号；取值 12px。 */
  font-size: 12px;
  /* 设置每行文字的高度；取值 18px。 */
  line-height: 18px;
}
/* 以下规则应用于 .agent-tool 选中的元素。 */
.agent-tool {
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */
  display: inline-flex;
  /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */
  align-items: center;
  /* 设置相邻布局条目之间的间隔；取值 4px。 */
  gap: 4px;
  /* 设置内容与边框之间的内侧留白；取值 2px 8px。 */
  padding: 2px 8px;
  /* 设置边角圆弧大小；取值 999px。 */
  border-radius: 999px;
  /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color-light)。 */
  background: var(--el-fill-color-light);
  /* 设置文字或继承该颜色的图形颜色；取值 var(—el-text-color-secondary)。 */
  color: var(--el-text-color-secondary);
}
/* 以下规则应用于 .agent-tool.running .aw-icon 选中的元素。 */
.agent-tool.running .aw-icon {
  /* 设置使用的动画及其时间规则；取值 agent-tool-spin 1500ms var(—aw-motion) infinite。 */
  animation: agent-tool-spin 1500ms var(--aw-motion) infinite;
}
/* 以下规则应用于 .agent-tool.error 选中的元素。 */
.agent-tool.error {
  /* 设置文字或继承该颜色的图形颜色；取值 var(—el-color-danger)。 */
  color: var(--el-color-danger);
}
/* 定义动画在不同进度时的外观：agent-tool-spin。 */
@keyframes agent-tool-spin {
  /* 以下规则应用于 to 选中的元素。 */
  to {
    /* 对显示位置、角度或缩放做变换；取值 rotate(360deg)。 */
    transform: rotate(360deg);
  }
}
/* 以下规则应用于 .agent-inline-note 选中的元素。 */
.agent-inline-note {
  /* 设置字号；取值 14px。 */
  font-size: 14px;
  /* 设置每行文字的高度；取值 20px。 */
  line-height: 20px;
}
/* 以下规则应用于 .agent-answer-actions 选中的元素。 */
.agent-answer-actions {
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */
  display: flex;
  /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */
  align-items: center;
  /* 决定一行放不下时是否换行；取值 wrap。 */
  flex-wrap: wrap;
  /* 设置相邻布局条目之间的间隔；取值 8px。 */
  gap: 8px;
  /* 设置上方外侧留白；取值 12px。 */
  margin-top: 12px;
  /* 设置字号；取值 12px。 */
  font-size: 12px;
  /* 设置每行文字的高度；取值 16px。 */
  line-height: 16px;
}
/* 以下规则应用于 .agent-answer-actions :deep(.el-button + .el-button) 选中的元素。 */
.agent-answer-actions :deep(.el-button + .el-button) {
  /* 设置左侧外侧留白；取值 0。 */
  margin-left: 0;
}
/* 以下规则应用于 .agent-answer-actions :deep(.el-button) 选中的元素。 */
.agent-answer-actions :deep(.el-button) {
  /* 设置相邻布局条目之间的间隔；取值 4px。 */
  gap: 4px;
}
/* 以下规则应用于 .agent-trace 选中的元素。 */
.agent-trace {
  /* 设置 CSS 属性 overflow-wrap；取值 anywhere。 */
  overflow-wrap: anywhere;
}
/* 以下规则应用于 .agent-skeleton 选中的元素。 */
.agent-skeleton {
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */
  display: grid;
  /* 设置相邻布局条目之间的间隔；取值 8px。 */
  gap: 8px;
}
/* 以下规则应用于 .agent-skeleton span 选中的元素。 */
.agent-skeleton span {
  /* 设置高度；取值 16px。 */
  height: 16px;
  /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color)。 */
  background: var(--el-fill-color);
  /* 设置边角圆弧大小；取值 4px。 */
  border-radius: 4px;
  /* 设置使用的动画及其时间规则；取值 agent-wait 1500ms cubic-bezier(0.22, 1, 0.36, 1) infinite alternate。 */
  animation: agent-wait 1500ms cubic-bezier(0.22, 1, 0.36, 1) infinite alternate;
}
/* 以下规则应用于 .agent-skeleton span:nth-child(2) 选中的元素。 */
.agent-skeleton span:nth-child(2) {
  /* 设置宽度；取值 85%。 */
  width: 85%;
}
/* 以下规则应用于 .agent-skeleton span:nth-child(3) 选中的元素。 */
.agent-skeleton span:nth-child(3) {
  /* 设置宽度；取值 64%。 */
  width: 64%;
}
/* 定义动画在不同进度时的外观：agent-wait。 */
@keyframes agent-wait {
  /* 以下规则应用于 from 选中的元素。 */
  from {
    /* 设置整体透明度；取值 0.4。 */
    opacity: 0.4;
  }
  /* 以下规则应用于 to 选中的元素。 */
  to {
    /* 设置整体透明度；取值 1。 */
    opacity: 1;
  }
}
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(prefers-reduced-motion: reduce)。 */
@media (prefers-reduced-motion: reduce) {
  /* 以下规则应用于 .agent-skeleton span, .agent-tool.running .aw-icon 选中的元素。 */
  .agent-skeleton span, .agent-tool.running .aw-icon {
    /* 设置使用的动画及其时间规则；取值 none。 */
    animation: none;
  }
}
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width: 767px)。 */
@media (max-width: 767px) {
  /* 以下规则应用于 .agent-question 选中的元素。 */
  .agent-question {
    /* 限制最大宽度；取值 95%。 */
    max-width: 95%;
  }
}
</style>
