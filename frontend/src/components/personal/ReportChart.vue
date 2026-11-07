<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from "vue";
import { init, use } from "echarts/core";
import { BarChart, LineChart } from "echarts/charts";
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { chartTheme, resolvedTheme } from "../../stores/uiPreferences";
use([
  BarChart,
  LineChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
]);
const props = defineProps({
  rows: { type: Array, default: () => [] },
  kind: String,
  label: String,
});
const canvas = ref(null);
const hasData = computed(
  () =>
    props.rows.length > 0 &&
    (props.kind !== "habits" ||
      props.rows.some((row) => row.habitCompletionRate != null)),
);
let chart, observer;
function draw() {
  if (!chart) return;
  const rows = props.rows;
  const theme = chartTheme();
  const base = {
    animation: !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    color: theme.colors,
    textStyle: { color: theme.text, fontFamily: theme.font },
    tooltip: {
      trigger: "axis",
      confine: true,
      backgroundColor: theme.surface,
      borderColor: theme.border,
      textStyle: { color: theme.text },
    },
    grid: { left: 48, right: 24, top: 48, bottom: 40 },
    xAxis: {
      type: "category",
      data: rows.map((row) => row.businessDate),
      axisLabel: { fontSize: 12, color: theme.text },
      axisLine: { lineStyle: { color: theme.border } },
      axisTick: { alignWithLabel: true },
    },
    yAxis: {
      type: "value",
      nameTextStyle: { color: theme.text },
      axisLabel: { color: theme.text },
      splitLine: { lineStyle: { color: theme.border } },
    },
    legend: { top: 0, textStyle: { fontSize: 12, color: theme.text } },
  };
  if (props.kind === "effort")
    Object.assign(base, {
      yAxis: { ...base.yAxis, name: "小时" },
      series: [
        {
          name: "实际投入（小时）",
          type: "bar",
          barMaxWidth: 32,
          itemStyle: { borderRadius: [4, 4, 0, 0] },
          data: rows.map((row) =>
            Number((row.actualMilliseconds / 3600000).toFixed(4)),
          ),
        },
      ],
    });
  else if (props.kind === "habits")
    Object.assign(base, {
      yAxis: {
        ...base.yAxis,
        name: "完成率",
        min: 0,
        max: 100,
        axisLabel: { formatter: "{value}%", color: theme.text },
      },
      series: [
        {
          name: "习惯完成率（%）",
          type: "line",
          connectNulls: false,
          smooth: false,
          data: rows.map((row) =>
            row.habitCompletionRate == null
              ? null
              : Number(row.habitCompletionRate),
          ),
          symbolSize: 8,
          lineStyle: { width: 2 },
        },
      ],
    });
  else
    Object.assign(base, {
      yAxis: { ...base.yAxis, name: "完成动作", minInterval: 1 },
      series: [
        {
          name: "目标",
          type: "bar",
          data: rows.map((row) => row.goalCompletionActions),
          barMaxWidth: 24,
        },
        {
          name: "阶段",
          type: "bar",
          data: rows.map((row) => row.stageCompletionActions),
          barMaxWidth: 24,
        },
        {
          name: "任务",
          type: "bar",
          data: rows.map((row) => row.taskCompletionActions),
          barMaxWidth: 24,
        },
      ],
    });
  chart.setOption(base, { notMerge: true });
}
onMounted(() => {
  chart = init(canvas.value);
  draw();
  observer = new ResizeObserver(() => chart?.resize());
  observer.observe(canvas.value);
});
watch(() => [props.rows, props.kind], draw, { deep: true });
watch(resolvedTheme, draw, { flush: "post" });
onBeforeUnmount(() => {
  observer?.disconnect();
  chart?.dispose();
  chart = null;
});
</script>
<template>
  <div class="report-chart-wrap">
    <div
      ref="canvas"
      class="report-chart"
      :class="{ 'is-empty': !hasData }"
      role="img"
      :aria-label="label"
      :aria-hidden="!hasData"
    />
    <div v-if="!hasData" class="chart-empty-state">
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
