<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api/http";
const props = defineProps({
  scene: { type: String, required: true },
  nonce: { type: String, required: true },
});
const emit = defineEmits(["verified"]);
const challenge = ref(null),
  offset = ref(0),
  message = ref(""),
  busy = ref(false),
  ticket = ref("");
async function refresh() {
  busy.value = true;
  ticket.value = "";
  emit("verified", "");
  offset.value = 0;
  message.value = "";
  try {
    challenge.value = await api("/captchas", {
      method: "POST",
      body: { scene: props.scene, clientNonce: props.nonce },
    });
  } catch (e) {
    message.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function verify() {
  busy.value = true;
  try {
    const result = await api("/captcha-verifications", {
      method: "POST",
      body: {
        challengeId: challenge.value.challengeId,
        clientNonce: props.nonce,
        offset: Number(offset.value),
      },
    });
    ticket.value = result.captchaTicket;
    emit("verified", ticket.value);
    message.value = "验证通过";
  } catch (e) {
    message.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(refresh);
defineExpose({ refresh });
</script>
<template>
  <div class="captcha">
    <div v-if="challenge" class="puzzle">
      <img :src="challenge.background" alt="拼图背景" /><img
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
    <el-slider
      v-if="challenge"
      v-model="offset"
      :max="challenge.width - challenge.pieceWidth"
      :disabled="busy || !!ticket"
    />
    <div class="row">
      <el-button @click="refresh" :loading="busy">换一张</el-button
      ><el-button
        type="primary"
        plain
        @click="verify"
        :disabled="!challenge || !!ticket"
        :loading="busy"
        >验证拼图</el-button
      >
    </div>
    <p v-if="message" class="hint">{{ message }}</p>
  </div>
</template>
