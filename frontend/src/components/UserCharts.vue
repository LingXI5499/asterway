<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
import { init, use } from "echarts/core";
import { PieChart, BarChart } from "echarts/charts";
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { chartTheme, resolvedTheme } from "../stores/uiPreferences";
use([
  PieChart,
  BarChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
]);
const props = defineProps({ statistics: Object });
defineEmits(["refresh"]);
const pie = ref(null),
  bar = ref(null);
let charts = [],
  observer;
function draw() {
  const s = props.statistics;
  const theme = chartTheme();
  const tooltip = {
    backgroundColor: theme.surface,
    borderColor: theme.border,
    textStyle: { color: theme.text },
  };
  charts[0]?.setOption({
    color: theme.colors,
    textStyle: { color: theme.text, fontFamily: theme.font },
    tooltip: { ...tooltip, trigger: "item" },
    legend: { bottom: 0, textStyle: { color: theme.text } },
    series: [
      {
        type: "pie",
        radius: ["45%", "70%"],
        data: [
          { name: "普通用户", value: s.users },
          { name: "管理账户", value: s.administrators },
        ],
      },
    ],
  });
  charts[1]?.setOption({
    color: [theme.colors[0]],
    textStyle: { color: theme.text, fontFamily: theme.font },
    tooltip: { ...tooltip, trigger: "axis" },
    grid: { left: 40, right: 16, bottom: 32, top: 24 },
    xAxis: {
      type: "category",
      data: ["启用", "禁用", "已删除"],
      axisLabel: { color: theme.text },
      axisLine: { lineStyle: { color: theme.border } },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      axisLabel: { color: theme.text },
      splitLine: { lineStyle: { color: theme.border } },
    },
    series: [
      {
        type: "bar",
        barMaxWidth: 40,
        data: [
          Math.max(0, s.users + s.administrators - s.disabled),
          s.disabled,
          s.deleted,
        ],
        itemStyle: { borderRadius: [4, 4, 0, 0] },
      },
    ],
  });
}
onMounted(async () => {
  await nextTick();
  charts = [init(pie.value), init(bar.value)];
  draw();
  observer = new ResizeObserver(() => charts.forEach((c) => c.resize()));
  observer.observe(pie.value);
  observer.observe(bar.value);
});
watch(() => props.statistics, draw, { deep: true });
watch(resolvedTheme, draw, { flush: "post" });
onBeforeUnmount(() => {
  observer?.disconnect();
  charts.forEach((c) => c.dispose());
});
</script>
<template>
  <div class="stats">
    <div v-for="(value, key) in statistics" :key="key" class="stat card">
      <span>{{
        {
          users: "普通用户",
          administrators: "管理账户",
          disabled: "已禁用",
          deleted: "已删除",
        }[key]
      }}</span
      ><strong>{{ value }}</strong>
    </div>
  </div>
  <div class="chart-grid">
    <section class="card">
      <h2>账户构成</h2>
      <div
        ref="pie"
        class="chart"
        role="img"
        aria-label="普通用户与管理账户构成"
      />
    </section>
    <section class="card">
      <div class="section-heading">
        <h2>账户状态</h2>
        <el-button text @click="$emit('refresh')">刷新统计</el-button>
      </div>
      <div
        ref="bar"
        class="chart"
        role="img"
        aria-label="启用禁用及已删除账户数量"
      />
    </section>
  </div>
</template>
