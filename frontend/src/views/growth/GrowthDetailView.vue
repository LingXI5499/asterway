<!-- 渲染成长业务详情页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, computed, onMounted, defineAsyncComponent } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute, useRouter } from "vue-router";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, getResource, states, canGrow } from "../../api/growth";
/* 引入 ../../components/growth/GrowthActions.vue 提供的组件、状态函数或请求方法。 */
import GrowthActions from "../../components/growth/GrowthActions.vue";
/* 引入 ../../components/growth/GrowthTaskTable.vue 提供的组件、状态函数或请求方法。 */
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
/* 引入 ../../components/growth/ProgressValue.vue 提供的组件、状态函数或请求方法。 */
import ProgressValue from "../../components/growth/ProgressValue.vue";
/* 引入 ../../components/personal/GrowthEvidence.vue 提供的组件、状态函数或请求方法。 */
import GrowthEvidence from "../../components/personal/GrowthEvidence.vue";
const /* 进度Ring（ProgressRing）保存调用 defineAsyncComponent 完成define异步Component处理。 */ ProgressRing = defineAsyncComponent(
  () => import("../../components/growth/ProgressRing.vue"),
);
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 种类（kind）字段使用可为空的文字（String）。 */ kind: String });
const /* route保存取得当前页面路径和查询参数。 */ route = useRoute(),
  /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */
  router = useRouter(),
  /* 记录（record）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  record = ref(null),
  /* 上级目标（parentGoal）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  parentGoal = ref(null),
  /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  rows = ref([]),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 每页条数（size）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  size = ref(20),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* actions保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  actions = ref(null);
const /* 下级种类（childKind）保存随相关状态变化自动重算的值。 */ childKind = computed(() => (props.kind === "goal" ? "stage" : "task")),
  /* 已归档（archived）保存随相关状态变化自动重算的值。 */
  archived = computed(
    () =>
      !!(props.kind === "goal"
        ? record.value?.archivedAt
        : parentGoal.value?.archivedAt),
  );
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 读取 Resource 的值 完成后取得结果写入 record.value。 */
    record.value = await getResource(props.kind, route.params.id);
    /* 把根据前面的条件，从两个候选结果中选择一个：props.kind === "goal" ? record.value : await getResource("goal", record.value.goalId)写入 parentGoal.value。 */
    parentGoal.value =
      props.kind === "goal"
        ? record.value
        : await getResource("goal", record.value.goalId);
    /* 检查当前用户是否具有需要的权限：canGrow(`${childKind.value}:${childKind.value}:list`)，成立时走这一分支。 */
    if (canGrow(`${childKind.value}:${childKind.value}:list`)) {
      const /* d保存等待 根据前面的条件，从两个候选结果中选择一个：query( props.kind === "goal" ? `/goals/${record.value.id}/stages` : "/tasks", { stageId: props.kind … 完成后取得结果。 */ d = await query(
        props.kind === "goal" ? `/goals/${record.value.id}/stages` : "/tasks",
        {
          /* 阶段编号（stageId）字段使用根据前面的条件，从两个候选结果中选择一个：props.kind === "stage" ? record.value.id : null。 */
          stageId: props.kind === "stage" ? record.value.id : null,
          /* 当前页（page）字段使用表达式 page.value 的结果。 */
          page: page.value,
          /* 每页条数（size）字段使用表达式 size.value 的结果。 */
          size: size.value,
        },
      );
      /* 把表达式 d.items 的结果写入 rows.value。 */
      rows.value = d.items;
      /* 把表达式 d.total 的结果写入 total.value。 */
      total.value = d.total;
    }
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
  <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
  <el-alert
    v-if="error"
    :title="error"
    type="error"
    :closable="false"
  /><!-- 显示Element Plus 的 skeleton 组件；仅当 busy && !record 成立时渲染；rows 取自 8。 --><el-skeleton v-if="busy && !record" :rows="8" /><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 record 成立时渲染。 --><template v-if="record"
    ><!-- 显示一个有独立含义的内容区；用样式类 card detail-overview 控制外观。 --><section class="card detail-overview">
      <!-- 显示一个布局容器；用样式类 detail-copy 控制外观。 -->
      <div class="detail-copy">
        <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
        <div class="toolbar">
          <!-- 显示Element Plus 的 button 组件；启用 text 属性；click 事件触发时执行 router.push( kind === 'goal' ? '/growth/goals' : `/growth/goals/${record.goalId}`, )。 -->
          <el-button
            text
            @click="
              router.push(
                kind === 'goal'
                  ? '/growth/goals'
                  : `/growth/goals/${record.goalId}`,
              )
            "
            ><!-- 把 kind === "goal" ? "返回目标管理" : "返回所属目标" 的当前值转成文字显示。 -->{{ kind === "goal" ? "返回目标管理" : "返回所属目标" }}</el-button
          ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 record.status。 --><AppStatusTag :value="record.status" /><!-- 显示Element Plus 的 tag 组件；仅当 archived 成立时渲染；类型设为 info。 --><el-tag
            v-if="archived"
            type="info"
            >已归档，只读</el-tag
          >
        </div>
        <!-- 显示二级标题。 -->
        <h2><!-- 把 record.name 的当前值转成文字显示。 -->{{ record.name }}</h2>
        <!-- 显示一段文字；用样式类 preserve-text 控制外观。 -->
        <p class="preserve-text"><!-- 把 record.description || "尚未填写描述" 的当前值转成文字显示。 -->{{ record.description || "尚未填写描述" }}</p>
        <!-- 显示一个布局容器；用样式类 criteria 控制外观。 -->
        <div class="criteria">
          <!-- 显示重点文字。 -->
          <strong>完成条件</strong>
          <!-- 显示一段文字；用样式类 preserve-text 控制外观。 -->
          <p class="preserve-text">
            <!-- 把 record.completionCriteria || "尚未填写完成条件" 的当前值转成文字显示。 -->
            {{ record.completionCriteria || "尚未填写完成条件" }}
          </p>
        </div>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">
          开始：<!-- 把 record.startDate || "未指定" 的当前值转成文字显示。 -->{{ record.startDate || "未指定" }}　结束：<!-- 把 record.expectedEndDate || record.endDate || "未指定" 的当前值转成文字显示。 -->{{
            record.expectedEndDate || record.endDate || "未指定"
          }}
        </p>
        <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
        <div class="toolbar">
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow(`${kind}:${kind}:update`) 成立时渲染；disabled 取自 archived；类型设为 primary；启用 plain 属性；click 事件触发时执行 actions.edit(kind, record)。 -->
          <el-button
            v-if="canGrow(`${kind}:${kind}:update`)"
            :disabled="archived"
            type="primary"
            plain
            @click="actions.edit(kind, record)"
            >编辑<!-- 把 kind === "goal" ? "目标" : "阶段" 的当前值转成文字显示。 -->{{ kind === "goal" ? "目标" : "阶段" }}</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 canGrow(`${kind}:${kind}:status`) 成立时渲染；disabled 取自 archived；click 事件触发时执行 actions.status(kind, record)。 --><el-button
            v-if="canGrow(`${kind}:${kind}:status`)"
            :disabled="archived"
            @click="actions.status(kind, record)"
            >修改状态</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 kind === 'goal' && !archived && canGrow('goal:goal:archive') 成立时渲染；类型设为 warning；启用 plain 属性；click 事件触发时执行 actions.archive(record)。 --><el-button
            v-if="kind === 'goal' && !archived && canGrow('goal:goal:archive')"
            type="warning"
            plain
            @click="actions.archive(record)"
            >归档目标</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 kind === 'goal' && archived && canGrow('goal:goal:restore') 成立时渲染；click 事件触发时执行 actions.restore(record)。 --><el-button
            v-if="kind === 'goal' && archived && canGrow('goal:goal:restore')"
            @click="actions.restore(record)"
            >恢复目标</el-button
          >
        </div>
      </div>
      <!-- 显示进度Ring（ProgressRing）组件；value 取自 record.progress。 -->
      <ProgressRing :value="record.progress" />
    </section>
    <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
    <section class="card">
      <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
      <div class="section-heading">
        <!-- 显示二级标题。 -->
        <h2><!-- 把 kind === "goal" ? "目标阶段" : "阶段任务" 的当前值转成文字显示。 -->{{ kind === "goal" ? "目标阶段" : "阶段任务" }}</h2>
        <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
        <div class="toolbar">
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow(`${childKind}:${childKind}:create`) 成立时渲染；disabled 取自 archived；类型设为 primary；click 事件触发时执行 actions.edit(childKind, null, record.id)。 -->
          <el-button
            v-if="canGrow(`${childKind}:${childKind}:create`)"
            :disabled="archived"
            type="primary"
            @click="actions.edit(childKind, null, record.id)"
            >新增<!-- 把 kind === "goal" ? "阶段" : "任务" 的当前值转成文字显示。 -->{{ kind === "goal" ? "阶段" : "任务" }}</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 canGrow(`${childKind}:${childKind}:sort`) 成立时渲染；disabled 取自 archived；click 事件触发时执行 actions.sort(childKind, record.id)。 --><el-button
            v-if="canGrow(`${childKind}:${childKind}:sort`)"
            :disabled="archived"
            @click="actions.sort(childKind, record.id)"
            >调整顺序</el-button
          >
        </div>
      </div>
      <!-- 显示Element Plus 的 alert 组件；仅当 archived 成立时渲染；title 固定为 请先恢复目标，再修改其阶段、任务或计划安排；类型设为 info；closable 取自 false。 -->
      <el-alert
        v-if="archived"
        title="请先恢复目标，再修改其阶段、任务或计划安排"
        type="info"
        :closable="false"
      /><!-- 显示应用空状态当前状态（AppEmptyState）组件；仅当 !canGrow(`${childKind}:${childKind}:list`) 成立时渲染；description 固定为 当前未授予子项列表权限。 --><AppEmptyState
        v-if="!canGrow(`${childKind}:${childKind}:list`)"
        description="当前未授予子项列表权限"
      /><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 --><template v-else
        ><!-- 显示Element Plus 的 table 组件；仅当 kind === 'goal' 成立时渲染；data 取自 rows；loading 指令使用 busy。 --><el-table v-if="kind === 'goal'" :data="rows" v-loading="busy"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
            ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无记录，可调整筛选条件或从第一条记录开始。'。 --><AppEmptyState
              :description="
                error
                  ? '加载失败，请查看提示后重试。'
                  : '暂无记录，可调整筛选条件或从第一条记录开始。'
              " /></template
          ><!-- 显示Element Plus 的 table-column 组件；label 固定为 阶段；min-width 固定为 180。 --><el-table-column label="阶段" min-width="180"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.details('stage', row)。 --><el-button
                v-if="canGrow('stage:stage:read')"
                link
                type="primary"
                @click="actions.details('stage', row)"
                ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</el-button
              ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</span></template
            ></el-table-column
          ><!-- 显示Element Plus 的 table-column 组件；label 固定为 进度；min-width 固定为 180。 --><el-table-column label="进度" min-width="180"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示进度值（ProgressValue）组件；value 取自 row.progress。 --><ProgressValue
                :value="row.progress" /></template></el-table-column
          ><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态。 --><el-table-column label="状态"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template></el-table-column
          ><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 210。 --><el-table-column label="操作" width="210"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:update') 成立时渲染；disabled 取自 archived；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.edit('stage', row)。 --><el-button
                v-if="canGrow('stage:stage:update')"
                :disabled="archived"
                link
                type="primary"
                @click="actions.edit('stage', row)"
                >编辑</el-button
              ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:status') 成立时渲染；disabled 取自 archived；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.status('stage', row)。 --><el-button
                v-if="canGrow('stage:stage:status')"
                :disabled="archived"
                link
                type="primary"
                @click="actions.status('stage', row)"
                >状态</el-button
              ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:delete') 成立时渲染；disabled 取自 archived；启用 link 属性；类型设为 danger；click 事件触发时执行 actions.remove('stage', row)。 --><el-button
                v-if="canGrow('stage:stage:delete')"
                :disabled="archived"
                link
                type="danger"
                @click="actions.remove('stage', row)"
                >删除</el-button
              ></template
            ></el-table-column
          ></el-table
        ><!-- 显示成长业务任务表（GrowthTaskTable）组件；前面条件均不成立时显示这个备用分支；rows 取自 rows.map((r) => ({ ...r, archivedAt: parentGoal?.archivedAt }))；loading 取自 busy；detail 事件触发时执行 (r) => actions.details('task', r)；edit 事件触发时执行 (r) => actions.edit('task', r)；status 事件触发时执行 (r) => actions.status('task', r)；remove 事件触发时执行 (r) => actions.remove('task', r)；assign 事件触发时执行 (r) => actions.assign(r)；unassign 事件触发时执行 (r) => actions.unassign(r)；history 事件触发时执行 (r) => actions.history(r)。 --><GrowthTaskTable
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
          @history="(r) => actions.history(r)" /><!-- 显示Element Plus 的 pagination 组件；用样式类 pagination 控制外观；与 page 双向同步（current-page）；与 size 双向同步（page-size）；total 取自 total；page-sizes 取自 [10, 20, 50, 100]；layout 固定为 total,sizes,prev,pager,next；change 事件触发时执行 load。 --><el-pagination
          class="pagination"
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total,sizes,prev,pager,next"
          @change="load"
      /></template>
    </section>
    <!-- 显示成长业务证明信息（GrowthEvidence）组件；kind 取自 kind；id 取自 record.id。 -->
    <GrowthEvidence :kind="kind" :id="record.id" /></template
  ><!-- 显示成长业务Actions（GrowthActions）组件；把组件或元素引用保存为 actions，供脚本读取；changed 事件触发时执行 load。 --><GrowthActions ref="actions" @changed="load" />
</template>
