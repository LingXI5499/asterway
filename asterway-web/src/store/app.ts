import { defineStore } from 'pinia';
import { computed, ref, toRaw, watch } from 'vue';
import type { AppData, AgentProposal } from '../domain/types';
import { localDate } from '../domain/dates';
import { seedDemo, emptyData } from '../data/seed';
import { validateData } from '../domain/schema';
import { setTaskDone, setHabitDone, removeGoal } from '../domain/mutations';
import { totalXp, visibleTasks } from '../domain/selectors';
import { applyProposal, undoProposal } from '../agent/actions';
export const STORAGE_KEY = 'asterway:data:v1';
export const useAppStore = defineStore('asterway', () => {
  const recoveryError = ref(''),
    storageError = ref(''),
    toasts = ref<{ id: number; message: string; error: boolean }[]>([]);
  let initial: AppData = emptyData();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    initial = raw === null ? seedDemo() : validateData(JSON.parse(raw));
  } catch (error) {
    recoveryError.value = `本地数据读取失败：${error instanceof Error ? error.message : '浏览器存储不可用'}`;
  }
  const data = ref(initial);
  function toast(message: string, error = false) {
    const id = Date.now() + Math.random();
    toasts.value.push({ id, message, error });
    setTimeout(() => {
      toasts.value = toasts.value.filter((t) => t.id !== id);
    }, 5000);
  }
  function persist() {
    if (recoveryError.value) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data.value));
      storageError.value = '';
    } catch {
      storageError.value = '无法保存到浏览器，请导出数据备份。当前改动仅在本次打开期间保留。';
    }
  }
  watch(data, persist, { deep: true, flush: 'sync' });
  if (!recoveryError.value) persist();
  const today = ref(localDate());
  // Refresh the date after midnight and when returning to this tab.
  setInterval(() => {
    today.value = localDate();
  }, 30000);
  if (typeof window !== 'undefined')
    window.addEventListener('focus', () => {
      today.value = localDate();
    });
  const tasks = computed(() => visibleTasks(data.value)),
    xp = computed(() => totalXp(data.value));
  const level = computed(() => Math.floor(xp.value / 100) + 1);
  function safely(action: () => void, message: string) {
    try {
      action();
      toast(message);
      return true;
    } catch (error) {
      toast(error instanceof Error ? error.message : '操作失败', true);
      return false;
    }
  }
  function toggleTask(id: string) {
    const task = data.value.tasks.find((t) => t.id === id);
    if (task)
      safely(
        () => setTaskDone(data.value, id, task.status !== 'done'),
        task.status === 'done' ? '已取消完成，XP 已恢复' : '行动完成，+20 XP！',
      );
  }
  function toggleHabit(id: string, date: string) {
    const done = !data.value.habitLogs.some((l) => l.habitId === id && l.date === date);
    safely(
      () => setHabitDone(data.value, id, date, done, today.value),
      done ? '打卡成功，+10 XP！' : '已取消打卡',
    );
  }
  function reset(demo: boolean) {
    recoveryError.value = '';
    data.value = demo ? seedDemo(today.value) : emptyData();
    toast(demo ? '已恢复演示数据' : '数据已清空，从自己的第一步开始');
  }
  function importData(value: unknown) {
    const checked = validateData(value);
    recoveryError.value = '';
    data.value = checked;
    toast('导入成功，所有页面已更新');
  }
  function apply(proposal: AgentProposal) {
    // Apply to a plain draft first: failures never partially mutate live state.
    return safely(() => {
      const draft = structuredClone(toRaw(data.value));
      applyProposal(draft, proposal);
      data.value = draft;
    }, '方案已应用，可撤销最近一次操作');
  }
  function undo() {
    return safely(() => {
      const draft = structuredClone(toRaw(data.value));
      undoProposal(draft);
      data.value = draft;
    }, '已撤销最近一次方案');
  }
  return {
    data,
    today,
    tasks,
    xp,
    level,
    toasts,
    recoveryError,
    storageError,
    toast,
    toggleTask,
    toggleHabit,
    reset,
    importData,
    apply,
    undo,
    deleteGoal: (id: string) => removeGoal(data.value, id),
  };
});
