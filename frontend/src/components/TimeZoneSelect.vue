<!-- 渲染时间时区选择界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { browserZone, report } from "../api/ui";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 模型值（modelValue）字段使用可为空的文字（String）。 */ modelValue: String });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue"]);
const /* zones保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ zones = ref([]),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false);
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0;
/* 搜索（search）：调用 search 完成搜索处理。 */
async function search(keyword = "") {
  const /* 本轮对话（turn）保存按表达式计算得到的值：++sequence。 */ turn = ++sequence;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 api 完成接口处理 完成后取得结果。 */ data = await api(
      "/time-zones?" + new URLSearchParams({ /* 直接使用已有的keyword作为同名字段。 */ keyword, /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 100。 */ size: 100 }),
    );
    /* 检查条件 turn === sequence 是否成立，成立时走这一分支。 */
    if (turn === sequence) /* 把表达式 data.items 的结果写入 zones.value。 */ zones.value = data.items;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 检查条件 turn === sequence 是否成立，成立时走这一分支。 */
    if (turn === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(search);
</script>
<template>
  <!-- 显示一个布局容器；用样式类 zone-control 控制外观。 -->
  <div class="zone-control">
    <!-- 显示Element Plus 的 select 组件；model-value 取自 props.modelValue；update:model-value 事件触发时执行 emit('update:modelValue', $event)；启用 filterable 属性；启用 remote 属性；remote-method 取自 search；loading 取自 busy；未输入时提示 搜索 IANA 时区。 -->
    <el-select
      :model-value="props.modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      filterable
      remote
      :remote-method="search"
      :loading="busy"
      placeholder="搜索 IANA 时区"
      ><!-- 显示Element Plus 的 option 组件；仅当 modelValue 成立时渲染；value 取自 modelValue；label 取自 modelValue。 --><el-option
        v-if="modelValue"
        :value="modelValue"
        :label="modelValue" /><!-- 显示Element Plus 的 option 组件；按 zone in zones.filter((z) => z !== modelValue) 为每条数据重复生成内容；key 取自 zone；value 取自 zone；label 取自 zone。 --><el-option
        v-for="zone in zones.filter((z) => z !== modelValue)"
        :key="zone"
        :value="zone"
        :label="zone" /></el-select
    ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 emit('update:modelValue', browserZone())。 --><el-button @click="emit('update:modelValue', browserZone())"
      >识别设备时区</el-button
    >
  </div>
</template>
