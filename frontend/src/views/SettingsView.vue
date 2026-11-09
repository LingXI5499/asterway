<!-- 渲染配置页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api, hasPermission } from "../api/http";
const /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ form = ref(null),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 form.value。 */
    form.value = await api("/settings/account");
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 检查条件 !form.value.siteName.trim() 是否成立，成立时走这一分支。 */
  if (!form.value.siteName.trim()) {
    /* 把固定文字 '站点名称不能为空'写入 error.value。 */
    error.value = "站点名称不能为空";
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 api 完成接口处理。 */
    await api("/settings/account", { /* 方法（method）字段使用固定状态或编码 PUT（PUT）。 */ method: "PUT", /* 请求正文（body）字段使用表达式 form.value 的结果。 */ body: form.value });
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("设置已保存");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 button 组件；仅当 error && !form 成立时渲染；click 事件触发时执行 load。 --><el-button v-if="error && !form" @click="load">重新加载设置</el-button
    ><!-- 显示Element Plus 的 skeleton 组件；仅当 !form && !error 成立时渲染；rows 取自 3。 --><el-skeleton v-if="!form && !error" :rows="3" /><!-- 显示Element Plus 的 form 组件；前一条件不成立且 form 成立时渲染；label-position 固定为 top；用样式类 form-narrow 控制外观；submit 事件触发时执行 save。 --><el-form
      v-else-if="form"
      label-position="top"
      class="form-narrow"
      @submit.prevent="save"
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 站点名称。 --><el-form-item label="站点名称"
        ><!-- 显示Element Plus 的 input 组件；与 form.siteName 双向同步；maxlength 固定为 64。 --><el-input v-model="form.siteName" maxlength="64" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 开放账户注册。 --><el-form-item label="开放账户注册"
        ><!-- 显示Element Plus 的 switch 组件；与 form.registrationEnabled 双向同步。 --><el-switch v-model="form.registrationEnabled"
      /></el-form-item>
      <!-- 显示一段文字；用样式类 hint 控制外观。 -->
      <p class="hint">关闭注册后，管理员仍可在用户管理中创建账户。</p>
      <!-- 显示Element Plus 的 button 组件；仅当 hasPermission('identity:setting:update') 成立时渲染；native-type 固定为 submit；类型设为 primary；loading 取自 busy。 -->
      <el-button
        v-if="hasPermission('identity:setting:update')"
        native-type="submit"
        type="primary"
        :loading="busy"
        >保存设置</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 load；disabled 取自 busy。 --><el-button @click="load" :disabled="busy">重新加载</el-button></el-form
    >
  </section>
</template>
