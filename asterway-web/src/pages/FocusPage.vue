<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '../store/app';
import { useFocusStore } from '../store/focus';
import { addDays } from '../domain/dates';
import { ui } from '../store/ui';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
const app = useAppStore(),
  timer = useFocusStore(),
  route = useRoute();
const task = computed(() => app.data.tasks.find((t) => t.id === timer.data.taskId));
const todaySessions = computed(() => app.data.focusSessions.filter((s) => s.date === app.today));
const todayMinutes = computed(() => todaySessions.value.reduce((sum, s) => sum + s.minutes, 0));
const week = computed(() =>
  Array.from({ length: 7 }, (_, index) => {
    const date = addDays(app.today, index - 6);
    return {
      date,
      minutes: app.data.focusSessions
        .filter((s) => s.date === date)
        .reduce((sum, s) => sum + s.minutes, 0),
    };
  }),
);
const max = computed(() => Math.max(25, ...week.value.map((day) => day.minutes)));
watch(
  () => route.query.duration,
  (value) => {
    const minutes = Number(value);
    if ([15, 25, 45, 50].includes(minutes) && timer.elapsed === 0) timer.configure('work', minutes);
  },
  { immediate: true },
);
const progress = computed(() => Math.min(100, (timer.elapsed / (timer.data.duration * 60)) * 100));
const recorded = computed(
  () => timer.data.recorded || app.data.focusSessions.some((s) => s.id === timer.data.id),
);
watch(
  () => route.query.task,
  (id) => {
    if (typeof id !== 'string' || !app.tasks.some((t) => t.id === id && t.status === 'todo'))
      return;
    if (timer.elapsed > 0 && timer.data.taskId !== id) {
      app.toast('已有一段专注进行中，请先记录或重置，再选择新任务。');
      return;
    }
    timer.data.taskId = id;
  },
  { immediate: true },
);
function configure(mode: 'work' | 'rest', duration: number) {
  const action = () => timer.configure(mode, duration);
  if (timer.elapsed > 0 && !recorded.value)
    ui.confirm(
      '开始新的计时？',
      '当前未记录的计时将被重置。可以取消，先记录已经完成的专注。',
      action,
    );
  else action();
}
function reset() {
  configure(timer.data.mode, timer.data.duration);
}
</script>
<template>
  <PageHeader
    title="专注空间"
    subtitle="这段时间，只留给一件值得做的事。暂停和休息，也是节奏的一部分。"
    eyebrow="ONE THING AT A TIME"
    ><RouterLink class="button secondary" to="/methods?tab=top3"
      ><Icon name="target" :size="17" />先选今日重点</RouterLink
    ></PageHeader
  >
  <div class="focus-workspace">
    <section class="focus-stage">
      <div class="focus-mode-tabs">
        <button :class="{ active: timer.data.mode === 'work' }" @click="configure('work', 25)">
          <Icon name="target" :size="16" />专注</button
        ><button :class="{ active: timer.data.mode === 'rest' }" @click="configure('rest', 5)">
          <Icon name="leaf" :size="16" />休息
        </button>
      </div>
      <div class="focus-task-picker">
        <span>{{ timer.data.mode === 'work' ? '现在，为这件事留出时间' : '把注意力还给自己' }}</span
        ><select
          v-model="timer.data.taskId"
          aria-label="选择专注任务"
          :disabled="timer.elapsed > 0"
        >
          <option :value="undefined">自由专注 · 不关联任务</option>
          <option
            v-for="item in app.tasks.filter((t) => t.status === 'todo')"
            :key="item.id"
            :value="item.id"
          >
            {{ item.title }}
          </option>
        </select>
      </div>
      <div class="focus-orbit" :style="{ '--focus-progress': `${progress}%` }">
        <div class="focus-orbit-inner">
          <span>{{ timer.data.mode === 'work' ? 'FOCUS SESSION' : 'A LITTLE REST' }}</span
          ><strong aria-live="off" class="focus-countdown">{{ timer.remaining }}</strong>
          <p>
            {{
              recorded
                ? '这段专注已记录'
                : timer.finished
                  ? '这一段已经完成'
                  : timer.running
                    ? '慢慢来，持续向前'
                    : timer.elapsed > 0
                      ? '暂停一下，再出发'
                      : '准备好，就从现在开始'
            }}
          </p>
        </div>
      </div>
      <div class="focus-duration-chips">
        <button
          v-for="duration in timer.data.mode === 'work' ? [15, 25, 45, 50] : [5, 10]"
          :key="duration"
          :class="{ active: timer.data.duration === duration }"
          @click="configure(timer.data.mode, duration)"
        >
          {{ duration }} 分钟
        </button>
      </div>
      <div class="focus-controls">
        <button class="button primary" :disabled="timer.finished || recorded" @click="timer.toggle">
          <Icon :name="timer.running ? 'pause' : 'play'" :size="18" />{{
            timer.running
              ? '暂停'
              : timer.elapsed > 0
                ? '继续'
                : timer.data.mode === 'rest'
                  ? '开始休息'
                  : '开始专注'
          }}</button
        ><button class="focus-reset-button" aria-label="重置专注计时" @click="reset">
          <Icon name="undo" :size="19" />
        </button>
      </div>
      <button
        v-if="timer.data.mode === 'work'"
        class="focus-record-button"
        :disabled="timer.elapsed < 60 || recorded"
        @click="timer.record"
      >
        {{
          recorded ? '✓ 已记录这段专注' : `记录已完成的 ${Math.floor(timer.elapsed / 60)} 分钟`
        }}</button
      ><button v-if="recorded" class="text-button" @click="configure('rest', 5)">
        给自己 5 分钟休息<Icon name="arrow" :size="15" />
      </button>
      <p class="focus-state-hint">
        计时会在切换页面和刷新后继续，关闭弹窗也不会中断。休息不计入专注，任务需单独确认完成。
      </p>
      <p v-if="timer.storageError" class="error-message">{{ timer.storageError }}</p>
    </section>
    <aside class="focus-side">
      <section class="card">
        <span class="eyebrow">TODAY'S QUIET PROGRESS</span>
        <h2>今天留下的专注</h2>
        <div class="focus-stats">
          <div>
            <strong>{{ todayMinutes }}</strong
            ><span>分钟实际记录</span>
          </div>
          <div>
            <strong>{{ todaySessions.length }}</strong
            ><span>段专注</span>
          </div>
        </div>
        <div class="focus-week-bars">
          <div v-for="day in week" :key="day.date">
            <span>{{ day.minutes || '' }}</span
            ><i :style="{ height: `${Math.max(3, (day.minutes / max) * 85)}px` }"></i
            ><small>{{ day.date.slice(5) }}</small>
          </div>
        </div>
      </section>
      <section class="card focus-intention">
        <Icon name="sprout" :size="30" />
        <h3>{{ task?.title || '先让一个小行动发生' }}</h3>
        <p>
          {{
            task?.description || '不追求一次解决所有事。关掉一点干扰，从一个可以完成的动作开始。'
          }}
        </p>
        <button
          v-if="task && task.status === 'todo'"
          class="button secondary full-width"
          @click="app.toggleTask(task.id)"
        >
          <Icon name="check" :size="16" />确认这项任务已完成</button
        ><span v-else-if="task?.status === 'done'" class="positive">✓ 任务已完成</span>
      </section>
      <section class="focus-tips">
        <h3>让专注更轻一点</h3>
        <p><span>01</span>先确定这一段要完成的具体结果。</p>
        <p><span>02</span>暂停消息提醒，收起不需要的页面。</p>
        <p><span>03</span>走神时记下想法，然后回到这一件事。</p>
        <p><span>04</span>结束后主动记录，起身休息一下。</p>
        <RouterLink to="/insights" class="text-button"
          >回顾这段成长<Icon name="arrow" :size="14"
        /></RouterLink>
      </section>
    </aside>
  </div>
</template>
