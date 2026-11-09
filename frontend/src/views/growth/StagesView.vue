<!-- 渲染阶段列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, getResource, states, canGrow } from "../../api/growth";
/* 引入 ../../components/growth/GrowthSelector.vue 提供的组件、状态函数或请求方法。 */
import GrowthSelector from "../../components/growth/GrowthSelector.vue";
/* 引入 ../../components/growth/GrowthActions.vue 提供的组件、状态函数或请求方法。 */
import GrowthActions from "../../components/growth/GrowthActions.vue";
/* 引入 ../../components/growth/ProgressValue.vue 提供的组件、状态函数或请求方法。 */
import ProgressValue from "../../components/growth/ProgressValue.vue";
const /* 阶段States（stageStates）保存调用 Object.fromEntries 完成来源Entries处理。 */ stageStates = Object.fromEntries(
  Object.entries(states).filter(([code]) => code !== "PAUSED"),
);
const /* 目标（goal）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ goal = ref(""),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* 状态（status）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  status = ref(""),
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
  actions = ref(null),
  /* 已归档（archived）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  archived = ref(false);
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0;
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把一个空列表，用来保存后续多条数据写入 rows.value。 */
  rows.value = [];
  /* 把固定数值 0写入 total.value。 */
  total.value = 0;
  /* 检查条件 !goal.value 是否成立，成立时走这一分支。 */
  if (!goal.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 上级（parent）保存等待 读取 Resource 的值 完成后取得结果。 */ parent = await getResource("goal", goal.value);
    const /* 数据（data）保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ data = await query(`/goals/${goal.value}/stages`, {
      /* 当前页（page）字段使用表达式 page.value 的结果。 */
      page: page.value,
      /* 每页条数（size）字段使用表达式 size.value 的结果。 */
      size: size.value,
      /* keyword字段使用表达式 keyword.value 的结果。 */
      keyword: keyword.value,
      /* 状态（status）字段使用表达式 status.value 的结果。 */
      status: status.value,
    });
    /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
    if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把条件判断的结果：!!parent.archivedAt写入 archived.value。 */
    archived.value = !!parent.archivedAt;
    /* 把表达式 data.items 的结果写入 rows.value。 */
    rows.value = data.items;
    /* 把表达式 data.total 的结果写入 total.value。 */
    total.value = data.total;
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* reset：调用 reset 完成reset处理。 */
function reset() {
  /* 把固定文字 ''写入 keyword.value。 */
  keyword.value = "";
  /* 把固定文字 ''写入 status.value。 */
  status.value = "";
  /* 把固定数值 1写入 page.value。 */
  page.value = 1;
  /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  load();
}
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示成长业务Selector（GrowthSelector）组件；与 goal 双向同步（goal-id）；启用 goal-only 属性；update:goal-id 事件触发时执行 page = 1; $nextTick(load);。 -->
      <GrowthSelector
        v-model:goal-id="goal"
        goal-only
        @update:goal-id="
          page = 1;
          $nextTick(load);
        "
      /><!-- 显示Element Plus 的 input 组件；与 keyword 双向同步；启用 clearable 属性；未输入时提示 阶段名称；keyup 事件触发时执行 page = 1; load();。 --><el-input
        v-model="keyword"
        clearable
        placeholder="阶段名称"
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 select 组件；与 status 双向同步；启用 clearable 属性；未输入时提示 全部状态。 --><el-select v-model="status" clearable placeholder="全部状态"
        ><!-- 显示Element Plus 的 option 组件；按 (label, code) in stageStates 为每条数据重复生成内容；key 取自 code；label 取自 label；value 取自 code。 --><el-option
          v-for="(label, code) in stageStates"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 reset。 --><el-button @click="reset">重置</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:create') 成立时渲染；disabled 取自 !goal || archived；类型设为 primary；启用 plain 属性；click 事件触发时执行 actions.edit('stage', null, goal)。 --><el-button
        v-if="canGrow('stage:stage:create')"
        :disabled="!goal || archived"
        type="primary"
        plain
        @click="actions.edit('stage', null, goal)"
        >新增阶段</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:sort') 成立时渲染；disabled 取自 !goal || archived；click 事件触发时执行 actions.sort('stage', goal)。 --><el-button
        v-if="canGrow('stage:stage:sort')"
        :disabled="!goal || archived"
        @click="actions.sort('stage', goal)"
        >调整顺序</el-button
      >
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 archived 成立时渲染；title 固定为 来源目标已归档，阶段只读；类型设为 info；closable 取自 false。 -->
    <el-alert
      v-if="archived"
      title="来源目标已归档，阶段只读"
      type="info"
      :closable="false"
    /><!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 --><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示应用空状态当前状态（AppEmptyState）组件；仅当 !goal 成立时渲染；description 固定为 请先选择目标，查看其阶段。 --><AppEmptyState
      v-if="!goal"
      description="请先选择目标，查看其阶段"
    /><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 --><template v-else
      ><!-- 显示Element Plus 的 table 组件；data 取自 rows；loading 指令使用 busy；empty-text 固定为 该目标暂无阶段。 --><el-table :data="rows" v-loading="busy" empty-text="该目标暂无阶段"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '该目标暂无阶段'。 --><AppEmptyState
            :description="
              error ? '加载失败，请查看提示后重试。' : '该目标暂无阶段'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；label 固定为 阶段；min-width 固定为 200。 --><el-table-column label="阶段" min-width="200"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('stage:stage:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.details('stage', row)。 --><el-button
              v-if="canGrow('stage:stage:read')"
              link
              type="primary"
              @click="actions.details('stage', row)"
              ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</el-button
            ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</span></template
          ></el-table-column
        ><!-- 显示Element Plus 的 table-column 组件；label 固定为 加权进度；min-width 固定为 200。 --><el-table-column label="加权进度" min-width="200"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示进度值（ProgressValue）组件；value 取自 row.progress。 --><ProgressValue :value="row.progress" /></template></el-table-column
        ><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态。 --><el-table-column label="状态"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template></el-table-column
        ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 startDate；label 固定为 开始日期；min-width 固定为 120。 --><el-table-column
          prop="startDate"
          label="开始日期"
          min-width="120"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 endDate；label 固定为 结束日期；min-width 固定为 120。 --><el-table-column
          prop="endDate"
          label="结束日期"
          min-width="120"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 sortOrder；label 固定为 排序；width 固定为 80。 --><el-table-column
          prop="sortOrder"
          label="排序"
          width="80"
        /><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 220；fixed 固定为 right。 --><el-table-column label="操作" width="220" fixed="right"
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
      ><!-- 显示Element Plus 的 pagination 组件；用样式类 pagination 控制外观；与 page 双向同步（current-page）；与 size 双向同步（page-size）；total 取自 total；page-sizes 取自 [10, 20, 50, 100]；layout 固定为 total,sizes,prev,pager,next；change 事件触发时执行 load。 --><el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total,sizes,prev,pager,next"
        @change="load"
    /></template>
  </section>
  <!-- 显示成长业务Actions（GrowthActions）组件；把组件或元素引用保存为 actions，供脚本读取；changed 事件触发时执行 load。 -->
  <GrowthActions ref="actions" @changed="load" />
</template>
