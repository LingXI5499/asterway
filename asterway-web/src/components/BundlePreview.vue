<script setup lang="ts">
import type { GrowthBundle } from '../domain/prototype';
import Icon from './Icon.vue';
import { shortDate } from '../domain/dates';
defineProps<{ bundle: GrowthBundle }>();
</script>
<template>
  <div class="bundle-preview">
    <h3>{{ bundle.title }}</h3>
    <p class="muted">{{ bundle.explanation }}</p>
    <div class="bundle-counts">
      <span>{{ bundle.goals.length }} 目标</span><span>{{ bundle.tasks.length }} 任务</span
      ><span>{{ bundle.habits.length }} 习惯</span><span>{{ bundle.notes.length }} 记忆</span>
    </div>
    <div v-for="goal in bundle.goals" :key="goal.id" class="bundle-goal">
      <Icon name="target" :size="18" />
      <div>
        <strong>{{ goal.title }}</strong
        ><small>{{ shortDate(goal.startDate) }} → {{ shortDate(goal.targetDate) }}</small>
      </div>
    </div>
    <ol v-if="bundle.tasks.length" class="bundle-tasks">
      <li v-for="task in bundle.tasks" :key="task.id">
        <span>{{ task.title }}</span
        ><small
          >{{ task.scheduledDate ? shortDate(task.scheduledDate) : '待安排' }} ·
          {{ task.durationMinutes }} 分钟</small
        >
      </li>
    </ol>
    <p v-for="habit in bundle.habits" :key="habit.id" class="bundle-habit">
      <Icon name="checkCircle" :size="15" />{{ habit.title }}
    </p>
    <div v-for="note in bundle.notes" :key="note.id" class="memory-preview">
      <strong>{{ note.title }}</strong>
      <p>{{ note.content }}</p>
    </div>
  </div>
</template>
