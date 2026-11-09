<!-- 渲染应用空状态当前状态界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 声明父组件可以传入的属性及其类型或默认值。 */
defineProps({
  /* 标题（title）字段使用表达式 { type: String, default: "" } 的结果。 */
  title: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 ''。 */ default: "" },
  /* 说明（description）字段使用表达式 { type: String, default: "这里还没有记录" } 的结果。 */
  description: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 '这里还没有记录'。 */ default: "这里还没有记录" },
  /* icon字段使用表达式 { type: String, default: "inbox" } 的结果。 */
  icon: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 'inbox'。 */ default: "inbox" },
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 aw-empty 控制外观；role 固定为 status。 -->
  <div class="aw-empty" role="status">
    <!-- 显示一个布局容器；用样式类 aw-empty-icon 控制外观。 -->
    <div class="aw-empty-icon"><!-- 显示应用Icon（AppIcon）组件；name 取自 icon；size 取自 26。 --><AppIcon :name="icon" :size="26" /></div>
    <!-- 显示重点文字。 -->
    <strong><!-- 把 title || description 的当前值转成文字显示。 -->{{ title || description }}</strong>
    <!-- 显示一段文字；仅当 title && description 成立时渲染。 -->
    <p v-if="title && description"><!-- 把 description 的当前值转成文字显示。 -->{{ description }}</p>
    <!-- 显示一个布局容器；仅当 $slots.default 成立时渲染；用样式类 aw-empty-actions 控制外观。 -->
    <div v-if="$slots.default" class="aw-empty-actions"><!-- 显示由父组件补充内容的插槽。 --><slot /></div>
  </div>
</template>
