<!-- 渲染应用ThemeSwitch界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 ../../stores/uiPreferences 提供的组件、状态函数或请求方法。 */
import { uiPreferences, setTheme } from "../../stores/uiPreferences";
/* 引入 ./AppIcon.vue 提供的组件、状态函数或请求方法。 */
import AppIcon from "./AppIcon.vue";
const /* 选项（options）保存表达式 [ { value: "light", label: "浅色" }, { value: "dark", label: "深色" }, { value: "system", label: "跟随系统" }, ] 的结果。 */ options = [
  { /* 值（value）字段使用固定文字 'light'。 */ value: "light", /* 显示名称（label）字段使用固定文字 '浅色'。 */ label: "浅色" },
  { /* 值（value）字段使用固定文字 'dark'。 */ value: "dark", /* 显示名称（label）字段使用固定文字 '深色'。 */ label: "深色" },
  { /* 值（value）字段使用固定文字 'system'。 */ value: "system", /* 显示名称（label）字段使用固定文字 '跟随系统'。 */ label: "跟随系统" },
];
</script>
<template>
  <!-- 显示Element Plus 的 dropdown 组件；trigger 固定为 click；command 事件触发时执行 setTheme。 -->
  <el-dropdown trigger="click" @command="setTheme">
    <!-- 显示Element Plus 的 button 组件；启用 text 属性；用样式类 theme-switch 控制外观；aria-label 取自 '切换主题，当前' + options.find((o) => o.value === uiPreferences.theme).label；title 固定为 切换主题。 -->
    <el-button
      text
      class="theme-switch"
      :aria-label="
        '切换主题，当前' +
        options.find((o) => o.value === uiPreferences.theme).label
      "
      title="切换主题"
    >
      <!-- 显示应用Icon（AppIcon）组件；name 取自 uiPreferences.theme。 -->
      <AppIcon :name="uiPreferences.theme" />
      <!-- 显示行内文字区域；用样式类 theme-label 控制外观。 -->
      <span class="theme-label"><!-- 把 options.find((o) => o.value === uiPreferences.theme).label 的当前值转成文字显示。 -->{{
        options.find((o) => o.value === uiPreferences.theme).label
      }}</span>
    </el-button>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
    <template #dropdown>
      <!-- 显示Element Plus 的 dropdown-menu 组件。 -->
      <el-dropdown-menu>
        <!-- 显示Element Plus 的 dropdown-item 组件；按 option in options 为每条数据重复生成内容；key 取自 option.value；command 取自 option.value。 -->
        <el-dropdown-item
          v-for="option in options"
          :key="option.value"
          :command="option.value"
        >
          <!-- 显示行内文字区域；用样式类 theme-option 控制外观。 -->
          <span class="theme-option"
            ><!-- 显示应用Icon（AppIcon）组件；name 取自 option.value。 --><AppIcon :name="option.value" /><!-- 显示行内文字区域。 --><span><!-- 把 option.label 的当前值转成文字显示。 -->{{ option.label }}</span
            ><!-- 显示应用Icon（AppIcon）组件；仅当 uiPreferences.theme === option.value 成立时渲染；name 固定为 check；size 取自 16。 --><AppIcon
              v-if="uiPreferences.theme === option.value"
              name="check"
              :size="16"
          /></span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>
