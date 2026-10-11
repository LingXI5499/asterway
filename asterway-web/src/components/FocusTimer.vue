<script setup lang="ts">
import { useFocusStore } from '../store/focus';
import { ui } from '../store/ui';
import Modal from './Modal.vue';
import Icon from './Icon.vue';
const timer = useFocusStore();
function choose(minutes: number) {
  const action = () => timer.configure('work', minutes);
  if (timer.elapsed > 0 && !timer.data.recorded)
    ui.confirm('开始新的计时？', '当前未记录的计时将被重置。取消可继续原计时。', action);
  else action();
}
</script>
<template>
  <Modal title="给重要的事，一段专注时间" @close="ui.close()"
    ><div class="focus-timer">
      <div class="segmented">
        <button
          v-for="minutes in [25, 45]"
          :key="minutes"
          :class="{ active: timer.data.duration === minutes && timer.data.mode === 'work' }"
          @click="choose(minutes)"
        >
          {{ minutes }} 分钟
        </button>
      </div>
      <div class="timer-number">{{ timer.remaining }}</div>
      <p class="muted">
        {{ timer.data.mode === 'rest' ? '休息一下，再向前。' : '一次只做一件事。暂停也没关系。' }}
      </p>
      <div class="chip-row">
        <button
          class="button primary"
          :disabled="timer.finished || timer.data.recorded"
          @click="timer.toggle"
        >
          <Icon :name="timer.running ? 'pause' : 'play'" :size="16" />{{
            timer.running ? '暂停' : '开始'
          }}</button
        ><button class="button secondary" @click="choose(timer.data.duration)">
          <Icon name="undo" :size="16" />重置
        </button>
      </div>
      <button
        v-if="timer.data.mode === 'work'"
        class="text-button"
        :disabled="timer.elapsed < 60 || timer.data.recorded"
        @click="timer.record"
      >
        {{
          timer.data.recorded ? '已记录' : `记录完成（${Math.floor(timer.elapsed / 60)} 分钟）`
        }}</button
      ><RouterLink class="button secondary" to="/focus" @click="ui.close()"
        >进入完整专注空间<Icon name="arrow" :size="15" /></RouterLink
      ><small class="muted">关闭窗口、切换页面或刷新会继续计时；休息不计入专注。</small>
    </div></Modal
  >
</template>
