import { reactive, onMounted, onBeforeUnmount } from "vue";

// 每个列表独立管理请求和页码。迟到响应不会覆盖新筛选，局部加载不销毁页面。
export function usePaged(fetcher, { immediate = true, size = 20 } = {}) {
  const state = reactive({
    rows: [],
    total: 0,
    page: 1,
    size,
    busy: false,
    error: "",
    loaded: false,
  });
  let sequence = 0,
    controller,
    disposed = false;
  async function load(reset = false) {
    if (disposed) return;
    if (reset === true) state.page = 1;
    const request = ++sequence;
    controller?.abort();
    controller = new AbortController();
    state.busy = true;
    state.error = "";
    try {
      const data = await fetcher(
        { page: state.page, size: state.size },
        controller.signal,
      );
      if (disposed || request !== sequence) return;
      state.rows = data.items || [];
      state.total = data.total || 0;
      state.loaded = true;
      // 删除最后一页最后一行后退回有效页，不丢失筛选条件。
      if (!state.rows.length && state.page > 1) {
        state.page = Math.max(1, Math.ceil(state.total / state.size));
        return load();
      }
    } catch (error) {
      if (
        !disposed &&
        request === sequence &&
        error.name !== "AbortError" &&
        error.code !== "SESSION_CHANGED"
      )
        state.error = error.message || "加载失败，请重试";
    } finally {
      if (!disposed && request === sequence) state.busy = false;
    }
  }
  function cancel(clear = false) {
    sequence++;
    controller?.abort();
    state.busy = false;
    if (clear)
      Object.assign(state, {
        rows: [],
        total: 0,
        page: 1,
        error: "",
        loaded: false,
      });
  }
  if (immediate) onMounted(() => load());
  onBeforeUnmount(() => {
    disposed = true;
    cancel();
  });
  return Object.assign(state, { load, cancel });
}
