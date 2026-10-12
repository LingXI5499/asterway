<script setup lang="ts">
import { computed } from 'vue';
import type { Goal } from '../domain/types';
import { categories } from '../domain/types';
import { goalProgress } from '../domain/selectors';
import { shortDate } from '../domain/dates';
import { useAppStore } from '../store/app';
import { ui } from '../store/ui';
import Icon from './Icon.vue';
import { planningMethods } from '../domain/methodPlans';
const props = defineProps<{ goal: Goal; compact?: boolean }>();
const store = useAppStore();
const progress = computed(() => goalProgress(store.data, props.goal.id));
const milestones = computed(() =>
  store.data.milestones.filter((m) => m.goalId === props.goal.id).sort((a, b) => a.order - b.order),
);
</script>
<template>
  <button :class="['goal-card', { compact }]" @click="ui.open('goalDetail', { id: goal.id })">
    <div class="goal-card-top">
      <span :class="['category-icon', goal.category]"><Icon :name="goal.category" /></span
      ><span :class="['category-label', goal.category]">{{ categories[goal.category] }}</span
      ><Icon class="push-right" name="external" :size="17" />
    </div>
    <h3>{{ goal.title }}</h3>
    <span v-if="goal.planningMethod" class="goal-method-tag">{{
      planningMethods.find((m) => m.id === goal.planningMethod)?.name
    }}</span>
    <p v-if="!compact" class="goal-description">
      {{ goal.description || '从一个小行动开始，让方向变得清晰。' }}
    </p>
    <div class="progress-caption">
      <span>{{
        goal.status === 'active'
          ? milestones[0]?.title || '准备启程'
          : { paused: '已暂停', completed: '已标记完成', archived: '已归档' }[goal.status]
      }}</span
      ><strong>{{ progress.percent === null ? '待规划' : `${progress.percent}%` }}</strong>
    </div>
    <div class="progress-track"><span :style="{ width: `${progress.percent || 0}%` }" /></div>
    <div class="goal-card-bottom">
      <span>{{ progress.done }}/{{ progress.total }} 项行动</span
      ><span>{{ shortDate(goal.targetDate) }} 截止</span>
    </div>
  </button>
</template>
