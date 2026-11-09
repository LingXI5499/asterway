<!-- 渲染Paged选择界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 远程选项按页加载，不以 100 条截断用户的分类或业务对象。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 模型值（modelValue）字段使用表达式 [String, Number] 的结果。 */
  modelValue: [String, Number],
  /* 显示名称（label）字段使用可为空的文字（String）。 */
  label: String,
  /* placeholder字段使用可为空的文字（String）。 */
  placeholder: String,
  /* loader字段使用Function。 */
  loader: Function,
  /* resource键（resourceKey）字段使用表达式 [String, Number] 的结果。 */
  resourceKey: [String, Number],
  /* 已禁用（disabled）字段使用可为空的真假值（Boolean）。 */
  disabled: Boolean,
  /* 已选已禁用（selectedDisabled）字段使用可为空的真假值（Boolean）。 */
  selectedDisabled: Boolean,
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue"]);
const /* 选项（options）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ options = ref([]),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */
  disposed = false,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
/* 搜索（search）：调用 search 完成搜索处理。 */
async function search(value = "", more = false) {
  /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
  if (props.disabled || !props.loader || disposed) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 !more 是否成立，成立时走这一分支。 */
  if (!more) {
    /* 把值（value）写入 keyword.value。 */
    keyword.value = value;
    /* 把固定数值 1写入 page.value。 */
    page.value = 1;
  }
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
    const /* 数据（data）保存等待 加载 er 完成后取得结果。 */ data = await props.loader(
      { /* keyword字段使用表达式 keyword.value 的结果。 */ keyword: keyword.value, /* 当前页（page）字段使用表达式 page.value 的结果。 */ page: page.value, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 },
      controller.signal,
    );
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq !== sequence || disposed) /* 返回空内容，当前函数到此结束。 */ return;
    const /* merged保存根据前面的条件，从两个候选结果中选择一个：more ? [...options.value, ...data.items] : data.items。 */ merged = more ? [...options.value, ...data.items] : data.items;
    /* 把把每个条目转换成需要的形式写入 options.value。 */
    options.value = [
      ...new Map(merged.map((item) => [String(item.id), item])).values(),
    ];
    /* 把表达式 data.total 的结果写入 total.value。 */
    total.value = data.total;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed && e.name !== "AbortError")
      /* 把表达式 e.message 的结果写入 error.value。 */
      error.value = e.message;
  } finally {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (seq === sequence && !disposed) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* more：调用 more 完成more处理。 */
async function more() {
  /* 检查条件 busy.value 是否成立，成立时走这一分支。 */
  if (busy.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  page.value++;
  /* 等待调用 search 完成搜索处理。 */
  await search(keyword.value, true);
}
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => props.resourceKey,
  () => {
    /* 把一个空列表，用来保存后续多条数据写入 options.value。 */
    options.value = [];
    /* 把固定数值 0写入 total.value。 */
    total.value = 0;
    /* 调用 search 完成搜索处理。 */
    search();
  },
);
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => props.disabled,
  (disabled) => {
    /* 检查条件 !disabled 是否成立，成立时走这一分支。 */
    if (!disabled) /* 调用 search 完成搜索处理。 */ search();
  },
);
/* 注册组件首次挂载完成后的处理。 */
onMounted(() => search());
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 把真，表示条件成立或开关打开写入 disposed。 */
  disposed = true;
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 paged-select 控制外观。 -->
  <div class="paged-select">
    <!-- 显示Element Plus 的 select 组件；model-value 取自 modelValue || ''；update:model-value 事件触发时执行 emit('update:modelValue', $event || '')；placeholder 取自 placeholder；disabled 取自 disabled；启用 filterable 属性；启用 remote 属性；启用 clearable 属性；remote-method 取自 search；loading 取自 busy。 -->
    <el-select
      :model-value="modelValue || ''"
      @update:model-value="emit('update:modelValue', $event || '')"
      :placeholder="placeholder"
      :disabled="disabled"
      filterable
      remote
      clearable
      :remote-method="search"
      :loading="busy"
    >
      <!-- 显示Element Plus 的 option 组件；仅当 modelValue && !options.some((item) => String(item.id) === String(modelValue)) 成立时渲染；value 取自 String(modelValue)；label 取自 label || '已选择 · ' + modelValue；disabled 取自 selectedDisabled。 -->
      <el-option
        v-if="
          modelValue &&
          !options.some((item) => String(item.id) === String(modelValue))
        "
        :value="String(modelValue)"
        :label="label || '已选择 · ' + modelValue"
        :disabled="selectedDisabled"
      />
      <!-- 显示Element Plus 的 option 组件；按 item in options 为每条数据重复生成内容；key 取自 item.id；value 取自 String(item.id)；label 取自 item.name || item.taskName；disabled 取自 selectedDisabled && String(item.id) === String(modelValue)。 -->
      <el-option
        v-for="item in options"
        :key="item.id"
        :value="String(item.id)"
        :label="item.name || item.taskName"
        :disabled="selectedDisabled && String(item.id) === String(modelValue)"
      />
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 -->
      <template #footer>
        <!-- 显示一个布局容器；用样式类 select-footer 控制外观。 -->
        <div class="select-footer">
          <!-- 显示行内文字区域；仅当 error 成立时渲染；role 固定为 alert。 -->
          <span v-if="error" role="alert"><!-- 把 error 的当前值转成文字显示。 -->{{ error }}</span
          ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else>已加载 <!-- 把 options.length 的当前值转成文字显示。 -->{{ options.length }} / <!-- 把 total 的当前值转成文字显示。 -->{{ total }}</span>
          <!-- 显示Element Plus 的 button 组件；仅当 error 成立时渲染；启用 text 属性；click 事件触发时执行 search(keyword)。 -->
          <el-button v-if="error" text @click.stop="search(keyword)"
            >重试</el-button
          >
          <!-- 显示Element Plus 的 button 组件；前一条件不成立且 options.length < total 成立时渲染；启用 text 属性；类型设为 primary；loading 取自 busy；click 事件触发时执行 more。 -->
          <el-button
            v-else-if="options.length < total"
            text
            type="primary"
            :loading="busy"
            @click.stop="more"
            >加载更多</el-button
          >
        </div>
      </template>
    </el-select>
  </div>
</template>
