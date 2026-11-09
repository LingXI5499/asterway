<!-- 渲染执行计时任务Picker界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../../api/http";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, today, monday, canGrow } from "../../api/growth";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import { execution } from "../../stores/personal";
/* 引入 ./PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "./PagePagination.vue";
/* 引入 ./LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "./LoadState.vue";
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["selected"]);
const /* 列表（list）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ list = ref({ /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */ rows: [], /* 总数（total）字段使用固定数值 0。 */ total: 0, /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 }),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* chosen保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  chosen = ref("");
const /* allowed保存随相关状态变化自动重算的值。 */ allowed = computed(() =>
  [
    "planning:weekly:read",
    "task:task:read",
    "goal:goal:read",
    "stage:stage:read",
  ].every(canGrow),
);
const /* 日（day）保存随相关状态变化自动重算的值。 */ day = computed(() => {
  /* 更新本次处理状态：void execution.tick。 */
  void execution.tick;
  /* 返回调用 today 完成今天处理，当前函数到此结束。 */
  return today();
});
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */
  disposed = false,
  /* 周编号（weekId）保存固定文字 ''。 */
  weekId = "";
/* 原因（reason）：调用 reason 完成原因处理。 */
function reason(row) {
  /* 检查条件 row.status === "COMPLETED" 是否成立，成立时走这一分支。 */
  if (row.status === "COMPLETED") /* 返回固定文字 '任务已完成'，当前函数到此结束。 */ return "任务已完成";
  /* 检查条件 row.archivedAt 是否成立，成立时走这一分支。 */
  if (row.archivedAt) /* 返回固定文字 '目标已归档'，当前函数到此结束。 */ return "目标已归档";
  /* 检查条件 row.planDate && row.planDate !== day.value 是否成立，成立时走这一分支。 */
  if (row.planDate && row.planDate !== day.value)
    /* 返回按表达式计算得到的值："安排在 " + row.planDate，当前函数到此结束。 */
    return "安排在 " + row.planDate;
  /* 返回固定文字 ''，当前函数到此结束。 */
  return "";
}
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load(reset = false) {
  /* 检查条件 !allowed.value 是否成立，成立时走这一分支。 */
  if (!allowed.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 reset === true 是否成立，成立时走这一分支。 */
  if (reset === true) /* 把固定数值 1写入 list.value.page。 */ list.value.page = 1;
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 把固定文字 ''写入 chosen.value。 */
  chosen.value = "";
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("selected", null);
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 周（week）保存等待 调用 api 完成接口处理 完成后取得结果。 */ week = await api("/weekly-plans/by-week/" + monday(day.value));
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq !== sequence || disposed) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把沿 week?.id 读取数据；中间对象为空时返回空值写入 weekId。 */
    weekId = week?.id;
    /* 检查条件 !weekId 是否成立，成立时走这一分支。 */
    if (!weekId) {
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(list.value, { /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */ rows: [], /* 总数（total）字段使用固定数值 0。 */ total: 0 });
      /* 返回空内容，当前函数到此结束。 */
      return;
    }
    const /* 结果（result）保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ result = await query("/weekly-plans/" + weekId + "/tasks", {
      /* 当前页（page）字段使用表达式 list.value.page 的结果。 */
      page: list.value.page,
      /* 每页条数（size）字段使用表达式 list.value.size 的结果。 */
      size: list.value.size,
      /* keyword字段使用表达式 keyword.value 的结果。 */
      keyword: keyword.value,
    });
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed)
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(list.value, { /* 记录列表（rows）字段使用表达式 result.items 的结果。 */ rows: result.items, /* 总数（total）字段使用表达式 result.total 的结果。 */ total: result.total });
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 选择（select）：调用 select 完成选择处理。 */
async function select(row) {
  /* 检查条件 reason(row) || busy.value 是否成立，成立时走这一分支。 */
  if (reason(row) || busy.value) /* 返回空内容，当前函数到此结束。 */ return;
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 任务目标阶段（[task, goal, stage]）保存等待 调用 Promise.all 完成全部处理 完成后取得结果。 */ [task, goal, stage] = await Promise.all([
      api("/tasks/" + row.taskId),
      api("/goals/" + row.goalId),
      api("/stages/" + row.stageId),
    ]);
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq !== sequence || disposed) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 goal.archivedAt || goal.status === "COMPLETED" || stage.status === "COMPLETED" || task.status === "COMPLETED" || (goal.startDate && goal.startDat… 是否成立，成立时走这一分支。 */
    if (
      goal.archivedAt ||
      goal.status === "COMPLETED" ||
      stage.status === "COMPLETED" ||
      task.status === "COMPLETED" ||
      (goal.startDate && goal.startDate > day.value) ||
      (task.startDate && task.startDate > day.value)
    ) {
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error(
        "该任务或来源状态尚不支持执行，请先调整目标、阶段、任务或安排",
      );
    }
    /* 把表达式 row.taskId 的结果写入 chosen.value。 */
    chosen.value = row.taskId;
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("selected", task);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed) {
      /* 把表达式 e.message 的结果写入 error.value。 */
      error.value = e.message;
      /* 把固定文字 ''写入 chosen.value。 */
      chosen.value = "";
      /* 向父组件发送事件，通知数据或界面状态发生变化。 */
      emit("selected", null);
    }
  } finally {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(day, () => load(true));
/* 注册组件首次挂载完成后的处理。 */
onMounted(() => load());
/* 注册组件销毁前的清理，取消请求并释放定时器。 */
onBeforeUnmount(() => {
  /* 把真，表示条件成立或开关打开写入 disposed。 */
  disposed = true;
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
});
/* 声明父组件通过引用可以调用的功能。 */
defineExpose({ /* reload字段使用表达式 () => load() 的结果。 */ reload: () => load() });
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示二级标题。 -->
        <h2>从本周计划选择任务</h2>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">
          指定日期的任务在安排当天执行；本周尚未定日的任务可直接开始。
        </p>
      </div>
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 !allowed 成立时渲染；类型设为 info；closable 取自 false；title 固定为 选择任务需要周计划和来源资料读取权限，可由管理员在普通用户角色中补齐。 -->
    <el-alert
      v-if="!allowed"
      type="info"
      :closable="false"
      title="选择任务需要周计划和来源资料读取权限，可由管理员在普通用户角色中补齐"
    />
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 -->
    <template v-else>
      <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
      <div class="toolbar">
        <!-- 显示Element Plus 的 input 组件；与 keyword 双向同步；未输入时提示 搜索本周任务；启用 clearable 属性；keyup 事件触发时执行 load(true)。 -->
        <el-input
          v-model="keyword"
          placeholder="搜索本周任务"
          clearable
          @keyup.enter="load(true)"
        /><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 load(true)。 --><el-button @click="load(true)">查询</el-button
        ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 keyword = ''; load(true);。 --><el-button
          @click="
            keyword = '';
            load(true);
          "
          >重置</el-button
        >
      </div>
      <!-- 显示加载当前状态（LoadState）组件；loading 取自 busy && !list.rows.length；error 取自 error；retry 事件触发时执行 load。 -->
      <LoadState
        :loading="busy && !list.rows.length"
        :error="error"
        @retry="load"
      >
        <!-- 显示Element Plus 的 table 组件；data 取自 list.rows；loading 指令使用 busy；empty-text 固定为 本周暂无安排，可先前往周计划添加任务。 -->
        <el-table
          :data="list.rows"
          v-loading="busy"
          empty-text="本周暂无安排，可先前往周计划添加任务"
        >
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
          <template #empty
            ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '本周暂无安排，可先前往周计划添加任务'。 --><AppEmptyState
              :description="
                error
                  ? '加载失败，请查看提示后重试。'
                  : '本周暂无安排，可先前往周计划添加任务'
              " /></template
          ><!-- 显示Element Plus 的 table-column 组件；label 固定为 选择；width 固定为 80。 --><el-table-column label="选择" width="80"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示Element Plus 的 radio 组件；model-value 取自 chosen；value 取自 row.taskId；disabled 取自 !!reason(row)；change 事件触发时执行 select(row)；aria-label 取自 '选择' + row.taskName。 --><el-radio
                :model-value="chosen"
                :value="row.taskId"
                :disabled="!!reason(row)"
                @change="select(row)"
                :aria-label="'选择' + row.taskName"
                ><!-- 显示行内文字区域；用样式类 sr-only 控制外观。 --><span class="sr-only"><!-- 把 row.taskName 的当前值转成文字显示。 -->{{ row.taskName }}</span></el-radio
              ></template
            ></el-table-column
          >
          <!-- 显示Element Plus 的 table-column 组件；prop 固定为 taskName；label 固定为 任务；min-width 固定为 180。 -->
          <el-table-column
            prop="taskName"
            label="任务"
            min-width="180"
          /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 goalName；label 固定为 目标；min-width 固定为 140。 --><el-table-column prop="goalName" label="目标" min-width="140" />
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 安排；min-width 固定为 160。 -->
          <el-table-column label="安排" min-width="160"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示Element Plus 的 tag 组件；type 取自 reason(row) ? 'info' : 'success'。 --><el-tag :type="reason(row) ? 'info' : 'success'"><!-- 把 reason(row) || row.planDate || "本周未定日" 的当前值转成文字显示。 -->{{
                reason(row) || row.planDate || "本周未定日"
              }}</el-tag></template
            ></el-table-column
          >
        </el-table>
        <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 load。 -->
        <PagePagination :state="list" @change="load" />
      </LoadState>
    </template>
  </section>
</template>
