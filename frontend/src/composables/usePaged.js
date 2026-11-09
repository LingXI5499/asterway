/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 组合usePaged相关的页面状态、数据读取和清理逻辑。 */
import { reactive, onMounted, onBeforeUnmount } from "vue";

// 每个列表独立管理请求和页码。迟到响应不会覆盖新筛选，局部加载不销毁页面。
export /* usePaged：组合分页请求、加载状态和翻页行为。 */ function usePaged(fetcher, { /* 直接使用已有的immediate成立（immediate = true）作为同名字段。 */ immediate = true, /* 直接使用已有的每页条数20（size = 20）作为同名字段。 */ size = 20 } = {}) {
  const /* 当前状态（state）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ state = reactive({
    /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */
    rows: [],
    /* 总数（total）字段使用固定数值 0。 */
    total: 0,
    /* 当前页（page）字段使用固定数值 1。 */
    page: 1,
    /* 直接使用已有的每页条数（size）作为同名字段。 */
    size,
    /* 正在处理（busy）字段使用假，表示条件不成立或开关关闭。 */
    busy: false,
    /* 错误（error）字段使用固定文字 ''。 */
    error: "",
    /* 已加载（loaded）字段使用假，表示条件不成立或开关关闭。 */
    loaded: false,
  });
  let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
    /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
    controller,
    /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */
    disposed = false;
  /* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  async function load(reset = false) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 reset === true 是否成立，成立时走这一分支。 */
    if (reset === true) /* 把固定数值 1写入 state.page。 */ state.page = 1;
    const /* 请求（request）保存按表达式计算得到的值：++sequence。 */ request = ++sequence;
    /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
    controller?.abort();
    /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
    controller = new AbortController();
    /* 把真，表示条件成立或开关打开写入 state.busy。 */
    state.busy = true;
    /* 把固定文字 ''写入 state.error。 */
    state.error = "";
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      const /* 数据（data）保存等待 调用 fetcher 完成fetcher处理 完成后取得结果。 */ data = await fetcher(
        { /* 当前页（page）字段使用表达式 state.page 的结果。 */ page: state.page, /* 每页条数（size）字段使用表达式 state.size 的结果。 */ size: state.size },
        controller.signal,
      );
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (disposed || request !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
      /* 把优先使用前面的有效值，没有时采用后面的备用值：data.items || []写入 state.rows。 */
      state.rows = data.items || [];
      /* 把优先使用前面的有效值，没有时采用后面的备用值：data.total || 0写入 state.total。 */
      state.total = data.total || 0;
      /* 把真，表示条件成立或开关打开写入 state.loaded。 */
      state.loaded = true;
      // 删除最后一页最后一行后退回有效页，不丢失筛选条件。
      /* 检查条件 !state.rows.length && state.page > 1 是否成立，成立时走这一分支。 */
      if (!state.rows.length && state.page > 1) {
        /* 把调用 Math.max 完成最大处理写入 state.page。 */
        state.page = Math.max(1, Math.ceil(state.total / state.size));
        /* 返回读取当前对象或当前对话的数据，并用请求序号丢弃过时结果，当前函数到此结束。 */
        return load();
      }
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (
        !disposed &&
        request === sequence &&
        error.name !== "AbortError" &&
        error.code !== "SESSION_CHANGED"
      )
        /* 把优先使用前面的有效值，没有时采用后面的备用值：error.message || "加载失败，请重试"写入 state.error。 */
        state.error = error.message || "加载失败，请重试";
    } finally {
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (!disposed && request === sequence) /* 把假，表示条件不成立或开关关闭写入 state.busy。 */ state.busy = false;
    }
  }
  /* 取消（cancel）：调用 cancel 完成取消处理。 */
  function cancel(clear = false) {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    sequence++;
    /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
    controller?.abort();
    /* 把假，表示条件不成立或开关关闭写入 state.busy。 */
    state.busy = false;
    /* 检查条件 clear 是否成立，成立时走这一分支。 */
    if (clear)
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(state, {
        /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */
        rows: [],
        /* 总数（total）字段使用固定数值 0。 */
        total: 0,
        /* 当前页（page）字段使用固定数值 1。 */
        page: 1,
        /* 错误（error）字段使用固定文字 ''。 */
        error: "",
        /* 已加载（loaded）字段使用假，表示条件不成立或开关关闭。 */
        loaded: false,
      });
  }
  /* 检查条件 immediate 是否成立，成立时走这一分支。 */
  if (immediate) /* 注册组件首次挂载完成后的处理。 */ onMounted(() => load());
  /* 注册组件销毁前的清理，取消请求并释放定时器。 */
  onBeforeUnmount(() => {
    /* 把真，表示条件成立或开关打开写入 disposed。 */
    disposed = true;
    /* 调用 cancel 完成取消处理。 */
    cancel();
  });
  /* 返回调用 Object.assign 完成安排处理，当前函数到此结束。 */
  return Object.assign(state, { /* 直接使用已有的加载（load）作为同名字段。 */ load, /* 直接使用已有的取消（cancel）作为同名字段。 */ cancel });
}
