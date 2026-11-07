<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import AgentMessageItem from "./AgentMessageItem.vue";

const props = defineProps({
  conversation: { type: Object, default: null },
  turns: { type: Array, default: () => [] },
  draft: { type: String, default: "" },
  loading: Boolean,
  loadingOlder: Boolean,
  hasMore: Boolean,
  sending: Boolean,
  busy: Boolean,
  recovering: Boolean,
  canSend: Boolean,
  canCreate: Boolean,
  canUpdate: Boolean,
  canDelete: Boolean,
});
const emit = defineEmits([
  "update:draft",
  "send",
  "stop",
  "retry",
  "load-older",
  "rename",
  "remove",
  "create",
  "toggle-list",
]);

const examples = [
  { label: "拆解一个目标", icon: "goal", prompt: "帮我把一个大目标拆成可行动的小步骤" },
  { label: "安排今天", icon: "day", prompt: "结合我的目标和任务，帮我安排今天的行动" },
  { label: "建立好习惯", icon: "habit", prompt: "我想养成一个新习惯，帮我制定容易坚持的起步计划" },
  { label: "回顾最近的成长", icon: "statistics", prompt: "帮我回顾最近的投入和进步，找到下一步可以改进的地方" },
];
const input = ref(null);
const panel = ref(null);
const composerArea = ref(null);
const composerHeight = ref(180);
let composerObserver;
const autoScroll = ref(true);
let frame = 0;
let anchor = null;

const draftValue = computed({
  get: () => props.draft,
  set: (value) => emit("update:draft", value),
});
const composerEnabled = computed(
  () => props.canSend && !props.recovering,
);
const manageable = computed(
  () => !!props.conversation && (props.canUpdate || props.canDelete),
);
const title = computed(() => props.conversation?.title || "新的对话");
const welcome = computed(() => !props.turns.length && !props.loading);
const subtitle = computed(() => {
  if (props.loading) return "正在载入会话…";
  if (!props.conversation) return "发送第一个问题后会保存为新会话";
  if (props.recovering) return "正在恢复上一轮生成";
  if (props.busy) return "回复生成中";
  return "会话已保存，可以随时继续";
});

function focus() {
  input.value?.focus();
}
defineExpose({ focus });

function submit() {
  const text = (props.draft || "").trim();
  if (!text || props.sending || props.busy || !composerEnabled.value) return;
  emit("send", text);
}
function keydown(event) {
  if (
    event.key === "Enter" &&
    !event.shiftKey &&
    !event.isComposing
  ) {
    event.preventDefault();
    submit();
  }
}
async function useExample(example) {
  emit("update:draft", example.prompt);
  await nextTick();
  focus();
}
function scrollBottom(force = false) {
  if (!force && !autoScroll.value) return;
  if (frame) return;
  frame = requestAnimationFrame(() => {
    frame = 0;
    const element = panel.value;
    if (element) element.scrollTop = element.scrollHeight;
  });
}
function onScroll() {
  const element = panel.value;
  if (!element) return;
  autoScroll.value =
    element.scrollHeight - element.scrollTop - element.clientHeight < 80;
  // 只在用户主动上翻时续页，避免首次渲染把全部历史一次拉完。
  if (!autoScroll.value && element.scrollTop < 120) loadOlder();
}
function loadOlder() {
  const element = panel.value;
  if (!element || !props.hasMore || props.loadingOlder || props.loading) return;
  anchor = {
    length: props.turns.length,
    height: element.scrollHeight,
    firstId: props.turns[0]?.id || "",
  };
  emit("load-older");
}

