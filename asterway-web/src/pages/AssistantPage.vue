<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAssistantStore } from '../store/assistant';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { shortDate } from '../domain/dates';
import BundlePreview from '../components/BundlePreview.vue';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import EmptyState from '../components/EmptyState.vue';
const store = useAppStore(),
  proto = usePrototypeStore(),
  partner = useAssistantStore();
const {
  minutes,
  goalId,
  input,
  proposal,
  messages,
  bundle,
  conversationId,
  activeGoals,
  todayTasks,
  latest,
  pendingReminder,
} = storeToRefs(partner);
const {
  selectConversation,
  newConversation,
  assistantPlan,
  generate,
  send,
  apply,
  applyNewBundle,
  cancelProposal,
  taskTitle,
} = partner;
</script>
<template>
  <PageHeader
    title="成长伙伴"
    subtitle="计划不是一成不变的。让今天的行动，适应真实的你。"
    eyebrow="YOUR PERSONAL GROWTH AGENT"
    ><span class="local-badge"
      ><Icon name="shield" :size="16" />本地规则演示 · 未接入真实 AI</span
    ></PageHeader
  >
  <div class="assistant-layout">
    <section class="card chat-panel">
      <div class="chat-header">
        <span class="brand-symbol"><Icon name="sparkles" :size="21" /></span>
        <div>
          <h3>Aster · 你的成长伙伴</h3>
          <small>对话、执行、记忆 · 本地模拟 AI</small>
        </div>
        <span class="local-status">本地</span>
      </div>
      <div class="assistant-conversations">
        <select
          :value="conversationId"
          aria-label="选择成长会话"
          @change="selectConversation(($event.target as HTMLSelectElement).value)"
        >
          <option
            v-for="conversation in proto.data.conversations"
            :key="conversation.id"
            :value="conversation.id"
          >
            {{ conversation.title }}
          </option></select
        ><button class="text-button" @click="newConversation">+ 新对话</button>
      </div>
      <div class="assistant-suggestions">
        <button class="chip" @click="assistantPlan('为我制定 30 天雅思备考计划')">
          建立雅思计划</button
        ><button class="chip" @click="assistantPlan('添加习惯「每天阅读 20 分钟」')">
          帮我添加习惯</button
        ><button
          class="chip"
          @click="assistantPlan('记住：我每天有 40 分钟学习，想先理解框架再做练习')"
        >
          记住我的情况</button
        ><button class="chip" @click="assistantPlan('看看我的个人知识库')">查看个人记忆</button
        ><button class="chip" @click="assistantPlan('按四象限帮我安排今天 120 分钟的三件事')">
          按轻重缓急排今天
        </button>
      </div>
      <div class="quick-commands">
        <div class="quick-command">
          <span><Icon name="clock" :size="16" />今天时间不足</span>
          <div>
            <input
              v-model="minutes"
              type="number"
              min="1"
              max="1440"
              aria-label="今天剩余分钟"
            /><small>分钟</small
            ><button class="text-button" @click="generate('time')">
              帮我调整<Icon name="arrow" :size="14" />
            </button>
          </div>
        </div>
        <div class="quick-command">
          <span><Icon name="target" :size="16" />把目标拆成行动</span>
          <div>
            <select v-model="goalId" aria-label="选择拆解目标">
              <option value="" disabled>选择活跃目标</option>
              <option v-for="goal in activeGoals" :key="goal.id" :value="goal.id">
                {{ goal.title }}
              </option></select
            ><button class="text-button" :disabled="!goalId" @click="generate('goal')">
              拆解<Icon name="arrow" :size="14" />
            </button>
          </div>
        </div>
        <button class="quick-review" @click="generate('review')">
          <Icon name="chart" :size="16" />看看我本周的情况<Icon name="arrow" :size="14" />
        </button>
      </div>
      <div class="messages" aria-live="polite">
        <div v-for="(message, index) in messages" :key="index" :class="['message', message.role]">
          <span v-if="message.role === 'assistant'" class="message-avatar"
            ><Icon name="sparkles" :size="17"
          /></span>
          <p>{{ message.text }}</p>
        </div>
      </div>
      <form class="chat-input" @submit.prevent="send">
        <input
          v-model="input"
          maxlength="500"
          placeholder="创建计划、添加任务、记录信息，或一起复盘…"
          aria-label="给成长伙伴的指令"
        /><button class="button primary" type="submit" aria-label="发送指令">
          <Icon name="send" :size="18" />
        </button>
      </form>
      <label class="assistant-auto"
        ><input
          v-model="proto.data.assistantAutoAdd"
          type="checkbox"
        />允许发送后自动执行新增（仅本地演示，可撤销）</label
      >
      <small class="chat-disclaimer"
        >未连接大模型；默认预览后确认。开启自动执行仅影响新增，不自动删除或改期。</small
      >
    </section>
    <section class="proposal-column">
      <div class="context-card">
        <div class="section-heading">
          <h3>现在的你</h3>
          <Icon name="map" :size="17" />
        </div>
        <div class="context-stats">
          <div>
            <strong>{{ todayTasks.length }}</strong
            ><span>今日待办</span>
          </div>
          <div>
            <strong>{{ activeGoals.length }}</strong
            ><span>活跃目标</span>
          </div>
          <div>
            <strong
              >{{ todayTasks.reduce((n, t) => n + t.durationMinutes, 0) }}<small>m</small></strong
            ><span>预估时长</span>
          </div>
        </div>
      </div>
      <div class="card proposal-card">
        <div class="section-heading">
          <h2>计划变更预览</h2>
          <span class="demo-badge">确认后生效</span>
        </div>
        <template v-if="pendingReminder"
          ><h3>{{ pendingReminder.title }}</h3>
          <p>每天 {{ pendingReminder.time }} · 应用打开时显示站内提醒</p>
          <div class="proposal-actions">
            <button class="button secondary" @click="pendingReminder = null">取消提醒</button
            ><button class="button primary" @click="partner.applyReminder">确认添加提醒</button>
          </div></template
        >
        <template v-else-if="bundle"
          ><BundlePreview :bundle="bundle" />
          <div class="proposal-actions">
            <button class="button secondary" @click="bundle = null">取消新增</button
            ><button class="button primary" @click="applyNewBundle">确认批量创建</button>
          </div></template
        >
        <template v-else-if="proposal"
          ><h3>{{ proposal.title }}</h3>
          <p class="muted proposal-explanation">{{ proposal.explanation }}</p>
          <div v-if="proposal.keptTaskIds.length" class="kept-tasks">
            <strong
              ><Icon name="checkCircle" :size="15" />{{
                proposal.type === 'daily_plan' ? '准备排入时段' : '保留在今天'
              }}</strong
            >
            <p v-for="id in proposal.keptTaskIds" :key="id">{{ taskTitle(id) }}</p>
          </div>
          <div v-for="(change, index) in proposal.changes" :key="index" class="proposal-change">
            <span :class="['change-type', change.op === 'reschedule' ? 'reschedule' : 'create']">{{
              change.op === 'reschedule'
                ? change.toFocus === false
                  ? '重点'
                  : change.toStartTime
                    ? '安排'
                    : '延后'
                : '新增'
            }}</span>
            <div>
              <strong>{{
                change.op === 'reschedule' ? taskTitle(change.taskId) : change.task.title
              }}</strong>
              <div class="change-diff">
                <template v-if="change.op === 'reschedule'"
                  ><span>{{ change.from ? shortDate(change.from) : '待安排' }}</span
                  ><Icon name="arrow" :size="14" /><b
                    >{{ shortDate(change.to) }} {{ change.toStartTime || '' }}</b
                  ></template
                ><template v-else
                  ><b>{{
                    change.task.scheduledDate ? shortDate(change.task.scheduledDate) : '待安排'
                  }}</b
                  ><span>· {{ change.task.durationMinutes }} 分钟</span></template
                >
              </div>
              <p>{{ change.reason }}</p>
            </div>
          </div>
          <div class="proposal-actions">
            <button class="button secondary" @click="cancelProposal">
              {{ proposal.changes.length ? '取消方案' : '关闭摘要' }}</button
            ><button v-if="proposal.changes.length" class="button primary" @click="apply">
              <Icon name="check" :size="16" />确认应用
            </button>
          </div></template
        ><EmptyState
          v-else
          title="先聊聊，再决定"
          text="选择左侧指令，这里会展示具体改动。每一步都由你确认。"
          icon="sparkles"
        />
      </div>
      <div class="card assistant-memory">
        <div class="section-heading">
          <h3><Icon name="notebook" :size="17" />个人知识库</h3>
          <RouterLink class="text-button" to="/knowledge"
            >管理记忆<Icon name="arrow" :size="13"
          /></RouterLink>
        </div>
        <p v-for="note in proto.data.notes.slice(0, 2)" :key="note.id">
          <strong>{{ note.title }}</strong
          ><span>{{ note.content.slice(0, 90) }}</span>
        </p>
        <small class="muted">伙伴只引用你主动保存在本地的内容。</small>
      </div>
      <div v-if="proto.data.bundleLogs.length" class="card action-log">
        <div class="section-heading">
          <h3>最近一次批量创建</h3>
          <span class="demo-badge">{{
            proto.data.bundleLogs.at(-1)?.undone ? '已撤销' : '已建立'
          }}</span>
        </div>
        <p>{{ proto.data.bundleLogs.at(-1)?.title }}</p>
        <button
          v-if="!proto.data.bundleLogs.at(-1)?.undone"
          class="text-button"
          @click="proto.undoBundle"
        >
          <Icon name="undo" :size="15" />撤销最近一次创建
        </button>
      </div>
      <div v-if="latest" class="card action-log">
        <div class="section-heading">
          <h3>最近一次调整</h3>
          <span class="subtle-label">{{ latest.undoneAt ? '已撤销' : '已应用' }}</span>
        </div>
        <p>{{ latest.title }}</p>
        <small class="muted"
          >{{ new Date(latest.appliedAt).toLocaleString('zh-CN') }} ·
          {{ latest.afterTasks.length }} 项变更</small
        ><button v-if="!latest.undoneAt" class="text-button" @click="store.undo()">
          <Icon name="undo" :size="15" />撤销最近一次方案
        </button>
      </div>
    </section>
  </div>
</template>
