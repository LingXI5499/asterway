<script setup>
import { ref, computed, onMounted, defineAsyncComponent } from "vue";
import { useRoute, useRouter } from "vue-router";
import { query, getResource, states, canGrow } from "../../api/growth";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
import ProgressValue from "../../components/growth/ProgressValue.vue";
import GrowthEvidence from "../../components/personal/GrowthEvidence.vue";
const ProgressRing = defineAsyncComponent(
  () => import("../../components/growth/ProgressRing.vue"),
);
const props = defineProps({ kind: String });
const route = useRoute(),
  router = useRouter(),
  record = ref(null),
  parentGoal = ref(null),
  rows = ref([]),
  page = ref(1),
  size = ref(20),
  total = ref(0),
  busy = ref(false),
  error = ref(""),
  actions = ref(null);
const childKind = computed(() => (props.kind === "goal" ? "stage" : "task")),
  archived = computed(
    () =>
      !!(props.kind === "goal"
        ? record.value?.archivedAt
        : parentGoal.value?.archivedAt),
  );
async function load() {
  busy.value = true;
  error.value = "";
  try {
    record.value = await getResource(props.kind, route.params.id);
    parentGoal.value =
      props.kind === "goal"
        ? record.value
        : await getResource("goal", record.value.goalId);
    if (canGrow(`${childKind.value}:${childKind.value}:list`)) {
      const d = await query(
        props.kind === "goal" ? `/goals/${record.value.id}/stages` : "/tasks",
        {
          stageId: props.kind === "stage" ? record.value.id : null,
          page: page.value,
          size: size.value,
        },
      );
      rows.value = d.items;
      total.value = d.total;
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <el-alert
    v-if="error"
    :title="error"
    type="error"
    :closable="false"
  /><el-skeleton v-if="busy && !record" :rows="8" /><template v-if="record"
    ><section class="card detail-overview">
      <div class="detail-copy">
        <div class="toolbar">
          <el-button
            text
            @click="
              router.push(
                kind === 'goal'
                  ? '/growth/goals'
                  : `/growth/goals/${record.goalId}`,
              )
            "
            >{{ kind === "goal" ? "返回目标管理" : "返回所属目标" }}</el-button
          ><el-tag>{{ states[record.status] }}</el-tag
          ><el-tag v-if="archived" type="info">已归档，只读</el-tag>
        </div>
        <h2>{{ record.name }}</h2>
        <p class="preserve-text">{{ record.description || "尚未填写描述" }}</p>
        <div class="criteria">
          <strong>完成条件</strong>
          <p class="preserve-text">
            {{ record.completionCriteria || "尚未填写完成条件" }}
          </p>
        </div>
        <p class="hint">
          开始：{{ record.startDate || "未指定" }}　结束：{{
            record.expectedEndDate || record.endDate || "未指定"
          }}
        </p>
        <div class="toolbar">
          <el-button
            v-if="canGrow(`${kind}:${kind}:update`)"
            :disabled="archived"
            type="primary"
            plain
            @click="actions.edit(kind, record)"
            >编辑{{ kind === "goal" ? "目标" : "阶段" }}</el-button
          ><el-button
            v-if="canGrow(`${kind}:${kind}:status`)"
            :disabled="archived"
            @click="actions.status(kind, record)"
            >修改状态</el-button
          ><el-button
            v-if="kind === 'goal' && !archived && canGrow('goal:goal:archive')"
            type="warning"
            plain
            @click="actions.archive(record)"
            >归档目标</el-button
          ><el-button
            v-if="kind === 'goal' && archived && canGrow('goal:goal:restore')"
            @click="actions.restore(record)"
            >恢复目标</el-button
          >
        </div>
      </div>
      <ProgressRing :value="record.progress" />
    </section>
    <section class="card">
      <div class="section-heading">
        <h2>{{ kind === "goal" ? "目标阶段" : "阶段任务" }}</h2>
        <div class="toolbar">
          <el-button
            v-if="canGrow(`${childKind}:${childKind}:create`)"
            :disabled="archived"
            type="primary"
            @click="actions.edit(childKind, null, record.id)"
            >新增{{ kind === "goal" ? "阶段" : "任务" }}</el-button
          ><el-button
            v-if="canGrow(`${childKind}:${childKind}:sort`)"
            :disabled="archived"
            @click="actions.sort(childKind, record.id)"
            >调整顺序</el-button
          >
        </div>
      </div>
      <el-alert
        v-if="archived"
        title="请先恢复目标，再修改其阶段、任务或计划安排"
        type="info"
        :closable="false"
      /><el-empty
        v-if="!canGrow(`${childKind}:${childKind}:list`)"
        description="当前未授予子项列表权限"
      /><template v-else
        ><el-table v-if="kind === 'goal'" :data="rows" v-loading="busy"
          ><el-table-column label="阶段" min-width="180"
            ><template #default="{ row }"
              ><el-button
                v-if="canGrow('stage:stage:read')"
                link
                type="primary"
                @click="actions.details('stage', row)"
                >{{ row.name }}</el-button
              ><span v-else>{{ row.name }}</span></template
            ></el-table-column
          ><el-table-column label="进度" min-width="180"
            ><template #default="{ row }"
              ><ProgressValue
                :value="row.progress" /></template></el-table-column
          ><el-table-column label="状态"
            ><template #default="{ row }">{{
              states[row.status]
            }}</template></el-table-column
          ><el-table-column label="操作" width="210"
            ><template #default="{ row }"
              ><el-button
                v-if="canGrow('stage:stage:update')"
                :disabled="archived"
                link
                type="primary"
                @click="actions.edit('stage', row)"
                >编辑</el-button
              ><el-button
                v-if="canGrow('stage:stage:status')"
                :disabled="archived"
                link
                type="primary"
                @click="actions.status('stage', row)"
                >状态</el-button
              ><el-button
                v-if="canGrow('stage:stage:delete')"
                :disabled="archived"
                link
                type="danger"
                @click="actions.remove('stage', row)"
                >删除</el-button
              ></template
            ></el-table-column
          ></el-table
        ><GrowthTaskTable
          v-else
          :rows="
            rows.map((r) => ({ ...r, archivedAt: parentGoal?.archivedAt }))
          "
          :loading="busy"
          @detail="(r) => actions.details('task', r)"
          @edit="(r) => actions.edit('task', r)"
          @status="(r) => actions.status('task', r)"
          @remove="(r) => actions.remove('task', r)"
          @assign="(r) => actions.assign(r)"
          @unassign="(r) => actions.unassign(r)"
          @history="(r) => actions.history(r)" /><el-pagination
          class="pagination"
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total,sizes,prev,pager,next"
          @change="load"
      /></template>
    </section>
    <GrowthEvidence :kind="kind" :id="record.id" /></template
  ><GrowthActions ref="actions" @changed="load" />
</template>
