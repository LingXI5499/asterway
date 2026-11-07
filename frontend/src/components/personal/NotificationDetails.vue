<script setup>
import { ref, computed, watch, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import {
  notificationApi,
  handleStates,
  instantText,
  signalChange,
} from "../../api/personal";
import { canGrow } from "../../api/growth";
import { refreshUnread } from "../../stores/personal";
import { confirmAction, report } from "../../api/ui";
import { ElMessage } from "element-plus";
import LoadState from "./LoadState.vue";
const props = defineProps({ modelValue: Boolean, id: [String, Number] });
const emit = defineEmits(["update:modelValue", "changed"]);
const router = useRouter(),
  row = ref(null),
  busy = ref(false),
  pending = ref(""),
  error = ref("");
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
let sequence = 0,
  controller;
async function load() {
  if (!props.modelValue || !props.id) return;
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = "";
  try {
    const data = await notificationApi.detail(props.id, controller.signal);
    if (seq === sequence) row.value = data;
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
async function act(action) {
  if (pending.value) return;
  const expected = { ...row.value };
  try {
    if (action === "confirm")
      await confirmAction(
        "将该目标确认完成？会再次检查当前达标条件，已读状态和目标完成分别保存。",
        "确认目标完成",
      );
    if (action === "continue")
      await confirmAction(
        "继续保持目标当前状态？本轮不再重复提醒，之后回落再达标会产生新提醒。",
        "继续推进目标",
      );
    pending.value = action;
    error.value = "";
    row.value =
      action === "read"
        ? await notificationApi.markRead(expected)
        : await notificationApi.handle(action, expected);
    ElMessage.success(
      action === "read"
        ? "消息已标记为已读"
        : action === "confirm"
          ? "目标已确认完成"
          : "已选择继续保持",
    );
    signalChange(action === "read" ? "notification" : "goal");
    emit("changed");
    await refreshUnread();
  } catch (e) {
    if (["cancel", "close"].includes(e)) return;
    if (e.status === 409) {
      await load();
      error.value = e.message + "，已读取最新状态";
      emit("changed");
    } else error.value = e.message || "操作失败，请重试";
  } finally {
    pending.value = "";
  }
}
watch(
  () => [props.modelValue, props.id],
  () => {
    sequence++;
    row.value = null;
    if (props.modelValue) load();
    else controller?.abort();
  },
  { immediate: true },
);
onBeforeUnmount(() => {
  sequence++;
  controller?.abort();
});
</script>
<template>
  <el-drawer
    v-model="visible"
    title="站内消息"
    size="min(720px, 100vw)"
    destroy-on-close
    :before-close="
      (done) => {
        if (!pending) done();
      }
    "
  >
    <LoadState :loading="busy && !row" :error="error" @retry="load">
      <template v-if="row">
        <h2>{{ row.title }}</h2>
        <div class="toolbar">
          <el-tag
            :type="
              row.handleStatus === 'PENDING'
                ? 'warning'
                : row.handleStatus === 'CONFIRMED'
                  ? 'success'
                  : 'info'
            "
            >{{ handleStates[row.handleStatus] }}</el-tag
          ><el-tag :type="row.readAt ? 'info' : 'primary'">{{
            row.readAt ? "已读" : "未读"
          }}</el-tag>
        </div>
        <p class="preserve-text notice-content">{{ row.content }}</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="发送时间">{{
            instantText(row.createdAt)
          }}</el-descriptions-item>
          <el-descriptions-item label="首次已读">{{
            instantText(row.readAt)
          }}</el-descriptions-item>
          <el-descriptions-item label="业务处理">{{
            instantText(row.handledAt)
          }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="!row.canHandle && row.handleStatus === 'PENDING'"
          class="detail-toolbar"
          title="来源状态或达标轮次已变化，当前不能确认旧提醒，请重新读取状态"
          type="info"
          :closable="false"
        />
        <el-alert
          v-if="row.handleStatus === 'INVALIDATED'"
          class="detail-toolbar"
          title="这条提醒已失效，保留消息历史，不再计入未读数"
          type="info"
          :closable="false"
        />
        <div class="toolbar detail-toolbar">
          <el-button
            v-if="!row.readAt && canGrow('notification:message:mark_read')"
            :disabled="!!pending"
            @click="act('read')"
            >标为已读</el-button
          >
          <el-button
            v-if="canGrow('notification:message:handle')"
            type="primary"
            :disabled="!row.canHandle || !!pending"
            :loading="pending === 'confirm'"
            @click="act('confirm')"
            >确认目标完成</el-button
          >
          <el-button
            v-if="canGrow('notification:message:handle')"
            :disabled="!row.canHandle || !!pending"
            :loading="pending === 'continue'"
            @click="act('continue')"
            >继续保持</el-button
          >
          <el-button
            v-if="canGrow('goal:goal:read')"
            text
            @click="
              visible = false;
              router.push('/growth/goals/' + row.goalId);
            "
            >查看来源目标</el-button
          >
        </div>
        <p class="hint">
          标记已读不会完成目标；确认完成或继续保持也不会伪造已读时间。
        </p>
      </template>
    </LoadState>
  </el-drawer>
</template>
