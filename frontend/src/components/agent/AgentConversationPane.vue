<!-- 渲染成长助手对话区域界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
/* 引入 ./AgentMessageItem.vue 提供的组件、状态函数或请求方法。 */
import AgentMessageItem from "./AgentMessageItem.vue";
import { composerEnterAction, insertComposerNewline } from "../../composables/composerKeyboard";

const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 对话（conversation）字段使用表达式 { type: Object, default: null } 的结果。 */
  conversation: { /* 类型（type）字段使用对象（Object）。 */ type: Object, /* 默认值（default）字段使用空值，表示当前还没有对应内容。 */ default: null },
  /* 对话轮次（turns）字段使用表达式 { type: Array, default: () => [] } 的结果。 */
  turns: { /* 类型（type）字段使用Array。 */ type: Array, /* 默认值（default）字段使用表达式 () => [] 的结果。 */ default: () => [] },
  /* 草案（draft）字段使用表达式 { type: String, default: "" } 的结果。 */
  draft: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 ''。 */ default: "" },
  /* 正在加载（loading）字段使用可为空的真假值（Boolean）。 */
  loading: Boolean,
  /* 正在加载Older（loadingOlder）字段使用可为空的真假值（Boolean）。 */
  loadingOlder: Boolean,
  /* 是否存在More（hasMore）字段使用可为空的真假值（Boolean）。 */
  hasMore: Boolean,
  /* sending字段使用可为空的真假值（Boolean）。 */
  sending: Boolean,
  /* 正在处理（busy）字段使用可为空的真假值（Boolean）。 */
  busy: Boolean,
  /* recovering字段使用可为空的真假值（Boolean）。 */
  recovering: Boolean,
  /* 是否可以发送（canSend）字段使用可为空的真假值（Boolean）。 */
  canSend: Boolean,
  /* 是否可以创建（canCreate）字段使用可为空的真假值（Boolean）。 */
  canCreate: Boolean,
  /* 是否可以更新（canUpdate）字段使用可为空的真假值（Boolean）。 */
  canUpdate: Boolean,
  /* 是否可以删除（canDelete）字段使用可为空的真假值（Boolean）。 */
  canDelete: Boolean,
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits([
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

const /* examples保存表达式 [ { label: "拆解一个目标", icon: "goal", prompt: "帮我把一个大目标拆成可行动的小步骤" }, { label: "安排今天", icon: "day", prompt: "结合我的目标和任务，帮我安排今… 的结果。 */ examples = [
  { /* 显示名称（label）字段使用固定文字 '拆解一个目标'。 */ label: "拆解一个目标", /* icon字段使用固定文字 'goal'。 */ icon: "goal", /* 模型提示词（prompt）字段使用固定文字 '帮我把一个大目标拆成可行动的小步骤'。 */ prompt: "帮我把一个大目标拆成可行动的小步骤" },
  { /* 显示名称（label）字段使用固定文字 '安排今天'。 */ label: "安排今天", /* icon字段使用固定文字 'day'。 */ icon: "day", /* 模型提示词（prompt）字段使用固定文字 '结合我的目标和任务，帮我安排今天的行动'。 */ prompt: "结合我的目标和任务，帮我安排今天的行动" },
  { /* 显示名称（label）字段使用固定文字 '建立好习惯'。 */ label: "建立好习惯", /* icon字段使用固定文字 'habit'。 */ icon: "habit", /* 模型提示词（prompt）字段使用固定文字 '我想养成一个新习惯，帮我制定容易坚持的起步计划'。 */ prompt: "我想养成一个新习惯，帮我制定容易坚持的起步计划" },
  { /* 显示名称（label）字段使用固定文字 '回顾最近的成长'。 */ label: "回顾最近的成长", /* icon字段使用固定文字 'statistics'。 */ icon: "statistics", /* 模型提示词（prompt）字段使用固定文字 '帮我回顾最近的投入和进步，找到下一步可以改进的地方'。 */ prompt: "帮我回顾最近的投入和进步，找到下一步可以改进的地方" },
];
const /* 输入（input）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ input = ref(null);
const /* 面板（panel）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ panel = ref(null);
const /* composerArea保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ composerArea = ref(null);
const /* composerHeight保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ composerHeight = ref(180);
let /* 声明 composerObserver，稍后再填入处理结果。 */ composerObserver;
const /* autoScroll保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ autoScroll = ref(true);
let /* frame保存固定数值 0。 */ frame = 0;
let /* anchor保存空值，表示当前还没有对应内容。 */ anchor = null;

const /* 草案值（draftValue）保存随相关状态变化自动重算的值。 */ draftValue = computed({
  /* 读取（get）字段使用表达式 () => props.draft 的结果。 */
  get: () => props.draft,
  /* 设置（set）字段使用表达式 (value) => emit("update:draft", value) 的结果。 */
  set: (value) => emit("update:draft", value),
});
const /* composer是否启用（composerEnabled）保存随相关状态变化自动重算的值。 */ composerEnabled = computed(
  () => props.canSend && !props.recovering,
);
const /* manageable保存随相关状态变化自动重算的值。 */ manageable = computed(
  () => !!props.conversation && (props.canUpdate || props.canDelete),
);
const /* 标题（title）保存随相关状态变化自动重算的值。 */ title = computed(() => props.conversation?.title || "新的对话");
const /* welcome保存随相关状态变化自动重算的值。 */ welcome = computed(() => !props.turns.length && !props.loading);
const /* subtitle保存随相关状态变化自动重算的值。 */ subtitle = computed(() => {
  /* 检查条件 props.loading 是否成立，成立时走这一分支。 */
  if (props.loading) /* 返回固定文字 '正在载入会话…'，当前函数到此结束。 */ return "正在载入会话…";
  /* 检查条件 !props.conversation 是否成立，成立时走这一分支。 */
  if (!props.conversation) /* 返回固定文字 '发送第一个问题后会保存为新会话'，当前函数到此结束。 */ return "发送第一个问题后会保存为新会话";
  /* 检查条件 props.recovering 是否成立，成立时走这一分支。 */
  if (props.recovering) /* 返回固定文字 '正在恢复上一轮生成'，当前函数到此结束。 */ return "正在恢复上一轮生成";
  /* 检查条件 props.busy 是否成立，成立时走这一分支。 */
  if (props.busy) /* 返回固定文字 '回复生成中'，当前函数到此结束。 */ return "回复生成中";
  /* 返回固定文字 '会话已保存，可以随时继续'，当前函数到此结束。 */
  return "会话已保存，可以随时继续";
});

/* focus：调用 focus 完成focus处理。 */
function focus() {
  /* 调用 input.value?.focus 完成focus处理。 */
  input.value?.focus();
}
/* 声明父组件通过引用可以调用的功能。 */
defineExpose({ /* 直接使用已有的focus作为同名字段。 */ focus });

/* 提交（submit）：调用 submit 完成提交处理。 */
function submit() {
  const /* 文字（text）保存优先使用前面的有效值，没有时采用后面的备用值：(props.draft || "").trim()。 */ text = (props.draft || "").trim();
  /* 检查条件 !text || props.sending || props.busy || !composerEnabled.value 是否成立，成立时走这一分支。 */
  if (!text || props.sending || props.busy || !composerEnabled.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("send", text);
}
/* keydown：调用 keydown 完成keydown处理。 */
function keydown(event) {
  const action = composerEnterAction(event);
  if (action === "native") return;
  event.preventDefault();
  if (action === "send") { submit(); return; }
  // 根据当前选区插入换行，更新草稿后把光标放回换行之后。
  const element = event.currentTarget;
  const next = insertComposerNewline(draftValue.value, element.selectionStart, element.selectionEnd);
  // 程序插入换行也遵守输入框的字数上限。
  if (element.maxLength >= 0 && next.value.length > element.maxLength) return;
  draftValue.value = next.value;
  nextTick(() => { element.focus(); element.setSelectionRange(next.caret, next.caret); });
}
/* useExample：调用 useExample 完成useExample处理。 */
async function useExample(example) {
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("update:draft", example.prompt);
  /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
  await nextTick();
  /* 调用 focus 完成focus处理。 */
  focus();
}
/* scrollBottom：调用 scrollBottom 完成scrollBottom处理。 */
function scrollBottom(force = false) {
  /* 检查条件 !force && !autoScroll.value 是否成立，成立时走这一分支。 */
  if (!force && !autoScroll.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 frame 是否成立，成立时走这一分支。 */
  if (frame) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把调用 requestAnimationFrame 完成请求AnimationFrame处理写入 frame。 */
  frame = requestAnimationFrame(() => {
    /* 把固定数值 0写入 frame。 */
    frame = 0;
    const /* element保存表达式 panel.value 的结果。 */ element = panel.value;
    /* 检查条件 element 是否成立，成立时走这一分支。 */
    if (element) /* 把表达式 element.scrollHeight 的结果写入 element.scrollTop。 */ element.scrollTop = element.scrollHeight;
  });
}
/* 事件发生时Scroll（onScroll）：调用 onScroll 完成事件发生时Scroll处理。 */
function onScroll() {
  const /* element保存表达式 panel.value 的结果。 */ element = panel.value;
  /* 检查条件 !element 是否成立，成立时走这一分支。 */
  if (!element) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把条件判断的结果：element.scrollHeight - element.scrollTop - element.clientHeight < 80写入 autoScroll.value。 */
  autoScroll.value =
    element.scrollHeight - element.scrollTop - element.clientHeight < 80;
  // 只在用户主动上翻时续页，避免首次渲染把全部历史一次拉完。
  /* 检查条件 !autoScroll.value && element.scrollTop < 120 是否成立，成立时走这一分支。 */
  if (!autoScroll.value && element.scrollTop < 120) /* 加载 Older。 */ loadOlder();
}
/* 加载Older（loadOlder）：加载 Older。 */
function loadOlder() {
  const /* element保存表达式 panel.value 的结果。 */ element = panel.value;
  /* 检查条件 !element || !props.hasMore || props.loadingOlder || props.loading 是否成立，成立时走这一分支。 */
  if (!element || !props.hasMore || props.loadingOlder || props.loading) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把沿 { length: props.turns.length, height: element.scrollHeight, firstId: props.turns[0]?.id || "", } 读取数据；中间对象为空时返回空值写入 anchor。 */
  anchor = {
    /* 长度（length）字段使用集合或文字的长度。 */
    length: props.turns.length,
    /* height字段使用表达式 element.scrollHeight 的结果。 */
    height: element.scrollHeight,
    /* 首次编号（firstId）字段使用沿 props.turns[0]?.id || "" 读取数据；中间对象为空时返回空值。 */
    firstId: props.turns[0]?.id || "",
  };
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("load-older");
}

/* 调用 watch( () => props.turns.length, async () => { await nextTick(); const element = panel.value; if (!element) return; if ( anchor && props.turns.length > anchor.length && (props.turns[0]?.id || "") !== anchor.firstId ) { // 前插历史后保持当前阅读位置，不让视口跳动。 element.scrollTop += element.scrollHeight - anchor.height; anchor = null; return; } anchor = null; if (props.turns.at 完成at处理。 */
watch(
  () => props.turns.length,
  async () => {
    /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
    await nextTick();
    const /* element保存表达式 panel.value 的结果。 */ element = panel.value;
    /* 检查条件 !element 是否成立，成立时走这一分支。 */
    if (!element) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 anchor && props.turns.length > anchor.length && (props.turns[0]?.id || "") !== anchor.firstId 是否成立，成立时走这一分支。 */
    if (
      anchor &&
      props.turns.length > anchor.length &&
      (props.turns[0]?.id || "") !== anchor.firstId
    ) {
      // 前插历史后保持当前阅读位置，不让视口跳动。
      /* 更新本次处理状态：element.scrollTop += element.scrollHeight - anchor.height。 */
      element.scrollTop += element.scrollHeight - anchor.height;
      /* 把空值，表示当前还没有对应内容写入 anchor。 */
      anchor = null;
      /* 返回空内容，当前函数到此结束。 */
      return;
    }
    /* 把空值，表示当前还没有对应内容写入 anchor。 */
    anchor = null;
    /* 检查条件 props.turns.at(-1)?.answer?.pending 是否成立，成立时走这一分支。 */
    if (props.turns.at(-1)?.answer?.pending) {
      /* 把真，表示条件成立或开关打开写入 autoScroll.value。 */
      autoScroll.value = true;
      /* 调用 scrollBottom 完成scrollBottom处理。 */
      scrollBottom(true);
      /* 返回空内容，当前函数到此结束。 */
      return;
    }
    /* 检查条件 autoScroll.value 是否成立，成立时走这一分支。 */
    if (autoScroll.value) /* 调用 scrollBottom 完成scrollBottom处理。 */ scrollBottom(true);
  },
);
/* 调用 watch( () => props.turns.at 完成at处理。 */
watch(
  () => props.turns.at(-1)?.answer?.content?.length || 0,
  () => {
    /* 检查条件 autoScroll.value 是否成立，成立时走这一分支。 */
    if (autoScroll.value) /* 调用 scrollBottom 完成scrollBottom处理。 */ scrollBottom();
  },
);
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => props.conversation?.id || "",
  async () => {
    /* 把真，表示条件成立或开关打开写入 autoScroll.value。 */
    autoScroll.value = true;
    /* 把空值，表示当前还没有对应内容写入 anchor。 */
    anchor = null;
    /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
    await nextTick();
    /* 调用 scrollBottom 完成scrollBottom处理。 */
    scrollBottom(true);
  },
);
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => props.loading,
  async (value) => {
    /* 检查条件 value 是否成立，成立时走这一分支。 */
    if (value) /* 返回空内容，当前函数到此结束。 */ return;
    /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
    await nextTick();
    /* 调用 scrollBottom 完成scrollBottom处理。 */
    scrollBottom(true);
  },
);
/* 调用 onMounted(() => { composerObserver = new ResizeObserver(([entry]) => { composerHeight.value = Math.ceil 完成ceil处理。 */
onMounted(() => {
  /* 把新建 ResizeObserver 实例写入 composerObserver。 */
  composerObserver = new ResizeObserver(([entry]) => {
    /* 把调用 Math.ceil 完成ceil处理写入 composerHeight.value。 */
    composerHeight.value = Math.ceil(entry.target.getBoundingClientRect().height);
    /* 检查条件 autoScroll.value 是否成立，成立时走这一分支。 */
    if (autoScroll.value) /* 等待页面完成当前一轮渲染，再读取或调整界面。 */ nextTick(() => scrollBottom());
  });
  /* 检查条件 composerArea.value 是否成立，成立时走这一分支。 */
  if (composerArea.value) /* 调用 composerObserver.observe 完成observe处理。 */ composerObserver.observe(composerArea.value);
});
/* 调用 onBeforeUnmount(() => { composerObserver?.disconnect 完成disconnect处理。 */
onBeforeUnmount(() => {
  /* 调用 composerObserver?.disconnect 完成disconnect处理。 */
  composerObserver?.disconnect();
  /* 检查条件 frame 是否成立，成立时走这一分支。 */
  if (frame) /* 取消 AnimationFrame。 */ cancelAnimationFrame(frame);
});
</script>

<template>
  <!-- 显示一个布局容器；用样式类 agent-pane-inner 控制外观；class 取自 { 'is-welcome': welcome }；style 取自 { '—agent-input-height': composerHeight + 'px' }。 -->
  <div class="agent-pane-inner" :class="{ 'is-welcome': welcome }" :style="{ '--agent-input-height': composerHeight + 'px' }">
    <!-- 显示顶部区域；用样式类 agent-pane-head 控制外观。 -->
    <header class="agent-pane-head">
      <!-- 显示Element Plus 的 button 组件；用样式类 agent-list-toggle 控制外观；启用 text 属性；aria-label 提供无障碍说明 打开会话列表；click 事件触发时执行 $emit('toggle-list')。 -->
      <el-button
        class="agent-list-toggle"
        text
        aria-label="打开会话列表"
        @click="$emit('toggle-list')"
        ><!-- 显示应用Icon（AppIcon）组件；name 固定为 list；size 取自 18。 --><AppIcon name="list" :size="18" /></el-button
      >
      <!-- 显示一个布局容器；用样式类 agent-pane-title 控制外观。 -->
      <div class="agent-pane-title">
        <!-- 显示二级标题。 -->
        <h2><!-- 把 title 的当前值转成文字显示。 -->{{ title }}</h2>
        <!-- 显示行内文字区域；仅当 recovering || busy 成立时渲染；role 固定为 status。 -->
        <span v-if="recovering || busy" role="status"><!-- 把 subtitle 的当前值转成文字显示。 -->{{ subtitle }}</span>
      </div>
      <!-- 显示一个布局容器；用样式类 agent-pane-actions 控制外观。 -->
      <div class="agent-pane-actions">
        <!-- 显示由父组件补充内容的插槽；name 固定为 tools。 -->
        <slot name="tools" />
        <!-- 显示Element Plus 的 dropdown 组件；仅当 manageable 成立时渲染；trigger 固定为 click；placement 固定为 bottom-end；command 事件触发时执行 (command) => $emit(command)。 -->
        <el-dropdown
          v-if="manageable"
          trigger="click"
          placement="bottom-end"
          @command="(command) => $emit(command)"
        >
          <!-- 显示Element Plus 的 button 组件；启用 text 属性；size 固定为 small；aria-label 提供无障碍说明 管理当前会话。 -->
          <el-button text size="small" aria-label="管理当前会话"
            ><!-- 显示应用Icon（AppIcon）组件；name 固定为 more；size 取自 18。 --><AppIcon name="more" :size="18" /></el-button
          >
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
          <template #dropdown>
            <!-- 显示Element Plus 的 dropdown-menu 组件。 -->
            <el-dropdown-menu>
              <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canUpdate 成立时渲染；command 固定为 rename。 -->
              <el-dropdown-item v-if="canUpdate" command="rename"
                ><!-- 显示行内文字区域；用样式类 agent-menu-item 控制外观。 --><span class="agent-menu-item"
                  ><!-- 显示应用Icon（AppIcon）组件；name 固定为 edit；size 取自 14。 --><AppIcon name="edit" :size="14" />重命名</span
                ></el-dropdown-item
              >
              <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canDelete 成立时渲染；command 固定为 remove；启用 divided 属性。 -->
              <el-dropdown-item v-if="canDelete" command="remove" divided
                ><!-- 显示行内文字区域；用样式类 agent-menu-item 控制外观。 --><span class="agent-menu-item"
                  ><!-- 显示应用Icon（AppIcon）组件；name 固定为 delete；size 取自 14。 --><AppIcon name="delete" :size="14" />删除会话</span
                ></el-dropdown-item
              >
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 显示Element Plus 的 alert 组件；仅当 recovering 成立时渲染；用样式类 agent-banner 控制外观；类型设为 warning；closable 取自 false；启用 show-icon 属性。 -->
    <el-alert
      v-if="recovering"
      class="agent-banner"
      type="warning"
      :closable="false"
      show-icon
    >
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 title 插槽。 -->
      <template #title>
        <!-- 显示行内文字区域；用样式类 agent-banner-text 控制外观。 -->
        <span class="agent-banner-text"
          >上一轮回复仍在生成，正在等待结果。</span
        >
        <!-- 显示Element Plus 的 button 组件；启用 text 属性；类型设为 primary；size 固定为 small；click 事件触发时执行 $emit('stop')。 -->
        <el-button text type="primary" size="small" @click="$emit('stop')"
          >停止生成</el-button
        >
      </template>
    </el-alert>

    <!-- 显示一个布局容器；把组件或元素引用保存为 panel，供脚本读取；用样式类 agent-messages 控制外观；role 固定为 log；aria-label 提供无障碍说明 会话消息；aria-busy 取自 busy || loading；scroll 事件触发时执行 onScroll。 -->
    <div
      ref="panel"
      class="agent-messages"
      role="log"
      aria-label="会话消息"
      :aria-busy="busy || loading"
      @scroll="onScroll"
    >
      <!-- 显示一个布局容器；用样式类 agent-message-column 控制外观。 -->
      <div class="agent-message-column">
      <!-- 显示一个布局容器；仅当 hasMore 成立时渲染；用样式类 agent-history-top 控制外观。 -->
      <div v-if="hasMore" class="agent-history-top">
        <!-- 显示Element Plus 的 button 组件；启用 text 属性；size 固定为 small；loading 取自 loadingOlder；click 事件触发时执行 loadOlder。 -->
        <el-button
          text
          size="small"
          :loading="loadingOlder"
          @click="loadOlder"
          >加载更早的消息</el-button
        >
      </div>
      <!-- 显示Element Plus 的 skeleton 组件；仅当 loading 成立时渲染；rows 取自 6；启用 animated 属性。 -->
      <el-skeleton v-if="loading" :rows="6" animated />
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 -->
      <template v-else>
        <!-- 显示成长助手消息单条数据（AgentMessageItem）组件；按 turn in turns 为每条数据重复生成内容；key 取自 turn.id；turn 取自 turn；busy 取自 sending || busy；retry 事件触发时执行 $emit('retry', $event)。 -->
        <AgentMessageItem
          v-for="turn in turns"
          :key="turn.id"
          :turn="turn"
          :busy="sending || busy"
          @retry="$emit('retry', $event)"
        />
        <!-- 显示一个布局容器；仅当 !turns.length 成立时渲染；用样式类 agent-empty 控制外观。 -->
        <div v-if="!turns.length" class="agent-empty">
          <!-- 显示一个布局容器；用样式类 agent-welcome-mark 控制外观。 -->
          <div class="agent-welcome-mark"><!-- 显示应用Icon（AppIcon）组件；name 固定为 sparkle；size 取自 36。 --><AppIcon name="sparkle" :size="36" /><!-- 显示行内文字区域。 --><span>你的个人成长助手</span></div>
          <!-- 显示一级标题。 -->
          <h1>今天，想向前走哪一步？</h1>
          <!-- 显示一段文字。 -->
          <p>聊聊你的目标、计划，或是一个还没理清的想法。</p>
        </div>
        <!-- 方案卡片放在消息滚动区内，长方案可以随消息一起滚动查看。 -->
        <!-- 显示由父组件补充内容的插槽；仅当 turns.length 成立时渲染；name 固定为 plan。 -->
        <slot v-if="turns.length" name="plan"></slot>
      </template>
      </div>
    </div>

    <!-- 显示一个布局容器；把组件或元素引用保存为 composerArea，供脚本读取；用样式类 agent-input-area 控制外观。 -->
    <div ref="composerArea" class="agent-input-area">
    <slot name="knowledge"></slot>
    <!-- 显示表单；用样式类 agent-composer 控制外观；submit 事件触发时执行 submit。 -->
    <form class="agent-composer" @submit.prevent="submit">
      <!-- 显示输入项的文字标签；用样式类 agent-input-label 控制外观；for 固定为 agent-question-input。 -->
      <label class="agent-input-label" for="agent-question-input"
        >输入你的问题</label
      >
      <!-- 显示Element Plus 的 input 组件；id 固定为 agent-question-input；把组件或元素引用保存为 input，供脚本读取；与 draftValue 双向同步；类型设为 textarea；autosize 取自 { minRows: 2, maxRows: 8 }；maxlength 固定为 8000；disabled 取自 !composerEnabled；未输入时提示 有什么想一起想想的？；resize 固定为 none；keydown 事件触发时执行 keydown。 -->
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
      <!-- 显示一个布局容器；用样式类 agent-composer-footer 控制外观。 -->
      <div class="agent-composer-footer">
        <!-- 显示行内文字区域；用样式类 agent-composer-context 控制外观。 -->
        <span class="agent-composer-context"><!-- 显示应用Icon（AppIcon）组件；name 固定为 sparkle；size 取自 16。 --><AppIcon name="sparkle" :size="16" /><!-- 显示行内文字区域。 --><span>成长助手</span><!-- 显示行内文字区域；用样式类 agent-composer-mode 控制外观。 --><span class="agent-composer-mode">基于你的目标与记录</span></span>
        <!-- 显示行内文字区域；仅当 draft.length > 7200 成立时渲染；用样式类 agent-draft-count 控制外观。 -->
        <span v-if="draft.length > 7200" class="agent-draft-count"><!-- 把 draft.length 的当前值转成文字显示。 -->{{ draft.length }}/8000</span>
        <!-- 显示Element Plus 的 button 组件；仅当 busy 成立时渲染；用样式类 agent-send 控制外观；native-type 固定为 button；aria-label 提供无障碍说明 停止生成；title 固定为 停止生成；click 事件触发时执行 $emit('stop')。 -->
        <el-button v-if="busy" class="agent-send" native-type="button" aria-label="停止生成" title="停止生成" @click="$emit('stop')"
          ><!-- 显示应用Icon（AppIcon）组件；name 固定为 stop；size 取自 16。 --><AppIcon name="stop" :size="16" /></el-button
        >
        <!-- 显示Element Plus 的 button 组件；前面条件均不成立时显示这个备用分支；用样式类 agent-send 控制外观；类型设为 primary；native-type 固定为 submit；aria-label 提供无障碍说明 发送消息；title 固定为 发送消息；loading 取自 sending；disabled 取自 !composerEnabled || !draft.trim()。 -->
        <el-button
          v-else
          class="agent-send"
          type="primary"
          native-type="submit"
          aria-label="发送消息"
          title="发送消息"
          :loading="sending"
          :disabled="!composerEnabled || !draft.trim()"
          ><!-- 显示应用Icon（AppIcon）组件；name 固定为 send；size 取自 20。 --><AppIcon name="send" :size="20" /></el-button
        >
      </div>
    </form>
    <!-- 显示一个布局容器；仅当 welcome 成立时渲染；用样式类 agent-examples 控制外观。 -->
    <div v-if="welcome" class="agent-examples">
      <!-- 显示可点击按钮；按 example in examples 为每条数据重复生成内容；key 取自 example.label；类型设为 button；disabled 取自 !composerEnabled；click 事件触发时执行 useExample(example)。 -->
      <button v-for="example in examples" :key="example.label" type="button" :disabled="!composerEnabled" @click="useExample(example)"><!-- 显示应用Icon（AppIcon）组件；name 取自 example.icon；size 取自 16。 --><AppIcon :name="example.icon" :size="16" /><!-- 把 example.label 的当前值转成文字显示。 -->{{ example.label }}</button>
    </div>
    <!-- 显示一段文字；用样式类 agent-disclaimer 控制外观。 -->
    <p class="agent-disclaimer"><!-- 把 !canSend ? '当前账号暂未开通对话权限' : 'Enter 发送，Ctrl + Enter 换行 · 建议请结合实际判断' 的当前值转成文字显示。 -->{{ !canSend ? '当前账号暂未开通对话权限' : 'Enter 发送，Ctrl + Enter 换行 · 建议请结合实际判断' }}</p>
    </div>
  </div>
</template>

<style scoped>
/* 以下规则应用于 .agent-pane-inner 选中的元素。 */
.agent-pane-inner { /* 决定元素是否参与普通排版，以及按谁定位；取值 relative。 */ position: relative; /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 条目的排列方向；取值 column。 */ flex-direction: column; /* 设置高度；取值 100%。 */ height: 100%; /* 限制最小高度；取值 0。 */ min-height: 0; /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-pane-head 选中的元素。 */
.agent-pane-head { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置内容与边框之间的内侧留白；取值 8px 32px 12px。 */ padding: 8px 32px 12px; /* 限制最小高度；取值 48px。 */ min-height: 48px; }
/* 以下规则应用于 .agent-pane-title 选中的元素。 */
.agent-pane-title { /* 设置条目如何占用、伸展或压缩可用空间；取值 1。 */ flex: 1; /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-pane-title h2 选中的元素。 */
.agent-pane-title h2 { /* 设置元素外侧留白；取值 0。 */ margin: 0; /* 决定内容超出边界时显示、裁切还是滚动；取值 hidden。 */ overflow: hidden; /* 决定文字被截断时是否显示省略号；取值 ellipsis。 */ text-overflow: ellipsis; /* 决定空白和换行如何保留；取值 nowrap。 */ white-space: nowrap; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置字重；取值 500。 */ font-weight: 500; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .agent-pane-title span 选中的元素。 */
.agent-pane-title span { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-pane-actions 选中的元素。 */
.agent-pane-actions { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; }
/* 以下规则应用于 .agent-pane-actions :deep(.el-button + .el-button) 选中的元素。 */
.agent-pane-actions :deep(.el-button + .el-button) { /* 设置左侧外侧留白；取值 0。 */ margin-left: 0; }
/* 以下规则应用于 .agent-pane-actions :deep(.el-button) 选中的元素。 */
.agent-pane-actions :deep(.el-button) { /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 .agent-list-toggle 选中的元素。 */
.agent-list-toggle { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 none。 */ display: none; }
/* 以下规则应用于 .agent-banner 选中的元素。 */
.agent-banner { /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置宽度；取值 min(100% - 48px, 800px)。 */ width: min(100% - 48px, 800px); /* 设置元素外侧留白；取值 8px auto。 */ margin: 8px auto; }
/* 以下规则应用于 .agent-banner :deep(.el-alert__title) 选中的元素。 */
.agent-banner :deep(.el-alert__title) { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 .agent-messages 选中的元素。 */
.agent-messages { /* 设置条目如何占用、伸展或压缩可用空间；取值 1。 */ flex: 1; /* 限制最小高度；取值 0。 */ min-height: 0; /* 限制最小宽度；取值 0。 */ min-width: 0; /* 决定纵向超出时的处理；取值 auto。 */ overflow-y: auto; /* 决定横向超出时的处理；取值 hidden。 */ overflow-x: hidden; /* 设置内容与边框之间的内侧留白；取值 24px 32px calc(var(—agent-input-height) + 24px)。 */ padding: 24px 32px calc(var(--agent-input-height) + 24px); /* 设置 CSS 属性 overscroll-behavior；取值 contain。 */ overscroll-behavior: contain; /* 设置 CSS 属性 scrollbar-width；取值 thin。 */ scrollbar-width: thin; }
/* 以下规则应用于 .agent-message-column 选中的元素。 */
.agent-message-column { /* 设置宽度；取值 100%。 */ width: 100%; /* 限制最大宽度；取值 800px。 */ max-width: 800px; /* 设置元素外侧留白；取值 0 auto。 */ margin: 0 auto; /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-history-top 选中的元素。 */
.agent-history-top { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置条目在主方向上的对齐与空余空间分配；取值 center。 */ justify-content: center; /* 设置下方外侧留白；取值 16px。 */ margin-bottom: 16px; }
/* 以下规则应用于 .agent-empty 选中的元素。 */
.agent-empty { /* 设置宽度；取值 100%。 */ width: 100%; /* 设置元素外侧留白；取值 0。 */ margin: 0; /* 设置文字横向对齐方式；取值 left。 */ text-align: left; }
/* 以下规则应用于 .agent-welcome-mark 选中的元素。 */
.agent-welcome-mark { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置下方外侧留白；取值 16px。 */ margin-bottom: 16px; }
/* 以下规则应用于 .agent-empty h1 选中的元素。 */
.agent-empty h1 { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); /* 设置字号；取值 36px。 */ font-size: 36px; /* 设置每行文字的高度；取值 40px。 */ line-height: 40px; /* 设置字重；取值 500。 */ font-weight: 500; /* 设置字符之间的间距；取值 -.04em。 */ letter-spacing: -.04em; /* 设置元素外侧留白；取值 0 0 16px。 */ margin: 0 0 16px; /* 设置 CSS 属性 text-wrap；取值 balance。 */ text-wrap: balance; }
/* 以下规则应用于 .agent-empty p 选中的元素。 */
.agent-empty p { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 16px。 */ font-size: 16px; /* 设置每行文字的高度；取值 24px。 */ line-height: 24px; /* 设置元素外侧留白；取值 0。 */ margin: 0; }
/* 以下规则应用于 .agent-input-area 选中的元素。 */
.agent-input-area { /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 设置左侧定位距离；取值 0。 */ left: 0; /* 设置右侧定位距离；取值 8px。 */ right: 8px; /* 设置下侧定位距离；取值 0。 */ bottom: 0; /* 设置重叠时的前后层级；取值 2。 */ z-index: 2; /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置内容与边框之间的内侧留白；取值 8px 32px 12px。 */ padding: 8px 32px 12px; /* 设置背景颜色、图片或渐变；取值 var(—aw-bg)。 */ background: var(--aw-bg); }
/* 以下规则应用于 .agent-composer 选中的元素。 */
.agent-composer { /* 限制最大宽度；取值 800px。 */ max-width: 800px; /* 设置元素外侧留白；取值 0 auto。 */ margin: 0 auto; /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding: 16px; /* 设置背景颜色、图片或渐变；取值 var(—aw-surface)。 */ background: var(--aw-surface); /* 设置边框宽度、线型与颜色；取值 1px solid var(—aw-border-strong)。 */ border: 1px solid var(--aw-border-strong); /* 设置边角圆弧大小；取值 16px。 */ border-radius: 16px; /* 设置元素投影；取值 0 4px 16px color-mix(in srgb, var(—aw-text) 4%, transparent)。 */ box-shadow: 0 4px 16px color-mix(in srgb, var(--aw-text) 4%, transparent); }
/* 以下规则应用于 .agent-composer:focus-within 选中的元素。 */
.agent-composer:focus-within { /* 设置边框颜色；取值 color-mix(in srgb, var(—aw-text) 45%, var(—aw-border))。 */ border-color: color-mix(in srgb, var(--aw-text) 45%, var(--aw-border)); }
/* 以下规则应用于 .agent-input-label 选中的元素。 */
.agent-input-label { /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 设置宽度；取值 1px。 */ width: 1px; /* 设置高度；取值 1px。 */ height: 1px; /* 决定内容超出边界时显示、裁切还是滚动；取值 hidden。 */ overflow: hidden; /* 设置 CSS 属性 clip-path；取值 inset(50%)。 */ clip-path: inset(50%); /* 决定空白和换行如何保留；取值 nowrap。 */ white-space: nowrap; }
/* 以下规则应用于 .agent-composer :deep(.el-textarea__inner) 选中的元素。 */
.agent-composer :deep(.el-textarea__inner) { /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; /* 设置元素投影；取值 none。 */ box-shadow: none; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); /* 设置字体选择顺序；取值 var(—aw-font)。 */ font-family: var(--aw-font); /* 设置字号；取值 16px。 */ font-size: 16px; /* 设置每行文字的高度；取值 24px。 */ line-height: 24px; /* 设置边角圆弧大小；取值 0。 */ border-radius: 0; }
/* 以下规则应用于 .agent-composer :deep(.el-textarea__inner:focus) 选中的元素。 */
.agent-composer :deep(.el-textarea__inner:focus) { /* 设置元素投影；取值 none。 */ box-shadow: none; /* 设置不占布局尺寸的轮廓线；取值 0。 */ outline: 0; }
/* 以下规则应用于 .agent-composer :deep(.el-textarea.is-disabled .el-textarea__inner) 选中的元素。 */
.agent-composer :deep(.el-textarea.is-disabled .el-textarea__inner) { /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; }
/* 以下规则应用于 .agent-composer-footer 选中的元素。 */
.agent-composer-footer { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; }
/* 以下规则应用于 .agent-composer-context 选中的元素。 */
.agent-composer-context { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */ display: inline-flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-composer-context > .aw-icon 选中的元素。 */
.agent-composer-context > .aw-icon { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); }
/* 以下规则应用于 .agent-composer-mode 选中的元素。 */
.agent-composer-mode { /* 设置左侧外侧留白；取值 8px。 */ margin-left: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); }
/* 以下规则应用于 .agent-draft-count 选中的元素。 */
.agent-draft-count { /* 设置左侧外侧留白；取值 auto。 */ margin-left: auto; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-send.el-button 选中的元素。 */
.agent-send.el-button { /* 设置宽度；取值 32px。 */ width: 32px; /* 设置高度；取值 32px。 */ height: 32px; /* 限制最小高度；取值 32px。 */ min-height: 32px; /* 设置左侧外侧留白；取值 auto。 */ margin-left: auto; /* 设置内容与边框之间的内侧留白；取值 4px。 */ padding: 4px; /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; }
/* 以下规则应用于 .agent-send.el-button.is-disabled 选中的元素。 */
.agent-send.el-button.is-disabled { /* 设置背景颜色、图片或渐变；取值 var(—aw-surface-muted)。 */ background: var(--aw-surface-muted); /* 设置边框颜色；取值 var(—aw-border)。 */ border-color: var(--aw-border); /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); }
/* 以下规则应用于 .agent-disclaimer 选中的元素。 */
.agent-disclaimer { /* 限制最大宽度；取值 800px。 */ max-width: 800px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); /* 设置文字横向对齐方式；取值 center。 */ text-align: center; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置元素外侧留白；取值 12px auto 0。 */ margin: 12px auto 0; }
/* 以下规则应用于 .agent-examples 选中的元素。 */
.agent-examples { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置条目在主方向上的对齐与空余空间分配；取值 center。 */ justify-content: center; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 限制最大宽度；取值 800px。 */ max-width: 800px; /* 设置元素外侧留白；取值 24px auto 0。 */ margin: 24px auto 0; }
/* 以下规则应用于 .agent-examples button 选中的元素。 */
.agent-examples button { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */ display: inline-flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置内容与边框之间的内侧留白；取值 8px 12px。 */ padding: 8px 12px; /* 设置边框宽度、线型与颜色；取值 1px solid var(—aw-border)。 */ border: 1px solid var(--aw-border); /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置边角圆弧大小；取值 9999px。 */ border-radius: 9999px; /* 设置 CSS 属性 font；取值 inherit。 */ font: inherit; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; }
/* 以下规则应用于 .agent-examples button:hover 选中的元素。 */
.agent-examples button:hover { /* 设置背景颜色、图片或渐变；取值 var(—aw-surface-muted)。 */ background: var(--aw-surface-muted); /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); }
/* 以下规则应用于 .agent-examples button:active 选中的元素。 */
.agent-examples button:active { /* 对显示位置、角度或缩放做变换；取值 scale(.98)。 */ transform: scale(.98); }
/* 以下规则应用于 .agent-examples button:disabled 选中的元素。 */
.agent-examples button:disabled { /* 设置鼠标移上来时的指针形状；取值 not-allowed。 */ cursor: not-allowed; /* 设置整体透明度；取值 .5。 */ opacity: .5; }
/* 以下规则应用于 .agent-menu-item 选中的元素。 */
.agent-menu-item { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */ display: inline-flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 .is-welcome 选中的元素。 */
.is-welcome { /* 决定纵向超出时的处理；取值 auto。 */ overflow-y: auto; }
/* 以下规则应用于 .is-welcome .agent-pane-head 选中的元素。 */
.is-welcome .agent-pane-head { /* 限制最小高度；取值 48px。 */ min-height: 48px; }
/* 以下规则应用于 .is-welcome .agent-pane-title h2 选中的元素。 */
.is-welcome .agent-pane-title h2 { /* 设置 CSS 属性 visibility；取值 hidden。 */ visibility: hidden; }
/* 以下规则应用于 .is-welcome .agent-messages 选中的元素。 */
.is-welcome .agent-messages { /* 设置条目如何占用、伸展或压缩可用空间；取值 0 0 auto。 */ flex: 0 0 auto; /* 限制最小高度；取值 auto。 */ min-height: auto; /* 决定内容超出边界时显示、裁切还是滚动；取值 visible。 */ overflow: visible; /* 设置上方外侧留白；取值 clamp(48px, 14dvh, 160px)。 */ margin-top: clamp(48px, 14dvh, 160px); /* 设置内容与边框之间的内侧留白；取值 0 32px 32px。 */ padding: 0 32px 32px; }
/* 以下规则应用于 .is-welcome .agent-input-area 选中的元素。 */
.is-welcome .agent-input-area { /* 决定元素是否参与普通排版，以及按谁定位；取值 static。 */ position: static; /* 设置下方外侧留白；取值 auto。 */ margin-bottom: auto; /* 设置顶部内侧留白；取值 0。 */ padding-top: 0; /* 设置底部内侧留白；取值 40px。 */ padding-bottom: 40px; }
/* 以下规则应用于 .is-welcome .agent-composer 选中的元素。 */
.is-welcome .agent-composer { /* 设置内容与边框之间的内侧留白；取值 24px。 */ padding: 24px; }
/* 以下规则应用于 .is-welcome .agent-disclaimer 选中的元素。 */
.is-welcome .agent-disclaimer { /* 设置上方外侧留白；取值 24px。 */ margin-top: 24px; }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width: 1199px)。 */
@media (max-width: 1199px) { /* 以下规则应用于 .agent-composer-mode 选中的元素。 */ .agent-composer-mode { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 none。 */ display: none; } }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width: 899px)。 */
@media (max-width: 899px) {
 /* 以下规则应用于 .agent-pane-head 选中的元素。 */
 .agent-pane-head { /* 设置内容与边框之间的内侧留白；取值 8px 16px。 */ padding: 8px 16px; }
 /* 以下规则应用于 .agent-messages 选中的元素。 */
 .agent-messages { /* 设置内容与边框之间的内侧留白；取值 16px 16px calc(var(—agent-input-height) + 16px)。 */ padding: 16px 16px calc(var(--agent-input-height) + 16px); }
 /* 以下规则应用于 .agent-input-area 选中的元素。 */
 .agent-input-area { /* 设置内容与边框之间的内侧留白；取值 8px 16px max(12px, env(safe-area-inset-bottom))。 */ padding: 8px 16px max(12px, env(safe-area-inset-bottom)); }
 /* 以下规则应用于 .agent-empty h1 选中的元素。 */
 .agent-empty h1 { /* 设置字号；取值 30px。 */ font-size: 30px; /* 设置每行文字的高度；取值 36px。 */ line-height: 36px; }
 /* 以下规则应用于 .is-welcome .agent-messages 选中的元素。 */
 .is-welcome .agent-messages { /* 设置内容与边框之间的内侧留白；取值 0 16px 24px。 */ padding: 0 16px 24px; /* 设置上方外侧留白；取值 clamp(32px, 10dvh, 96px)。 */ margin-top: clamp(32px, 10dvh, 96px); }
 /* 以下规则应用于 .is-welcome .agent-composer 选中的元素。 */
 .is-welcome .agent-composer { /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding: 16px; }
 /* 以下规则应用于 .agent-examples 选中的元素。 */
 .agent-examples { /* 设置条目在主方向上的对齐与空余空间分配；取值 flex-start。 */ justify-content: flex-start; /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; }
 /* 以下规则应用于 .agent-examples button 选中的元素。 */
 .agent-examples button { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
 /* 以下规则应用于 .agent-disclaimer 选中的元素。 */
 .agent-disclaimer { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
}
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-height: 600px)。 */
@media (max-height: 600px) { /* 以下规则应用于 .is-welcome .agent-messages 选中的元素。 */ .is-welcome .agent-messages { /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; } /* 以下规则应用于 .is-welcome .agent-input-area 选中的元素。 */ .is-welcome .agent-input-area { /* 设置底部内侧留白；取值 16px。 */ padding-bottom: 16px; } }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(prefers-reduced-motion: no-preference)。 */
@media (prefers-reduced-motion: no-preference) { /* 以下规则应用于 .agent-examples button 选中的元素。 */ .agent-examples button { /* 设置属性变化时的过渡；取值 transform 300ms var(—aw-motion)。 */ transition: transform 300ms var(--aw-motion); } }
</style>
