<!-- 渲染任务Picker界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 从当前可安排任务池选择已有任务，不新建或复制任务。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, priorities } from "../../api/growth";
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["close", "selected"]);
const /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ rows = ref([]),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* d保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ d = await query("/tasks", {
      /* 任务安排状态（assignmentStatus）字段使用固定状态或编码 POOL（连接池）。 */
      assignmentStatus: "POOL",
      /* keyword字段使用表达式 keyword.value 的结果。 */
      keyword: keyword.value,
      /* 当前页（page）字段使用表达式 page.value 的结果。 */
      page: page.value,
      /* 每页条数（size）字段使用固定数值 20。 */
      size: 20,
    });
    /* 把表达式 d.items 的结果写入 rows.value。 */
    rows.value = d.items;
    /* 把表达式 d.total 的结果写入 total.value。 */
    total.value = d.total;
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
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
  <!-- 显示Element Plus 的 dialog 组件；model-value 取自 true；close 事件触发时执行 emit('close')；title 固定为 从任务池选择任务；width 固定为 min(800px,94vw)。 -->
  <el-dialog
    :model-value="true"
    align-center append-to-body class="plan-dialog"
    @close="emit('close')"
    title="从任务池选择任务"
    width="min(800px,94vw)"
    ><!-- 显示一个布局容器；用样式类 toolbar 控制外观。 --><div class="toolbar">
      <!-- 显示Element Plus 的 input 组件；与 keyword 双向同步；启用 clearable 属性；未输入时提示 搜索任务；keyup 事件触发时执行 page = 1; load();。 -->
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索任务"
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      >
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false" /><!-- 显示Element Plus 的 table 组件；data 取自 rows；loading 指令使用 busy。 --><el-table :data="rows" v-loading="busy"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无记录，可调整筛选条件或从第一条记录开始。'。 --><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 任务；min-width 固定为 180。 --><el-table-column
        prop="name"
        label="任务"
        min-width="180"
      /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 goalName；label 固定为 目标。 --><el-table-column prop="goalName" label="目标" /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 stageName；label 固定为 阶段。 --><el-table-column
        prop="stageName"
        label="阶段"
      /><!-- 显示Element Plus 的 table-column 组件；label 固定为 优先级。 --><el-table-column label="优先级"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 priorities[row.priority] 的当前值转成文字显示。 -->{{
          priorities[row.priority]
        }}</template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作。 --><el-table-column label="操作"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 button 组件；启用 link 属性；类型设为 primary；click 事件触发时执行 emit('selected', row)。 --><el-button link type="primary" @click="emit('selected', row)"
            >选择</el-button
          ></template
        ></el-table-column
      ></el-table
    ><!-- 显示Element Plus 的 pagination 组件；用样式类 pagination 控制外观；与 page 双向同步（current-page）；page-size 取自 20；total 取自 total；layout 固定为 total,prev,pager,next；current-change 事件触发时执行 load。 --><el-pagination
      class="pagination"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="load"
  /></el-dialog>
</template>
