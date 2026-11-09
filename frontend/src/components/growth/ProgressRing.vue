<!-- 渲染进度Ring界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 仅展示后端给出的加权进度，不根据概览样本推算总进度。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
/* 引入 echarts/core 提供的组件、状态函数或请求方法。 */
import { init, use } from "echarts/core";
/* 引入 echarts/charts 提供的组件、状态函数或请求方法。 */
import { PieChart } from "echarts/charts";
/* 引入 echarts/renderers 提供的组件、状态函数或请求方法。 */
import { CanvasRenderer } from "echarts/renderers";
/* 引入 ../../stores/uiPreferences 提供的组件、状态函数或请求方法。 */
import { chartTheme, resolvedTheme } from "../../stores/uiPreferences";
/* 调用 use 完成use处理。 */
use([PieChart, CanvasRenderer]);
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 值（value）字段使用表达式 [Number, String] 的结果。 */ value: [Number, String] });
const /* 节点（node）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ node = ref(null);
let /* 声明 chart，稍后再填入处理结果。 */ chart, /* 声明 observer，稍后再填入处理结果。 */ observer;
/* draw：调用 draw 完成draw处理。 */
function draw() {
  /* 检查条件 !chart 是否成立，成立时走这一分支。 */
  if (!chart) /* 返回空内容，当前函数到此结束。 */ return;
  const /* 值（value）保存优先使用前面的有效值，没有时采用后面的备用值：Number(props.value || 0)。 */ value = Number(props.value || 0);
  const /* theme保存调用 chartTheme 完成chartTheme处理。 */ theme = chartTheme();
  /* 设置 选项（Option） 的值。 */
  chart.setOption({
    /* animation字段使用条件判断的结果：!matchMedia("(prefers-reduced-motion: reduce)").matches。 */
    animation: !matchMedia("(prefers-reduced-motion: reduce)").matches,
    /* color字段使用表达式 [theme.colors[0], theme.border] 的结果。 */
    color: [theme.colors[0], theme.border],
    /* series字段使用按表达式计算得到的值：[ { type: "pie", radius: ["76%", "90%"], silent: true, label: { show: false }, data: [value, 100 - value], }, ]。 */
    series: [
      {
        /* 类型（type）字段使用固定文字 'pie'。 */
        type: "pie",
        /* radius字段使用表达式 ["76%", "90%"] 的结果。 */
        radius: ["76%", "90%"],
        /* silent字段使用真，表示条件成立或开关打开。 */
        silent: true,
        /* 显示名称（label）字段使用表达式 { show: false } 的结果。 */
        label: { /* show字段使用假，表示条件不成立或开关关闭。 */ show: false },
        /* 数据（data）字段使用按表达式计算得到的值：[value, 100 - value]。 */
        data: [value, 100 - value],
      },
    ],
  });
}
/* 调用 onMounted(() => { chart = init(node.value); draw(); observer = new ResizeObserver(() => chart.resize 完成resize处理。 */
onMounted(() => {
  /* 把调用 init 完成init处理写入 chart。 */
  chart = init(node.value);
  /* 调用 draw 完成draw处理。 */
  draw();
  /* 把新建 ResizeObserver 实例写入 observer。 */
  observer = new ResizeObserver(() => chart.resize());
  /* 调用 observer.observe 完成observe处理。 */
  observer.observe(node.value);
});
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => props.value, draw);
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(resolvedTheme, draw, { /* 发送缓冲内容（flush）字段使用固定文字 'post'。 */ flush: "post" });
/* 调用 onBeforeUnmount(() => { observer?.disconnect 完成disconnect处理。 */
onBeforeUnmount(() => {
  /* 调用 observer?.disconnect 完成disconnect处理。 */
  observer?.disconnect();
  /* 关闭连接池并释放数据库连接。 */
  chart?.dispose();
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 progress-ring 控制外观；role 固定为 img；aria-label 取自 value == null ? '暂无可计算的任务进度' : '加权完成进度 ' + value + '%'。 -->
  <div
    class="progress-ring"
    role="img"
    :aria-label="
      value == null ? '暂无可计算的任务进度' : '加权完成进度 ' + value + '%'
    "
  >
    <!-- 显示一个布局容器；把组件或元素引用保存为 node，供脚本读取；用样式类 ring-canvas 控制外观。 -->
    <div ref="node" class="ring-canvas" />
    <!-- 显示一个布局容器；用样式类 ring-label 控制外观。 -->
    <div class="ring-label">
      <!-- 显示重点文字。 -->
      <strong><!-- 把 value === null || value === undefined ? "—" : value + "%" 的当前值转成文字显示。 -->{{
        value === null || value === undefined ? "—" : value + "%"
      }}</strong
      ><!-- 显示行内文字区域。 --><span><!-- 把 value === null || value === undefined ? "暂无有效任务" : "加权完成进度" 的当前值转成文字显示。 -->{{
        value === null || value === undefined ? "暂无有效任务" : "加权完成进度"
      }}</span>
    </div>
  </div>
</template>
