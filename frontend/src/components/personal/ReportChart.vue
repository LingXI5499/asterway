<!-- 渲染复盘报告Chart界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, watch, onMounted, onBeforeUnmount, computed } from "vue";
/* 引入 echarts/core 提供的组件、状态函数或请求方法。 */
import { init, use } from "echarts/core";
/* 引入 echarts/charts 提供的组件、状态函数或请求方法。 */
import { BarChart, LineChart } from "echarts/charts";
/* 引入 echarts/components 提供的组件、状态函数或请求方法。 */
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from "echarts/components";
/* 引入 echarts/renderers 提供的组件、状态函数或请求方法。 */
import { CanvasRenderer } from "echarts/renderers";
/* 引入 ../../stores/uiPreferences 提供的组件、状态函数或请求方法。 */
import { chartTheme, resolvedTheme } from "../../stores/uiPreferences";
/* 调用 use 完成use处理。 */
use([
  BarChart,
  LineChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
]);
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 记录列表（rows）字段使用表达式 { type: Array, default: () => [] } 的结果。 */
  rows: { /* 类型（type）字段使用Array。 */ type: Array, /* 默认值（default）字段使用表达式 () => [] 的结果。 */ default: () => [] },
  /* 种类（kind）字段使用可为空的文字（String）。 */
  kind: String,
  /* 显示名称（label）字段使用可为空的文字（String）。 */
  label: String,
});
const /* canvas保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ canvas = ref(null);
const /* 是否存在数据（hasData）保存随相关状态变化自动重算的值。 */ hasData = computed(
  () =>
    props.rows.length > 0 &&
    (props.kind !== "habits" ||
      props.rows.some((row) => row.habitCompletionRate != null)),
);
let /* 声明 chart，稍后再填入处理结果。 */ chart, /* 声明 observer，稍后再填入处理结果。 */ observer;
/* draw：调用 draw 完成draw处理。 */
function draw() {
  /* 检查条件 !chart 是否成立，成立时走这一分支。 */
  if (!chart) /* 返回空内容，当前函数到此结束。 */ return;
  const /* 记录列表（rows）保存表达式 props.rows 的结果。 */ rows = props.rows;
  const /* theme保存调用 chartTheme 完成chartTheme处理。 */ theme = chartTheme();
  const /* 基础（base）保存调用 { animation: !window.matchMedia 完成matchMedia处理。 */ base = {
    /* animation字段使用调用 !window.matchMedia 完成matchMedia处理。 */
    animation: !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    /* color字段使用表达式 theme.colors 的结果。 */
    color: theme.colors,
    /* 文字Style（textStyle）字段使用表达式 { color: theme.text, fontFamily: theme.font } 的结果。 */
    textStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text, /* fontFamily字段使用表达式 theme.font 的结果。 */ fontFamily: theme.font },
    /* tooltip字段使用表达式 { trigger: "axis", confine: true, backgroundColor: theme.surface, borderColor: theme.border, textStyle: { color: theme.t… 的结果。 */
    tooltip: {
      /* trigger字段使用固定文字 'axis'。 */
      trigger: "axis",
      /* confine字段使用真，表示条件成立或开关打开。 */
      confine: true,
      /* 后台Color（backgroundColor）字段使用表达式 theme.surface 的结果。 */
      backgroundColor: theme.surface,
      /* borderColor字段使用表达式 theme.border 的结果。 */
      borderColor: theme.border,
      /* 文字Style（textStyle）字段使用表达式 { color: theme.text } 的结果。 */
      textStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
    },
    /* grid字段使用表达式 { left: 48, right: 24, top: 48, bottom: 40 } 的结果。 */
    grid: { /* left字段使用固定数值 48。 */ left: 48, /* right字段使用固定数值 24。 */ right: 24, /* top字段使用固定数值 48。 */ top: 48, /* bottom字段使用固定数值 40。 */ bottom: 40 },
    /* xAxis字段使用把每个条目转换成需要的形式。 */
    xAxis: {
      /* 类型（type）字段使用固定文字 'category'。 */
      type: "category",
      /* 数据（data）字段使用把每个条目转换成需要的形式。 */
      data: rows.map((row) => row.businessDate),
      /* axis显示名称（axisLabel）字段使用表达式 { fontSize: 12, color: theme.text } 的结果。 */
      axisLabel: { /* font每页条数（fontSize）字段使用固定数值 12。 */ fontSize: 12, /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
      /* axisLine字段使用表达式 { lineStyle: { color: theme.border } } 的结果。 */
      axisLine: { /* lineStyle字段使用表达式 { color: theme.border } 的结果。 */ lineStyle: { /* color字段使用表达式 theme.border 的结果。 */ color: theme.border } },
      /* axisTick字段使用表达式 { alignWithLabel: true } 的结果。 */
      axisTick: { /* alignWith显示名称（alignWithLabel）字段使用真，表示条件成立或开关打开。 */ alignWithLabel: true },
    },
    /* yAxis字段使用表达式 { type: "value", nameTextStyle: { color: theme.text }, axisLabel: { color: theme.text }, splitLine: { lineStyle: { color… 的结果。 */
    yAxis: {
      /* 类型（type）字段使用固定文字 'value'。 */
      type: "value",
      /* 名称文字Style（nameTextStyle）字段使用表达式 { color: theme.text } 的结果。 */
      nameTextStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
      /* axis显示名称（axisLabel）字段使用表达式 { color: theme.text } 的结果。 */
      axisLabel: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
      /* splitLine字段使用表达式 { lineStyle: { color: theme.border } } 的结果。 */
      splitLine: { /* lineStyle字段使用表达式 { color: theme.border } 的结果。 */ lineStyle: { /* color字段使用表达式 theme.border 的结果。 */ color: theme.border } },
    },
    /* legend字段使用表达式 { top: 0, textStyle: { fontSize: 12, color: theme.text } } 的结果。 */
    legend: { /* top字段使用固定数值 0。 */ top: 0, /* 文字Style（textStyle）字段使用表达式 { fontSize: 12, color: theme.text } 的结果。 */ textStyle: { /* font每页条数（fontSize）字段使用固定数值 12。 */ fontSize: 12, /* color字段使用表达式 theme.text 的结果。 */ color: theme.text } },
  };
  /* 检查条件 props.kind === "effort" 是否成立，成立时走这一分支。 */
  if (props.kind === "effort")
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(base, {
      /* yAxis字段使用表达式 { ...base.yAxis, name: "小时" } 的结果。 */
      yAxis: { ...base.yAxis, /* 名称（name）字段使用固定文字 '小时'。 */ name: "小时" },
      /* series字段使用把每个条目转换成需要的形式。 */
      series: [
        {
          /* 名称（name）字段使用固定文字 '实际投入（小时）'。 */
          name: "实际投入（小时）",
          /* 类型（type）字段使用固定文字 'bar'。 */
          type: "bar",
          /* bar最大宽度（barMaxWidth）字段使用固定数值 32。 */
          barMaxWidth: 32,
          /* 单条数据Style（itemStyle）字段使用表达式 { borderRadius: [4, 4, 0, 0] } 的结果。 */
          itemStyle: { /* borderRadius字段使用表达式 [4, 4, 0, 0] 的结果。 */ borderRadius: [4, 4, 0, 0] },
          /* 数据（data）字段使用把每个条目转换成需要的形式。 */
          data: rows.map((row) =>
            Number((row.actualMilliseconds / 3600000).toFixed(4)),
          ),
        },
      ],
    });
  else /* 检查条件 props.kind === "habits" 是否成立，成立时走这一分支。 */ if (props.kind === "habits")
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(base, {
      /* yAxis字段使用表达式 { ...base.yAxis, name: "完成率", min: 0, max: 100, axisLabel: { formatter: "{value}%", color: theme.text }, } 的结果。 */
      yAxis: {
        ...base.yAxis,
        /* 名称（name）字段使用固定文字 '完成率'。 */
        name: "完成率",
        /* 最小（min）字段使用固定数值 0。 */
        min: 0,
        /* 最大（max）字段使用固定数值 100。 */
        max: 100,
        /* axis显示名称（axisLabel）字段使用表达式 { formatter: "{value}%", color: theme.text } 的结果。 */
        axisLabel: { /* formatter字段使用固定文字 '{value}%'。 */ formatter: "{value}%", /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
      },
      /* series字段使用根据前面的条件，从两个候选结果中选择一个：[ { name: "习惯完成率（%）", type: "line", connectNulls: false, smooth: false, data: rows.map((row) => row.…。 */
      series: [
        {
          /* 名称（name）字段使用固定文字 '习惯完成率（%）'。 */
          name: "习惯完成率（%）",
          /* 类型（type）字段使用固定文字 'line'。 */
          type: "line",
          /* 连接Nulls（connectNulls）字段使用假，表示条件不成立或开关关闭。 */
          connectNulls: false,
          /* smooth字段使用假，表示条件不成立或开关关闭。 */
          smooth: false,
          /* 数据（data）字段使用根据前面的条件，从两个候选结果中选择一个：rows.map((row) => row.habitCompletionRate == null ? null : Number(row.habitCompletionRate), )。 */
          data: rows.map((row) =>
            row.habitCompletionRate == null
              ? null
              : Number(row.habitCompletionRate),
          ),
          /* symbol每页条数（symbolSize）字段使用固定数值 8。 */
          symbolSize: 8,
          /* lineStyle字段使用表达式 { width: 2 } 的结果。 */
          lineStyle: { /* 宽度（width）字段使用固定数值 2。 */ width: 2 },
        },
      ],
    });
  else
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(base, {
      /* yAxis字段使用表达式 { ...base.yAxis, name: "完成动作", minInterval: 1 } 的结果。 */
      yAxis: { ...base.yAxis, /* 名称（name）字段使用固定文字 '完成动作'。 */ name: "完成动作", /* 最小间隔（minInterval）字段使用固定数值 1。 */ minInterval: 1 },
      /* series字段使用把每个条目转换成需要的形式。 */
      series: [
        {
          /* 名称（name）字段使用固定文字 '目标'。 */
          name: "目标",
          /* 类型（type）字段使用固定文字 'bar'。 */
          type: "bar",
          /* 数据（data）字段使用把每个条目转换成需要的形式。 */
          data: rows.map((row) => row.goalCompletionActions),
          /* bar最大宽度（barMaxWidth）字段使用固定数值 24。 */
          barMaxWidth: 24,
        },
        {
          /* 名称（name）字段使用固定文字 '阶段'。 */
          name: "阶段",
          /* 类型（type）字段使用固定文字 'bar'。 */
          type: "bar",
          /* 数据（data）字段使用把每个条目转换成需要的形式。 */
          data: rows.map((row) => row.stageCompletionActions),
          /* bar最大宽度（barMaxWidth）字段使用固定数值 24。 */
          barMaxWidth: 24,
        },
        {
          /* 名称（name）字段使用固定文字 '任务'。 */
          name: "任务",
          /* 类型（type）字段使用固定文字 'bar'。 */
          type: "bar",
          /* 数据（data）字段使用把每个条目转换成需要的形式。 */
          data: rows.map((row) => row.taskCompletionActions),
          /* bar最大宽度（barMaxWidth）字段使用固定数值 24。 */
          barMaxWidth: 24,
        },
      ],
    });
  /* 设置 选项（Option） 的值。 */
  chart.setOption(base, { /* notMerge字段使用真，表示条件成立或开关打开。 */ notMerge: true });
}
/* 调用 onMounted(() => { chart = init(canvas.value); draw(); observer = new ResizeObserver(() => chart?.resize 完成resize处理。 */
onMounted(() => {
  /* 把调用 init 完成init处理写入 chart。 */
  chart = init(canvas.value);
  /* 调用 draw 完成draw处理。 */
  draw();
  /* 把新建 ResizeObserver 实例写入 observer。 */
  observer = new ResizeObserver(() => chart?.resize());
  /* 调用 observer.observe 完成observe处理。 */
  observer.observe(canvas.value);
});
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => [props.rows, props.kind], draw, { /* deep字段使用真，表示条件成立或开关打开。 */ deep: true });
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(resolvedTheme, draw, { /* 发送缓冲内容（flush）字段使用固定文字 'post'。 */ flush: "post" });
/* 调用 onBeforeUnmount(() => { observer?.disconnect 完成disconnect处理。 */
onBeforeUnmount(() => {
  /* 调用 observer?.disconnect 完成disconnect处理。 */
  observer?.disconnect();
  /* 关闭连接池并释放数据库连接。 */
  chart?.dispose();
  /* 把空值，表示当前还没有对应内容写入 chart。 */
  chart = null;
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 report-chart-wrap 控制外观。 -->
  <div class="report-chart-wrap">
    <!-- 显示一个布局容器；把组件或元素引用保存为 canvas，供脚本读取；用样式类 report-chart 控制外观；class 取自 { 'is-empty': !hasData }；role 固定为 img；aria-label 取自 label；aria-hidden 取自 !hasData。 -->
    <div
      ref="canvas"
      class="report-chart"
      :class="{ 'is-empty': !hasData }"
      role="img"
      :aria-label="label"
      :aria-hidden="!hasData"
    />
    <!-- 显示一个布局容器；仅当 !hasData 成立时渲染；用样式类 chart-empty-state 控制外观。 -->
    <div v-if="!hasData" class="chart-empty-state">
      <!-- 显示应用空状态当前状态（AppEmptyState）组件；title 取自 kind === 'habits' && rows.length ? '当前日期页无应做记录' : '当前区间暂无趋势记录'；description 取自 kind === 'habits' ? '没有应做次数时不计算完成率，也不显示为 0%。' : '可以调整查询区间，查看已有的真实投入。'；icon 固定为 statistics。 -->
      <AppEmptyState
        :title="
          kind === 'habits' && rows.length
            ? '当前日期页无应做记录'
            : '当前区间暂无趋势记录'
        "
        :description="
          kind === 'habits'
            ? '没有应做次数时不计算完成率，也不显示为 0%。'
            : '可以调整查询区间，查看已有的真实投入。'
        "
        icon="statistics"
      />
    </div>
  </div>
</template>
