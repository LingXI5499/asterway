<script setup>
import { ref, reactive, watch } from "vue";
import { useRouter } from "vue-router";
import {
  habitApi,
  habitStates,
  cycleText,
  signalChange,
} from "../../api/personal";
import { checkHabit, cancelHabitToday } from "../../api/habit-actions";
import { canGrow, today } from "../../api/growth";
import { usePaged } from "../../composables/usePaged";
import { ElMessage } from "element-plus";
import { confirmAction, report } from "../../api/ui";
import PagedSelect from "../../components/personal/PagedSelect.vue";
import HabitEditor from "../../components/personal/HabitEditor.vue";
import HabitHistory from "../../components/personal/HabitHistory.vue";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const router = useRouter(),
  mode = ref("TODAY"),
  filters = reactive({ keyword: "", categoryId: "", status: "" });
const editing = ref(false),
  selected = ref(null),
  historyMode = ref("checkins"),
  historySource = ref(null),
  historyOpen = ref(false),
  pending = ref("");
const list = usePaged((params, signal) =>
  mode.value === "TODAY"
    ? habitApi.today(params, signal)
    : habitApi.list(
        { ...params, ...filters, archived: mode.value === "ENDED" },
        signal,
      ),
);
const categories = (params, signal) => habitApi.categories(params, signal);
const idOf = (row) => row.habitId || row.id;
function sealed(row) {
  return row.historyLockedThrough && today() <= row.historyLockedThrough;
}
function edit(row = null) {
  selected.value = row ? { ...row, id: idOf(row) } : null;
  editing.value = true;
}
function history(row, value) {
  historySource.value = { ...row, id: idOf(row) };
  historyMode.value = value;
  historyOpen.value = true;
}
async function act(action, row) {
  if (pending.value) return;
  try {
    if (action === "end")
      await confirmAction(
        "结束并归档「" +
          row.name +
          "」？今日已有打卡和过去规则会保留，之后可重新开启。",
      );
    if (action === "delete")
      await confirmAction(
        "删除「" +
          row.name +
          "」？主体将逻辑删除，打卡和规则历史保留，删除后不能恢复。",
      );
    pending.value = idOf(row);
    if (action === "checkin") await checkHabit(row);
    else if (action === "cancel") await cancelHabitToday(row);
    else if (action === "delete") {
      await habitApi.remove(row);
      ElMessage.success("习惯已删除，历史保留");
    } else {
      await habitApi.control(action, row);
      ElMessage.success("习惯状态已更新");
    }
    signalChange("habit");
    await list.load();
  } catch (e) {
    report(e);
    if (e.status === 409 || e.status === 404) await list.load();
  } finally {
    pending.value = "";
  }
}
watch(mode, () => {
  filters.status = "";
  list.load(true);
});
</script>
<template>
  <section class="card personal-hero">
    <div>
      <span class="eyebrow">HABITS</span>
      <h2>把今天的一次坚持，留在记录里</h2>
      <p class="muted">
        每天或指定星期，一次打卡。规则可以调整，过去的完成事实不会消失。
      </p>
    </div>
    <div class="toolbar">
      <el-button
        v-if="canGrow('habit:category:list')"
        @click="router.push('/habits/categories')"
        >管理分类</el-button
      ><el-button
        v-if="canGrow('habit:habit:create')"
        type="primary"
        @click="edit()"
        >新增习惯</el-button
      >
    </div>
  </section>
  <section class="card">
    <el-tabs v-model="mode"
      ><el-tab-pane label="今日习惯" name="TODAY" /><el-tab-pane
        label="进行与暂停"
        name="CURRENT" /><el-tab-pane label="结束归档" name="ENDED"
    /></el-tabs>
    <div v-if="mode !== 'TODAY'" class="toolbar">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索习惯名称"
        @keyup.enter="list.load(true)"
      />
      <PagedSelect
        v-model="filters.categoryId"
        :loader="categories"
        placeholder="全部分类"
        :disabled="!canGrow('habit:category:list')"
      />
      <el-select
        v-if="mode !== 'ENDED'"
        v-model="filters.status"
        clearable
        placeholder="全部状态"
        ><el-option label="进行中" value="ACTIVE" /><el-option
          label="已暂停"
          value="PAUSED"
      /></el-select>
      <el-button type="primary" @click="list.load(true)">查询</el-button
      ><el-button
        @click="
          Object.assign(filters, { keyword: '', categoryId: '', status: '' });
          list.load(true);
        "
        >重置</el-button
      >
    </div>
    <div v-else class="section-heading">
      <p class="hint">
        只列出今天应做或已有有效打卡的习惯，已完成后暂停或删除仍保留一次完成。
      </p>
      <el-button @click="list.load">刷新今日</el-button>
    </div>
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <el-table
        :data="list.rows"
        v-loading="list.busy"
        :empty-text="
          mode === 'TODAY'
            ? '今天暂无应做习惯，可以先创建一个习惯'
            : '暂无习惯，试试创建或调整筛选条件'
        "
      >
        <el-table-column
          prop="name"
          label="习惯"
          min-width="190"
        /><el-table-column v-if="mode !== 'TODAY'" label="分类" min-width="140"
          ><template #default="{ row }"
            >{{ row.categoryName || "未分类"
            }}<el-tag v-if="row.categoryDeleted" size="small" type="info"
              >已停用</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column v-if="mode !== 'TODAY'" label="周期" min-width="150"
          ><template #default="{ row }">{{
            cycleText(row)
          }}</template></el-table-column
        >
        <el-table-column label="提示时间" width="100"
          ><template #default="{ row }">{{
            row.expectedTime || "未设置"
          }}</template></el-table-column
        >
        <el-table-column label="状态" width="110"
          ><template #default="{ row }"
            ><el-tag
              :type="
                row.status === 'ACTIVE'
                  ? 'success'
                  : row.status === 'PAUSED'
                    ? 'warning'
                    : 'info'
              "
              >{{ habitStates[row.status] }}</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column label="今日" width="100"
          ><template #default="{ row }"
            ><el-tag
              :type="row.checkinId || row.checkedToday ? 'success' : 'info'"
              >{{
                row.checkinId || row.checkedToday ? "已打卡" : "未打卡"
              }}</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column label="操作" min-width="440" fixed="right"
          ><template #default="{ row }">
            <template v-if="mode === 'TODAY'">
              <el-button
                v-if="
                  canGrow('habit:checkin:create') && canGrow('habit:habit:read')
                "
                link
                type="primary"
                :disabled="!!pending || !row.canCheckin"
                @click="act('checkin', row)"
                >打卡</el-button
              >
              <el-button
                v-if="
                  canGrow('habit:checkin:cancel') &&
                  canGrow('habit:checkin:read')
                "
                link
                type="warning"
                :disabled="!!pending || !row.canCancel"
                @click="act('cancel', row)"
                >撤销今日</el-button
              >
            </template>
            <template v-else>
              <el-button
                v-if="canGrow('habit:habit:update')"
                link
                type="primary"
                :disabled="!!pending || row.status === 'ENDED' || !!sealed(row)"
                @click="edit(row)"
                >编辑</el-button
              >
              <el-button
                v-if="row.status === 'ACTIVE' && canGrow('habit:habit:pause')"
                link
                type="warning"
                :disabled="!!pending || !!sealed(row)"
                @click="act('pause', row)"
                >暂停</el-button
              >
              <el-button
                v-if="row.status === 'PAUSED' && canGrow('habit:habit:resume')"
                link
                type="primary"
                :disabled="!!pending || !!sealed(row)"
                @click="act('resume', row)"
                >恢复</el-button
              >
              <el-button
                v-if="row.status !== 'ENDED' && canGrow('habit:habit:end')"
                link
                :disabled="!!pending || !!sealed(row)"
                @click="act('end', row)"
                >结束归档</el-button
              >
              <el-button
                v-if="row.status === 'ENDED' && canGrow('habit:habit:reopen')"
                link
                type="primary"
                :disabled="!!pending || !!sealed(row)"
                @click="act('reopen', row)"
                >重新开启</el-button
              >
              <el-button
                v-if="canGrow('habit:habit:delete')"
                link
                type="danger"
                :disabled="!!pending || !!sealed(row)"
                @click="act('delete', row)"
                >删除</el-button
              >
            </template>
            <el-button
              v-if="canGrow('habit:checkin:read')"
              link
              type="primary"
              @click="history(row, 'checkins')"
              >打卡历史</el-button
            >
            <el-button
              v-if="canGrow('habit:habit:history')"
              link
              type="primary"
              @click="history(row, 'revisions')"
              >规则历史</el-button
            >
          </template></el-table-column
        >
      </el-table>
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <HabitEditor v-model="editing" :source="selected" @saved="list.load" />
  <HabitHistory
    v-model="historyOpen"
    :source="historySource"
    :mode="historyMode"
    @changed="list.load"
  />
</template>
