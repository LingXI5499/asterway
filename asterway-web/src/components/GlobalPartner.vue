<script setup lang="ts">
import { onMounted, onBeforeUnmount, watch, nextTick, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useAssistantStore } from '../store/assistant';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { localDate } from '../domain/dates';
import Modal from './Modal.vue';
import Icon from './Icon.vue';
import BundlePreview from './BundlePreview.vue';
const partner = useAssistantStore(),
  app = useAppStore(),
  proto = usePrototypeStore(),
  route = useRoute();
const history = ref<HTMLElement>();
const prompts = [
  { icon: 'target', label: '制定目标', text: '创建目标「30 天建立阅读习惯」' },
  { icon: 'calendar', label: '安排今天', text: '按四象限帮我安排今天 120 分钟的三件事' },
  { icon: 'checkCircle', label: '养成习惯', text: '添加习惯「每天阅读 20 分钟」' },
  { icon: 'notebook', label: '回顾记忆', text: '分析我的知识和记忆记录' },
  { icon: 'clock', label: '设置提醒', text: '每天 20:30 提醒我复盘' },
  { icon: 'chart', label: '成长复盘', text: '看看我本周的情况' },
];
watch(
  () => partner.messages.length,
  async () => {
    await nextTick();
    history.value?.scrollTo({ top: history.value.scrollHeight, behavior: 'smooth' });
  },
);
function reminderTick() {
  if (!app.data.profile.reminder) return;
  const date = localDate(),
    clock = new Date().toTimeString().slice(0, 5);
  const due = (proto.data.reminders || []).filter(
    (item) => item.enabled && item.time === clock && item.lastFiredDate !== date,
  );
  due.forEach((item) => (item.lastFiredDate = date));
  if (due.length) app.toast(`成长提醒：${due.map((item) => item.title).join('、')}`);
}
let interval: ReturnType<typeof setInterval>;
onMounted(() => {
  reminderTick();
  interval = setInterval(reminderTick, 15000);
});
onBeforeUnmount(() => clearInterval(interval));
function cancelDraft() {
  partner.bundle = null;
  partner.proposal = null;
  partner.pendingReminder = null;
}
</script>
<template>
  <button class="partner-launcher" aria-label="打开全局成长伙伴" @click="partner.open()">
    <Icon name="sparkles" :size="23" /><span>Aster 伙伴</span
    ><i v-if="partner.bundle || partner.proposal || partner.pendingReminder" />
  </button>
  <Modal
    v-if="partner.opened"
    class="partner-sheet"
    title="Aster · 全局成长伙伴"
    @close="partner.opened = false"
  >
    <div class="partner-context">
      <span class="local-badge">本地规则演示</span><span>当前：{{ route.meta.title }}</span
      ><RouterLink to="/assistant" @click="partner.opened = false"
        >完整工作台 <Icon name="external" :size="13"
      /></RouterLink>
    </div>
    <p class="partner-intro">一个伙伴，陪你串起目标、行动、习惯和记忆。</p>
    <div class="partner-tools">
      <button v-for="prompt in prompts" :key="prompt.label" @click="partner.ask(prompt.text)">
        <Icon :name="prompt.icon" :size="16" />{{ prompt.label }}
      </button>
    </div>
    <button class="partner-page-help" @click="partner.ask('这个页面怎么用？')">
      <Icon name="info" :size="15" />帮我理解当前页面
    </button>
    <div class="partner-history" ref="history" aria-live="polite">
      <div
        v-for="(message, index) in partner.messages"
        :key="index"
        :class="['partner-message', message.role]"
      >
        <span v-if="message.role === 'assistant'"><Icon name="sparkles" :size="15" /></span>
        <p>{{ message.text }}</p>
      </div>
    </div>
    <section
      v-if="partner.bundle || partner.proposal || partner.pendingReminder"
      class="partner-draft"
    >
      <div class="section-heading">
        <h3>操作草案</h3>
        <small>确认后执行</small>
      </div>
      <template v-if="partner.bundle"
        ><BundlePreview :bundle="partner.bundle" /><button
          class="button primary full-width"
          @click="partner.applyNewBundle"
        >
          确认批量创建
        </button></template
      >
      <template v-else-if="partner.proposal"
        ><h4>{{ partner.proposal.title }}</h4>
        <p>{{ partner.proposal.explanation }}</p>
        <article v-for="(change, index) in partner.proposal.changes" :key="index">
          <strong>{{
            change.op === 'reschedule' ? partner.taskTitle(change.taskId) : change.task.title
          }}</strong
          ><small>{{
            change.op === 'reschedule'
              ? `${change.to} ${change.toStartTime || ''}`
              : `${change.task.durationMinutes} 分钟`
          }}</small>
          <p>{{ change.reason }}</p>
        </article>
        <button
          v-if="partner.proposal.changes.length"
          class="button primary full-width"
          @click="partner.apply"
        >
          确认应用
        </button></template
      >
      <template v-else-if="partner.pendingReminder"
        ><strong>{{ partner.pendingReminder.title }}</strong>
        <p>每天 {{ partner.pendingReminder.time }} · 确认后开启站内提醒，仅应用打开时显示</p>
        <button class="button primary full-width" @click="partner.applyReminder">
          确认添加提醒
        </button></template
      >
      <button class="text-button" @click="cancelDraft">取消草案</button>
    </section>
    <details class="partner-reminders">
      <summary>
        我的提醒 <span>{{ proto.data.reminders?.length || 0 }}</span>
      </summary>
      <p v-if="!proto.data.reminders?.length">试试“每天 20:30 提醒我复盘”。仅应用打开时显示。</p>
      <div v-for="item in proto.data.reminders" :key="item.id">
        <label
          ><input type="checkbox" v-model="item.enabled" />{{ item.time }} · {{ item.title }}</label
        ><button
          class="icon-button"
          :aria-label="`删除提醒：${item.title}`"
          @click="proto.data.reminders = proto.data.reminders?.filter((r) => r.id !== item.id)"
        >
          <Icon name="trash" :size="15" />
        </button>
      </div>
    </details>
    <div class="partner-undo">
      <button
        v-if="proto.data.bundleLogs.at(-1) && !proto.data.bundleLogs.at(-1)?.undone"
        class="text-button"
        @click="proto.undoBundle"
      >
        撤销最近一次创建</button
      ><button
        v-if="app.data.agentActionLog.at(-1) && !app.data.agentActionLog.at(-1)?.undoneAt"
        class="text-button"
        @click="app.undo"
      >
        撤销最近一次调整
      </button>
    </div>
    <form class="partner-composer" @submit.prevent="partner.send">
      <textarea
        v-model="partner.input"
        rows="2"
        maxlength="500"
        aria-label="全局伙伴指令"
        placeholder="说说你想做什么，或让我记住你的情况…"
        @keydown.enter.exact.prevent="partner.send"
      /><button
        class="button primary"
        type="submit"
        aria-label="发送全局指令"
        :disabled="!partner.input.trim()"
      >
        <Icon name="send" :size="19" />
      </button>
    </form>
    <small class="partner-limit"
      >真实 AI、后台推送与联网操作将在后续接入；当前支持展示的本地操作。</small
    >
  </Modal>
</template>
