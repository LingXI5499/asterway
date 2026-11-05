<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
import { init, use } from "echarts/core";
import { BarChart, LineChart } from "echarts/charts";
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
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
let chart, observer;
function draw() {
  if (!chart) return;
  const rows = props.rows;
  const base = {
    animation: !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    color: ["#416ee5", "#238871", "#c78a25"],
    tooltip: { trigger: "axis", confine: true },
    grid: { left: 48, right: 24, top: 48, bottom: 40 },
    xAxis: {
      type: "category",
      data: rows.map((row) => row.businessDate),
      axisLabel: { fontSize: 12 },
      axisTick: { alignWithLabel: true },
    },
    yAxis: { type: "value", splitLine: { lineStyle: { color: "#edf0f7" } } },
    legend: { top: 0, textStyle: { fontSize: 12 } },
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
        axisLabel: { formatter: "{value}%" },
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
onBeforeUnmount(() => {
  observer?.disconnect();
  chart?.dispose();
  chart = null;
});
</script>
<template>
  <div ref="canvas" class="report-chart" role="img" :aria-label="label" />
</template>
