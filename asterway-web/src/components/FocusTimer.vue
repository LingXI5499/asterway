<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import { useAppStore } from '../store/app';
import { now, uid } from '../domain/dates';
import { ui } from '../store/ui';
import Modal from './Modal.vue';
import Icon from './Icon.vue';
const store = useAppStore(),
  duration = ref(25),
  elapsed = ref(0),
  running = ref(false),
  started = ref(0),
  base = ref(0);
const timer = setInterval(() => {
  if (running.value) {
    elapsed.value = Math.min(
      duration.value * 60,
      base.value + Math.floor((Date.now() - started.value) / 1000),
    );
    if (elapsed.value >= duration.value * 60) running.value = false;
  }
}, 250);
onBeforeUnmount(() => clearInterval(timer));
const remaining = computed(() => {
  const value = Math.max(0, duration.value * 60 - elapsed.value);
  return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`;
});
function toggle() {
  if (running.value) {
    base.value = elapsed.value;
    running.value = false;
  } else {
    started.value = Date.now();
    base.value = elapsed.value;
    running.value = true;
  }
}
function reset() {
  running.value = false;
  elapsed.value = 0;
  base.value = 0;
}
function chooseDuration(minutes: number) {
  duration.value = minutes;
  reset();
}
function record() {
  const minutes = Math.floor(elapsed.value / 60);
  if (minutes < 1) return;
  store.data.focusSessions.push({ id: uid(), date: store.today, minutes, createdAt: now() });
  store.toast(`已记录 ${minutes} 分钟专注，不会自动完成任务`);
  ui.close();
}
</script>
<template>
  <Modal title="给重要的事，一段专注时间" @close="ui.close()"
    ><div class="focus-timer">
      <div class="segmented">
        <button
          v-for="minutes in [25, 45]"
          :key="minutes"
          :class="{ active: duration === minutes }"
          @click="chooseDuration(minutes)"
        >
          {{ minutes }} 分钟
        </button>
      </div>
      <div class="timer-number">{{ remaining }}</div>
      <p class="muted">一次只做一件事。暂停也没关系。</p>
      <div class="chip-row">
        <button class="button primary" :disabled="elapsed >= duration * 60" @click="toggle">
          <Icon :name="running ? 'pause' : 'play'" :size="16" />{{
            running ? '暂停' : '开始'
          }}</button
        ><button class="button secondary" @click="reset">
          <Icon name="undo" :size="16" />重置
        </button>
      </div>
      <button class="text-button" :disabled="elapsed < 60" @click="record">
        记录完成（{{ Math.floor(elapsed / 60) }} 分钟）</button
      ><small class="muted">仅记录实际计时时长，关闭窗口后不继续计时。</small>
    </div></Modal
  >
</template>
