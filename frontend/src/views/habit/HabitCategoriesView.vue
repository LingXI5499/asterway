<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { habitApi, signalChange } from "../../api/personal";
import { canGrow } from "../../api/growth";
import { confirmAction, report } from "../../api/ui";
import { ElMessage } from "element-plus";
import { usePaged } from "../../composables/usePaged";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const router = useRouter(),
  keyword = ref(""),
  dialog = ref(false),
  saving = ref(false),
  error = ref(""),
  formRef = ref(null),
  pending = ref("");
const form = reactive({ id: "", name: "", sortOrder: 0, version: undefined });
const list = usePaged((params, signal) =>
  habitApi.categories({ ...params, keyword: keyword.value }, signal),
);
function edit(row = {}) {
  Object.assign(form, {
    id: row.id || "",
    name: row.name || "",
    sortOrder: row.sortOrder || 0,
    version: row.version,
  });
  error.value = "";
  dialog.value = true;
}
async function save() {
  if (saving.value) return;
  if (!(await formRef.value?.validate().catch(() => false))) return;
  saving.value = true;
  error.value = "";
  try {
    await habitApi.saveCategory(form.id, {
      name: form.name.trim(),
      sortOrder: form.sortOrder,
      version: form.version,
    });
    dialog.value = false;
    ElMessage.success("分类已保存");
    signalChange("habit");
    await list.load();
  } catch (e) {
    error.value = e.message;
    if (e.status === 409) await list.load();
  } finally {
    saving.value = false;
  }
}
async function remove(row) {
  if (pending.value) return;
  try {
    await confirmAction(
      "删除分类「" +
        row.name +
        "」？已有习惯与历史引用会保留，之后不能再新选择这个分类。",
    );
    pending.value = row.id;
    await habitApi.removeCategory(row);
    signalChange("habit");
    ElMessage.success("分类已删除，旧引用保留");
    await list.load();
  } catch (e) {
    report(e);
    if (e.status === 409) await list.load();
  } finally {
    pending.value = "";
  }
}
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <div>
        <h2>自己的分类，自己的组织方式</h2>
        <p class="hint">删除分类不会删除习惯或改写过去的归属。</p>
      </div>
      <div class="toolbar">
        <el-button @click="router.push('/habits')">返回习惯</el-button
        ><el-button
          v-if="canGrow('habit:category:create')"
          type="primary"
          @click="edit()"
          >新增分类</el-button
        >
      </div>
    </div>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="分类名称"
        @keyup.enter="list.load(true)"
      /><el-button type="primary" @click="list.load(true)">查询</el-button
      ><el-button
        @click="
          keyword = '';
          list.load(true);
        "
        >重置</el-button
      >
    </div>
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <el-table
        :data="list.rows"
        v-loading="list.busy"
        empty-text="还没有分类，可以先创建一个"
      >
        <el-table-column
          prop="name"
          label="分类名称"
          min-width="200"
        /><el-table-column prop="sortOrder" label="顺序" width="110" />
        <el-table-column label="操作" width="180"
          ><template #default="{ row }"
            ><el-button
              v-if="canGrow('habit:category:update')"
              link
              type="primary"
              @click="edit(row)"
              >编辑</el-button
            ><el-button
              v-if="canGrow('habit:category:delete')"
              link
              type="danger"
              :disabled="!!pending"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        > </el-table
      ><PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
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
    <el-form
      ref="formRef"
      :model="form"
      label-position="top"
      @submit.prevent="save"
    >
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
      /><el-form-item
        label="名称"
        prop="name"
        :rules="[
          { required: true, whitespace: true, message: '请填写分类名称' },
        ]"
        ><el-input v-model="form.name" maxlength="50" show-word-limit
      /></el-form-item>
      <el-form-item label="显示顺序"
        ><el-input-number
          v-model="form.sortOrder"
          :min="0"
          :max="2147483647"
          :precision="0"
      /></el-form-item>
      <p v-if="error" class="hint">
        版本冲突时保留当前输入；取消后从列表重新打开可读取最新版本。
      </p>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="dialog = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save"
        >保存分类</el-button
      ></template
    >
  </el-dialog>
</template>
