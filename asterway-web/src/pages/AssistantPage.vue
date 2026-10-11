<script setup lang="ts">
import { computed, ref, watch, toRaw } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '../store/app'
import { ruleAgent } from '../agent/rules'
import type { AgentProposal } from '../domain/types'
import { shortDate } from '../domain/dates'
import PageHeader from '../components/PageHeader.vue'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
const store = useAppStore(),
  route = useRoute(),
  minutes = ref(40),
  goalId = ref(''),
  input = ref(''),
  proposal = ref<AgentProposal | null>(null),
  messages = ref<{ role: 'assistant' | 'user'; text: string }[]>([
    {
      role: 'assistant',
      text: '你好，我是你的成长伙伴。计划可以随生活调整。我可以帮你精简今天、拆解目标，或回顾这一周。每次改动都会先给你看，确认后才应用。',
    },
  ])
const activeGoals = computed(() => store.data.goals.filter((g) => g.status === 'active'))
const todayTasks = computed(() =>
  store.tasks.filter((t) => t.scheduledDate === store.today && t.status === 'todo'),
)
const latest = computed(() => store.data.agentActionLog.at(-1))
watch(
  activeGoals,
  (goals) => {
    if (!goals.some((g) => g.id === goalId.value)) goalId.value = goals[0]?.id || ''
  },
  { immediate: true },
)
function generate(kind: 'time' | 'goal' | 'review') {
  try {
    const data = toRaw(store.data)
    const value =
      kind === 'time'
        ? ruleAgent.reschedule(data, Number(minutes.value), store.today)
        : kind === 'goal'
          ? ruleAgent.decompose(data, goalId.value, store.today)
          : ruleAgent.review(data, store.today)
    proposal.value = value
    messages.value.push(
      {
        role: 'user',
        text:
          kind === 'time'
            ? `今天只有 ${minutes.value} 分钟，帮我调整计划。`
            : kind === 'goal'
              ? '把这个目标拆成具体行动。'
              : '看看我本周的情况。',
      },
      {
        role: 'assistant',
        text: `${value.title}。${value.changes.length ? '右侧准备好了变更草案，先检查一下，再决定是否应用。' : value.explanation}`,
      },
    )
  } catch (error) {
    store.toast(error instanceof Error ? error.message : '生成失败', true)
  }
}
function send() {
  const text = input.value.trim()
  if (!text) return
  input.value = ''
  const number = text.match(/(\d+)\s*分钟/)
  if (number || /时间|不够|疲惫/.test(text)) {
    if (number) minutes.value = Number(number[1])
    generate('time')
  } else if (/拆|目标/.test(text)) generate('goal')
  else if (/复盘|本周|总结/.test(text)) generate('review')
  else
    messages.value.push(
      { role: 'user', text },
      {
        role: 'assistant',
        text: '当前是本地规则演示，暂不支持开放问答。试试「今天只有 40 分钟」「拆解目标」或「看看本周情况」。',
      },
    )
}
function apply() {
  if (proposal.value && store.apply(toRaw(proposal.value))) {
    messages.value.push({
      role: 'assistant',
      text: '方案已应用，今日和日程已同步。你可以撤销最近一次方案。',
    })
    proposal.value = null
  }
}
watch(
  () => route.query,
  (query) => {
    if (typeof query.goal === 'string') {
      goalId.value = query.goal
      generate('goal')
    } else if (query.mode === 'time') generate('time')
    else if (query.mode === 'review') generate('review')
  },
  { immediate: true },
)
const taskTitle = (id: string) => store.data.tasks.find((t) => t.id === id)?.title || '任务已删除'
function cancelProposal() {
  proposal.value = null
  store.toast('已取消，任务没有改变')
}
</script>
<template>
  <PageHeader
    title="成长伙伴"
    subtitle="计划不是一成不变的。让今天的行动，适应真实的你。"
    eyebrow="A COMPANION FOR YOUR JOURNEY"
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
          <small>根据现有数据和预设规则提供建议</small>
        </div>
        <span class="local-status">本地</span>
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
          placeholder="试试：今天只有 40 分钟…"
          aria-label="给成长伙伴的指令"
        /><button class="button primary" type="submit" aria-label="发送指令">
          <Icon name="send" :size="18" />
        </button>
      </form>
      <small class="chat-disclaimer">规则演示只支持示例指令，不会自动修改你的数据。</small>
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
        <template v-if="proposal"
          ><h3>{{ proposal.title }}</h3>
          <p class="muted proposal-explanation">{{ proposal.explanation }}</p>
          <div v-if="proposal.keptTaskIds.length" class="kept-tasks">
            <strong><Icon name="checkCircle" :size="15" />保留在今天</strong>
            <p v-for="id in proposal.keptTaskIds" :key="id">{{ taskTitle(id) }}</p>
          </div>
          <div v-for="(change, index) in proposal.changes" :key="index" class="proposal-change">
            <span :class="['change-type', change.op === 'reschedule' ? 'reschedule' : 'create']">{{
              change.op === 'reschedule' ? '延后' : '新增'
            }}</span>
            <div>
              <strong>{{
                change.op === 'reschedule' ? taskTitle(change.taskId) : change.task.title
              }}</strong>
              <div class="change-diff">
                <template v-if="change.op === 'reschedule'"
                  ><span>{{ change.from ? shortDate(change.from) : '待安排' }}</span
                  ><Icon name="arrow" :size="14" /><b>{{ shortDate(change.to) }}</b></template
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
