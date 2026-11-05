<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from "vue";
import {
  habitApi,
  instantText,
  cycleText,
  actions,
  signalChange,
} from "../../api/personal";
import { today, canGrow } from "../../api/growth";
import { confirmAction, report } from "../../api/ui";
import { ElMessage } from "element-plus";
import PagePagination from "./PagePagination.vue";
import LoadState from "./LoadState.vue";
const props = defineProps({
  modelValue: Boolean,
  source: Object,
  mode: String,
});
const emit = defineEmits(["update:modelValue", "changed"]);
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
const list = reactive({ rows: [], total: 0, page: 1, size: 20 }),
  dates = ref([]),
  cancelled = ref(null),
  busy = ref(false),
  error = ref(""),
  pending = ref("");
let sequence = 0,
  controller;
const id = computed(() => props.source?.habitId || props.source?.id);
async function load(reset = false) {
  if (!id.value || !props.modelValue) return;
  if (reset === true) list.page = 1;
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = "";
  try {
    const values = dates.value || [];
    if (values.length && (values.length !== 2 || values[0] > values[1]))
      throw new Error("请选择顺序正确的日期区间");
    const data =
      props.mode === "revisions"
        ? await habitApi.revisions(
            id.value,
            { page: list.page, size: list.size },
            controller.signal,
          )
        : await habitApi.checkins(
            id.value,
            {
              page: list.page,
              size: list.size,
              dateFrom: values[0],
              dateTo: values[1],
              cancelled: cancelled.value,
            },
            controller.signal,
          );
    if (seq === sequence)
      Object.assign(list, { rows: data.items, total: data.total });
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
async function cancel(row) {
  try {
    await confirmAction(
      "撤销今天这次打卡？原始记录仍保留，今日应做按最新规则重新判断。",
    );
    pending.value = row.id;
    await habitApi.cancel(id.value, row);
    ElMessage.success("今日打卡已撤销");
    signalChange("habit");
    emit("changed");
    await load();
  } catch (e) {
    report(e);
    if (e.status === 409) await load();
  } finally {
    pending.value = "";
  }
}
watch(
  () => [props.modelValue, id.value, props.mode],
  () => {
    sequence++;
    list.rows = [];
    list.page = 1;
    dates.value = [];
    cancelled.value = null;
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
    :title="
      (source?.name || '习惯') +
      (mode === 'revisions' ? ' · 规则与生命周期' : ' · 打卡历史')
    "
    size="min(960px, 100vw)"
    destroy-on-close
  >
    <div v-if="mode !== 'revisions'" class="toolbar">
      <el-date-picker
        v-model="dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="起始日期"
        end-placeholder="结束日期"
      />
      <el-select v-model="cancelled" clearable placeholder="全部记录"
        ><el-option label="有效打卡" :value="false" /><el-option
          label="已撤销"
          :value="true"
      /></el-select>
      <el-button @click="load(true)">查询</el-button
      ><el-button
        @click="
          dates = [];
          cancelled = null;
          load(true);
        "
        >重置</el-button
      >
    </div>
    <LoadState
      :loading="busy && !list.rows.length"
      :error="error"
      @retry="load"
    >
      <el-table
        v-if="mode !== 'revisions'"
        :data="list.rows"
        v-loading="busy"
        empty-text="还没有打卡记录"
      >
        <el-table-column
          prop="businessDate"
          label="原始打卡日"
          min-width="130"
        /><el-table-column
          prop="nameSnapshot"
          label="当时习惯名称"
          min-width="150"
        />
        <el-table-column label="打卡时间" min-width="180"
          ><template #default="{ row }">{{
            instantText(row.checkedAt, row.timeZone)
          }}</template></el-table-column
        >
        <el-table-column label="状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="row.cancelledAt ? 'info' : 'success'">{{
              row.cancelledAt ? "已撤销" : "有效"
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="撤销时间" min-width="180"
          ><template #default="{ row }">{{
            instantText(row.cancelledAt, row.timeZone)
          }}</template></el-table-column
        >
        <el-table-column prop="timeZone" label="打卡时区" min-width="140" />
        <el-table-column label="操作" width="100" fixed="right"
          ><template #default="{ row }">
            <el-button
              v-if="
                !row.cancelledAt &&
                row.businessDate === today() &&
                !source?.deletedAt &&
                source?.canCancel !== false &&
                canGrow('habit:checkin:cancel')
              "
              link
              type="warning"
              :disabled="
                !!pending ||
                (source?.historyLockedThrough &&
                  today() <= source.historyLockedThrough)
              "
              @click="cancel(row)"
              >撤销</el-button
            >
          </template></el-table-column
        >
      </el-table>
      <el-table
        v-else
        :data="list.rows"
        v-loading="busy"
        empty-text="还没有规则修订"
      >
        <el-table-column type="expand"
          ><template #default="{ row }"
            ><div class="history-expanded">
              <strong>完成标准</strong>
              <p class="preserve-text">
                {{ row.completionCriteria || "未填写" }}
              </p>
              <strong>描述</strong>
              <p class="preserve-text">{{ row.description || "未填写" }}</p>
              <p class="hint">
                分类归属：{{
                  row.categoryId ? "分类 " + row.categoryId : "未分类"
                }}
                · 提示时间 {{ row.expectedTime || "未设置" }} · 开始日期
                {{ row.startDate }}
              </p>
            </div></template
          ></el-table-column
        >
        <el-table-column
          prop="revisionNo"
          label="修订"
          width="80"
        /><el-table-column label="动作" min-width="160"
          ><template #default="{ row }">{{
            actions[row.actionType] || "规则变化"
          }}</template></el-table-column
        >
        <el-table-column
          prop="name"
          label="名称快照"
          min-width="140"
        /><el-table-column label="当时周期" min-width="150"
          ><template #default="{ row }">{{
            cycleText(row)
          }}</template></el-table-column
        >
        <el-table-column
          prop="effectiveDate"
          label="生效日期"
          min-width="120"
        /><el-table-column label="发生时间" min-width="180"
          ><template #default="{ row }">{{
            instantText(row.changedAt, row.timeZone)
          }}</template></el-table-column
        >
      </el-table>
      <PagePagination :state="list" @change="load" />
    </LoadState>
  </el-drawer>
</template>