watch(
  () => props.turns.length,
  async () => {
    await nextTick();
    const element = panel.value;
    if (!element) return;
    if (
      anchor &&
      props.turns.length > anchor.length &&
      (props.turns[0]?.id || "") !== anchor.firstId
    ) {
      // 前插历史后保持当前阅读位置，不让视口跳动。
      element.scrollTop += element.scrollHeight - anchor.height;
      anchor = null;
      return;
    }
    anchor = null;
    if (props.turns.at(-1)?.answer?.pending) {
      autoScroll.value = true;
      scrollBottom(true);
      return;
    }
    if (autoScroll.value) scrollBottom(true);
  },
);
watch(
  () => props.turns.at(-1)?.answer?.content?.length || 0,
  () => {
    if (autoScroll.value) scrollBottom();
  },
);
watch(
  () => props.conversation?.id || "",
  async () => {
    autoScroll.value = true;
    anchor = null;
    await nextTick();
    scrollBottom(true);
  },
);
watch(
  () => props.loading,
  async (value) => {
    if (value) return;
    await nextTick();
    scrollBottom(true);
  },
);
onMounted(() => {
  composerObserver = new ResizeObserver(([entry]) => {
    composerHeight.value = Math.ceil(entry.target.getBoundingClientRect().height);
    if (autoScroll.value) nextTick(() => scrollBottom());
  });
  if (composerArea.value) composerObserver.observe(composerArea.value);
});
onBeforeUnmount(() => {
  composerObserver?.disconnect();
  if (frame) cancelAnimationFrame(frame);
});
</script>

<template>
  <div class="agent-pane-inner" :class="{ 'is-welcome': welcome }" :style="{ '--agent-input-height': composerHeight + 'px' }">
    <header class="agent-pane-head">
      <el-button
        class="agent-list-toggle"
        text
        aria-label="打开会话列表"
        @click="$emit('toggle-list')"
        ><AppIcon name="list" :size="18" /></el-button
      >
      <div class="agent-pane-title">
        <h2>{{ title }}</h2>
        <span v-if="recovering || busy" role="status">{{ subtitle }}</span>
      </div>
      <div class="agent-pane-actions">
        <slot name="tools" />
        <el-dropdown
          v-if="manageable"
          trigger="click"
          placement="bottom-end"
          @command="(command) => $emit(command)"
        >
          <el-button text size="small" aria-label="管理当前会话"
            ><AppIcon name="more" :size="18" /></el-button
          >
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="canUpdate" command="rename"
                ><span class="agent-menu-item"
                  ><AppIcon name="edit" :size="14" />重命名</span
                ></el-dropdown-item
              >
              <el-dropdown-item v-if="canDelete" command="remove" divided
                ><span class="agent-menu-item"
                  ><AppIcon name="delete" :size="14" />删除会话</span
                ></el-dropdown-item
              >
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <el-alert
      v-if="recovering"
      class="agent-banner"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        <span class="agent-banner-text"
          >上一轮回复仍在生成，正在等待结果。</span
        >
        <el-button text type="primary" size="small" @click="$emit('stop')"
          >停止生成</el-button
        >
      </template>
    </el-alert>

    <div
      ref="panel"
      class="agent-messages"
      role="log"
      aria-label="会话消息"
      :aria-busy="busy || loading"
      @scroll="onScroll"
    >
      <div class="agent-message-column">
      <div v-if="hasMore" class="agent-history-top">
        <el-button
          text
          size="small"
          :loading="loadingOlder"
          @click="loadOlder"
          >加载更早的消息</el-button
        >
      </div>
      <el-skeleton v-if="loading" :rows="6" animated />
      <template v-else>
        <AgentMessageItem
          v-for="turn in turns"
          :key="turn.id"
          :turn="turn"
          :busy="sending || busy"
          @retry="$emit('retry', $event)"
        />
        <div v-if="!turns.length" class="agent-empty">
          <div class="agent-welcome-mark"><AppIcon name="sparkle" :size="36" /><span>你的个人成长助手</span></div>
          <h1>今天，想向前走哪一步？</h1>
          <p>聊聊你的目标、计划，或是一个还没理清的想法。</p>
        </div>
        <!-- 方案卡片放在消息滚动区内，长方案可以随消息一起滚动查看。 -->
        <slot v-if="turns.length" name="plan"></slot>
      </template>
      </div>
    </div>

    <div ref="composerArea" class="agent-input-area">
    <form class="agent-composer" @submit.prevent="submit">
      <label class="agent-input-label" for="agent-question-input"
        >输入你的问题</label
      >
      <el-input
        id="agent-question-input"
        ref="input"
        v-model="draftValue"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 8 }"
        maxlength="8000"
        :disabled="!composerEnabled"
        placeholder="有什么想一起想想的？"
        resize="none"
        @keydown="keydown"
      />
      <div class="agent-composer-footer">
        <span class="agent-composer-context"><AppIcon name="sparkle" :size="16" /><span>成长助手</span><span class="agent-composer-mode">基于你的目标与记录</span></span>
        <span v-if="draft.length > 7200" class="agent-draft-count">{{ draft.length }}/8000</span>
        <el-button v-if="busy" class="agent-send" native-type="button" aria-label="停止生成" title="停止生成" @click="$emit('stop')"
          ><AppIcon name="stop" :size="16" /></el-button
        >
        <el-button
          v-else
          class="agent-send"
          type="primary"
          native-type="submit"
          aria-label="发送消息"
          title="发送消息"
          :loading="sending"
          :disabled="!composerEnabled || !draft.trim()"
          ><AppIcon name="send" :size="20" /></el-button
        >
      </div>
    </form>
    <div v-if="welcome" class="agent-examples">
      <button v-for="example in examples" :key="example.label" type="button" :disabled="!composerEnabled" @click="useExample(example)"><AppIcon :name="example.icon" :size="16" />{{ example.label }}</button>
    </div>
    <p class="agent-disclaimer">{{ !canSend ? '当前账号暂未开通对话权限' : 'Enter 发送，Shift + Enter 换行 · 建议请结合实际判断' }}</p>
    </div>
  </div>
