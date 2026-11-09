<!-- 渲染CaptchaPuzzle界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../api/http";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* scene字段使用表达式 { type: String, required: true } 的结果。 */
  scene: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true },
  /* nonce字段使用表达式 { type: String, required: true } 的结果。 */
  nonce: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true },
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["verified"]);
const /* challenge保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ challenge = ref(null),
  /* 分页时跳过的记录数（offset）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  offset = ref(0),
  /* 消息（message）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  message = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 本次请求的序号（ticket）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  ticket = ref("");
/* 刷新（refresh）：调用 refresh 完成刷新处理。 */
async function refresh() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 ticket.value。 */
  ticket.value = "";
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("verified", "");
  /* 把固定数值 0写入 offset.value。 */
  offset.value = 0;
  /* 把固定文字 ''写入 message.value。 */
  message.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 challenge.value。 */
    challenge.value = await api("/captchas", {
      /* 方法（method）字段使用固定状态或编码 POST（POST）。 */
      method: "POST",
      /* 请求正文（body）字段使用表达式 { scene: props.scene, clientNonce: props.nonce } 的结果。 */
      body: { /* scene字段使用表达式 props.scene 的结果。 */ scene: props.scene, /* 客户端Nonce（clientNonce）字段使用表达式 props.nonce 的结果。 */ clientNonce: props.nonce },
    });
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 message.value。 */
    message.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 校验（verify）：调用 verify 完成校验处理。 */
async function verify() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 结果（result）保存等待 调用 api 完成接口处理 完成后取得结果。 */ result = await api("/captcha-verifications", {
      /* 方法（method）字段使用固定状态或编码 POST（POST）。 */
      method: "POST",
      /* 请求正文（body）字段使用表达式 { challengeId: challenge.value.challengeId, clientNonce: props.nonce, offset: Number(offset.value), } 的结果。 */
      body: {
        /* challenge编号（challengeId）字段使用表达式 challenge.value.challengeId 的结果。 */
        challengeId: challenge.value.challengeId,
        /* 客户端Nonce（clientNonce）字段使用表达式 props.nonce 的结果。 */
        clientNonce: props.nonce,
        /* 分页时跳过的记录数（offset）字段使用把输入转换成数值，供比较、计算或按编号查询。 */
        offset: Number(offset.value),
      },
    });
    /* 把表达式 result.captchaTicket 的结果写入 ticket.value。 */
    ticket.value = result.captchaTicket;
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("verified", ticket.value);
    /* 把固定文字 '验证通过'写入 message.value。 */
    message.value = "验证通过";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 message.value。 */
    message.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(refresh);
/* 声明父组件通过引用可以调用的功能。 */
defineExpose({ /* 直接使用已有的刷新（refresh）作为同名字段。 */ refresh });
</script>
<template>
  <!-- 显示一个布局容器；用样式类 captcha 控制外观。 -->
  <div class="captcha">
    <!-- 显示一个布局容器；仅当 challenge 成立时渲染；用样式类 puzzle 控制外观。 -->
    <div v-if="challenge" class="puzzle">
      <!-- 显示图片；src 取自 challenge.background；alt 固定为 拼图背景。 -->
      <img :src="challenge.background" alt="拼图背景" /><!-- 显示图片；用样式类 piece 控制外观；src 取自 challenge.piece；alt 固定为 拼图片；style 取自 { left: (offset / challenge.width) * 100 + '%', top: (challenge.pieceTop / challenge.height) * 100 + '%',…。 --><img
        class="piece"
        :src="challenge.piece"
        alt="拼图片"
        :style="{
          left: (offset / challenge.width) * 100 + '%',
          top: (challenge.pieceTop / challenge.height) * 100 + '%',
          width: (challenge.pieceWidth / challenge.width) * 100 + '%',
        }"
      />
    </div>
    <!-- 显示Element Plus 的 slider 组件；仅当 challenge 成立时渲染；与 offset 双向同步；max 取自 challenge.width - challenge.pieceWidth；disabled 取自 busy || !!ticket。 -->
    <el-slider
      v-if="challenge"
      v-model="offset"
      :max="challenge.width - challenge.pieceWidth"
      :disabled="busy || !!ticket"
    />
    <!-- 显示一个布局容器；用样式类 row 控制外观。 -->
    <div class="row">
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 refresh；loading 取自 busy。 -->
      <el-button @click="refresh" :loading="busy">换一张</el-button
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；启用 plain 属性；click 事件触发时执行 verify；disabled 取自 !challenge || !!ticket；loading 取自 busy。 --><el-button
        type="primary"
        plain
        @click="verify"
        :disabled="!challenge || !!ticket"
        :loading="busy"
        >验证拼图</el-button
      >
    </div>
    <!-- 显示一段文字；仅当 message 成立时渲染；用样式类 hint 控制外观。 -->
    <p v-if="message" class="hint"><!-- 把 message 的当前值转成文字显示。 -->{{ message }}</p>
  </div>
</template>
