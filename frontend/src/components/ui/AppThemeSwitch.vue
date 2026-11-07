<script setup>
import { uiPreferences, setTheme } from "../../stores/uiPreferences";
import AppIcon from "./AppIcon.vue";
const options = [
  { value: "light", label: "浅色" },
  { value: "dark", label: "深色" },
  { value: "system", label: "跟随系统" },
];
</script>
<template>
  <el-dropdown trigger="click" @command="setTheme">
    <el-button
      text
      class="theme-switch"
      :aria-label="
        '切换主题，当前' +
        options.find((o) => o.value === uiPreferences.theme).label
      "
      title="切换主题"
    >
      <AppIcon :name="uiPreferences.theme" />
      <span class="theme-label">{{
        options.find((o) => o.value === uiPreferences.theme).label
      }}</span>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="option in options"
          :key="option.value"
          :command="option.value"
        >
          <span class="theme-option"
            ><AppIcon :name="option.value" /><span>{{ option.label }}</span
            ><AppIcon
              v-if="uiPreferences.theme === option.value"
              name="check"
              :size="16"
          /></span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>
