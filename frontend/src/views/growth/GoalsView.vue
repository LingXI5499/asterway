<!-- 渲染目标列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, states, canGrow } from "../../api/growth";
/* 引入 ../../components/growth/GrowthActions.vue 提供的组件、状态函数或请求方法。 */
import GrowthActions from "../../components/growth/GrowthActions.vue";
/* 引入 ../../components/growth/ProgressValue.vue 提供的组件、状态函数或请求方法。 */
import ProgressValue from "../../components/growth/ProgressValue.vue";
const /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ rows = ref([]),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 每页条数（size）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  size = ref(20),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* mode保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  mode = ref("CURRENT"),
  /* density保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  density = ref("cards"),
  /* 区间（range）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  range = ref([]),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* actions保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  actions = ref(null);
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0;
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 根据前面的条件，从两个候选结果中选择一个：query("/goals", { page: page.value, size: size.value, keyword: keyword.value, archived: mode.value =… 完成后取得结果。 */ data = await query("/goals", {
      /* 当前页（page）字段使用表达式 page.value 的结果。 */
      page: page.value,
      /* 每页条数（size）字段使用表达式 size.value 的结果。 */
      size: size.value,
      /* keyword字段使用表达式 keyword.value 的结果。 */
      keyword: keyword.value,
      /* 已归档（archived）字段使用条件判断的结果：mode.value === "ARCHIVED"。 */
      archived: mode.value === "ARCHIVED",
      /* timing字段使用根据前面的条件，从两个候选结果中选择一个：["CURRENT", "FUTURE"].includes(mode.value) ? mode.value : "ALL"。 */
      timing: ["CURRENT", "FUTURE"].includes(mode.value) ? mode.value : "ALL",
      /* 状态（status）字段使用根据前面的条件，从两个候选结果中选择一个：mode.value === "COMPLETED" ? "COMPLETED" : null。 */
      status: mode.value === "COMPLETED" ? "COMPLETED" : null,
      /* 开始来源（startFrom）字段使用沿 range.value?.[0] 读取数据；中间对象为空时返回空值。 */
      startFrom: range.value?.[0],
      /* 开始To（startTo）字段使用沿 range.value?.[1] 读取数据；中间对象为空时返回空值。 */
      startTo: range.value?.[1],
    });
    /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
    if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把表达式 data.items 的结果写入 rows.value。 */
    rows.value = data.items;
    /* 把表达式 data.total 的结果写入 total.value。 */
    total.value = data.total;
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
  /* 把一个空列表，用来保存后续多条数据写入 range.value。 */
  range.value = [];
  /* 把固定数值 1写入 page.value。 */
  page.value = 1;
  /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  load();
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card growth-hero 控制外观。 -->
  <section class="card growth-hero">
    <!-- 显示一个布局容器。 -->
    <div>
      <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
      <span class="eyebrow">GROWTH · GOALS</span>
      <!-- 显示二级标题。 -->
      <h2>把想做的事，变成清晰的目标</h2>
      <!-- 显示一段文字；用样式类 muted 控制外观。 -->
      <p class="muted">从目标拆分阶段，再把任务安排到合适的一周与一天。</p>
    </div>
    <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:create') 成立时渲染；类型设为 primary；click 事件触发时执行 actions.edit('goal')。 -->
    <el-button
      v-if="canGrow('goal:goal:create')"
      type="primary"
      @click="actions.edit('goal')"
      >新增目标</el-button
    >
  </section>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示Element Plus 的 tabs 组件；与 mode 双向同步；tab-change 事件触发时执行 page = 1; load();。 -->
    <el-tabs
      v-model="mode"
      @tab-change="
        page = 1;
        load();
      "
      ><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 当前目标；name 固定为 CURRENT。 --><el-tab-pane label="当前目标" name="CURRENT" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 未来目标；name 固定为 FUTURE。 --><el-tab-pane
        label="未来目标"
        name="FUTURE" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 全部目标；name 固定为 ALL。 --><el-tab-pane label="全部目标" name="ALL" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 已完成；name 固定为 COMPLETED。 --><el-tab-pane
        label="已完成"
        name="COMPLETED" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 已归档；name 固定为 ARCHIVED。 --><el-tab-pane label="已归档" name="ARCHIVED"
    /></el-tabs>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 input 组件；与 keyword 双向同步；启用 clearable 属性；未输入时提示 搜索目标名称；keyup 事件触发时执行 page = 1; load();。 -->
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索目标名称"
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 date-picker 组件；与 range 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；start-placeholder 固定为 开始日期下限；end-placeholder 固定为 开始日期上限。 --><el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期下限"
        end-placeholder="开始日期上限"
      /><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 reset。 --><el-button @click="reset">重置</el-button>
      <!-- 显示一个布局容器；用样式类 view-density 控制外观；role 固定为 group；aria-label 提供无障碍说明 目标展示方式。 -->
      <div class="view-density" role="group" aria-label="目标展示方式">
        <!-- 显示Element Plus 的 button 组件；启用 text 属性；type 取自 density === 'cards' ? 'primary' : ''；aria-pressed 取自 density === 'cards'；aria-label 提供无障碍说明 卡片视图；title 固定为 卡片视图；click 事件触发时执行 density = 'cards'。 -->
        <el-button
          text
          :type="density === 'cards' ? 'primary' : ''"
          :aria-pressed="density === 'cards'"
          aria-label="卡片视图"
          title="卡片视图"
          @click="density = 'cards'"
          ><!-- 显示应用Icon（AppIcon）组件；name 固定为 grid。 --><AppIcon name="grid"
        /></el-button>
        <!-- 显示Element Plus 的 button 组件；启用 text 属性；type 取自 density === 'list' ? 'primary' : ''；aria-pressed 取自 density === 'list'；aria-label 提供无障碍说明 列表视图；title 固定为 列表视图；click 事件触发时执行 density = 'list'。 -->
        <el-button
          text
          :type="density === 'list' ? 'primary' : ''"
          :aria-pressed="density === 'list'"
          aria-label="列表视图"
          title="列表视图"
          @click="density = 'list'"
          ><!-- 显示应用Icon（AppIcon）组件；name 固定为 list。 --><AppIcon name="list"
        /></el-button>
      </div>
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <!-- 显示一个布局容器；仅当 density === 'cards' 成立时渲染；loading 指令使用 busy；用样式类 goal-grid 控制外观。 -->
    <div v-if="density === 'cards'" v-loading="busy" class="goal-grid">
      <!-- 显示一条独立内容；按 row in rows 为每条数据重复生成内容；key 取自 row.id；用样式类 goal-card 控制外观。 -->
      <article v-for="row in rows" :key="row.id" class="goal-card">
        <!-- 显示一个布局容器；用样式类 goal-card-heading 控制外观。 -->
        <div class="goal-card-heading">
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.details('goal', row)。 -->
          <el-button
            v-if="canGrow('goal:goal:read')"
            link
            type="primary"
            @click="actions.details('goal', row)"
            ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</el-button
          ><!-- 显示重点文字；前面条件均不成立时显示这个备用分支。 --><strong v-else><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</strong>
          <!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status；archived 取自 !!row.archivedAt。 -->
          <AppStatusTag :value="row.status" :archived="!!row.archivedAt" />
        </div>
        <!-- 显示一个布局容器；用样式类 goal-card-dates 控制外观。 -->
        <div class="goal-card-dates">
          <!-- 显示应用Icon（AppIcon）组件；name 固定为 day；size 取自 14。 -->
          <AppIcon name="day" :size="14" /><!-- 显示行内文字区域。 --><span><!-- 把 row.startDate || "未设开始日" 的当前值转成文字显示。 -->{{
            row.startDate || "未设开始日"
          }}</span
          ><!-- 显示行内文字区域。 --><span>至</span
          ><!-- 显示行内文字区域。 --><span><!-- 把 row.expectedEndDate || "未设预计结束" 的当前值转成文字显示。 -->{{ row.expectedEndDate || "未设预计结束" }}</span>
        </div>
        <!-- 显示进度值（ProgressValue）组件；value 取自 row.progress。 -->
        <ProgressValue :value="row.progress" />
        <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
        <div class="toolbar">
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.details('goal', row)。 -->
          <el-button
            v-if="canGrow('goal:goal:read')"
            link
            type="primary"
            @click="actions.details('goal', row)"
            >查看详情<!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 14。 --><AppIcon name="arrow" :size="14"
          /></el-button>
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 !row.archivedAt 成立时渲染。 -->
          <template v-if="!row.archivedAt">
            <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:update') 成立时渲染；启用 link 属性；click 事件触发时执行 actions.edit('goal', row)。 -->
            <el-button
              v-if="canGrow('goal:goal:update')"
              link
              @click="actions.edit('goal', row)"
              >编辑</el-button
            >
            <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:status') 成立时渲染；启用 link 属性；click 事件触发时执行 actions.status('goal', row)。 -->
            <el-button
              v-if="canGrow('goal:goal:status')"
              link
              @click="actions.status('goal', row)"
              >状态</el-button
            >
            <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:archive') 成立时渲染；启用 link 属性；类型设为 warning；click 事件触发时执行 actions.archive(row)。 -->
            <el-button
              v-if="canGrow('goal:goal:archive')"
              link
              type="warning"
              @click="actions.archive(row)"
              >归档</el-button
            >
          </template>
          <!-- 显示Element Plus 的 button 组件；前一条件不成立且 canGrow('goal:goal:restore') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.restore(row)。 -->
          <el-button
            v-else-if="canGrow('goal:goal:restore')"
            link
            type="primary"
            @click="actions.restore(row)"
            >恢复</el-button
          >
        </div>
      </article>
    </div>
    <!-- 显示应用空状态当前状态（AppEmptyState）组件；仅当 density === 'cards' && !rows.length && !busy && !error 成立时渲染；title 取自 keyword || range?.length ? '没有匹配的目标' : '给下一步成长一个方向'；description 固定为 从一个目标开始，再拆分成清晰的阶段与任务。；icon 固定为 goal。 -->
    <AppEmptyState
      v-if="density === 'cards' && !rows.length && !busy && !error"
      :title="
        keyword || range?.length ? '没有匹配的目标' : '给下一步成长一个方向'
      "
      description="从一个目标开始，再拆分成清晰的阶段与任务。"
      icon="goal"
    >
      <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:create') 成立时渲染；类型设为 primary；click 事件触发时执行 actions.edit('goal')。 -->
      <el-button
        v-if="canGrow('goal:goal:create')"
        type="primary"
        @click="actions.edit('goal')"
        >创建目标</el-button
      >
      <!-- 显示Element Plus 的 button 组件；仅当 keyword || range?.length 成立时渲染；click 事件触发时执行 reset。 -->
      <el-button v-if="keyword || range?.length" @click="reset"
        >清空筛选</el-button
      >
    </AppEmptyState>
    <!-- 显示Element Plus 的 table 组件；前一条件不成立且 density === 'list' 成立时渲染；data 取自 rows；loading 指令使用 busy；empty-text 固定为 暂无目标，可以先创建一个目标。 -->
    <el-table
      v-else-if="density === 'list'"
      :data="rows"
      v-loading="busy"
      empty-text="暂无目标，可以先创建一个目标"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无目标，可以先创建一个目标'。 --><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无目标，可以先创建一个目标'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 目标；min-width 固定为 200。 --><el-table-column prop="name" label="目标" min-width="200"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.details('goal', row)。 --><el-button
            v-if="canGrow('goal:goal:read')"
            link
            type="primary"
            @click="actions.details('goal', row)"
            ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</el-button
          ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</span></template
        ></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 进度；min-width 固定为 200。 --><el-table-column label="进度" min-width="200"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示进度值（ProgressValue）组件；value 取自 row.progress。 --><ProgressValue :value="row.progress" /></template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 100。 --><el-table-column label="状态" width="100"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 startDate；label 固定为 开始日期；min-width 固定为 120。 --><el-table-column
        prop="startDate"
        label="开始日期"
        min-width="120"
      /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 expectedEndDate；label 固定为 预计结束；min-width 固定为 120。 --><el-table-column
        prop="expectedEndDate"
        label="预计结束"
        min-width="120"
      /><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 290；fixed 固定为 right。 --><el-table-column label="操作" width="290" fixed="right"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 !row.archivedAt 成立时渲染。 --><template v-if="!row.archivedAt"
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:update') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.edit('goal', row)。 --><el-button
              v-if="canGrow('goal:goal:update')"
              link
              type="primary"
              @click="actions.edit('goal', row)"
              >编辑</el-button
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:status') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.status('goal', row)。 --><el-button
              v-if="canGrow('goal:goal:status')"
              link
              type="primary"
              @click="actions.status('goal', row)"
              >状态</el-button
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:archive') 成立时渲染；启用 link 属性；类型设为 warning；click 事件触发时执行 actions.archive(row)。 --><el-button
              v-if="canGrow('goal:goal:archive')"
              link
              type="warning"
              @click="actions.archive(row)"
              >归档</el-button
            ></template
          ><!-- 显示Element Plus 的 button 组件；前一条件不成立且 canGrow('goal:goal:restore') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 actions.restore(row)。 --><el-button
            v-else-if="canGrow('goal:goal:restore')"
            link
            type="primary"
            @click="actions.restore(row)"
            >恢复</el-button
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
    />
  </section>
  <!-- 显示成长业务Actions（GrowthActions）组件；把组件或元素引用保存为 actions，供脚本读取；changed 事件触发时执行 load。 -->
  <GrowthActions ref="actions" @changed="load" />
</template>
