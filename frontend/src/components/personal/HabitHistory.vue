<!-- 渲染习惯历史界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive, computed, watch, onBeforeUnmount } from "vue";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  habitApi,
  instantText,
  cycleText,
  actions,
  signalChange,
} from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { today, canGrow } from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ./PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "./PagePagination.vue";
/* 引入 ./LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "./LoadState.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 模型值（modelValue）字段使用可为空的真假值（Boolean）。 */
  modelValue: Boolean,
  /* 来源（source）字段使用对象（Object）。 */
  source: Object,
  /* mode字段使用可为空的文字（String）。 */
  mode: String,
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue", "changed"]);
const /* 是否显示（visible）保存随相关状态变化自动重算的值。 */ visible = computed({
  /* 读取（get）字段使用表达式 () => props.modelValue 的结果。 */
  get: () => props.modelValue,
  /* 设置（set）字段使用表达式 (v) => emit("update:modelValue", v) 的结果。 */
  set: (v) => emit("update:modelValue", v),
});
const /* 列表（list）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ list = reactive({ /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */ rows: [], /* 总数（total）字段使用固定数值 0。 */ total: 0, /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 }),
  /* dates保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  dates = ref([]),
  /* 已取消（cancelled）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  cancelled = ref(null),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* 等待处理（pending）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  pending = ref("");
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
const /* 编号（id）保存随相关状态变化自动重算的值。 */ id = computed(() => props.source?.habitId || props.source?.id);
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load(reset = false) {
  /* 检查条件 !id.value || !props.modelValue 是否成立，成立时走这一分支。 */
  if (!id.value || !props.modelValue) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 reset === true 是否成立，成立时走这一分支。 */
  if (reset === true) /* 把固定数值 1写入 list.page。 */ list.page = 1;
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
  /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
  controller = new AbortController();
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 值列表（values）保存优先使用前面的有效值，没有时采用后面的备用值：dates.value || []。 */ values = dates.value || [];
    /* 检查条件 values.length && (values.length !== 2 || values[0] > values[1]) 是否成立，成立时走这一分支。 */
    if (values.length && (values.length !== 2 || values[0] > values[1]))
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请选择顺序正确的日期区间");
    const /* 数据（data）保存根据前面的条件，从两个候选结果中选择一个：props.mode === "revisions" ? await habitApi.revisions( id.value, { page: list.page, size: list.size …。 */ data =
      props.mode === "revisions"
        ? await habitApi.revisions(
            id.value,
            { /* 当前页（page）字段使用表达式 list.page 的结果。 */ page: list.page, /* 每页条数（size）字段使用表达式 list.size 的结果。 */ size: list.size },
            controller.signal,
          )
        : await habitApi.checkins(
            id.value,
            {
              /* 当前页（page）字段使用表达式 list.page 的结果。 */
              page: list.page,
              /* 每页条数（size）字段使用表达式 list.size 的结果。 */
              size: list.size,
              /* 日期来源（dateFrom）字段使用表达式 values[0] 的结果。 */
              dateFrom: values[0],
              /* 日期To（dateTo）字段使用表达式 values[1] 的结果。 */
              dateTo: values[1],
              /* 已取消（cancelled）字段使用表达式 cancelled.value 的结果。 */
              cancelled: cancelled.value,
            },
            controller.signal,
          );
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence)
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(list, { /* 记录列表（rows）字段使用表达式 data.items 的结果。 */ rows: data.items, /* 总数（total）字段使用表达式 data.total 的结果。 */ total: data.total });
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence && e.name !== "AbortError" 是否成立，成立时走这一分支。 */
    if (seq === sequence && e.name !== "AbortError") /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 取消（cancel）：调用 cancel 完成取消处理。 */
