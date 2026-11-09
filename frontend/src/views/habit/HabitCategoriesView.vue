<!-- 渲染习惯分类列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { habitApi, signalChange } from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* 对话框（dialog）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  dialog = ref(false),
  /* saving保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  saving = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* form响应式引用（formRef）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  formRef = ref(null),
  /* 等待处理（pending）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  pending = ref("");
const /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ form = reactive({ /* 编号（id）字段使用固定文字 ''。 */ id: "", /* 名称（name）字段使用固定文字 ''。 */ name: "", /* 排序顺序（sortOrder）字段使用固定数值 0。 */ sortOrder: 0, /* 版本号（version）字段使用空值，表示当前还没有对应内容。 */ version: undefined });
const /* 列表（list）保存调用 usePaged((params, signal) => habitApi.categories 完成分类列表处理。 */ list = usePaged((params, signal) =>
  habitApi.categories({ ...params, /* keyword字段使用表达式 keyword.value 的结果。 */ keyword: keyword.value }, signal),
);
/* edit：调用 edit 完成edit处理。 */
function edit(row = {}) {
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(form, {
    /* 编号（id）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.id || ""。 */
    id: row.id || "",
    /* 名称（name）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.name || ""。 */
    name: row.name || "",
    /* 排序顺序（sortOrder）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.sortOrder || 0。 */
    sortOrder: row.sortOrder || 0,
    /* 版本号（version）字段使用表达式 row.version 的结果。 */
    version: row.version,
  });
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 把真，表示条件成立或开关打开写入 dialog.value。 */
  dialog.value = true;
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 检查条件 saving.value 是否成立，成立时走这一分支。 */
  if (saving.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 !(await formRef.value?.validate().catch(() => false)) 是否成立，成立时走这一分支。 */
  if (!(await formRef.value?.validate().catch(() => false))) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把真，表示条件成立或开关打开写入 saving.value。 */
  saving.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待保存 分类（Category）。 */
    await habitApi.saveCategory(form.id, {
      /* 名称（name）字段使用去掉文字两端的空白。 */
      name: form.name.trim(),
      /* 排序顺序（sortOrder）字段使用表达式 form.sortOrder 的结果。 */
      sortOrder: form.sortOrder,
      /* 版本号（version）字段使用表达式 form.version 的结果。 */
      version: form.version,
    });
    /* 把假，表示条件不成立或开关关闭写入 dialog.value。 */
    dialog.value = false;
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("分类已保存");
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("habit");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await list.load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
    /* 检查条件 e.status === 409 是否成立，成立时走这一分支。 */
    if (e.status === 409) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await list.load();
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 saving.value。 */
    saving.value = false;
  }
}
/* 移除（remove）：调用 remove 完成移除处理。 */
async function remove(row) {
  /* 检查条件 pending.value 是否成立，成立时走这一分支。 */
  if (pending.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction(
      "删除分类「" +
        row.name +
        "」？已有习惯与历史引用会保留，之后不能再新选择这个分类。",
    );
    /* 把表达式 row.id 的结果写入 pending.value。 */
    pending.value = row.id;
    /* 等待移除 分类（Category）。 */
    await habitApi.removeCategory(row);
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("habit");
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("分类已删除，旧引用保留");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await list.load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 检查条件 e.status === 409 是否成立，成立时走这一分支。 */
    if (e.status === 409) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await list.load();
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示二级标题。 -->
        <h2>自己的分类，自己的组织方式</h2>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">删除分类不会删除习惯或改写过去的归属。</p>
      </div>
      <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
      <div class="toolbar">
        <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 router.push('/habits')。 -->
        <el-button @click="router.push('/habits')">返回习惯</el-button
        ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:category:create') 成立时渲染；类型设为 primary；click 事件触发时执行 edit()。 --><el-button
          v-if="canGrow('habit:category:create')"
          type="primary"
          @click="edit()"
          >新增分类</el-button
        >
      </div>
    </div>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 input 组件；与 keyword 双向同步；启用 clearable 属性；未输入时提示 分类名称；keyup 事件触发时执行 list.load(true)。 -->
      <el-input
        v-model="keyword"
        clearable
        placeholder="分类名称"
        @keyup.enter="list.load(true)"
      /><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 list.load(true)。 --><el-button type="primary" @click="list.load(true)">查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 keyword = ''; list.load(true);。 --><el-button
        @click="
          keyword = '';
          list.load(true);
        "
        >重置</el-button
      >
    </div>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 list.busy && !list.loaded；error 取自 list.error；retry 事件触发时执行 list.load。 -->
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <!-- 显示Element Plus 的 table 组件；data 取自 list.rows；loading 指令使用 list.busy；empty-text 固定为 还没有分类，可以先创建一个。 -->
      <el-table
        :data="list.rows"
        v-loading="list.busy"
        empty-text="还没有分类，可以先创建一个"
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '还没有分类，可以先创建一个'。 --><AppEmptyState
            :description="
              error
                ? '加载失败，请查看提示后重试。'
                : '还没有分类，可以先创建一个'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 分类名称；min-width 固定为 200。 --><el-table-column
          prop="name"
          label="分类名称"
          min-width="200"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 sortOrder；label 固定为 顺序；width 固定为 110。 --><el-table-column prop="sortOrder" label="顺序" width="110" />
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 180。 -->
        <el-table-column label="操作" width="180"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:category:update') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 edit(row)。 --><el-button
              v-if="canGrow('habit:category:update')"
              link
              type="primary"
              @click="edit(row)"
              >编辑</el-button
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:category:delete') 成立时渲染；启用 link 属性；类型设为 danger；disabled 取自 !!pending；click 事件触发时执行 remove(row)。 --><el-button
              v-if="canGrow('habit:category:delete')"
              link
              type="danger"
              :disabled="!!pending"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        > </el-table
      ><!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 list.load。 --><PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <!-- 显示Element Plus 的 dialog 组件；与 dialog 双向同步；title 取自 form.id ? '编辑分类' : '新增分类'；width 固定为 min(480px, 96vw)；close-on-click-modal 取自 false；before-close 取自 (done) => { if (!saving) done(); }；启用 destroy-on-close 属性。 -->
  <el-dialog
    v-model="dialog"
    :title="form.id ? '编辑分类' : '新增分类'"
    width="min(480px, 96vw)"
    :close-on-click-modal="false"
    :before-close="
      (done) => {
        if (!saving) done();
      }
    "
    destroy-on-close
  >
    <!-- 显示Element Plus 的 form 组件；把组件或元素引用保存为 formRef，供脚本读取；model 取自 form；label-position 固定为 top；submit 事件触发时执行 save。 -->
    <el-form
      ref="formRef"
      :model="form"
      label-position="top"
      @submit.prevent="save"
    >
      <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
      /><!-- 显示Element Plus 的 form-item 组件；label 固定为 名称；prop 固定为 name；rules 取自 [ { required: true, whitespace: true, message: '请填写分类名称' }, ]。 --><el-form-item
        label="名称"
        prop="name"
        :rules="[
          { required: true, whitespace: true, message: '请填写分类名称' },
        ]"
        ><!-- 显示Element Plus 的 input 组件；与 form.name 双向同步；maxlength 固定为 50；启用 show-word-limit 属性。 --><el-input v-model="form.name" maxlength="50" show-word-limit
      /></el-form-item>
      <!-- 显示Element Plus 的 form-item 组件；label 固定为 显示顺序。 -->
      <el-form-item label="显示顺序"
        ><!-- 显示Element Plus 的 input-number 组件；与 form.sortOrder 双向同步；min 取自 0；max 取自 2147483647；precision 取自 0。 --><el-input-number
          v-model="form.sortOrder"
          :min="0"
          :max="2147483647"
          :precision="0"
      /></el-form-item>
      <!-- 显示一段文字；仅当 error 成立时渲染；用样式类 hint 控制外观。 -->
      <p v-if="error" class="hint">
        版本冲突时保留当前输入；取消后从列表重新打开可读取最新版本。
      </p>
    </el-form>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 -->
    <template #footer
      ><!-- 显示Element Plus 的 button 组件；disabled 取自 saving；click 事件触发时执行 dialog = false。 --><el-button :disabled="saving" @click="dialog = false">取消</el-button
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 saving；click 事件触发时执行 save。 --><el-button type="primary" :loading="saving" @click="save"
        >保存分类</el-button
      ></template
    >
  </el-dialog>
</template>
