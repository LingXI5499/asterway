<!-- 会话选择保存到服务端，发送消息时还会复制一份范围，避免生成中被后续选择改变。 -->
<script setup>
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { canGrow } from "../../api/growth";
import { workspaceApi } from "../../api/agentWorkspace";
import { agentApi } from "../../api/agent";

const props = defineProps({ conversation: Object, modelValue: { type: Array, default: () => [] }, loading: Boolean });
const emit = defineEmits(["update:modelValue", "saved"]);
const open = ref(false), bases = ref([]), selected = ref([]), version = ref(0);
const busy = ref(false), saving = ref(false), loaded = ref(false), error = ref("");
let sequence = 0;
const ids = computed(() => props.conversation?.knowledgeBaseIds || props.modelValue);
const canRead = computed(() => canGrow("agent:knowledge:read"));
const canSave = computed(() => !props.conversation || canGrow("agent:conversation:update"));
const summary = computed(() => {
  if (!ids.value.length) return "未选择知识库";
  const names = ids.value.map(id => bases.value.find(b => b.id === String(id))?.name).filter(Boolean);
  return names.length === ids.value.length && names.length <= 2 ? names.join("、") : `已选 ${ids.value.length} 个知识库`;
});
async function load() {
  const seq = ++sequence, id = props.conversation?.id;
  busy.value = true; loaded.value = false; error.value = "";
  try {
    const [libraries, detail] = await Promise.all([
      canRead.value ? workspaceApi.get("knowledge/bases") : { items: [] },
      id ? agentApi.conversation(id) : null,
    ]);
    if (seq !== sequence) return;
    bases.value = libraries.items;
    selected.value = [...(detail?.knowledgeBaseIds || props.modelValue)].map(String);
    version.value = detail?.version ?? 0;
    loaded.value = true;
  } catch (e) { if (seq === sequence) error.value = e.message; }
  finally { if (seq === sequence) busy.value = false; }
}
async function show() { open.value = true; await load(); }
async function save() {
  if (saving.value || busy.value || !loaded.value || !canSave.value) return;
  const seq = sequence, id = props.conversation?.id;
  saving.value = true; error.value = "";
  try {
    if (id) {
      const detail = await agentApi.knowledgeBases(id, selected.value, version.value);
      if (seq === sequence && props.conversation?.id === id) emit("saved", detail);
    } else emit("update:modelValue", [...selected.value]);
    if (seq === sequence) open.value = false;
  } catch (e) { if (seq === sequence) error.value = e.message; }
  finally { if (seq === sequence) saving.value = false; }
}
watch(() => props.conversation?.id, () => { sequence++; open.value = false; saving.value = false; loaded.value = false; });
onBeforeUnmount(() => { sequence++; });
</script>
<template>
  <div class="knowledge-scope-entry">
    <el-button text :disabled="loading || (!canRead && !ids.length)" @click="show"><AppIcon name="book" :size="16" /><span>{{ summary }}</span></el-button>
    <span v-if="!ids.length" class="knowledge-scope-hint">本次对话不检索知识资料</span>
  </div>
  <el-dialog v-model="open" title="选择本会话的知识库" width="min(560px,94vw)" align-center append-to-body :close-on-click-modal="false" :show-close="!saving" :close-on-press-escape="!saving">
    <p>只检索选中的知识库。清空选择后不检索文档，已有消息与引用会保留。</p>
    <p class="knowledge-scope-note">修改会影响后续消息，正在生成的回复继续使用发送时的选择。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button v-if="error" @click="load">重新读取当前选择</el-button>
    <el-skeleton v-if="busy" :rows="3" animated />
    <template v-else>
      <el-alert v-if="!canRead" title="当前没有知识库读取权限，可以清空原有选择。" type="info" :closable="false" />
      <el-select v-model="selected" multiple filterable :disabled="saving || !canRead || !canSave" placeholder="未选择，不引用知识库" aria-label="会话知识库" style="width:100%"><el-option v-for="base in bases" :key="base.id" :value="base.id" :label="base.name + '（' + base.documentCount + '篇）'" /></el-select>
      <p v-if="selected.some(id => !bases.some(base => base.id === id))" class="knowledge-scope-invalid">有知识库已失效或不可访问，请移除后重新选择。</p>
      <p v-if="canRead && !bases.length">还没有知识库，可先到“资料与记录 → 知识库”创建并导入文档。</p>
    </template>
    <template #footer><el-button :disabled="saving || !canSave" @click="selected = []">清空选择</el-button><el-button :disabled="saving" @click="open = false">取消</el-button><el-button type="primary" :disabled="busy || !loaded || !canSave" :loading="saving" @click="save">保存选择</el-button></template>
  </el-dialog>
</template>
<style scoped>
.knowledge-scope-entry { display:flex; align-items:center; flex-wrap:wrap; gap:8px; margin-bottom:8px; }
.knowledge-scope-entry .el-button { min-width:0; max-width:100%; }
.knowledge-scope-entry .el-button span { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.knowledge-scope-hint, .knowledge-scope-note { font-size:12px; line-height:16px; color:var(--aw-text-secondary); }
.knowledge-scope-invalid { color:var(--aw-danger); font-size:14px; line-height:20px; }
</style>
