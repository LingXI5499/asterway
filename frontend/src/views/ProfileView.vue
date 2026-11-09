<!-- 渲染个人资料页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed, onMounted, ref } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute, useRouter } from "vue-router";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api, session, loadCurrent, clearSession } from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import {
  report,
  confirmAction,
  dateText,
  validateAccount,
  validatePassword,
} from "../api/ui";
/* 引入 ../components/TimeZoneSelect.vue 提供的组件、状态函数或请求方法。 */
import TimeZoneSelect from "../components/TimeZoneSelect.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 被强制（forced）字段使用可为空的真假值（Boolean）。 */ forced: Boolean });
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* route保存取得当前页面路径和查询参数。 */
  route = useRoute(),
  /* tab保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  tab = ref(
    ["profile", "password", "zone"].includes(route.query.tab)
      ? route.query.tab
      : "profile",
  ),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* 个人资料（profile）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  profile = ref({}),
  /* 密码（password）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  password = ref({}),
  /* 时区（zone）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  zone = ref(null),
  /* 已选时区（selectedZone）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  selectedZone = ref("");
const /* 用户（user）保存随相关状态变化自动重算的值。 */ user = computed(() => session.user || {});
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待加载 当前（Current）。 */
    await loadCurrent();
    /* 把表达式 { username: user.value.username, nickname: user.value.nickname, version: user.value.version, } 的结果写入 profile.value。 */
    profile.value = {
      /* 用户名（username）字段使用表达式 user.value.username 的结果。 */
      username: user.value.username,
      /* 昵称（nickname）字段使用表达式 user.value.nickname 的结果。 */
      nickname: user.value.nickname,
      /* 版本号（version）字段使用表达式 user.value.version 的结果。 */
      version: user.value.version,
    };
    /* 检查条件 !props.forced 是否成立，成立时走这一分支。 */
    if (!props.forced) {
      /* 把等待 调用 api 完成接口处理 完成后取得结果写入 zone.value。 */
      zone.value = await api("/me/time-zone");
      /* 把优先使用前面的有效值，没有时采用后面的备用值：zone.value.pendingTimeZone || zone.value.timeZone写入 selectedZone.value。 */
      selectedZone.value = zone.value.pendingTimeZone || zone.value.timeZone;
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  }
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save(kind) {
  /* 检查条件 busy.value 是否成立，成立时走这一分支。 */
  if (busy.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 kind === "profile" 是否成立，成立时走这一分支。 */
    if (kind === "profile") {
      /* 检查 账号（Account）。 */
      validateAccount(profile.value);
      /* 等待调用 api 完成接口处理。 */
      await api("/me", { /* 方法（method）字段使用固定状态或编码 PATCH（PATCH）。 */ method: "PATCH", /* 请求正文（body）字段使用表达式 profile.value 的结果。 */ body: profile.value });
    }
    /* 检查条件 kind === "password" 是否成立，成立时走这一分支。 */
    if (kind === "password") {
      /* 检查 密码（Password）。 */
      validatePassword(
        password.value.newPassword,
        password.value.confirmPassword,
      );
      /* 检查条件 !password.value.currentPassword 是否成立，成立时走这一分支。 */
      if (!password.value.currentPassword) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("请输入当前密码");
      /* 等待确认 操作（Action）。 */
      await confirmAction("修改密码后全部设备都需要重新登录，是否继续？");
      /* 等待调用 api 完成接口处理。 */
      await api("/me/password", {
        /* 方法（method）字段使用固定状态或编码 PUT（PUT）。 */
        method: "PUT",
        /* 请求正文（body）字段使用表达式 { ...password.value, version: user.value.version } 的结果。 */
        body: { ...password.value, /* 版本号（version）字段使用表达式 user.value.version 的结果。 */ version: user.value.version },
      });
      /* 调用 clearSession 完成清除会话处理。 */
      clearSession();
      /* 调用 ElMessage.success 完成成功处理。 */
      ElMessage.success("密码已修改，请重新登录");
      /* 返回生成保留原对象其余字段的新副本，只替换明确指定的值，当前函数到此结束。 */
      return router.replace("/login");
    }
    /* 检查条件 kind === "zone" || kind === "cancelZone" 是否成立，成立时走这一分支。 */
    if (kind === "zone" || kind === "cancelZone") {
      /* 检查条件 !selectedZone.value 是否成立，成立时走这一分支。 */
      if (!selectedZone.value) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("请选择时区");
      /* 等待调用 api 完成接口处理。 */
      await api("/me/time-zone", {
        /* 方法（method）字段使用固定状态或编码 PUT（PUT）。 */
        method: "PUT",
        /* 请求正文（body）字段使用根据前面的条件，从两个候选结果中选择一个：{ timeZone: kind === "cancelZone" ? zone.value.timeZone : selectedZone.value, version: zone.value.ve…。 */
        body: {
          /* 用户使用的时区（timeZone）字段使用根据前面的条件，从两个候选结果中选择一个：kind === "cancelZone" ? zone.value.timeZone : selectedZone.value。 */
          timeZone:
            kind === "cancelZone" ? zone.value.timeZone : selectedZone.value,
          /* 版本号（version）字段使用表达式 zone.value.version 的结果。 */
          version: zone.value.version,
        },
      });
    }
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("保存成功");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 !["cancel", "close"].includes(e) 是否成立，成立时走这一分支。 */
    if (!["cancel", "close"].includes(e)) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
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
    <!-- 显示Element Plus 的 alert 组件；仅当 forced 成立时渲染；title 固定为 首次登录或密码被重置，请先修改密码；类型设为 warning；closable 取自 false。 -->
    <el-alert
      v-if="forced"
      title="首次登录或密码被重置，请先修改密码"
      type="warning"
      :closable="false"
    /><!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 --><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 tabs 组件；仅当 !forced 成立时渲染；与 tab 双向同步。 --><el-tabs v-if="!forced" v-model="tab"
      ><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 基本资料；name 固定为 profile。 --><el-tab-pane label="基本资料" name="profile" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 密码安全；name 固定为 password。 --><el-tab-pane
        label="密码安全"
        name="password" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 时区设置；name 固定为 zone。 --><el-tab-pane label="时区设置" name="zone" /></el-tabs
    ><!-- 显示Element Plus 的 form 组件；仅当 !forced && tab === 'profile' 成立时渲染；label-position 固定为 top；用样式类 form-narrow 控制外观；submit 事件触发时执行 save('profile')。 --><el-form
      v-if="!forced && tab === 'profile'"
      label-position="top"
      class="form-narrow"
      @submit.prevent="save('profile')"
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 用户名。 --><el-form-item label="用户名"
        ><!-- 显示Element Plus 的 input 组件；与 profile.username 双向同步；maxlength 固定为 30。 --><el-input v-model="profile.username" maxlength="30" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 昵称。 --><el-form-item label="昵称"
        ><!-- 显示Element Plus 的 input 组件；与 profile.nickname 双向同步；maxlength 固定为 30。 --><el-input v-model="profile.nickname" maxlength="30" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 当前角色。 --><el-form-item label="当前角色"
        ><!-- 显示Element Plus 的 tag 组件；按 role in user.roles 为每条数据重复生成内容；key 取自 role。 --><el-tag v-for="role in user.roles" :key="role"><!-- 把 role 的当前值转成文字显示。 -->{{
          role
        }}</el-tag></el-form-item
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；native-type 固定为 submit；loading 取自 busy。 --><el-button type="primary" native-type="submit" :loading="busy"
        >保存资料</el-button
      ></el-form
    ><!-- 显示Element Plus 的 form 组件；前一条件不成立且 forced || tab === 'password' 成立时渲染；label-position 固定为 top；用样式类 form-narrow 控制外观；submit 事件触发时执行 save('password')。 --><el-form
      v-else-if="forced || tab === 'password'"
      label-position="top"
      class="form-narrow"
      @submit.prevent="save('password')"
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 当前密码。 --><el-form-item label="当前密码"
        ><!-- 显示Element Plus 的 input 组件；与 password.currentPassword 双向同步；类型设为 password；启用 show-password 属性；autocomplete 固定为 current-password。 --><el-input
          v-model="password.currentPassword"
          type="password"
          show-password
          autocomplete="current-password" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 新密码（8～50 个字符）。 --><el-form-item label="新密码（8～50 个字符）"
        ><!-- 显示Element Plus 的 input 组件；与 password.newPassword 双向同步；类型设为 password；启用 show-password 属性；autocomplete 固定为 new-password。 --><el-input
          v-model="password.newPassword"
          type="password"
          show-password
          autocomplete="new-password" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 确认新密码。 --><el-form-item label="确认新密码"
        ><!-- 显示Element Plus 的 input 组件；与 password.confirmPassword 双向同步；类型设为 password；启用 show-password 属性；autocomplete 固定为 new-password。 --><el-input
          v-model="password.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password" /></el-form-item
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；native-type 固定为 submit；loading 取自 busy。 --><el-button type="primary" native-type="submit" :loading="busy"
        >修改密码</el-button
      ></el-form
    >
    <!-- 显示一个布局容器；前一条件不成立且 zone 成立时渲染；用样式类 form-narrow 控制外观。 -->
    <div v-else-if="zone" class="form-narrow">
      <!-- 显示一段文字。 -->
      <p>当前时区：<!-- 把 zone.timeZone 的当前值转成文字显示。 -->{{ zone.timeZone }}</p>
      <!-- 显示Element Plus 的 alert 组件；仅当 zone.pendingTimeZone 成立时渲染；title 取自 `待生效时区：${zone.pendingTimeZone}，生效时间：${dateText(zone.effectiveAt)}`；类型设为 info；closable 取自 false。 -->
      <el-alert
        v-if="zone.pendingTimeZone"
        :title="`待生效时区：${zone.pendingTimeZone}，生效时间：${dateText(zone.effectiveAt)}`"
        type="info"
        :closable="false"
      /><!-- 显示时间时区选择（TimeZoneSelect）组件；与 selectedZone 双向同步。 --><TimeZoneSelect v-model="selectedZone" />
      <!-- 显示一段文字；用样式类 hint 控制外观。 -->
      <p class="hint">修改将在原时区次日午夜生效。</p>
      <!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 busy；click 事件触发时执行 save('zone')。 -->
      <el-button type="primary" :loading="busy" @click="save('zone')"
        >保存时区</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 zone.pendingTimeZone 成立时渲染；loading 取自 busy；click 事件触发时执行 save('cancelZone')。 --><el-button
        v-if="zone.pendingTimeZone"
        :loading="busy"
        @click="save('cancelZone')"
        >取消待生效修改</el-button
      >
    </div>
  </section>
</template>
