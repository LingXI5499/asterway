<!-- 渲染习惯列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive, watch, computed } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  habitApi,
  habitStates,
  cycleText,
  signalChange,
} from "../../api/personal";
/* 引入 ../../api/habit-actions 提供的组件、状态函数或请求方法。 */
import { checkHabit, cancelHabitToday } from "../../api/habit-actions";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow, today } from "../../api/growth";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 ../../components/personal/PagedSelect.vue 提供的组件、状态函数或请求方法。 */
import PagedSelect from "../../components/personal/PagedSelect.vue";
/* 引入 ../../components/personal/HabitEditor.vue 提供的组件、状态函数或请求方法。 */
import HabitEditor from "../../components/personal/HabitEditor.vue";
/* 引入 ../../components/personal/HabitHistory.vue 提供的组件、状态函数或请求方法。 */
import HabitHistory from "../../components/personal/HabitHistory.vue";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* mode保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  mode = ref("TODAY"),
  /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  filters = reactive({ /* keyword字段使用固定文字 ''。 */ keyword: "", /* 分类编号（categoryId）字段使用固定文字 ''。 */ categoryId: "", /* 状态（status）字段使用固定文字 ''。 */ status: "" });
const /* editing保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ editing = ref(false),
  /* 已选（selected）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  selected = ref(null),
  /* 历史Mode（historyMode）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  historyMode = ref("checkins"),
  /* 历史来源（historySource）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  historySource = ref(null),
  /* 历史是否展开（historyOpen）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  historyOpen = ref(false),
  /* 等待处理（pending）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  pending = ref("");
const /* 列表（list）保存根据前面的条件，从两个候选结果中选择一个：usePaged((params, signal) => mode.value === "TODAY" ? habitApi.today(params, signal) : habitApi.list…。 */ list = usePaged((params, signal) =>
  mode.value === "TODAY"
    ? habitApi.today(params, signal)
    : habitApi.list(
        { ...params, ...filters, /* 已归档（archived）字段使用条件判断的结果：mode.value === "ENDED"。 */ archived: mode.value === "ENDED" },
        signal,
      ),
);
const /* 分类列表（categories）保存调用 (params, signal) => habitApi.categories 完成分类列表处理。 */ categories = (params, signal) => habitApi.categories(params, signal);
const /* 编号Of（idOf）保存优先使用前面的有效值，没有时采用后面的备用值：(row) => row.habitId || row.id。 */ idOf = (row) => row.habitId || row.id;
const /* 已核对（checked）保存优先使用前面的有效值，没有时采用后面的备用值：(row) => !!(row.checkinId || row.checkedToday)。 */ checked = (row) => !!(row.checkinId || row.checkedToday);
const /* 今天分组列表（todayGroups）保存随依赖自动重算的筛选结果或数量。 */ todayGroups = computed(() => [
  {
    /* 标题（title）字段使用固定文字 '等待打卡'。 */
    title: "等待打卡",
    /* 已完成（done）字段使用假，表示条件不成立或开关关闭。 */
    done: false,
    /* 记录列表（rows）字段使用只保留符合条件的条目。 */
    rows: list.rows.filter((row) => !checked(row)),
  },
  { /* 标题（title）字段使用固定文字 '已经完成'。 */ title: "已经完成", /* 已完成（done）字段使用真，表示条件成立或开关打开。 */ done: true, /* 记录列表（rows）字段使用只保留符合条件的条目。 */ rows: list.rows.filter(checked) },
]);
/* sealed：调用 sealed 完成sealed处理。 */
function sealed(row) {
  /* 返回条件判断的结果：row.historyLockedThrough && today() <= row.historyLockedThrough，当前函数到此结束。 */
  return row.historyLockedThrough && today() <= row.historyLockedThrough;
}
/* edit：调用 edit 完成edit处理。 */
function edit(row = null) {
  /* 把根据前面的条件，从两个候选结果中选择一个：row ? { ...row, id: idOf(row) } : null写入 selected.value。 */
  selected.value = row ? { ...row, /* 编号（id）字段使用调用 idOf 完成编号Of处理。 */ id: idOf(row) } : null;
  /* 把真，表示条件成立或开关打开写入 editing.value。 */
  editing.value = true;
}
/* 历史（history）：调用 history 完成历史处理。 */
function history(row, value) {
  /* 把表达式 { ...row, id: idOf(row) } 的结果写入 historySource.value。 */
  historySource.value = { ...row, /* 编号（id）字段使用调用 idOf 完成编号Of处理。 */ id: idOf(row) };
  /* 把值（value）写入 historyMode.value。 */
  historyMode.value = value;
  /* 把真，表示条件成立或开关打开写入 historyOpen.value。 */
  historyOpen.value = true;
}
/* act：调用 act 完成act处理。 */
async function act(action, row) {
  /* 检查条件 pending.value 是否成立，成立时走这一分支。 */
  if (pending.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 action === "end" 是否成立，成立时走这一分支。 */
    if (action === "end")
      /* 等待确认 操作（Action）。 */
      await confirmAction(
        "结束并归档「" +
          row.name +
          "」？今日已有打卡和过去规则会保留，之后可重新开启。",
      );
    /* 检查条件 action === "delete" 是否成立，成立时走这一分支。 */
    if (action === "delete")
      /* 等待确认 操作（Action）。 */
      await confirmAction(
        "删除「" +
          row.name +
          "」？主体将逻辑删除，打卡和规则历史保留，删除后不能恢复。",
      );
    /* 把调用 idOf 完成编号Of处理写入 pending.value。 */
    pending.value = idOf(row);
    /* 检查条件 action === "checkin" 是否成立，成立时走这一分支。 */
    if (action === "checkin") /* 等待核对 习惯（Habit）。 */ await checkHabit(row);
    else /* 检查条件 action === "cancel" 是否成立，成立时走这一分支。 */ if (action === "cancel") /* 等待取消 习惯今天（HabitToday）。 */ await cancelHabitToday(row);
    else /* 检查条件 action === "delete" 是否成立，成立时走这一分支。 */ if (action === "delete") {
      /* 等待调用 habitApi.remove 完成移除处理。 */
      await habitApi.remove(row);
      /* 调用 ElMessage.success 完成成功处理。 */
      ElMessage.success("习惯已删除，历史保留");
    } else {
      /* 等待调用 habitApi.control 完成control处理。 */
      await habitApi.control(action, row);
      /* 调用 ElMessage.success 完成成功处理。 */
      ElMessage.success("习惯状态已更新");
    }
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("habit");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await list.load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 检查条件 e.status === 409 || e.status === 404 是否成立，成立时走这一分支。 */
    if (e.status === 409 || e.status === 404) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await list.load();
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
/* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
watch(mode, () => {
  /* 把固定文字 ''写入 filters.status。 */
  filters.status = "";
  /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  list.load(true);
});
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card personal-hero 控制外观。 -->
  <section class="card personal-hero">
    <!-- 显示一个布局容器。 -->
    <div>
      <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
      <span class="eyebrow">HABITS</span>
      <!-- 显示二级标题。 -->
      <h2>把今天的一次坚持，留在记录里</h2>
      <!-- 显示一段文字；用样式类 muted 控制外观。 -->
      <p class="muted">
        每天或指定星期，一次打卡。规则可以调整，过去的完成事实不会消失。
      </p>
    </div>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:category:list') 成立时渲染；click 事件触发时执行 router.push('/habits/categories')。 -->
      <el-button
        v-if="canGrow('habit:category:list')"
        @click="router.push('/habits/categories')"
        >管理分类</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:habit:create') 成立时渲染；类型设为 primary；click 事件触发时执行 edit()。 --><el-button
        v-if="canGrow('habit:habit:create')"
        type="primary"
        @click="edit()"
        >新增习惯</el-button
      >
    </div>
  </section>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示Element Plus 的 tabs 组件；与 mode 双向同步。 -->
    <el-tabs v-model="mode"
      ><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 今日习惯；name 固定为 TODAY。 --><el-tab-pane label="今日习惯" name="TODAY" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 进行与暂停；name 固定为 CURRENT。 --><el-tab-pane
        label="进行与暂停"
        name="CURRENT" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 结束归档；name 固定为 ENDED。 --><el-tab-pane label="结束归档" name="ENDED"
    /></el-tabs>
    <!-- 显示一个布局容器；仅当 mode !== 'TODAY' 成立时渲染；用样式类 toolbar 控制外观。 -->
    <div v-if="mode !== 'TODAY'" class="toolbar">
      <!-- 显示Element Plus 的 input 组件；与 filters.keyword 双向同步；启用 clearable 属性；未输入时提示 搜索习惯名称；keyup 事件触发时执行 list.load(true)。 -->
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索习惯名称"
        @keyup.enter="list.load(true)"
      />
      <!-- 显示Paged选择（PagedSelect）组件；与 filters.categoryId 双向同步；loader 取自 categories；未输入时提示 全部分类；disabled 取自 !canGrow('habit:category:list')。 -->
      <PagedSelect
        v-model="filters.categoryId"
        :loader="categories"
        placeholder="全部分类"
        :disabled="!canGrow('habit:category:list')"
      />
      <!-- 显示Element Plus 的 select 组件；仅当 mode !== 'ENDED' 成立时渲染；与 filters.status 双向同步；启用 clearable 属性；未输入时提示 全部状态。 -->
      <el-select
        v-if="mode !== 'ENDED'"
        v-model="filters.status"
        clearable
        placeholder="全部状态"
        ><!-- 显示Element Plus 的 option 组件；label 固定为 进行中；value 固定为 ACTIVE。 --><el-option label="进行中" value="ACTIVE" /><!-- 显示Element Plus 的 option 组件；label 固定为 已暂停；value 固定为 PAUSED。 --><el-option
          label="已暂停"
          value="PAUSED"
      /></el-select>
      <!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 list.load(true)。 -->
      <el-button type="primary" @click="list.load(true)">查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 Object.assign(filters, { keyword: '', categoryId: '', status: '' }); list.load(true);。 --><el-button
        @click="
          Object.assign(filters, { keyword: '', categoryId: '', status: '' });
          list.load(true);
        "
        >重置</el-button
      >
    </div>
    <!-- 显示一个布局容器；前面条件均不成立时显示这个备用分支；用样式类 section-heading 控制外观。 -->
    <div v-else class="section-heading">
      <!-- 显示一段文字；用样式类 hint 控制外观。 -->
      <p class="hint">
        只列出今天应做或已有有效打卡的习惯，已完成后暂停或删除仍保留一次完成。
      </p>
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 list.load。 -->
      <el-button @click="list.load">刷新今日</el-button>
    </div>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 list.busy && !list.loaded；error 取自 list.error；retry 事件触发时执行 list.load。 -->
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <!-- 显示一个布局容器；仅当 mode === 'TODAY' 成立时渲染；loading 指令使用 list.busy；用样式类 habit-today 控制外观。 -->
      <div v-if="mode === 'TODAY'" v-loading="list.busy" class="habit-today">
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；按 group in todayGroups 为每条数据重复生成内容；key 取自 group.title。 -->
        <template v-for="group in todayGroups" :key="group.title">
          <!-- 显示一个有独立含义的内容区；仅当 group.rows.length 成立时渲染；用样式类 habit-group 控制外观。 -->
          <section v-if="group.rows.length" class="habit-group">
            <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
            <div class="section-heading">
              <!-- 显示三级标题。 -->
              <h3><!-- 把 group.title 的当前值转成文字显示。 -->{{ group.title }}</h3>
              <!-- 显示行内文字区域；用样式类 hint 控制外观。 -->
              <span class="hint">本页 <!-- 把 group.rows.length 的当前值转成文字显示。 -->{{ group.rows.length }} 项</span>
            </div>
            <!-- 显示一个布局容器；用样式类 habit-check-list 控制外观。 -->
            <div class="habit-check-list">
              <!-- 显示一条独立内容；按 row in group.rows 为每条数据重复生成内容；key 取自 idOf(row)；用样式类 habit-check-row 控制外观；class 取自 { checked: group.done }。 -->
              <article
                v-for="row in group.rows"
                :key="idOf(row)"
                class="habit-check-row"
                :class="{ checked: group.done }"
              >
                <!-- 显示行内文字区域；用样式类 habit-indicator 控制外观。 -->
                <span class="habit-indicator"
                  ><!-- 显示应用Icon（AppIcon）组件；name 取自 group.done ? 'check' : 'habit'。 --><AppIcon :name="group.done ? 'check' : 'habit'"
                /></span>
                <!-- 显示一个布局容器；用样式类 habit-row-copy 控制外观。 -->
                <div class="habit-row-copy">
                  <!-- 显示重点文字。 -->
                  <strong><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</strong>
                  <!-- 显示一段文字；用样式类 hint 控制外观。 -->
                  <p class="hint">
                    <!-- 把 row.expectedTime ? "提示时间 " + row.expectedTime : "今天留出一点时间，坚持一次" 的当前值转成文字显示。 -->
                    {{
                      row.expectedTime
                        ? "提示时间 " + row.expectedTime
                        : "今天留出一点时间，坚持一次"
                    }}
                    · <!-- 把 habitStates[row.status] 的当前值转成文字显示。 -->{{ habitStates[row.status] }}
                  </p>
                </div>
                <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
                <div class="toolbar">
                  <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:checkin:create') && canGrow('habit:habit:read') && !group.done 成立时渲染；类型设为 primary；disabled 取自 !!pending || !row.canCheckin；loading 取自 pending === idOf(row)；click 事件触发时执行 act('checkin', row)。 -->
                  <el-button
                    v-if="
                      canGrow('habit:checkin:create') &&
                      canGrow('habit:habit:read') &&
                      !group.done
                    "
                    type="primary"
                    :disabled="!!pending || !row.canCheckin"
                    :loading="pending === idOf(row)"
                    @click="act('checkin', row)"
                    ><!-- 显示应用Icon（AppIcon）组件；name 固定为 check；size 取自 16。 --><AppIcon name="check" :size="16" /><!-- 显示行内文字区域。 --><span
                      >今日打卡</span
                    ></el-button
                  >
                  <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:checkin:cancel') && canGrow('habit:checkin:read') && group.done 成立时渲染；disabled 取自 !!pending || !row.canCancel；click 事件触发时执行 act('cancel', row)。 -->
                  <el-button
                    v-if="
                      canGrow('habit:checkin:cancel') &&
                      canGrow('habit:checkin:read') &&
                      group.done
                    "
                    :disabled="!!pending || !row.canCancel"
                    @click="act('cancel', row)"
                    >撤销今日</el-button
                  >
                  <!-- 显示Element Plus 的 dropdown 组件；仅当 canGrow('habit:checkin:read') || canGrow('habit:habit:history') 成立时渲染；trigger 固定为 click；command 事件触发时执行 (value) => history(row, value)。 -->
                  <el-dropdown
                    v-if="
                      canGrow('habit:checkin:read') ||
                      canGrow('habit:habit:history')
                    "
                    trigger="click"
                    @command="(value) => history(row, value)"
                  >
                    <!-- 显示Element Plus 的 button 组件；启用 text 属性；aria-label 提供无障碍说明 查看习惯历史；title 固定为 查看习惯历史。 -->
                    <el-button
                      text
                      aria-label="查看习惯历史"
                      title="查看习惯历史"
                      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 more。 --><AppIcon name="more"
                    /></el-button>
                    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
                    <template #dropdown
                      ><!-- 显示Element Plus 的 dropdown-menu 组件。 --><el-dropdown-menu>
                        <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canGrow('habit:checkin:read') 成立时渲染；command 固定为 checkins。 -->
                        <el-dropdown-item
                          v-if="canGrow('habit:checkin:read')"
                          command="checkins"
                          >打卡历史</el-dropdown-item
                        >
                        <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canGrow('habit:habit:history') 成立时渲染；command 固定为 revisions。 -->
                        <el-dropdown-item
                          v-if="canGrow('habit:habit:history')"
                          command="revisions"
                          >规则历史</el-dropdown-item
                        >
                      </el-dropdown-menu></template
                    >
                  </el-dropdown>
                </div>
              </article>
            </div>
          </section>
        </template>
        <!-- 显示应用空状态当前状态（AppEmptyState）组件；仅当 !list.rows.length && !list.busy 成立时渲染；title 固定为 今天还没有应做的习惯；description 固定为 从一个小习惯开始，每天留下一次真实记录。；icon 固定为 habit。 -->
        <AppEmptyState
          v-if="!list.rows.length && !list.busy"
          title="今天还没有应做的习惯"
          description="从一个小习惯开始，每天留下一次真实记录。"
          icon="habit"
          ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:habit:create') 成立时渲染；类型设为 primary；click 事件触发时执行 edit()。 --><el-button
            v-if="canGrow('habit:habit:create')"
            type="primary"
            @click="edit()"
            >创建习惯</el-button
          ></AppEmptyState
        >
      </div>
      <!-- 显示Element Plus 的 table 组件；前面条件均不成立时显示这个备用分支；data 取自 list.rows；loading 指令使用 list.busy；empty-text 取自 mode === 'TODAY' ? '今天暂无应做习惯，可以先创建一个习惯' : '暂无习惯，试试创建或调整筛选条件'。 -->
      <el-table
        v-else
        :data="list.rows"
        v-loading="list.busy"
        :empty-text="
          mode === 'TODAY'
            ? '今天暂无应做习惯，可以先创建一个习惯'
            : '暂无习惯，试试创建或调整筛选条件'
        "
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 mode === 'TODAY' ? '今天暂无应做习惯，可以先创建一个习惯' : '暂无习惯，试试创建或调整筛选条件'。 --><AppEmptyState
            :description="
              mode === 'TODAY'
                ? '今天暂无应做习惯，可以先创建一个习惯'
                : '暂无习惯，试试创建或调整筛选条件'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 习惯；min-width 固定为 190。 --><el-table-column
          prop="name"
          label="习惯"
          min-width="190"
        /><!-- 显示Element Plus 的 table-column 组件；仅当 mode !== 'TODAY' 成立时渲染；label 固定为 分类；min-width 固定为 140。 --><el-table-column v-if="mode !== 'TODAY'" label="分类" min-width="140"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 把 row.categoryName || "未分类" 的当前值转成文字显示。 -->{{ row.categoryName || "未分类"
            }}<!-- 显示Element Plus 的 tag 组件；仅当 row.categoryDeleted 成立时渲染；size 固定为 small；类型设为 info。 --><el-tag v-if="row.categoryDeleted" size="small" type="info"
              >已停用</el-tag
            ></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；仅当 mode !== 'TODAY' 成立时渲染；label 固定为 周期；min-width 固定为 150。 -->
        <el-table-column v-if="mode !== 'TODAY'" label="周期" min-width="150"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 cycleText(row) 的当前值转成文字显示。 -->{{
            cycleText(row)
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 提示时间；width 固定为 100。 -->
        <el-table-column label="提示时间" width="100"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 row.expectedTime || "未设置" 的当前值转成文字显示。 -->{{
            row.expectedTime || "未设置"
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 110。 -->
        <el-table-column label="状态" width="110"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示Element Plus 的 tag 组件；type 取自 row.status === 'ACTIVE' ? 'success' : row.status === 'PAUSED' ? 'warning' : 'info'。 --><el-tag
              :type="
                row.status === 'ACTIVE'
                  ? 'success'
                  : row.status === 'PAUSED'
                    ? 'warning'
                    : 'info'
              "
              ><!-- 把 habitStates[row.status] 的当前值转成文字显示。 -->{{ habitStates[row.status] }}</el-tag
            ></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 今日；width 固定为 100。 -->
        <el-table-column label="今日" width="100"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示Element Plus 的 tag 组件；type 取自 row.checkinId || row.checkedToday ? 'success' : 'info'。 --><el-tag
              :type="row.checkinId || row.checkedToday ? 'success' : 'info'"
              ><!-- 把 row.checkinId || row.checkedToday ? "已打卡" : "未打卡" 的当前值转成文字显示。 -->{{
                row.checkinId || row.checkedToday ? "已打卡" : "未打卡"
              }}</el-tag
            ></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；min-width 固定为 440；fixed 固定为 right。 -->
        <el-table-column label="操作" min-width="440" fixed="right"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }">
            <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 mode === 'TODAY' 成立时渲染。 -->
            <template v-if="mode === 'TODAY'">
              <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:checkin:create') && canGrow('habit:habit:read') 成立时渲染；启用 link 属性；类型设为 primary；disabled 取自 !!pending || !row.canCheckin；click 事件触发时执行 act('checkin', row)。 -->
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
              <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:checkin:cancel') && canGrow('habit:checkin:read') 成立时渲染；启用 link 属性；类型设为 warning；disabled 取自 !!pending || !row.canCancel；click 事件触发时执行 act('cancel', row)。 -->
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
            <!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 -->
            <template v-else>
              <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:habit:update') 成立时渲染；启用 link 属性；类型设为 primary；disabled 取自 !!pending || row.status === 'ENDED' || !!sealed(row)；click 事件触发时执行 edit(row)。 -->
              <el-button
                v-if="canGrow('habit:habit:update')"
                link
                type="primary"
                :disabled="!!pending || row.status === 'ENDED' || !!sealed(row)"
                @click="edit(row)"
                >编辑</el-button
              >
              <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'ACTIVE' && canGrow('habit:habit:pause') 成立时渲染；启用 link 属性；类型设为 warning；disabled 取自 !!pending || !!sealed(row)；click 事件触发时执行 act('pause', row)。 -->
              <el-button
                v-if="row.status === 'ACTIVE' && canGrow('habit:habit:pause')"
                link
                type="warning"
                :disabled="!!pending || !!sealed(row)"
                @click="act('pause', row)"
                >暂停</el-button
              >
              <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'PAUSED' && canGrow('habit:habit:resume') 成立时渲染；启用 link 属性；类型设为 primary；disabled 取自 !!pending || !!sealed(row)；click 事件触发时执行 act('resume', row)。 -->
              <el-button
                v-if="row.status === 'PAUSED' && canGrow('habit:habit:resume')"
                link
                type="primary"
                :disabled="!!pending || !!sealed(row)"
                @click="act('resume', row)"
                >恢复</el-button
              >
              <!-- 显示Element Plus 的 button 组件；仅当 row.status !== 'ENDED' && canGrow('habit:habit:end') 成立时渲染；启用 link 属性；disabled 取自 !!pending || !!sealed(row)；click 事件触发时执行 act('end', row)。 -->
              <el-button
                v-if="row.status !== 'ENDED' && canGrow('habit:habit:end')"
                link
                :disabled="!!pending || !!sealed(row)"
                @click="act('end', row)"
                >结束归档</el-button
              >
              <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'ENDED' && canGrow('habit:habit:reopen') 成立时渲染；启用 link 属性；类型设为 primary；disabled 取自 !!pending || !!sealed(row)；click 事件触发时执行 act('reopen', row)。 -->
              <el-button
                v-if="row.status === 'ENDED' && canGrow('habit:habit:reopen')"
                link
                type="primary"
                :disabled="!!pending || !!sealed(row)"
                @click="act('reopen', row)"
                >重新开启</el-button
              >
              <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:habit:delete') 成立时渲染；启用 link 属性；类型设为 danger；disabled 取自 !!pending || !!sealed(row)；click 事件触发时执行 act('delete', row)。 -->
              <el-button
                v-if="canGrow('habit:habit:delete')"
                link
                type="danger"
                :disabled="!!pending || !!sealed(row)"
                @click="act('delete', row)"
                >删除</el-button
              >
            </template>
            <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:checkin:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 history(row, 'checkins')。 -->
            <el-button
              v-if="canGrow('habit:checkin:read')"
              link
              type="primary"
              @click="history(row, 'checkins')"
              >打卡历史</el-button
            >
            <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:habit:history') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 history(row, 'revisions')。 -->
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
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 list.load。 -->
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <!-- 显示习惯Editor（HabitEditor）组件；与 editing 双向同步；source 取自 selected；saved 事件触发时执行 list.load。 -->
  <HabitEditor v-model="editing" :source="selected" @saved="list.load" />
  <!-- 显示习惯历史（HabitHistory）组件；与 historyOpen 双向同步；source 取自 historySource；mode 取自 historyMode；changed 事件触发时执行 list.load。 -->
  <HabitHistory
    v-model="historyOpen"
    :source="historySource"
    :mode="historyMode"
    @changed="list.load"
  />
</template>
