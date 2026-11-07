<script setup>
// 仅展示后端给出的加权进度，不根据概览样本推算总进度。
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
import { init, use } from "echarts/core";
import { PieChart } from "echarts/charts";
import { CanvasRenderer } from "echarts/renderers";
import { chartTheme, resolvedTheme } from "../../stores/uiPreferences";
use([PieChart, CanvasRenderer]);
const props = defineProps({ value: [Number, String] });
const node = ref(null);
let chart, observer;
function draw() {
  if (!chart) return;
  const value = Number(props.value || 0);
  const theme = chartTheme();
  chart.setOption({
    animation: !matchMedia("(prefers-reduced-motion: reduce)").matches,
    color: [theme.colors[0], theme.border],
    series: [
      {
        type: "pie",
        radius: ["76%", "90%"],
        silent: true,
        label: { show: false },
        data: [value, 100 - value],
      },
    ],
  });
}
onMounted(() => {
  chart = init(node.value);
  draw();
  observer = new ResizeObserver(() => chart.resize());
  observer.observe(node.value);
});
watch(() => props.value, draw);
watch(resolvedTheme, draw, { flush: "post" });
onBeforeUnmount(() => {
  observer?.disconnect();
  chart?.dispose();
});
</script>
<template>
  <div
    class="progress-ring"
    role="img"
    :aria-label="
      value == null ? '暂无可计算的任务进度' : '加权完成进度 ' + value + '%'
    "
  >
    <div ref="node" class="ring-canvas" />
    <div class="ring-label">
      <strong>{{
        value === null || value === undefined ? "—" : value + "%"
      }}</strong
      ><span>{{
        value === null || value === undefined ? "暂无有效任务" : "加权完成进度"
      }}</span>
    </div>
  </div>
</template>