async function cancel(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction(
      "撤销今天这次打卡？原始记录仍保留，今日应做按最新规则重新判断。",
    );
    /* 把表达式 row.id 的结果写入 pending.value。 */
    pending.value = row.id;
    /* 等待调用 habitApi.cancel 完成取消处理。 */
    await habitApi.cancel(id.value, row);
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("今日打卡已撤销");
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("habit");
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("changed");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 检查条件 e.status === 409 是否成立，成立时走这一分支。 */
    if (e.status === 409) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load();
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
watch(
  () => [props.modelValue, id.value, props.mode],
  () => {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    sequence++;
    /* 把一个空列表，用来保存后续多条数据写入 list.rows。 */
    list.rows = [];
    /* 把固定数值 1写入 list.page。 */
    list.page = 1;
    /* 把一个空列表，用来保存后续多条数据写入 dates.value。 */
    dates.value = [];
    /* 把空值，表示当前还没有对应内容写入 cancelled.value。 */
    cancelled.value = null;
    /* 检查条件 props.modelValue 是否成立，成立时走这一分支。 */
    if (props.modelValue) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
    else /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort();
  },
  { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
);
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
});
</script>
<template>
  <!-- 显示Element Plus 的 drawer 组件；与 visible 双向同步；title 取自 (source?.name || '习惯') + (mode === 'revisions' ? ' · 规则与生命周期' : ' · 打卡历史')；size 固定为 min(960px, 100vw)；启用 destroy-on-close 属性。 -->
  <el-drawer
    v-model="visible"
    :title="
      (source?.name || '习惯') +
      (mode === 'revisions' ? ' · 规则与生命周期' : ' · 打卡历史')
    "
    size="min(960px, 100vw)"
    destroy-on-close
  >
    <!-- 显示一个布局容器；仅当 mode !== 'revisions' 成立时渲染；用样式类 toolbar 控制外观。 -->
    <div v-if="mode !== 'revisions'" class="toolbar">
      <!-- 显示Element Plus 的 date-picker 组件；与 dates 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；start-placeholder 固定为 起始日期；end-placeholder 固定为 结束日期。 -->
      <el-date-picker
        v-model="dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="起始日期"
        end-placeholder="结束日期"
      />
      <!-- 显示Element Plus 的 select 组件；与 cancelled 双向同步；启用 clearable 属性；未输入时提示 全部记录。 -->
      <el-select v-model="cancelled" clearable placeholder="全部记录"
        ><!-- 显示Element Plus 的 option 组件；label 固定为 有效打卡；value 取自 false。 --><el-option label="有效打卡" :value="false" /><!-- 显示Element Plus 的 option 组件；label 固定为 已撤销；value 取自 true。 --><el-option
          label="已撤销"
          :value="true"
      /></el-select>
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 load(true)。 -->
      <el-button @click="load(true)">查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 dates = []; cancelled = null; load(true);。 --><el-button
        @click="
          dates = [];
          cancelled = null;
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
      <!-- 显示Element Plus 的 table 组件；仅当 mode !== 'revisions' 成立时渲染；data 取自 list.rows；loading 指令使用 busy；empty-text 固定为 还没有打卡记录。 -->
      <el-table
        v-if="mode !== 'revisions'"
        :data="list.rows"
        v-loading="busy"
        empty-text="还没有打卡记录"
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '还没有打卡记录'。 --><AppEmptyState
            :description="
              error ? '加载失败，请查看提示后重试。' : '还没有打卡记录'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 businessDate；label 固定为 原始打卡日；min-width 固定为 130。 --><el-table-column
          prop="businessDate"
          label="原始打卡日"
          min-width="130"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 nameSnapshot；label 固定为 当时习惯名称；min-width 固定为 150。 --><el-table-column
          prop="nameSnapshot"
          label="当时习惯名称"
          min-width="150"
        />
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 打卡时间；min-width 固定为 180。 -->
        <el-table-column label="打卡时间" min-width="180"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 instantText(row.checkedAt, row.timeZone) 的当前值转成文字显示。 -->{{
            instantText(row.checkedAt, row.timeZone)
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 100。 -->
        <el-table-column label="状态" width="100"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示Element Plus 的 tag 组件；type 取自 row.cancelledAt ? 'info' : 'success'。 --><el-tag :type="row.cancelledAt ? 'info' : 'success'"><!-- 把 row.cancelledAt ? "已撤销" : "有效" 的当前值转成文字显示。 -->{{
              row.cancelledAt ? "已撤销" : "有效"
            }}</el-tag></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 撤销时间；min-width 固定为 180。 -->
        <el-table-column label="撤销时间" min-width="180"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 instantText(row.cancelledAt, row.timeZone) 的当前值转成文字显示。 -->{{
            instantText(row.cancelledAt, row.timeZone)
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 timeZone；label 固定为 打卡时区；min-width 固定为 140。 -->
        <el-table-column prop="timeZone" label="打卡时区" min-width="140" />
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 100；fixed 固定为 right。 -->
        <el-table-column label="操作" width="100" fixed="right"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }">
            <!-- 显示Element Plus 的 button 组件；仅当 !row.cancelledAt && row.businessDate === today() && !source?.deletedAt && source?.canCancel !== false && … 成立时渲染；启用 link 属性；类型设为 warning；disabled 取自 !!pending || (source?.historyLockedThrough && today() <= source.historyLockedThrough)；click 事件触发时执行 cancel(row)。 -->
            <el-button
              v-if="
                !row.cancelledAt &&
                row.businessDate === today() &&
                !source?.deletedAt &&
                source?.canCancel !== false &&
                canGrow('habit:checkin:cancel')
              "
              link
              type="warning"
              :disabled="
                !!pending ||
                (source?.historyLockedThrough &&
                  today() <= source.historyLockedThrough)
              "
              @click="cancel(row)"
              >撤销</el-button
            >
          </template></el-table-column
        >
      </el-table>
      <!-- 显示Element Plus 的 table 组件；前面条件均不成立时显示这个备用分支；data 取自 list.rows；loading 指令使用 busy；empty-text 固定为 还没有规则修订。 -->
      <el-table
        v-else
        :data="list.rows"
        v-loading="busy"
        empty-text="还没有规则修订"
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '还没有规则修订'。 --><AppEmptyState
            :description="
              error ? '加载失败，请查看提示后重试。' : '还没有规则修订'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；类型设为 expand。 --><el-table-column type="expand"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示一个布局容器；用样式类 history-expanded 控制外观。 --><div class="history-expanded">
              <!-- 显示重点文字。 -->
              <strong>完成标准</strong>
              <!-- 显示一段文字；用样式类 preserve-text 控制外观。 -->
              <p class="preserve-text">
                <!-- 把 row.completionCriteria || "未填写" 的当前值转成文字显示。 -->
                {{ row.completionCriteria || "未填写" }}
              </p>
              <!-- 显示重点文字。 -->
              <strong>描述</strong>
              <!-- 显示一段文字；用样式类 preserve-text 控制外观。 -->
              <p class="preserve-text"><!-- 把 row.description || "未填写" 的当前值转成文字显示。 -->{{ row.description || "未填写" }}</p>
              <!-- 显示一段文字；用样式类 hint 控制外观。 -->
              <p class="hint">
                分类归属：<!-- 把 row.categoryId ? "分类 " + row.categoryId : "未分类" 的当前值转成文字显示。 -->{{
                  row.categoryId ? "分类 " + row.categoryId : "未分类"
                }}
                · 提示时间 <!-- 把 row.expectedTime || "未设置" 的当前值转成文字显示。 -->{{ row.expectedTime || "未设置" }} · 开始日期
                <!-- 把 row.startDate 的当前值转成文字显示。 -->
                {{ row.startDate }}
              </p>
            </div></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 revisionNo；label 固定为 修订；width 固定为 80。 -->
        <el-table-column
          prop="revisionNo"
          label="修订"
          width="80"
        /><!-- 显示Element Plus 的 table-column 组件；label 固定为 动作；min-width 固定为 160。 --><el-table-column label="动作" min-width="160"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 actions[row.actionType] || "规则变化" 的当前值转成文字显示。 -->{{
            actions[row.actionType] || "规则变化"
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 名称快照；min-width 固定为 140。 -->
        <el-table-column
          prop="name"
          label="名称快照"
          min-width="140"
        /><!-- 显示Element Plus 的 table-column 组件；label 固定为 当时周期；min-width 固定为 150。 --><el-table-column label="当时周期" min-width="150"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 cycleText(row) 的当前值转成文字显示。 -->{{
            cycleText(row)
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 effectiveDate；label 固定为 生效日期；min-width 固定为 120。 -->
        <el-table-column
          prop="effectiveDate"
          label="生效日期"
          min-width="120"
        /><!-- 显示Element Plus 的 table-column 组件；label 固定为 发生时间；min-width 固定为 180。 --><el-table-column label="发生时间" min-width="180"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 instantText(row.changedAt, row.timeZone) 的当前值转成文字显示。 -->{{
            instantText(row.changedAt, row.timeZone)
          }}</template></el-table-column
        >
      </el-table>
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 load。 -->
      <PagePagination :state="list" @change="load" />
    </LoadState>
  </el-drawer>
</template>
