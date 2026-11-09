// 日期切换时取消旧请求；每次写入状态前再次检查，防止慢响应覆盖新日期。
import { ref, onBeforeUnmount } from "vue";
import { api } from "../api/http";
import { query, monday, canGrow } from "../api/growth";

export function usePlanTasks() {
  const week = ref(null), day = ref(null), rows = ref([]), total = ref(0);
  const busy = ref(false), error = ref("");
  let sequence = 0, controller;

  async function load(date, page = 1, size = 20) {
    const seq = ++sequence;
    controller?.abort();
    controller = new AbortController();
    const options = { signal: controller.signal };
    busy.value = true;
    error.value = "";
    week.value = day.value = null;
    try {
      if (!canGrow("planning:weekly:read")) {
        rows.value = []; total.value = 0;
        return;
      }
      const foundWeek = await api(`/weekly-plans/by-week/${monday(date)}`, options);
      if (seq !== sequence) return;
      const foundDay = foundWeek && canGrow("planning:daily:read")
        ? await api(`/weekly-plans/${foundWeek.id}/daily-plans/${date}`, options) : null;
      if (seq !== sequence) return;
      const data = foundDay
        ? await query(`/daily-plans/${foundDay.id}/tasks`, { page, size }, options)
        : { items: [], total: 0 };
      if (seq !== sequence) return;
      week.value = foundWeek; day.value = foundDay;
      rows.value = data.items; total.value = data.total;
    } catch (e) {
      if (seq === sequence && e.name !== "AbortError") {
        rows.value = []; total.value = 0; error.value = e.message;
      }
    } finally {
      if (seq === sequence) busy.value = false;
    }
  }

  onBeforeUnmount(() => { sequence++; controller?.abort(); });
  return { week, day, rows, total, busy, error, load };
}
