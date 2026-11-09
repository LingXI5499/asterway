<!-- 渲染用户Charts界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
/* 引入 echarts/core 提供的组件、状态函数或请求方法。 */
import { init, use } from "echarts/core";
/* 引入 echarts/charts 提供的组件、状态函数或请求方法。 */
import { PieChart, BarChart } from "echarts/charts";
/* 引入 echarts/components 提供的组件、状态函数或请求方法。 */
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from "echarts/components";
/* 引入 echarts/renderers 提供的组件、状态函数或请求方法。 */
import { CanvasRenderer } from "echarts/renderers";
/* 引入 ../stores/uiPreferences 提供的组件、状态函数或请求方法。 */
import { chartTheme, resolvedTheme } from "../stores/uiPreferences";
/* 调用 use 完成use处理。 */
use([
  PieChart,
  BarChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
]);
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 统计（statistics）字段使用对象（Object）。 */ statistics: Object });
/* 声明当前组件可以通知父组件的事件。 */
defineEmits(["refresh"]);
const /* pie保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ pie = ref(null),
  /* bar保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  bar = ref(null);
let /* charts保存一个空列表，用来保存后续多条数据。 */ charts = [],
  /* 声明 observer，稍后再填入处理结果。 */
  observer;
/* draw：调用 draw 完成draw处理。 */
function draw() {
  const /* 当前配置（s）保存表达式 props.statistics 的结果。 */ s = props.statistics;
  const /* theme保存调用 chartTheme 完成chartTheme处理。 */ theme = chartTheme();
  const /* tooltip保存表达式 { backgroundColor: theme.surface, borderColor: theme.border, textStyle: { color: theme.text }, } 的结果。 */ tooltip = {
    /* 后台Color（backgroundColor）字段使用表达式 theme.surface 的结果。 */
    backgroundColor: theme.surface,
    /* borderColor字段使用表达式 theme.border 的结果。 */
    borderColor: theme.border,
    /* 文字Style（textStyle）字段使用表达式 { color: theme.text } 的结果。 */
    textStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
  };
  /* 设置 选项（Option） 的值。 */
  charts[0]?.setOption({
    /* color字段使用表达式 theme.colors 的结果。 */
    color: theme.colors,
    /* 文字Style（textStyle）字段使用表达式 { color: theme.text, fontFamily: theme.font } 的结果。 */
    textStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text, /* fontFamily字段使用表达式 theme.font 的结果。 */ fontFamily: theme.font },
    /* tooltip字段使用表达式 { ...tooltip, trigger: "item" } 的结果。 */
    tooltip: { ...tooltip, /* trigger字段使用固定文字 'item'。 */ trigger: "item" },
    /* legend字段使用表达式 { bottom: 0, textStyle: { color: theme.text } } 的结果。 */
    legend: { /* bottom字段使用固定数值 0。 */ bottom: 0, /* 文字Style（textStyle）字段使用表达式 { color: theme.text } 的结果。 */ textStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text } },
    /* series字段使用表达式 [ { type: "pie", radius: ["45%", "70%"], data: [ { name: "普通用户", value: s.users }, { name: "管理账户", value: s.administrato… 的结果。 */
    series: [
      {
        /* 类型（type）字段使用固定文字 'pie'。 */
        type: "pie",
        /* radius字段使用表达式 ["45%", "70%"] 的结果。 */
        radius: ["45%", "70%"],
        /* 数据（data）字段使用表达式 [ { name: "普通用户", value: s.users }, { name: "管理账户", value: s.administrators }, ] 的结果。 */
        data: [
          { /* 名称（name）字段使用固定文字 '普通用户'。 */ name: "普通用户", /* 值（value）字段使用表达式 s.users 的结果。 */ value: s.users },
          { /* 名称（name）字段使用固定文字 '管理账户'。 */ name: "管理账户", /* 值（value）字段使用表达式 s.administrators 的结果。 */ value: s.administrators },
        ],
      },
    ],
  });
  /* 设置 选项（Option） 的值。 */
  charts[1]?.setOption({
    /* color字段使用表达式 [theme.colors[0]] 的结果。 */
    color: [theme.colors[0]],
    /* 文字Style（textStyle）字段使用表达式 { color: theme.text, fontFamily: theme.font } 的结果。 */
    textStyle: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text, /* fontFamily字段使用表达式 theme.font 的结果。 */ fontFamily: theme.font },
    /* tooltip字段使用表达式 { ...tooltip, trigger: "axis" } 的结果。 */
    tooltip: { ...tooltip, /* trigger字段使用固定文字 'axis'。 */ trigger: "axis" },
    /* grid字段使用表达式 { left: 40, right: 16, bottom: 32, top: 24 } 的结果。 */
    grid: { /* left字段使用固定数值 40。 */ left: 40, /* right字段使用固定数值 16。 */ right: 16, /* bottom字段使用固定数值 32。 */ bottom: 32, /* top字段使用固定数值 24。 */ top: 24 },
    /* xAxis字段使用表达式 { type: "category", data: ["启用", "禁用", "已删除"], axisLabel: { color: theme.text }, axisLine: { lineStyle: { color: theme.b… 的结果。 */
    xAxis: {
      /* 类型（type）字段使用固定文字 'category'。 */
      type: "category",
      /* 数据（data）字段使用表达式 ["启用", "禁用", "已删除"] 的结果。 */
      data: ["启用", "禁用", "已删除"],
      /* axis显示名称（axisLabel）字段使用表达式 { color: theme.text } 的结果。 */
      axisLabel: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
      /* axisLine字段使用表达式 { lineStyle: { color: theme.border } } 的结果。 */
      axisLine: { /* lineStyle字段使用表达式 { color: theme.border } 的结果。 */ lineStyle: { /* color字段使用表达式 theme.border 的结果。 */ color: theme.border } },
    },
    /* yAxis字段使用表达式 { type: "value", minInterval: 1, axisLabel: { color: theme.text }, splitLine: { lineStyle: { color: theme.border } }, } 的结果。 */
    yAxis: {
      /* 类型（type）字段使用固定文字 'value'。 */
      type: "value",
      /* 最小间隔（minInterval）字段使用固定数值 1。 */
      minInterval: 1,
      /* axis显示名称（axisLabel）字段使用表达式 { color: theme.text } 的结果。 */
      axisLabel: { /* color字段使用表达式 theme.text 的结果。 */ color: theme.text },
      /* splitLine字段使用表达式 { lineStyle: { color: theme.border } } 的结果。 */
      splitLine: { /* lineStyle字段使用表达式 { color: theme.border } 的结果。 */ lineStyle: { /* color字段使用表达式 theme.border 的结果。 */ color: theme.border } },
    },
    /* series字段使用调用 [ { type: "bar", barMaxWidth: 40, data: [ Math.max 完成最大处理。 */
    series: [
      {
        /* 类型（type）字段使用固定文字 'bar'。 */
        type: "bar",
        /* bar最大宽度（barMaxWidth）字段使用固定数值 40。 */
        barMaxWidth: 40,
        /* 数据（data）字段使用调用 [ Math.max 完成最大处理。 */
        data: [
          Math.max(0, s.users + s.administrators - s.disabled),
          s.disabled,
          s.deleted,
        ],
        /* 单条数据Style（itemStyle）字段使用表达式 { borderRadius: [4, 4, 0, 0] } 的结果。 */
        itemStyle: { /* borderRadius字段使用表达式 [4, 4, 0, 0] 的结果。 */ borderRadius: [4, 4, 0, 0] },
      },
    ],
  });
}
/* 调用 onMounted(async () => { await nextTick(); charts = [init(pie.value), init(bar.value)]; draw(); observer = new ResizeObserver(() => charts.forEach 完成forEach处理。 */
onMounted(async () => {
  /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
  await nextTick();
  /* 把表达式 [init(pie.value), init(bar.value)] 的结果写入 charts。 */
  charts = [init(pie.value), init(bar.value)];
  /* 调用 draw 完成draw处理。 */
  draw();
  /* 把新建 ResizeObserver 实例写入 observer。 */
  observer = new ResizeObserver(() => charts.forEach((c) => c.resize()));
  /* 调用 observer.observe 完成observe处理。 */
  observer.observe(pie.value);
  /* 调用 observer.observe 完成observe处理。 */
  observer.observe(bar.value);
});
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => props.statistics, draw, { /* deep字段使用真，表示条件成立或开关打开。 */ deep: true });
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(resolvedTheme, draw, { /* 发送缓冲内容（flush）字段使用固定文字 'post'。 */ flush: "post" });
/* 调用 onBeforeUnmount(() => { observer?.disconnect 完成disconnect处理。 */
onBeforeUnmount(() => {
  /* 调用 observer?.disconnect 完成disconnect处理。 */
  observer?.disconnect();
  /* 调用 charts.forEach 完成forEach处理。 */
  charts.forEach((c) => c.dispose());
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 stats 控制外观。 -->
  <div class="stats">
    <!-- 显示一个布局容器；按 (value, key) in statistics 为每条数据重复生成内容；key 取自 key；用样式类 stat card 控制外观。 -->
    <div v-for="(value, key) in statistics" :key="key" class="stat card">
      <!-- 显示行内文字区域。 -->
      <span><!-- 把 { users: "普通用户", administrators: "管理账户", disabled: "已禁用", deleted: "已删除", }[key] 的当前值转成文字显示。 -->{{
        {
          users: "普通用户",
          administrators: "管理账户",
          disabled: "已禁用",
          deleted: "已删除",
        }[key]
      }}</span
      ><!-- 显示重点文字。 --><strong><!-- 把 value 的当前值转成文字显示。 -->{{ value }}</strong>
    </div>
  </div>
  <!-- 显示一个布局容器；用样式类 chart-grid 控制外观。 -->
  <div class="chart-grid">
    <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
    <section class="card">
      <!-- 显示二级标题。 -->
      <h2>账户构成</h2>
      <!-- 显示一个布局容器；把组件或元素引用保存为 pie，供脚本读取；用样式类 chart 控制外观；role 固定为 img；aria-label 提供无障碍说明 普通用户与管理账户构成。 -->
      <div
        ref="pie"
        class="chart"
        role="img"
        aria-label="普通用户与管理账户构成"
      />
    </section>
    <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
    <section class="card">
      <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
      <div class="section-heading">
        <!-- 显示二级标题。 -->
        <h2>账户状态</h2>
        <!-- 显示Element Plus 的 button 组件；启用 text 属性；click 事件触发时执行 $emit('refresh')。 -->
        <el-button text @click="$emit('refresh')">刷新统计</el-button>
      </div>
      <!-- 显示一个布局容器；把组件或元素引用保存为 bar，供脚本读取；用样式类 chart 控制外观；role 固定为 img；aria-label 提供无障碍说明 启用禁用及已删除账户数量。 -->
      <div
        ref="bar"
        class="chart"
        role="img"
        aria-label="启用禁用及已删除账户数量"
      />
    </section>
  </div>
</template>
