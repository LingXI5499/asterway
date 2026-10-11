import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import { z } from 'zod';
import { now, uid } from '../domain/dates';
import { useAppStore } from './app';
export const FOCUS_KEY = 'asterway:focus:v1';
const schema = z.object({
  version: z.literal(1),
  id: z.string().min(1),
  mode: z.enum(['work', 'rest']),
  duration: z.number().int().min(1).max(90),
  base: z.number().int().min(0).max(5400),
  startedAt: z.number().min(0).nullable(),
  taskId: z.string().optional(),
  recorded: z.boolean(),
});
type FocusState = z.infer<typeof schema>;
const fresh = (mode: 'work' | 'rest' = 'work', duration = 25): FocusState => ({
  version: 1,
  id: uid(),
  mode,
  duration,
  base: 0,
  startedAt: null,
  recorded: false,
});
export const useFocusStore = defineStore('focus', () => {
  const app = useAppStore(),
    clock = ref(Date.now()),
    storageError = ref('');
  let initial = fresh();
  try {
    const raw = localStorage.getItem(FOCUS_KEY);
    if (raw) initial = schema.parse(JSON.parse(raw));
  } catch {
    app.toast('上次计时状态无法恢复，已开始新的计时；已保存的专注记录不受影响。', true);
  }
  const data = ref(initial);
  const running = computed(() => data.value.startedAt !== null);
  const elapsed = computed(() =>
    Math.min(
      data.value.duration * 60,
      data.value.base +
        (data.value.startedAt === null
          ? 0
          : Math.max(0, Math.floor((clock.value - data.value.startedAt) / 1000))),
    ),
  );
  const finished = computed(() => elapsed.value >= data.value.duration * 60);
  const remaining = computed(() => {
    const seconds = Math.max(0, data.value.duration * 60 - elapsed.value);
    return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  });
  function persist() {
    try {
      localStorage.setItem(FOCUS_KEY, JSON.stringify(data.value));
      storageError.value = '';
    } catch {
      storageError.value = '计时状态无法保存，刷新后可能无法恢复。';
    }
  }
  watch(data, persist, { deep: true, flush: 'sync' });
  const tick = () => {
    clock.value = Date.now();
    if (running.value && finished.value) {
      data.value.base = data.value.duration * 60;
      data.value.startedAt = null;
    }
  };
  const interval = setInterval(tick, 500);
  tick();
  function toggle() {
    tick();
    if (finished.value || data.value.recorded) return;
    if (running.value) {
      data.value.base = elapsed.value;
      data.value.startedAt = null;
    } else {
      clock.value = Date.now();
      data.value.startedAt = clock.value;
    }
  }
  function configure(mode: 'work' | 'rest', duration = mode === 'rest' ? 5 : 25) {
    const taskId = data.value.taskId;
    data.value = { ...fresh(mode, duration), taskId };
    persist();
  }
  function record() {
    tick();
    const minutes = Math.floor(elapsed.value / 60);
    if (data.value.mode !== 'work' || minutes < 1 || data.value.recorded) return false;
    if (app.data.focusSessions.some((session) => session.id === data.value.id)) {
      data.value.recorded = true;
      return false;
    }
    if (running.value) {
      data.value.base = elapsed.value;
      data.value.startedAt = null;
    }
    app.data.focusSessions.push({
      id: data.value.id,
      date: app.today,
      minutes,
      taskId: data.value.taskId,
      createdAt: now(),
    });
    data.value.recorded = true;
    app.toast(`已记录 ${minutes} 分钟专注，任务完成仍由你确认`);
    return true;
  }
  function clear() {
    data.value = fresh();
    persist();
  }
  function dispose() {
    clearInterval(interval);
  }
  return {
    data,
    running,
    elapsed,
    finished,
    remaining,
    storageError,
    toggle,
    configure,
    record,
    clear,
    dispose,
  };
});