</template>

<style scoped>
.agent-pane-inner { position: relative; display: flex; flex-direction: column; height: 100%; min-height: 0; min-width: 0; }
.agent-pane-head { display: flex; align-items: center; gap: 12px; flex-shrink: 0; padding: 8px 32px 12px; min-height: 48px; }
.agent-pane-title { flex: 1; min-width: 0; }
.agent-pane-title h2 { margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; line-height: 20px; font-weight: 500; color: var(--aw-text-secondary); }
.agent-pane-title span { display: block; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.agent-pane-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.agent-pane-actions :deep(.el-button + .el-button) { margin-left: 0; }
.agent-pane-actions :deep(.el-button) { gap: 8px; color: var(--aw-text-secondary); }
.agent-list-toggle { display: none; }
.agent-banner { flex-shrink: 0; width: min(100% - 48px, 800px); margin: 8px auto; }
.agent-banner :deep(.el-alert__title) { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.agent-messages { flex: 1; min-height: 0; min-width: 0; overflow-y: auto; overflow-x: hidden; padding: 24px 32px calc(var(--agent-input-height) + 24px); overscroll-behavior: contain; scrollbar-width: thin; }
.agent-message-column { width: 100%; max-width: 800px; margin: 0 auto; min-width: 0; }
.agent-history-top { display: flex; justify-content: center; margin-bottom: 16px; }
.agent-empty { width: 100%; margin: 0; text-align: left; }
.agent-welcome-mark { display: flex; align-items: center; gap: 12px; color: var(--aw-brand); font-size: 14px; line-height: 20px; margin-bottom: 16px; }
.agent-empty h1 { color: var(--aw-text); font-size: 36px; line-height: 40px; font-weight: 500; letter-spacing: -.04em; margin: 0 0 16px; text-wrap: balance; }
.agent-empty p { color: var(--aw-text-secondary); font-size: 16px; line-height: 24px; margin: 0; }
.agent-input-area { position: absolute; left: 0; right: 8px; bottom: 0; z-index: 2; flex-shrink: 0; padding: 8px 32px 12px; background: var(--aw-bg); }
.agent-composer { max-width: 800px; margin: 0 auto; padding: 16px; background: var(--aw-surface); border: 1px solid var(--aw-border-strong); border-radius: 16px; box-shadow: 0 4px 16px color-mix(in srgb, var(--aw-text) 4%, transparent); }
.agent-composer:focus-within { border-color: color-mix(in srgb, var(--aw-text) 45%, var(--aw-border)); }
.agent-input-label { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.agent-composer :deep(.el-textarea__inner) { padding: 0; background: transparent; box-shadow: none; color: var(--aw-text); font-family: var(--aw-font); font-size: 16px; line-height: 24px; border-radius: 0; }
.agent-composer :deep(.el-textarea__inner:focus) { box-shadow: none; outline: 0; }
.agent-composer :deep(.el-textarea.is-disabled .el-textarea__inner) { background: transparent; }
.agent-composer-footer { display: flex; gap: 12px; align-items: center; margin-top: 16px; }
.agent-composer-context { display: inline-flex; align-items: center; gap: 8px; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.agent-composer-context > .aw-icon { color: var(--aw-brand); }
.agent-composer-mode { margin-left: 8px; color: var(--aw-text-tertiary); }
.agent-draft-count { margin-left: auto; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.agent-send.el-button { width: 32px; height: 32px; min-height: 32px; margin-left: auto; padding: 4px; border-radius: 8px; }
.agent-send.el-button.is-disabled { background: var(--aw-surface-muted); border-color: var(--aw-border); color: var(--aw-text-tertiary); }
.agent-disclaimer { max-width: 800px; color: var(--aw-text-tertiary); text-align: center; font-size: 12px; line-height: 16px; margin: 12px auto 0; }
.agent-examples { display: flex; justify-content: center; flex-wrap: wrap; gap: 8px; max-width: 800px; margin: 24px auto 0; }
.agent-examples button { display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; border: 1px solid var(--aw-border); background: transparent; color: var(--aw-text-secondary); border-radius: 9999px; font: inherit; font-size: 14px; line-height: 20px; cursor: pointer; }
.agent-examples button:hover { background: var(--aw-surface-muted); color: var(--aw-text); }
.agent-examples button:active { transform: scale(.98); }
.agent-examples button:disabled { cursor: not-allowed; opacity: .5; }
.agent-menu-item { display: inline-flex; align-items: center; gap: 8px; }
.is-welcome { overflow-y: auto; }
.is-welcome .agent-pane-head { min-height: 48px; }
.is-welcome .agent-pane-title h2 { visibility: hidden; }
.is-welcome .agent-messages { flex: 0 0 auto; min-height: auto; overflow: visible; margin-top: clamp(48px, 14dvh, 160px); padding: 0 32px 32px; }
.is-welcome .agent-input-area { position: static; margin-bottom: auto; padding-top: 0; padding-bottom: 40px; }
.is-welcome .agent-composer { padding: 24px; }
.is-welcome .agent-disclaimer { margin-top: 24px; }
@media (max-width: 1199px) { .agent-composer-mode { display: none; } }
@media (max-width: 899px) {
 .agent-pane-head { padding: 8px 16px; }
 .agent-messages { padding: 16px 16px calc(var(--agent-input-height) + 16px); }
 .agent-input-area { padding: 8px 16px max(12px, env(safe-area-inset-bottom)); }
 .agent-empty h1 { font-size: 30px; line-height: 36px; }
 .is-welcome .agent-messages { padding: 0 16px 24px; margin-top: clamp(32px, 10dvh, 96px); }
 .is-welcome .agent-composer { padding: 16px; }
 .agent-examples { justify-content: flex-start; margin-top: 16px; }
 .agent-examples button { font-size: 12px; line-height: 16px; }
 .agent-disclaimer { font-size: 12px; line-height: 16px; }
}
@media (max-height: 600px) { .is-welcome .agent-messages { margin-top: 16px; } .is-welcome .agent-input-area { padding-bottom: 16px; } }
@media (prefers-reduced-motion: no-preference) { .agent-examples button { transition: transform 300ms var(--aw-motion); } }
</style>
