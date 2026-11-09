<!-- 渲染应用当前页请求头界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({ /* 标题（title）字段使用可为空的文字（String）。 */ title: String, /* 说明（description）字段使用可为空的文字（String）。 */ description: String, /* eyebrow字段使用可为空的文字（String）。 */ eyebrow: String });
</script>
<template>
  <!-- 显示一个布局容器；用样式类 page-heading aw-page-header 控制外观。 -->
  <div class="page-heading aw-page-header">
    <!-- 显示一个布局容器。 -->
    <div>
      <!-- 显示行内文字区域；仅当 eyebrow 成立时渲染；用样式类 eyebrow 控制外观。 -->
      <span v-if="eyebrow" class="eyebrow"><!-- 把 eyebrow 的当前值转成文字显示。 -->{{ eyebrow }}</span>
      <!-- 显示一级标题。 -->
      <h1><!-- 把 title 的当前值转成文字显示。 -->{{ title }}</h1>
      <!-- 显示一段文字；仅当 description 成立时渲染；用样式类 muted 控制外观。 -->
      <p v-if="description" class="muted"><!-- 把 description 的当前值转成文字显示。 -->{{ description }}</p>
    </div>
    <!-- 显示一个布局容器；仅当 $slots.default 成立时渲染；用样式类 toolbar 控制外观。 -->
    <div v-if="$slots.default" class="toolbar"><!-- 显示由父组件补充内容的插槽。 --><slot /></div>
  </div>
</template>
