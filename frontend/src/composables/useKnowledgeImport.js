// 一批文件逐项上传；入队时记录目标库，切换页面中的知识库不会改变目标。
import { computed, onBeforeUnmount, ref } from "vue";
import { workspaceApi, query } from "../api/agentWorkspace";

export const importLabels = {
  WAITING: "等待上传", UPLOADING: "上传中", WAIT_PARSE: "等待解析", PARSING: "解析中",
  INDEXING: "索引中", READY: "可检索", DUPLICATE: "已存在", FAILED: "失败", INVALID: "校验不通过",
};
export function documentState(doc) {
  if (doc.status === "FAILED" || doc.indexStatus === "FAILED") return "FAILED";
  if (doc.status === "PARSING") return "PARSING";
  if (doc.status === "READY") return doc.indexStatus === "READY" ? "READY" : "INDEXING";
  return "WAIT_PARSE";
}
export function validateImportFile(file, capabilities) {
  if (!file.size) return "文件为空";
  if (file.size > capabilities.maxFileBytes) return `文件超过 ${(capabilities.maxFileBytes / 1024 / 1024).toFixed(0)} MB`;
  if (!capabilities.formats.some(f => file.name.toLowerCase().endsWith(f.extension))) return "不支持此文件格式";
  return "";
}

export function useKnowledgeImport(capabilities, changed = () => {}) {
  const items = ref([]), open = ref(false), paused = ref(false);
  const pending = computed(() => items.value.some(i => ["WAITING", "UPLOADING"].includes(i.state)));
  const processing = computed(() => items.value.some(i => ["WAIT_PARSE", "PARSING", "INDEXING"].includes(i.state)));
  let active = 0, disposed = false, polling = false;
  const controllers = new Map();

  function enqueue(files, base) {
    if (!base?.id || !capabilities.value) throw new Error("请先选择知识库，并等待上传说明加载完成");
    if (!files.length) return;
    const max = capabilities.value.maxBatchFiles;
    if (files.length > max) throw new Error(`每批最多 ${max} 个文件，请分批选择`);
    for (const file of files) {
      const error = validateImportFile(file, capabilities.value);
      items.value.push({ id: crypto.randomUUID(), file, name: file.name, baseId: base.id,
        baseName: base.name, size: file.size, state: error ? "INVALID" : "WAITING", error, doc: null, retrying: false });
    }
    paused.value = false; open.value = true; pump();
  }
  function pump() {
    if (disposed || paused.value) return;
    while (active < capabilities.value.uploadConcurrency) {
      const item = items.value.find(i => i.state === "WAITING");
      if (!item) break;
      item.state = "UPLOADING"; active++;
      const controller = new AbortController(); controllers.set(item.id, controller);
      workspaceApi.upload(item.file, { knowledgeBaseId: item.baseId, signal: controller.signal })
        .then(result => {
          if (disposed) return;
          item.doc = result.document;
          item.state = result.created ? documentState(item.doc) : "DUPLICATE";
          item.error = item.doc.errorMessage || item.doc.indexError || "";
          changed();
        }).catch(error => {
          if (disposed) return;
          item.state = "FAILED"; item.error = error.message;
          if (error.status === 401 || error.code === "SESSION_CHANGED") paused.value = true;
        }).finally(() => { active--; controllers.delete(item.id); pump(); });
    }
  }
  async function poll() {
    if (polling || disposed || document.hidden) return;
    polling = true;
    try {
      await Promise.allSettled(items.value.filter(i => i.doc && ["WAIT_PARSE", "PARSING", "INDEXING"].includes(i.state)).map(async item => {
        try {
          const doc = await workspaceApi.get(`knowledge/${item.doc.id}`);
          if (disposed) return;
          item.doc = doc; item.state = documentState(doc); item.error = doc.errorMessage || doc.indexError || "";
        } catch (error) {
          if (!disposed && [403, 404].includes(error.status)) { item.state = "FAILED"; item.error = error.message; }
        }
      }));
    } finally { polling = false; }
  }
  async function retry(item) {
    if (item.retrying || item.state !== "FAILED") return;
    if (!item.doc) { item.error = ""; item.state = "WAITING"; paused.value = false; pump(); return; }
    item.retrying = true;
    try {
      // 重新读取版本，避免后台状态推进后拿旧版本重试。
      const doc = await workspaceApi.get(`knowledge/${item.doc.id}`);
      if (documentState(doc) === "FAILED") {
        await workspaceApi.post(`knowledge/${doc.id}/retry${query({ version: doc.version })}`);
      }
      item.doc = await workspaceApi.get(`knowledge/${doc.id}`);
      item.state = documentState(item.doc); item.error = ""; changed();
    } catch (error) { item.error = error.message; }
    finally { item.retrying = false; }
  }
  function remove(item) { if (item.state === "WAITING") items.value = items.value.filter(i => i.id !== item.id); }
  function clearFinished() { items.value = items.value.filter(i => ["WAITING", "UPLOADING", "WAIT_PARSE", "PARSING", "INDEXING"].includes(i.state)); }
  const timer = setInterval(poll, 4000);
  const beforeUnload = event => { if (pending.value) { event.preventDefault(); event.returnValue = ""; } };
  window.addEventListener("beforeunload", beforeUnload);
  onBeforeUnmount(() => {
    disposed = true; clearInterval(timer); controllers.forEach(c => c.abort());
    window.removeEventListener("beforeunload", beforeUnload);
  });
  return { items, open, pending, processing, paused, enqueue, retry, remove, clearFinished, poll };
}
