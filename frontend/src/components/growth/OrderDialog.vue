<!-- 渲染顺序对话框界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 同一父资源下的当前页排序，批量提交每条记录的版本。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../../api/http";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, resources } from "../../api/growth";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 种类（kind）字段使用可为空的文字（String）。 */ kind: String, /* 上级编号（parentId）字段使用表达式 [String, Number] 的结果。 */ parentId: [String, Number] });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["close", "saved"]);
const /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ rows = ref([]),
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
    const /* 路径（path）保存根据前面的条件，从两个候选结果中选择一个：props.kind === "stage" ? `/goals/${props.parentId}/stages` : "/tasks"。 */ path =
      props.kind === "stage" ? `/goals/${props.parentId}/stages` : "/tasks";
    const /* 数据（data）保存等待 根据前面的条件，从两个候选结果中选择一个：query(path, { stageId: props.kind === "task" ? props.parentId : null, page: page.value, size: 100, }… 完成后取得结果。 */ data = await query(path, {
      /* 阶段编号（stageId）字段使用根据前面的条件，从两个候选结果中选择一个：props.kind === "task" ? props.parentId : null。 */
      stageId: props.kind === "task" ? props.parentId : null,
      /* 当前页（page）字段使用表达式 page.value 的结果。 */
      page: page.value,
      /* 每页条数（size）字段使用固定数值 100。 */
      size: 100,
    });
    /* 把表达式 data.items 的结果写入 rows.value。 */
    rows.value = data.items;
    /* 把表达式 data.total 的结果写入 total.value。 */
    total.value = data.total;
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
/* move：调用 move 完成move处理。 */
function move(index, delta) {
  const /* 目标值（target）保存按表达式计算得到的值：index + delta。 */ target = index + delta;
  /* 检查条件 target < 0 || target >= rows.value.length 是否成立，成立时走这一分支。 */
  if (target < 0 || target >= rows.value.length) /* 返回空内容，当前函数到此结束。 */ return;
  /* 更新本次处理状态：[rows.value[index], rows.value[target]] = [ rows.value[target], rows.value[index], ]。 */
  [rows.value[index], rows.value[target]] = [
    rows.value[target],
    rows.value[index],
  ];
  /* 调用 rows.value.forEach 完成forEach处理。 */
  rows.value.forEach((r, i) => (r.sortOrder = (page.value - 1) * 100 + i));
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 上级（parent）保存根据前面的条件，从两个候选结果中选择一个：props.kind === "stage" ? "goals" : "stages"。 */ parent = props.kind === "stage" ? "goals" : "stages";
    /* 等待把每个条目转换成需要的形式。 */
    await api(`/${parent}/${props.parentId}/${resources[props.kind]}/order`, {
      /* 方法（method）字段使用固定状态或编码 PUT（PUT）。 */
      method: "PUT",
      /* 请求正文（body）字段使用把每个条目转换成需要的形式。 */
      body: {
        /* 数据列表（items）字段使用把每个条目转换成需要的形式。 */
        items: rows.value.map((r) => ({
          /* 编号（id）字段使用表达式 r.id 的结果。 */
          id: r.id,
          /* 版本号（version）字段使用表达式 r.version 的结果。 */
          version: r.version,
          /* 排序顺序（sortOrder）字段使用表达式 r.sortOrder 的结果。 */
          sortOrder: r.sortOrder,
        })),
      },
    });
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("saved");
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
  <!-- 显示Element Plus 的 dialog 组件；model-value 取自 true；close 事件触发时执行 emit('close')；title 固定为 调整顺序；width 固定为 min(640px,94vw)；close-on-click-modal 取自 false。 -->
  <el-dialog
    :model-value="true"
    align-center append-to-body class="plan-dialog"
    @close="emit('close')"
    title="调整顺序"
    width="min(640px,94vw)"
    :close-on-click-modal="false"
    ><!-- 显示Element Plus 的 alert 组件；title 固定为 本次保存只修改当前页排序，未加载的记录保持原值。排序值越小越靠前。；类型设为 info；closable 取自 false。 --><el-alert
      title="本次保存只修改当前页排序，未加载的记录保持原值。排序值越小越靠前。"
      type="info"
      :closable="false"
    /><!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 --><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 table 组件；data 取自 rows；loading 指令使用 busy。 --><el-table :data="rows" v-loading="busy"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无记录，可调整筛选条件或从第一条记录开始。'。 --><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 名称。 --><el-table-column prop="name" label="名称" /><!-- 显示Element Plus 的 table-column 组件；label 固定为 排序值；width 固定为 180。 --><el-table-column
        label="排序值"
        width="180"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 input-number 组件；与 row.sortOrder 双向同步；min 取自 0；max 取自 2147483647；precision 取自 0；size 固定为 small。 --><el-input-number
            v-model="row.sortOrder"
            :min="0"
            :max="2147483647"
            :precision="0"
            size="small" /></template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 调整；width 固定为 120。 --><el-table-column label="调整" width="120"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { $index }。 --><template #default="{ $index }"
          ><!-- 显示Element Plus 的 button 组件；启用 text 属性；disabled 取自 $index === 0；click 事件触发时执行 move($index, -1)。 --><el-button text :disabled="$index === 0" @click="move($index, -1)"
            >上移</el-button
          ><!-- 显示Element Plus 的 button 组件；启用 text 属性；disabled 取自 $index === rows.length - 1；click 事件触发时执行 move($index, 1)。 --><el-button
            text
            :disabled="$index === rows.length - 1"
            @click="move($index, 1)"
            >下移</el-button
          ></template
        ></el-table-column
      ></el-table
    ><!-- 显示Element Plus 的 pagination 组件；与 page 双向同步（current-page）；page-size 取自 100；total 取自 total；layout 固定为 total,prev,pager,next；current-change 事件触发时执行 load。 --><el-pagination
      v-model:current-page="page"
      :page-size="100"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="load"
    /><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 --><template #footer
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 emit('close')。 --><el-button @click="emit('close')">取消</el-button
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 save；disabled 取自 !rows.length；loading 取自 busy。 --><el-button
        type="primary"
        @click="save"
        :disabled="!rows.length"
        :loading="busy"
        >保存当前页</el-button
      ></template
    ></el-dialog
  >
</template>
