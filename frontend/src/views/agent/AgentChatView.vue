<!-- 组织聊天工作区、历史恢复、流式回复及左右面板的联动。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import {
  computed,
  inject,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute, useRouter } from "vue-router";
/* 引入 ../../components/agent/AgentWorkspaceNav.vue 提供的组件、状态函数或请求方法。 */
import AgentWorkspaceNav from "../../components/agent/AgentWorkspaceNav.vue";
/* 引入 ../../components/agent/AgentPlanWorkspace.vue 提供的组件、状态函数或请求方法。 */
import AgentPlanWorkspace from "../../components/agent/AgentPlanWorkspace.vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage, ElMessageBox } from "element-plus";
/* 引入 ../../api/agent 提供的组件、状态函数或请求方法。 */
import { agentApi, streamConversationMessage } from "../../api/agent";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { authSessionKey } from "../../api/http";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 ../../components/agent/AgentConversationList.vue 提供的组件、状态函数或请求方法。 */
import AgentConversationList from "../../components/agent/AgentConversationList.vue";
/* 引入 ../../components/agent/AgentConversationPane.vue 提供的组件、状态函数或请求方法。 */
import AgentConversationPane from "../../components/agent/AgentConversationPane.vue";
import KnowledgeScopePicker from "../../components/agent/KnowledgeScopePicker.vue";

const /* route保存取得当前页面路径和查询参数。 */ route = useRoute();
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter();
const /* sidebar保存调用 inject 完成inject处理。 */ sidebar = inject("workspaceSidebar", { /* toggle：调用 toggle 完成toggle处理。 */ toggle() {}, /* 关闭（close）：释放连接、文件或客户端占用的资源。 */ close() {} });
const /* resourceLinks保存按表达式计算得到的值：[ ["/agent/memories", "agent:memory:read"], ["/agent/knowledge", "agent:knowledge:read"], ["/agent/runs", "agent:run:rea…。 */ resourceLinks = [
  ["/agent/memories", "agent:memory:read"], ["/agent/knowledge", "agent:knowledge:read"],
  ["/agent/runs", "agent:run:read"], ["/agent/reviews", "agent:review:read"],
];
const /* 首次Resource（firstResource）保存随依赖自动重算、从列表中找到的匹配对象。 */ firstResource = computed(() => resourceLinks.find(([, permission]) => canGrow(permission))?.[0]);
const /* resources是否展开（resourcesOpen）保存随相关状态变化自动重算的值。 */ resourcesOpen = computed(() => route.path.startsWith("/agent/"));
/* 关闭Resources（closeResources）：调用 closeResources 完成关闭Resources处理。 */
function closeResources() { /* 调用 router.push 完成push处理。 */ router.push({ /* 路径（path）字段使用固定文字 '/agent'。 */ path: "/agent", /* 查询条件（query）字段使用根据前面的条件，从两个候选结果中选择一个：route.query.conversationId ? { conversationId: route.query.conversationId } : {}。 */ query: route.query.conversationId ? { /* 对话编号（conversationId）字段使用表达式 route.query.conversationId 的结果。 */ conversationId: route.query.conversationId } : {} }); }
const /* 标题默认值（TITLE_DEFAULT）保存固定文字 '新对话'。 */ TITLE_DEFAULT = "新对话";
const /* 存储键（STORAGE_KEY）保存固定文字 'asterway.agent.conversation'。 */ STORAGE_KEY = "asterway.agent.conversation";
const /* 列表每页条数（LIST_SIZE）保存固定数值 30。 */ LIST_SIZE = 30;
const /* 消息每页条数（MESSAGE_SIZE）保存固定数值 24。 */ MESSAGE_SIZE = 24;
const /* RECOVERYDELAY（RECOVERY_DELAY）保存固定数值 2000。 */ RECOVERY_DELAY = 2000;

const /* 是否可以发送（canSend）保存随相关状态变化自动重算的值。 */ canSend = computed(() => canGrow("agent:chat:send"));
const /* 是否可以创建（canCreate）保存随相关状态变化自动重算的值。 */ canCreate = computed(() => canGrow("agent:conversation:create"));
const /* 是否可以列表（canList）保存随相关状态变化自动重算的值。 */ canList = computed(() => canGrow("agent:conversation:list"));
const /* 是否可以更新（canUpdate）保存随相关状态变化自动重算的值。 */ canUpdate = computed(() => canGrow("agent:conversation:update"));
const /* 是否可以移除（canRemove）保存随相关状态变化自动重算的值。 */ canRemove = computed(() => canGrow("agent:conversation:delete"));

const /* 列表（list）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ list = reactive({
  /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */
  rows: [],
  /* 总数（total）字段使用固定数值 0。 */
  total: 0,
  /* 当前页（page）字段使用固定数值 1。 */
  page: 1,
  /* 每页条数（size）字段使用列表每页条数（LIST_SIZE）。 */
  size: LIST_SIZE,
  /* 是否存在More（hasMore）字段使用假，表示条件不成立或开关关闭。 */
  hasMore: false,
  /* 正在处理（busy）字段使用假，表示条件不成立或开关关闭。 */
  busy: false,
  /* 错误（error）字段使用固定文字 ''。 */
  error: "",
  /* 已加载（loaded）字段使用假，表示条件不成立或开关关闭。 */
  loaded: false,
});
const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = reactive({ /* keyword字段使用固定文字 ''。 */ keyword: "" });
const /* 当前有效编号（activeId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ activeId = ref("");
const /* 对话（conversation）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ conversation = ref(null);
const /* 消息列表（messages）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ messages = ref([]);
const /* 是否存在More（hasMore）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ hasMore = ref(false);
const /* 下一个变化前（nextBefore）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ nextBefore = ref(null);
const /* 正在加载对话（loadingConversation）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ loadingConversation = ref(false);
const /* older正在处理（olderBusy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ olderBusy = ref(false);
const /* sending保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ sending = ref(false);
const /* generating保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ generating = ref(false);
const /* creating保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ creating = ref(false);
const /* recovering保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ recovering = ref(false);
const /* recovery本轮对话（recoveryTurn）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ recoveryTurn = ref(null);
const /* 草案（draft）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ draft = ref("");
const draftKnowledgeBaseIds = ref([]);
const /* 区域（pane）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ pane = ref(null);
const /* 方案是否展开（planOpen）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ planOpen = ref(false), /* 方案是否可用（planAvailable）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ planAvailable = ref(false), /* 方案事件（planEvent）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ planEvent = ref(null), /* 方案刷新键（planRefreshKey）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ planRefreshKey = ref(0);
const /* 复盘事件（reviewEvent）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ reviewEvent = ref(null), /* 复盘Working（reviewWorking）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ reviewWorking = ref(false);
const /* external复盘编号（externalReviewId）保存随相关状态变化自动重算的值。 */ externalReviewId = computed(() => /^\d+$/.test(String(route.query.reviewId || '')) ? String(route.query.reviewId) : '');
const /* 方案宽度（planWidth）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ planWidth = ref(400), /* resizing方案（resizingPlan）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ resizingPlan = ref(false), /* 方案Layout（planLayout）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ planLayout = ref(null);
/* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
try { const /* 已保存（saved）保存把输入转换成数值，供比较、计算或按编号查询。 */ saved = Number(localStorage.getItem("asterway.ui.planWidth")); /* 检查条件 saved >= 320 && saved <= 720 是否成立，成立时走这一分支。 */ if (saved >= 320 && saved <= 720) /* 把已保存（saved）写入 planWidth.value。 */ planWidth.value = saved; } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {}
let /* 方案Drag（planDrag）保存空值，表示当前还没有对应内容。 */ planDrag = null;
/* 保存方案宽度（savePlanWidth）：保存 方案宽度（PlanWidth）。 */
function savePlanWidth() { /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */ try { /* 设置 单条数据（Item） 的值。 */ localStorage.setItem("asterway.ui.planWidth", String(planWidth.value)); } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {} }
/* 方案宽度上限（planWidthLimit）：调用 planWidthLimit 完成方案宽度上限处理。 */
function planWidthLimit() { /* 返回沿 Math.max(320, Math.min(720, (planLayout.value?.clientWidth || 1080) - 360)) 读取数据；中间对象为空时返回空值，当前函数到此结束。 */ return Math.max(320, Math.min(720, (planLayout.value?.clientWidth || 1080) - 360)); }
/* 方案Resize开始（planResizeStart）：调用 planResizeStart 完成方案Resize开始处理。 */
function planResizeStart(event) {
  /* 检查条件 event.button !== 0 || window.matchMedia('(max-width: 1199px)').matches 是否成立，成立时走这一分支。 */
  if (event.button !== 0 || window.matchMedia('(max-width: 1199px)').matches) /* 返回空内容，当前函数到此结束。 */ return;
  const /* 实际（actual）保存沿 planLayout.value?.querySelector('.agent-plan-workspace')?.getBoundingClientRect().width || planWidth.value 读取数据；中间对象为空时返回空值。 */ actual = planLayout.value?.querySelector('.agent-plan-workspace')?.getBoundingClientRect().width || planWidth.value;
  /* 把表达式 { pointer: event.pointerId, x: event.clientX, width: actual } 的结果写入 planDrag。 */
  planDrag = { /* pointer字段使用表达式 event.pointerId 的结果。 */ pointer: event.pointerId, /* x字段使用表达式 event.clientX 的结果。 */ x: event.clientX, /* 宽度（width）字段使用实际（actual）。 */ width: actual };
  /* 设置 PointerCapture 的值。 */
  event.currentTarget.setPointerCapture(event.pointerId); /* 把真，表示条件成立或开关打开写入 resizingPlan.value。 */ resizingPlan.value = true; /* 调用 event.preventDefault 完成prevent默认值处理。 */ event.preventDefault();
}
/* 方案ResizeMove（planResizeMove）：调用 planResizeMove 完成方案ResizeMove处理。 */
function planResizeMove(event) {
  /* 检查条件 !planDrag || planDrag.pointer !== event.pointerId 是否成立，成立时走这一分支。 */
  if (!planDrag || planDrag.pointer !== event.pointerId) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把调用 Math.round 完成轮处理写入 planWidth.value。 */
  planWidth.value = Math.round(Math.max(320, Math.min(planWidthLimit(), planDrag.width + planDrag.x - event.clientX)));
}
/* 方案Resize结束（planResizeEnd）：调用 planResizeEnd 完成方案Resize结束处理。 */
function planResizeEnd() { /* 检查条件 planDrag 是否成立，成立时走这一分支。 */ if (planDrag) { /* 把空值，表示当前还没有对应内容写入 planDrag。 */ planDrag = null; /* 把假，表示条件不成立或开关关闭写入 resizingPlan.value。 */ resizingPlan.value = false; /* 保存 方案宽度（PlanWidth）。 */ savePlanWidth(); } }
/* 方案Resize键（planResizeKey）：调用 planResizeKey 完成方案Resize键处理。 */
function planResizeKey(event) {
  /* 检查条件 !['ArrowLeft', 'ArrowRight', 'Home', 'End', 'Enter'].includes(event.key) 是否成立，成立时走这一分支。 */
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End', 'Enter'].includes(event.key)) /* 返回空内容，当前函数到此结束。 */ return;
  /* 调用 event.preventDefault 完成prevent默认值处理。 */
  event.preventDefault();
  const /* step保存根据前面的条件，从两个候选结果中选择一个：event.shiftKey ? 32 : 16。 */ step = event.shiftKey ? 32 : 16;
  /* 把根据前面的条件，从两个候选结果中选择一个：event.key === 'Home' ? 320 : event.key === 'End' ? planWidthLimit() : event.key === 'Enter' ? Math.m…写入 planWidth.value。 */
  planWidth.value = event.key === 'Home' ? 320 : event.key === 'End' ? planWidthLimit() : event.key === 'Enter' ? Math.min(400, planWidthLimit())
    : Math.max(320, Math.min(planWidthLimit(), planWidth.value + (event.key === 'ArrowLeft' ? step : -step)));
  /* 保存 方案宽度（PlanWidth）。 */
  savePlanWidth();
}
/* reset方案宽度（resetPlanWidth）：调用 resetPlanWidth 完成reset方案宽度处理。 */
function resetPlanWidth() { /* 把调用 Math.min 完成最小处理写入 planWidth.value。 */ planWidth.value = Math.min(400, planWidthLimit()); /* 保存 方案宽度（PlanWidth）。 */ savePlanWidth(); }
let /* presented方案编号（presentedPlanId）保存固定文字 ''。 */ presentedPlanId = "";
/* 方案Availability（planAvailability）：调用 planAvailability 完成方案Availability处理。 */
function planAvailability(info) {
  /* 把表达式 info.available 的结果写入 planAvailable.value。 */
  planAvailable.value = info.available;
  /* 检查条件 info.available && info.id !== presentedPlanId && ["DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING", "FAILED"].includes(info.status) 是否成立，成立时走这一分支。 */
  if (info.available && info.id !== presentedPlanId && ["DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING", "FAILED"].includes(info.status)) {
    /* 把表达式 info.id 的结果写入 presentedPlanId。 */
    presentedPlanId = info.id;
    /* 把真，表示条件成立或开关打开写入 planOpen.value。 */
    planOpen.value = true;
  }
}
/* revise方案（revisePlan）：调用 revisePlan 完成revise方案处理。 */
async function revisePlan(text) { /* 把文字（text）写入 draft.value。 */ draft.value = text; /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */ await nextTick(); /* 调用 pane.value?.focus 完成focus处理。 */ pane.value?.focus(); }
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(activeId, () => { /* 把条件判断的结果：!!externalReviewId.value写入 planOpen.value。 */ planOpen.value = !!externalReviewId.value; /* 把假，表示条件不成立或开关关闭写入 planAvailable.value。 */ planAvailable.value = false; /* 把空值，表示当前还没有对应内容写入 planEvent.value。 */ planEvent.value = null; /* 把空值，表示当前还没有对应内容写入 reviewEvent.value。 */ reviewEvent.value = null; /* 把固定文字 ''写入 presentedPlanId。 */ presentedPlanId = ""; });
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(externalReviewId, id => { /* 检查条件 id 是否成立，成立时走这一分支。 */ if (id) /* 把真，表示条件成立或开关打开写入 planOpen.value。 */ planOpen.value = true; }, { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true });

let /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */ disposed = false;
let /* 列表请求序号（listSequence）保存固定数值 0。 */ listSequence = 0;
let /* 列表请求入口（listController）保存空值，表示当前还没有对应内容。 */ listController = null;
let /* 是否展开请求序号（openSequence）保存固定数值 0。 */ openSequence = 0;
let /* 分段回复流请求入口（streamController）保存空值，表示当前还没有对应内容。 */ streamController = null;
let /* 当前有效本轮对话（activeTurn）保存空值，表示当前还没有对应内容。 */ activeTurn = null;
let /* recovery定时器（recoveryTimer）保存固定数值 0。 */ recoveryTimer = 0;

/* filtered：调用 filtered 完成filtered处理。 */
function filtered() {
  /* 返回去掉文字两端的空白，当前函数到此结束。 */
  return !!filters.keyword.trim();
}
/* 标题来源（titleFrom）：调用 titleFrom 完成标题来源处理。 */
function titleFrom(text) {
  const /* clean保存生成保留原对象其余字段的新副本，只替换明确指定的值。 */ clean = text.replace(/\s+/g, " ").trim();
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：Array.from(clean).slice(0, 24).join("") || TITLE_DEFAULT，当前函数到此结束。 */
  return Array.from(clean).slice(0, 24).join("") || TITLE_DEFAULT;
}
/* to当前记录（toRow）：调用 toRow 完成to当前记录处理。 */
function toRow(detail) {
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：{ id: String(detail.id), title: detail.title, lastMessageAt: detail.lastMessageAt || null, version: Number(det…，当前函数到此结束。 */
  return {
    /* 编号（id）字段使用把输入转换成文字，统一比较或展示时的格式。 */
    id: String(detail.id),
    /* 标题（title）字段使用表达式 detail.title 的结果。 */
    title: detail.title,
    /* 最后一次消息At（lastMessageAt）字段使用优先使用前面的有效值，没有时采用后面的备用值：detail.lastMessageAt || null。 */
    lastMessageAt: detail.lastMessageAt || null,
    /* 版本号（version）字段使用优先使用前面的有效值，没有时采用后面的备用值：Number(detail.version) || 0。 */
    version: Number(detail.version) || 0,
  };
}
/* to消息（toMessage）：调用 toMessage 完成to消息处理。 */
function toMessage(row) {
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：{ id: String(row.id), messageNo: Number(row.messageNo) || 0, role: row.role, content: row.content || "", // Ja…，当前函数到此结束。 */
  return {
    /* 编号（id）字段使用把输入转换成文字，统一比较或展示时的格式。 */
    id: String(row.id),
    /* 消息No（messageNo）字段使用优先使用前面的有效值，没有时采用后面的备用值：Number(row.messageNo) || 0。 */
    messageNo: Number(row.messageNo) || 0,
    /* 角色（role）字段使用表达式 row.role 的结果。 */
    role: row.role,
    /* 内容（content）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.content || ""。 */
    content: row.content || "",
    // Java 公开 VO 字段是 complete；Python 内部的 isComplete 不对外暴露。
    /* 完成（complete）字段使用条件判断的结果：row.complete === true。 */
    complete: row.complete === true,
    /* finish原因（finishReason）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.finishReason || ""。 */
    finishReason: row.finishReason || "",
    /* 模型（model）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.model || ""。 */
    model: row.model || "",
    /* 用量（usage）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.usage || null。 */
    usage: row.usage || null,
    /* citations字段使用优先使用前面的有效值，没有时采用后面的备用值：row.citations || []。 */
    citations: row.citations || [],
    /* used长期记忆编号列表（usedMemoryIds）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.usedMemoryIds || []。 */
    usedMemoryIds: row.usedMemoryIds || [],
    /* 创建时At（createdAt）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.createdAt || ""。 */
    createdAt: row.createdAt || "",
    /* 等待处理（pending）字段使用假，表示条件不成立或开关关闭。 */
    pending: false,
    /* stopped字段使用假，表示条件不成立或开关关闭。 */
    stopped: false,
    /* 错误（error）字段使用固定文字 ''。 */
    error: "",
    /* 本次请求的追踪编号（traceId）字段使用固定文字 ''。 */
    traceId: "",
    /* 业务工具列表（tools）字段使用一个空列表，用来保存后续多条数据。 */
    tools: [],
    /* activities字段使用一个空列表，用来保存后续多条数据。 */
    activities: [],
  };
}
/* merge记录列表（mergeRows）：调用 mergeRows 完成merge记录列表处理。 */
function mergeRows(existing, incoming) {
  const /* 已读（seen）保存新建 设置（Set） 实例。 */ seen = new Set(existing.map((row) => row.id));
  /* 返回只保留符合条件的条目，当前函数到此结束。 */
  return [...existing, ...incoming.filter((row) => !seen.has(row.id))];
}
/* 更新当前记录（updateRow）：更新 当前记录（Row）。 */
function updateRow(detail) {
  /* 检查条件 !detail 是否成立，成立时走这一分支。 */
  if (!detail) /* 返回空内容，当前函数到此结束。 */ return;
  const /* 当前记录（row）保存调用 toRow 完成to当前记录处理。 */ row = toRow(detail);
  const /* 索引（index）保存查找 索引（Index）。 */ index = list.rows.findIndex((item) => item.id === row.id);
  /* 检查条件 index < 0 是否成立，成立时走这一分支。 */
  if (index < 0) /* 返回空内容，当前函数到此结束。 */ return;
  /* 调用 list.rows.splice 完成splice处理。 */
  list.rows.splice(index, 1);
  /* 调用 list.rows.splice 完成splice处理。 */
  list.rows.splice(filtered() ? index : 0, 0, row);
}
/* 加入创建时当前记录（addCreatedRow）：调用 addCreatedRow 完成加入创建时当前记录处理。 */
function addCreatedRow(detail) {
  const /* 当前记录（row）保存调用 toRow 完成to当前记录处理。 */ row = toRow(detail);
  /* 检查条件 list.rows.some((item) => item.id === row.id) || filtered() 是否成立，成立时走这一分支。 */
  if (list.rows.some((item) => item.id === row.id) || filtered()) /* 返回空内容，当前函数到此结束。 */ return;
  /* 调用 list.rows.unshift 完成unshift处理。 */
  list.rows.unshift(row);
  /* 更新本次处理状态：list.total += 1。 */
  list.total += 1;
  /* 把集合或文字的长度写入 list.hasMore。 */
  list.hasMore = list.rows.length < list.total;
}
/* 当前有效当前记录（activeRow）：调用 activeRow 完成当前有效当前记录处理。 */
function activeRow() {
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  /* 返回根据前面的条件，从两个候选结果中选择一个：( list.rows.find((row) => row.id === id) || (conversation.value ? toRow(conversation.value) : null) …，当前函数到此结束。 */
  return (
    list.rows.find((row) => row.id === id) ||
    (conversation.value ? toRow(conversation.value) : null)
  );
}

const /* 对话轮次（turns）保存随相关状态变化自动重算的值。 */ turns = computed(() => {
  const /* 结果（result）保存一个空列表，用来保存后续多条数据。 */ result = [];
  let /* 当前（current）保存空值，表示当前还没有对应内容。 */ current = null;
  /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
  for (const /* 声明 消息（message），稍后再填入处理结果。 */ message of messages.value) {
    /* 检查条件 message.role === "USER" 是否成立，成立时走这一分支。 */
    if (message.role === "USER") {
      /* 把表达式 { id: message.id, question: message.content, answer: null } 的结果写入 current。 */
      current = { /* 编号（id）字段使用表达式 message.id 的结果。 */ id: message.id, /* question字段使用表达式 message.content 的结果。 */ question: message.content, /* answer字段使用空值，表示当前还没有对应内容。 */ answer: null };
      /* 调用 result.push 完成push处理。 */
      result.push(current);
      continue;
    }
    /* 检查条件 message.role === "ASSISTANT" && current && !current.answer 是否成立，成立时走这一分支。 */
    if (message.role === "ASSISTANT" && current && !current.answer) {
      /* 把消息（message）写入 current.answer。 */
      current.answer = message;
      /* 把空值，表示当前还没有对应内容写入 current。 */
      current = null;
    }
  }
  /* 返回结果（result），当前函数到此结束。 */
  return result;
});


/* 加载对话列表（loadConversations）：加载 对话列表（Conversations）。 */
async function loadConversations({ /* 直接使用已有的reset不成立（reset = false）作为同名字段。 */ reset = false } = {}) {
  /* 检查条件 !canList.value 是否成立，成立时走这一分支。 */
  if (!canList.value) {
    /* 把固定文字 '当前账号尚未开通会话列表权限'写入 list.error。 */
    list.error = "当前账号尚未开通会话列表权限";
    /* 把真，表示条件成立或开关打开写入 list.loaded。 */
    list.loaded = true;
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  const /* 请求（request）保存按表达式计算得到的值：++listSequence。 */ request = ++listSequence;
  const /* 当前页（page）保存根据前面的条件，从两个候选结果中选择一个：reset ? 1 : list.page。 */ page = reset ? 1 : list.page;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  listController?.abort();
  /* 把新建 Abort请求入口（AbortController） 实例写入 listController。 */
  listController = new AbortController();
  /* 检查条件 reset 是否成立，成立时走这一分支。 */
  if (reset) /* 把固定数值 1写入 list.page。 */ list.page = 1;
  /* 把真，表示条件成立或开关打开写入 list.busy。 */
  list.busy = true;
  /* 把固定文字 ''写入 list.error。 */
  list.error = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 agentApi.conversations 完成对话列表处理 完成后取得结果。 */ data = await agentApi.conversations(
      {
        /* 直接使用已有的当前页（page）作为同名字段。 */
        page,
        /* 每页条数（size）字段使用表达式 list.size 的结果。 */
        size: list.size,
        /* keyword字段使用去掉文字两端的空白。 */
        keyword: filters.keyword.trim(),
      },
      listController.signal,
    );
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || request !== listSequence) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 数据列表（items）保存根据前面的条件，从两个候选结果中选择一个：Array.isArray(data.items) ? data.items : []。 */ items = Array.isArray(data.items) ? data.items : [];
    /* 把根据前面的条件，从两个候选结果中选择一个：reset ? items : mergeRows(list.rows, items)写入 list.rows。 */
    list.rows = reset ? items : mergeRows(list.rows, items);
    /* 把优先使用前面的有效值，没有时采用后面的备用值：Number(data.total) || 0写入 list.total。 */
    list.total = Number(data.total) || 0;
    /* 把集合或文字的长度写入 list.hasMore。 */
    list.hasMore = list.rows.length < list.total;
    /* 把真，表示条件成立或开关打开写入 list.loaded。 */
    list.loaded = true;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || request !== listSequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 error.name === "AbortError" || error.code === "SESSION_CHANGED" 是否成立，成立时走这一分支。 */
    if (error.name === "AbortError" || error.code === "SESSION_CHANGED") /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 !reset && page > 1 && list.page === page 是否成立，成立时走这一分支。 */
    if (!reset && page > 1 && list.page === page) /* 把按表达式计算得到的值：page - 1写入 list.page。 */ list.page = page - 1;
    /* 把根据前面的条件，从两个候选结果中选择一个：error.status === 403 ? "当前账号尚未开通会话列表权限" : error.message || "会话列表加载失败，请重试"写入 list.error。 */
    list.error =
      error.status === 403
        ? "当前账号尚未开通会话列表权限"
        : error.message || "会话列表加载失败，请重试";
    /* 把真，表示条件成立或开关打开写入 list.loaded。 */
    list.loaded = true;
  } finally {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (!disposed && request === listSequence) /* 把假，表示条件不成立或开关关闭写入 list.busy。 */ list.busy = false;
  }
}
/* 加载More（loadMore）：加载 More。 */
async function loadMore() {
  /* 检查条件 list.busy || !list.hasMore || !canList.value 是否成立，成立时走这一分支。 */
  if (list.busy || !list.hasMore || !canList.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 更新本次处理状态：list.page += 1。 */
  list.page += 1;
  /* 等待加载 对话列表（Conversations）。 */
  await loadConversations();
}

/* 清除当前有效（clearActive）：调用 clearActive 完成清除当前有效处理。 */
function clearActive() {
  /* 更新本次处理状态：openSequence += 1。 */
  openSequence += 1;
  /* 调用 stopRecovery 完成停止Recovery处理。 */
  stopRecovery();
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  streamController?.abort();
  /* 把空值，表示当前还没有对应内容写入 streamController。 */
  streamController = null;
  /* 把空值，表示当前还没有对应内容写入 activeTurn。 */
  activeTurn = null;
  /* 把固定文字 ''写入 activeId.value。 */
  activeId.value = "";
  /* 把空值，表示当前还没有对应内容写入 conversation.value。 */
  conversation.value = null;
  draftKnowledgeBaseIds.value = [];
  /* 把一个空列表，用来保存后续多条数据写入 messages.value。 */
  messages.value = [];
  /* 把假，表示条件不成立或开关关闭写入 hasMore.value。 */
  hasMore.value = false;
  /* 把空值，表示当前还没有对应内容写入 nextBefore.value。 */
  nextBefore.value = null;
  /* 把假，表示条件不成立或开关关闭写入 loadingConversation.value。 */
  loadingConversation.value = false;
  /* 把假，表示条件不成立或开关关闭写入 olderBusy.value。 */
  olderBusy.value = false;
  /* 移除 单条数据（Item）。 */
  sessionStorage.removeItem(STORAGE_KEY);
}

/* 是否展开对话（openConversation）：调用 openConversation 完成是否展开对话处理。 */
async function openConversation(id) {
  const /* 值（value）保存优先使用前面的有效值，没有时采用后面的备用值：String(id || "")。 */ value = String(id || "");
  /* 检查条件 !value 是否成立，成立时走这一分支。 */
  if (!value) /* 返回空内容，当前函数到此结束。 */ return;
  const /* 请求（request）保存按表达式计算得到的值：++openSequence。 */ request = ++openSequence;
  /* 调用 stopRecovery 完成停止Recovery处理。 */
  stopRecovery();
  /* 把值（value）写入 activeId.value。 */
  activeId.value = value;
  /* 把空值，表示当前还没有对应内容写入 conversation.value。 */
  conversation.value = null;
  /* 把一个空列表，用来保存后续多条数据写入 messages.value。 */
  messages.value = [];
  /* 把假，表示条件不成立或开关关闭写入 hasMore.value。 */
  hasMore.value = false;
  /* 把空值，表示当前还没有对应内容写入 nextBefore.value。 */
  nextBefore.value = null;
  /* 把真，表示条件成立或开关打开写入 loadingConversation.value。 */
  loadingConversation.value = true;
  /* 设置 单条数据（Item） 的值。 */
  sessionStorage.setItem(STORAGE_KEY, value);
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 详情当前页（[detail, page]）保存等待 调用 Promise.all 完成全部处理 完成后取得结果。 */ [detail, page] = await Promise.all([
      agentApi.conversation(value),
      agentApi.messages(value, { /* 每页条数（size）字段使用消息每页条数（MESSAGE_SIZE）。 */ size: MESSAGE_SIZE }),
    ]);
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || request !== openSequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把详情（detail）写入 conversation.value。 */
    conversation.value = detail;
    /* 把优先使用前面的有效值，没有时采用后面的备用值：(page.items || []).map(toMessage)写入 messages.value。 */
    messages.value = (page.items || []).map(toMessage);
    /* 把条件判断的结果：page.hasMore === true写入 hasMore.value。 */
    hasMore.value = page.hasMore === true;
    /* 把前面的值为空时才使用后面的默认值：page.nextBeforeMessageNo ?? null写入 nextBefore.value。 */
    nextBefore.value = page.nextBeforeMessageNo ?? null;
    /* 检查条件 detail.runningTurn?.status === "RUNNING" 是否成立，成立时走这一分支。 */
    if (detail.runningTurn?.status === "RUNNING")
      /* 调用 scheduleRecovery 完成安排Recovery处理。 */
      scheduleRecovery(detail.runningTurn);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || request !== openSequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 error.status === 404 是否成立，成立时走这一分支。 */
    if (error.status === 404) {
      /* 调用 ElMessage.info 完成信息处理。 */
      ElMessage.info("会话不存在或已被删除");
      /* 调用 clearActive 完成清除当前有效处理。 */
      clearActive();
      /* 等待加载 对话列表（Conversations）。 */
      await loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
      /* 返回空内容，当前函数到此结束。 */
      return;
    }
    /* 检查条件 error.status === 403 是否成立，成立时走这一分支。 */
    if (error.status === 403) /* 调用 ElMessage.error 完成错误处理。 */ ElMessage.error("当前账号没有读取该会话的权限");
    else /* 调用 report 完成复盘报告处理。 */ report(error);
    /* 调用 clearActive 完成清除当前有效处理。 */
    clearActive();
  } finally {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (!disposed && request === openSequence)
      /* 把假，表示条件不成立或开关关闭写入 loadingConversation.value。 */
      loadingConversation.value = false;
  }
}
/* 选择对话（selectConversation）：调用 selectConversation 完成选择对话处理。 */
async function selectConversation(id) {
  const /* 值（value）保存优先使用前面的有效值，没有时采用后面的备用值：String(id || "")。 */ value = String(id || "");
  /* 释放连接、文件或客户端占用的资源。 */
  sidebar.close();
  /* 检查条件 !value || value === activeId.value 是否成立，成立时走这一分支。 */
  if (!value || value === activeId.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 sending.value 是否成立，成立时走这一分支。 */
  if (sending.value) {
    /* 调用 ElMessage.info 完成信息处理。 */
    ElMessage.info("请先停止当前生成，再切换会话");
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 等待调用 openConversation 完成是否展开对话处理。 */
  await openConversation(value);
}
/* 刷新当前有效（refreshActive）：刷新 页面是否仍处于活动状态（Active）。 */
async function refreshActive({ /* 直接使用已有的恢复成立（recover = true）作为同名字段。 */ recover = true } = {}) {
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
  if (!id || disposed) /* 返回空值，表示当前还没有对应内容，当前函数到此结束。 */ return null;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 详情（detail）保存等待 调用 agentApi.conversation 完成对话处理 完成后取得结果。 */ detail = await agentApi.conversation(id);
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || activeId.value !== id) /* 返回空值，表示当前还没有对应内容，当前函数到此结束。 */ return null;
    /* 把详情（detail）写入 conversation.value。 */
    conversation.value = detail;
    /* 更新 当前记录（Row）。 */
    updateRow(detail);
    /* 检查条件 recover && detail.runningTurn?.status === "RUNNING" && !generating.value 是否成立，成立时走这一分支。 */
    if (
      recover &&
      detail.runningTurn?.status === "RUNNING" &&
      !generating.value
    )
      /* 调用 scheduleRecovery 完成安排Recovery处理。 */
      scheduleRecovery(detail.runningTurn);
    /* 返回详情（detail），当前函数到此结束。 */
    return detail;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || activeId.value !== id) /* 返回空值，表示当前还没有对应内容，当前函数到此结束。 */ return null;
    /* 检查条件 error.status === 404 是否成立，成立时走这一分支。 */
    if (error.status === 404) {
      /* 调用 ElMessage.info 完成信息处理。 */
      ElMessage.info("会话不存在或已被删除");
      /* 调用 clearActive 完成清除当前有效处理。 */
      clearActive();
      /* 等待加载 对话列表（Conversations）。 */
      await loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
    }
    /* 返回空值，表示当前还没有对应内容，当前函数到此结束。 */
    return null;
  }
}

/* 加载Older消息列表（loadOlderMessages）：加载 Older消息列表（OlderMessages）。 */
async function loadOlderMessages() {
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  /* 检查条件 !id || olderBusy.value || !hasMore.value || loadingConversation.value 是否成立，成立时走这一分支。 */
  if (!id || olderBusy.value || !hasMore.value || loadingConversation.value)
    /* 返回空内容，当前函数到此结束。 */
    return;
  /* 把真，表示条件成立或开关打开写入 olderBusy.value。 */
  olderBusy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 当前页（page）保存等待 调用 agentApi.messages 完成消息列表处理 完成后取得结果。 */ page = await agentApi.messages(id, {
      /* 变化前消息No（beforeMessageNo）字段使用表达式 nextBefore.value 的结果。 */
      beforeMessageNo: nextBefore.value,
      /* 每页条数（size）字段使用消息每页条数（MESSAGE_SIZE）。 */
      size: MESSAGE_SIZE,
    });
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || activeId.value !== id) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 已读（seen）保存新建 设置（Set） 实例。 */ seen = new Set(messages.value.map((message) => message.id));
    const /* older保存优先使用前面的有效值，没有时采用后面的备用值：(page.items || []) .map(toMessage) .filter((message) => !seen.has(message.id))。 */ older = (page.items || [])
      .map(toMessage)
      .filter((message) => !seen.has(message.id));
    /* 把表达式 [...older, ...messages.value] 的结果写入 messages.value。 */
    messages.value = [...older, ...messages.value];
    /* 把条件判断的结果：page.hasMore === true写入 hasMore.value。 */
    hasMore.value = page.hasMore === true;
    /* 把前面的值为空时才使用后面的默认值：page.nextBeforeMessageNo ?? null写入 nextBefore.value。 */
    nextBefore.value = page.nextBeforeMessageNo ?? null;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (!disposed && error.status !== 401) /* 调用 report 完成复盘报告处理。 */ report(error);
  } finally {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (!disposed) /* 把假，表示条件不成立或开关关闭写入 olderBusy.value。 */ olderBusy.value = false;
  }
}
/* 刷新最新消息列表（refreshLatestMessages）：刷新 最新消息列表（LatestMessages）。 */
async function refreshLatestMessages() {
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  /* 检查条件 !id 是否成立，成立时走这一分支。 */
  if (!id) /* 返回空内容，当前函数到此结束。 */ return;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 当前页（page）保存等待 调用 agentApi.messages 完成消息列表处理 完成后取得结果。 */ page = await agentApi.messages(id, { /* 每页条数（size）字段使用消息每页条数（MESSAGE_SIZE）。 */ size: MESSAGE_SIZE });
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || activeId.value !== id) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 上一次Answer（previousAnswer）保存只保留符合条件的条目。 */ previousAnswer = messages.value.filter(item => item.role === "ASSISTANT").at(-1);
    const /* incoming保存沿 (page.items || []).map(row => { const item = toMessage(row); const previous = messages.value.find(old => old.id ===… 读取数据；中间对象为空时返回空值。 */ incoming = (page.items || []).map(row => {
      const /* 单条数据（item）保存调用 toMessage 完成to消息处理。 */ item = toMessage(row);
      const /* 上一次（previous）保存沿 messages.value.find(old => old.id === item.id) || (item.role === "ASSISTANT" && row === page.items?.at(-1) ? previo… 读取数据；中间对象为空时返回空值。 */ previous = messages.value.find(old => old.id === item.id)
        || (item.role === "ASSISTANT" && row === page.items?.at(-1) ? previousAnswer : null);
      /* 检查条件 previous 是否成立，成立时走这一分支。 */
      if (previous) { /* 把优先使用前面的有效值，没有时采用后面的备用值：previous.tools || []写入 item.tools。 */ item.tools = previous.tools || []; /* 把优先使用前面的有效值，没有时采用后面的备用值：previous.activities || []写入 item.activities。 */ item.activities = previous.activities || []; }
      /* 返回单条数据（item），当前函数到此结束。 */
      return item;
    });
    const /* 编号列表（ids）保存新建 设置（Set） 实例。 */ ids = new Set(incoming.map((message) => message.id));
    const /* kept保存只保留符合条件的条目。 */ kept = messages.value.filter(
      (message) => !message.id.startsWith("local-") && !ids.has(message.id),
    );
    /* 把按指定规则排列数据写入 messages.value。 */
    messages.value = [...kept, ...incoming].sort(
      (left, right) => left.messageNo - right.messageNo,
    );
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    // 最新页回读失败不影响已展示的流式正文。
  }
}

/* 停止Recovery（stopRecovery）：调用 stopRecovery 完成停止Recovery处理。 */
function stopRecovery() {
  /* 检查条件 recoveryTimer 是否成立，成立时走这一分支。 */
  if (recoveryTimer) /* 取消尚未触发的一次性定时器。 */ clearTimeout(recoveryTimer);
  /* 把固定数值 0写入 recoveryTimer。 */
  recoveryTimer = 0;
  /* 把假，表示条件不成立或开关关闭写入 recovering.value。 */
  recovering.value = false;
  /* 把空值，表示当前还没有对应内容写入 recoveryTurn.value。 */
  recoveryTurn.value = null;
}
/* 通知父组件Recovery（announceRecovery）：调用 announceRecovery 完成通知父组件Recovery处理。 */
function announceRecovery(turn) {
  const /* 消息列表按状态（messagesByStatus）保存表达式 { COMPLETED: ["success", "上一轮回复已完成"], CANCELLED: ["warning", "上一轮回复已停止"], FAILED: ["warning", "上一轮回复生成失败，已保留部分内容"], INCO… 的结果。 */ messagesByStatus = {
    /* 已完成（COMPLETED）字段使用表达式 ["success", "上一轮回复已完成"] 的结果。 */
    COMPLETED: ["success", "上一轮回复已完成"],
    /* 已取消（CANCELLED）字段使用表达式 ["warning", "上一轮回复已停止"] 的结果。 */
    CANCELLED: ["warning", "上一轮回复已停止"],
    /* 失败（FAILED）字段使用表达式 ["warning", "上一轮回复生成失败，已保留部分内容"] 的结果。 */
    FAILED: ["warning", "上一轮回复生成失败，已保留部分内容"],
    /* INCOMPLETE字段使用表达式 ["warning", "上一轮回复未完整结束"] 的结果。 */
    INCOMPLETE: ["warning", "上一轮回复未完整结束"],
  };
  const /* 单条数据（item）保存沿 messagesByStatus[turn?.status] 读取数据；中间对象为空时返回空值。 */ item = messagesByStatus[turn?.status];
  /* 检查条件 item 是否成立，成立时走这一分支。 */
  if (item) /* 调用 ElMessage 完成El消息处理。 */ ElMessage({ /* 类型（type）字段使用表达式 item[0] 的结果。 */ type: item[0], /* 消息（message）字段使用表达式 item[1] 的结果。 */ message: item[1] });
}
/* 安排Recovery（scheduleRecovery）：调用 scheduleRecovery 完成安排Recovery处理。 */
function scheduleRecovery(turn) {
  /* 调用 stopRecovery 完成停止Recovery处理。 */
  stopRecovery();
  /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
  if (!turn || turn.status !== "RUNNING" || disposed) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把真，表示条件成立或开关打开写入 recovering.value。 */
  recovering.value = true;
  /* 把本轮对话（turn）写入 recoveryTurn.value。 */
  recoveryTurn.value = turn;
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  const /* tick保存优先使用前面的有效值，没有时采用后面的备用值：async () => { recoveryTimer = 0; if (disposed || !recovering.value) return; if (!id || activeId.value !== id) …。 */ tick = async () => {
    /* 把固定数值 0写入 recoveryTimer。 */
    recoveryTimer = 0;
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (disposed || !recovering.value) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 !id || activeId.value !== id 是否成立，成立时走这一分支。 */
    if (!id || activeId.value !== id) /* 返回调用 stopRecovery 完成停止Recovery处理，当前函数到此结束。 */ return stopRecovery();
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      const /* 最新（latest）保存等待 调用 agentApi.turn 完成本轮对话处理 完成后取得结果。 */ latest = await agentApi.turn(id, turn.id);
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (disposed || !recovering.value || activeId.value !== id) /* 返回空内容，当前函数到此结束。 */ return;
      /* 把最新（latest）写入 recoveryTurn.value。 */
      recoveryTurn.value = latest;
      /* 检查条件 latest.status === "RUNNING" 是否成立，成立时走这一分支。 */
      if (latest.status === "RUNNING") {
        /* 把过指定毫秒后执行一次处理写入 recoveryTimer。 */
        recoveryTimer = setTimeout(tick, RECOVERY_DELAY);
        /* 返回空内容，当前函数到此结束。 */
        return;
      }
      /* 调用 stopRecovery 完成停止Recovery处理。 */
      stopRecovery();
      /* 等待刷新 最新消息列表（LatestMessages）。 */
      await refreshLatestMessages();
      /* 等待刷新 页面是否仍处于活动状态（Active）。 */
      await refreshActive();
      /* 调用 announceRecovery 完成通知父组件Recovery处理。 */
      announceRecovery(latest);
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
      /* 调用 stopRecovery 完成停止Recovery处理。 */
      stopRecovery();
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (!disposed && error.status !== 401) /* 调用 report 完成复盘报告处理。 */ report(error);
    }
  };
  /* 把过指定毫秒后执行一次处理写入 recoveryTimer。 */
  recoveryTimer = setTimeout(tick, 800);
}
/* 停止Recovery本轮对话（stopRecoveryTurn）：调用 stopRecoveryTurn 完成停止Recovery本轮对话处理。 */
async function stopRecoveryTurn() {
  const /* 本轮对话（turn）保存表达式 recoveryTurn.value 的结果。 */ turn = recoveryTurn.value;
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  /* 检查条件 !turn || !id 是否成立，成立时走这一分支。 */
  if (!turn || !id) /* 返回空内容，当前函数到此结束。 */ return;
  /* 调用 stopRecovery 完成停止Recovery处理。 */
  stopRecovery();
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待取消 本轮对话（Turn）。 */
    await agentApi.cancelTurn(id, turn.id, turn.version);
    /* 调用 ElMessage.info 完成信息处理。 */
    ElMessage.info("已停止上一轮生成");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查条件 error.status !== 409 是否成立，成立时走这一分支。 */
    if (error.status !== 409) /* 调用 report 完成复盘报告处理。 */ report(error);
  }
  /* 等待刷新 最新消息列表（LatestMessages）。 */
  await refreshLatestMessages();
  /* 等待刷新 页面是否仍处于活动状态（Active）。 */
  await refreshActive();
}

/* 创建对话（createConversation）：创建 对话（Conversation）。 */
async function createConversation() {
  /* 检查条件 !canCreate.value 是否成立，成立时走这一分支。 */
  if (!canCreate.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 creating.value 是否成立，成立时走这一分支。 */
  if (creating.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 sending.value 是否成立，成立时走这一分支。 */
  if (sending.value) {
    /* 调用 ElMessage.info 完成信息处理。 */
    ElMessage.info("请先停止当前生成，再新建会话");
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 调用 clearActive 完成清除当前有效处理。 */
  clearActive();
  /* 把固定文字 ''写入 draft.value。 */
  draft.value = "";
  /* 释放连接、文件或客户端占用的资源。 */
  sidebar.close();
  /* 检查条件 route.path !== "/agent" || route.query.conversationId 是否成立，成立时走这一分支。 */
  if (route.path !== "/agent" || route.query.conversationId) /* 等待调用 router.push 完成push处理。 */ await router.push("/agent");
  /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */
  await nextTick();
  /* 调用 pane.value?.focus 完成focus处理。 */
  pane.value?.focus();
}
/* 重命名对话（renameConversation）：调用 renameConversation 完成重命名对话处理。 */
async function renameConversation(row) {
  /* 检查条件 !row || !canUpdate.value 是否成立，成立时走这一分支。 */
  if (!row || !canUpdate.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 sending.value 是否成立，成立时走这一分支。 */
  if (sending.value) /* 返回调用 ElMessage.info 完成信息处理，当前函数到此结束。 */ return ElMessage.info("请先停止当前生成，再修改标题");
  let /* 值（value）保存固定文字 ''。 */ value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 调用 ({ value } = await ElMessageBox.prompt 完成模型提示词处理。 */
    ({ /* 直接使用已有的值（value）作为同名字段。 */ value } = await ElMessageBox.prompt("请输入新的会话标题", "重命名会话", {
      /* 确认Button文字（confirmButtonText）字段使用固定文字 '保存'。 */
      confirmButtonText: "保存",
      /* 取消Button文字（cancelButtonText）字段使用固定文字 '取消'。 */
      cancelButtonText: "取消",
      /* 输入值（inputValue）字段使用表达式 row.title 的结果。 */
      inputValue: row.title,
      /* 输入Validator（inputValidator）：调用 inputValidator 完成输入Validator处理。 */
      inputValidator(text) {
        const /* 标题（title）保存优先使用前面的有效值，没有时采用后面的备用值：(text || "").trim()。 */ title = (text || "").trim();
        /* 检查条件 !title 是否成立，成立时走这一分支。 */
        if (!title) /* 返回固定文字 '标题不能为空'，当前函数到此结束。 */ return "标题不能为空";
        /* 检查条件 Array.from(title).length > 100 是否成立，成立时走这一分支。 */
        if (Array.from(title).length > 100) /* 返回固定文字 '标题不能超过 100 字'，当前函数到此结束。 */ return "标题不能超过 100 字";
        /* 返回真，表示条件成立或开关打开，当前函数到此结束。 */
        return true;
      },
    }));
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  const /* 标题（title）保存优先使用前面的有效值，没有时采用后面的备用值：(value || "").trim()。 */ title = (value || "").trim();
  /* 检查条件 title === row.title 是否成立，成立时走这一分支。 */
  if (title === row.title) /* 返回空内容，当前函数到此结束。 */ return;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 详情（detail）保存等待 调用 agentApi.renameConversation 完成重命名对话处理 完成后取得结果。 */ detail = await agentApi.renameConversation(
      row.id,
      title,
      row.version,
    );
    /* 检查条件 activeId.value === String(row.id) 是否成立，成立时走这一分支。 */
    if (activeId.value === String(row.id)) /* 把详情（detail）写入 conversation.value。 */ conversation.value = detail;
    /* 更新 当前记录（Row）。 */
    updateRow(detail);
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("会话已重命名");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 调用 report 完成复盘报告处理。 */
    report(error);
    /* 检查条件 error.status === 409 是否成立，成立时走这一分支。 */
    if (error.status === 409) /* 等待刷新 页面是否仍处于活动状态（Active）。 */ await refreshActive();
  }
}
/* 移除对话（removeConversation）：移除 对话（Conversation）。 */
async function removeConversation(row) {
  /* 检查条件 !row || !canRemove.value 是否成立，成立时走这一分支。 */
  if (!row || !canRemove.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 sending.value 是否成立，成立时走这一分支。 */
  if (sending.value) /* 返回调用 ElMessage.info 完成信息处理，当前函数到此结束。 */ return ElMessage.info("请先停止当前生成，再删除会话");
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction(
      `确定删除会话「${row.title}」？删除后会从列表移除，历史消息仍保留在数据库中。`,
      "删除会话",
    );
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待删除 对话（Conversation）。 */
    await agentApi.deleteConversation(row.id, row.version);
    /* 把只保留符合条件的条目写入 list.rows。 */
    list.rows = list.rows.filter((item) => item.id !== String(row.id));
    /* 把调用 Math.max 完成最大处理写入 list.total。 */
    list.total = Math.max(0, list.total - 1);
    /* 把集合或文字的长度写入 list.hasMore。 */
    list.hasMore = list.rows.length < list.total;
    /* 检查条件 activeId.value === String(row.id) 是否成立，成立时走这一分支。 */
    if (activeId.value === String(row.id)) {
      /* 调用 clearActive 完成清除当前有效处理。 */
      clearActive();
      /* 设置 草案（Draft） 的值。 */
      setDraft("");
    }
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("会话已删除");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 调用 report 完成复盘报告处理。 */
    report(error);
    /* 检查条件 error.status === 409 是否成立，成立时走这一分支。 */
    if (error.status === 409) {
      /* 等待刷新 页面是否仍处于活动状态（Active）。 */
      await refreshActive();
      /* 等待加载 对话列表（Conversations）。 */
      await loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
    }
  }
}

/* 设置草案（setDraft）：设置 草案（Draft） 的值。 */
function setDraft(value) {
  /* 把值（value）写入 draft.value。 */
  draft.value = value;
}
/* 停止分段回复流（stopStream）：调用 stopStream 完成停止分段回复流处理。 */
function stopStream() {
  const /* 本轮对话（turn）保存当前有效本轮对话（activeTurn）。 */ turn = activeTurn;
  const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  streamController?.abort();
  /* 检查条件 turn?.id && id && canSend.value 是否成立，成立时走这一分支。 */
  if (turn?.id && id && canSend.value)
    /* 取消 本轮对话（Turn）。 */
    agentApi.cancelTurn(id, turn.id, turn.version).catch((error) => {
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (!disposed && error.status !== 409) /* 调用 report 完成复盘报告处理。 */ report(error);
    });
}
/* handle停止（handleStop）：调用 handleStop 完成handle停止处理。 */
function handleStop() {
  /* 检查条件 recovering.value 是否成立，成立时走这一分支。 */
  if (recovering.value) /* 调用 stopRecoveryTurn 完成停止Recovery本轮对话处理。 */ stopRecoveryTurn();
  else /* 调用 stopStream 完成停止分段回复流处理。 */ stopStream();
}

/* 发送（send）：调用 send 完成发送处理。 */
async function send(question = draft.value) {
  /* 检查条件 reviewWorking.value 是否成立，成立时走这一分支。 */
  if (reviewWorking.value) /* 返回调用 ElMessage.info 完成信息处理，当前函数到此结束。 */ return ElMessage.info('正在整理复盘调整方案，请完成后继续对话');
  const /* 文字（text）保存优先使用前面的有效值，没有时采用后面的备用值：(question || "").trim()。 */ text = (question || "").trim();
  /* 检查条件 !text || sending.value 是否成立，成立时走这一分支。 */
  if (!text || sending.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 !canSend.value 是否成立，成立时走这一分支。 */
  if (!canSend.value) /* 返回调用 ElMessage.warning 完成warning处理，当前函数到此结束。 */ return ElMessage.warning("当前账号没有发送提问的权限");
  /* 检查条件 Array.from(text).length > 8000 是否成立，成立时走这一分支。 */
  if (Array.from(text).length > 8000)
    /* 返回调用 ElMessage.warning 完成warning处理，当前函数到此结束。 */
    return ElMessage.warning("提问不能超过 8000 字");
  /* 检查条件 recovering.value 是否成立，成立时走这一分支。 */
  if (recovering.value)
    /* 返回调用 ElMessage.info 完成信息处理，当前函数到此结束。 */
    return ElMessage.info("上一轮回复仍在生成，请先等待或停止生成");
  /* 把真，表示条件成立或开关打开写入 sending.value。 */
  const knowledgeForSend = [...(conversation.value?.knowledgeBaseIds || draftKnowledgeBaseIds.value)];
  sending.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    let /* 详情（detail）保存表达式 conversation.value 的结果。 */ detail = conversation.value;
    /* 检查条件 !detail 是否成立，成立时走这一分支。 */
    if (!detail) {
      /* 把等待 创建 对话（Conversation） 完成后取得结果写入 detail。 */
      detail = await agentApi.createConversation(titleFrom(text), knowledgeForSend);
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (disposed) /* 返回空内容，当前函数到此结束。 */ return;
      /* 把详情（detail）写入 conversation.value。 */
      conversation.value = detail;
      /* 把把输入转换成文字，统一比较或展示时的格式写入 activeId.value。 */
      activeId.value = String(detail.id);
      /* 设置 单条数据（Item） 的值。 */
      sessionStorage.setItem(STORAGE_KEY, activeId.value);
      /* 检查条件 filtered() 是否成立，成立时走这一分支。 */
      if (filtered()) /* 等待加载 对话列表（Conversations）。 */ await loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
      else /* 调用 addCreatedRow 完成加入创建时当前记录处理。 */ addCreatedRow(detail);
    } else /* 检查条件 detail.title === TITLE_DEFAULT && canUpdate.value 是否成立，成立时走这一分支。 */ if (detail.title === TITLE_DEFAULT && canUpdate.value) {
      /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
      try {
        /* 把等待 调用 agentApi.renameConversation 完成重命名对话处理 完成后取得结果写入 detail。 */
        detail = await agentApi.renameConversation(
          detail.id,
          titleFrom(text),
          detail.version,
        );
        /* 检查条件 activeId.value === String(detail.id) 是否成立，成立时走这一分支。 */
        if (activeId.value === String(detail.id)) /* 把详情（detail）写入 conversation.value。 */ conversation.value = detail;
        /* 更新 当前记录（Row）。 */
        updateRow(detail);
      } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
        // 标题生成失败不阻断提问。
      }
    }

    const /* 对话编号（conversationId）保存表达式 activeId.value 的结果。 */ conversationId = activeId.value;
    const /* 请求编号（requestId）保存调用 crypto.randomUUID 完成randomUUID处理。 */ requestId = crypto.randomUUID();
    const /* 当前时间（now）保存新建 日期（Date） 实例。 */ now = new Date().toISOString();
    const /* question消息（questionMessage）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ questionMessage = reactive({
      /* 编号（id）字段使用按表达式计算得到的值："local-user-" + requestId。 */
      id: "local-user-" + requestId,
      /* 消息No（messageNo）字段使用按表达式计算得到的值：Number.MAX_SAFE_INTEGER - 1。 */
      messageNo: Number.MAX_SAFE_INTEGER - 1,
      /* 角色（role）字段使用固定状态或编码 USER（用户）。 */
      role: "USER",
      /* 内容（content）字段使用文字（text）。 */
      content: text,
      /* 完成（complete）字段使用真，表示条件成立或开关打开。 */
      complete: true,
      /* finish原因（finishReason）字段使用固定文字 ''。 */
      finishReason: "",
      /* 模型（model）字段使用固定文字 ''。 */
      model: "",
      /* 用量（usage）字段使用空值，表示当前还没有对应内容。 */
      usage: null,
      /* 创建时At（createdAt）字段使用当前时间（now）。 */
      createdAt: now,
      /* 等待处理（pending）字段使用假，表示条件不成立或开关关闭。 */
      pending: false,
      /* stopped字段使用假，表示条件不成立或开关关闭。 */
      stopped: false,
      /* 错误（error）字段使用固定文字 ''。 */
      error: "",
      /* 本次请求的追踪编号（traceId）字段使用固定文字 ''。 */
      traceId: "",
    });
    const /* answer消息（answerMessage）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ answerMessage = reactive({
      /* 编号（id）字段使用按表达式计算得到的值："local-assistant-" + requestId。 */
      id: "local-assistant-" + requestId,
      /* 消息No（messageNo）字段使用表达式 Number.MAX_SAFE_INTEGER 的结果。 */
      messageNo: Number.MAX_SAFE_INTEGER,
      /* 角色（role）字段使用固定状态或编码 ASSISTANT（ASSISTANT）。 */
      role: "ASSISTANT",
      /* 内容（content）字段使用固定文字 ''。 */
      content: "",
      /* 完成（complete）字段使用假，表示条件不成立或开关关闭。 */
      complete: false,
      /* finish原因（finishReason）字段使用固定文字 ''。 */
      finishReason: "",
      /* 模型（model）字段使用固定文字 ''。 */
      model: "",
      /* 用量（usage）字段使用空值，表示当前还没有对应内容。 */
      usage: null,
      /* 创建时At（createdAt）字段使用当前时间（now）。 */
      createdAt: now,
      /* 等待处理（pending）字段使用真，表示条件成立或开关打开。 */
      pending: true,
      /* stopped字段使用假，表示条件不成立或开关关闭。 */
      stopped: false,
      /* 错误（error）字段使用固定文字 ''。 */
      error: "",
      /* 本次请求的追踪编号（traceId）字段使用固定文字 ''。 */
      traceId: "",
      /* 业务工具列表（tools）字段使用一个空列表，用来保存后续多条数据。 */
      tools: [],
      /* activities字段使用一个空列表，用来保存后续多条数据。 */
      activities: [],
    });
    /* 调用 messages.value.push 完成push处理。 */
    messages.value.push(questionMessage, answerMessage);
    /* 检查条件 draft.value.trim() === text 是否成立，成立时走这一分支。 */
    if (draft.value.trim() === text) /* 把固定文字 ''写入 draft.value。 */ draft.value = "";

    /* 把真，表示条件成立或开关打开写入 generating.value。 */
    generating.value = true;
    const /* 用于取消旧请求的控制器（controller）保存新建 Abort请求入口（AbortController） 实例。 */ controller = new AbortController();
    const /* 本轮对话（turn）保存表达式 { id: "", version: 0, requestId } 的结果。 */ turn = { /* 编号（id）字段使用固定文字 ''。 */ id: "", /* 版本号（version）字段使用固定数值 0。 */ version: 0, /* 直接使用已有的请求编号（requestId）作为同名字段。 */ requestId };
    let /* 已结束（ended）保存假，表示条件不成立或开关关闭。 */ ended = false;
    /* 把用于取消旧请求的控制器（controller）写入 streamController。 */
    streamController = controller;
    /* 把本轮对话（turn）写入 activeTurn。 */
    activeTurn = turn;
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      /* 等待判断是否为 Array。 */
      await streamConversationMessage(
        conversationId,
        { /* 消息（message）字段使用文字（text）。 */ message: text, knowledgeBaseIds: knowledgeForSend, /* 直接使用已有的请求编号（requestId）作为同名字段。 */ requestId },
        {
          /* 取消信号（signal）字段使用表达式 controller.signal 的结果。 */
          signal: controller.signal,
          /* 事件发生时开始（onStart）：调用 onStart 完成事件发生时开始处理。 */
          onStart(data) {
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.turnId || ""写入 turn.id。 */
            turn.id = data.turnId || "";
            /* 把优先使用前面的有效值，没有时采用后面的备用值：Number(data.turnVersion) || 0写入 turn.version。 */
            turn.version = Number(data.turnVersion) || 0;
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.model || ""写入 answerMessage.model。 */
            answerMessage.model = data.model || "";
          },
          /* 事件发生时业务工具（onTool）：调用 onTool 完成事件发生时业务工具处理。 */
          onTool(event, data) {
            /* 检查条件 !Array.isArray(answerMessage.tools) 是否成立，成立时走这一分支。 */
            if (!Array.isArray(answerMessage.tools)) /* 把一个空列表，用来保存后续多条数据写入 answerMessage.tools。 */ answerMessage.tools = [];
            /* 检查条件 event === "tool_start" 是否成立，成立时走这一分支。 */
            if (event === "tool_start") {
              // 查询轮中的过渡说明不是最终回答；与新的助手占位保持一致。
              /* 检查条件 data.resetReply === true 是否成立，成立时走这一分支。 */
              if (data.resetReply === true) /* 把固定文字 ''写入 answerMessage.content。 */ answerMessage.content = "";
              /* 调用 answerMessage.tools.push 完成push处理。 */
              answerMessage.tools.push({
                /* 编号（id）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.id || ""。 */
                id: data.id || "",
                /* 名称（name）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.name || ""。 */
                name: data.name || "",
                /* 显示名称（label）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.label || data.name || "业务数据"。 */
                label: data.label || data.name || "业务数据",
                /* 状态（status）字段使用固定文字 'running'。 */
                status: "running",
              });
              /* 返回空内容，当前函数到此结束。 */
              return;
            }
            const /* 运行中（running）保存优先使用前面的有效值，没有时采用后面的备用值：[...answerMessage.tools] .reverse() .find( (tool) => tool.status === "running" && (!data.id || tool.id === dat…。 */ running = [...answerMessage.tools]
              .reverse()
              .find(
                (tool) =>
                  tool.status === "running" &&
                  (!data.id || tool.id === data.id) &&
                  (!data.name || tool.name === data.name),
              );
            /* 检查条件 running 是否成立，成立时走这一分支。 */
            if (running) {
              /* 把根据前面的条件，从两个候选结果中选择一个：['ok', 'limited'].includes(data.status) ? data.status : 'error'写入 running.status。 */
              running.status = ['ok', 'limited'].includes(data.status) ? data.status : 'error';
              /* 把优先使用前面的有效值，没有时采用后面的备用值：Number(data.durationMs) || 0写入 running.durationMs。 */
              running.durationMs = Number(data.durationMs) || 0;
              /* 把优先使用前面的有效值，没有时采用后面的备用值：data.summary || ""写入 running.summary。 */
              running.summary = data.summary || "";
            }
          },
          /* 事件发生时Activity（onActivity）：调用 onActivity 完成事件发生时Activity处理。 */
          onActivity(data) {
            const /* existing保存取得第一条符合条件的数据。 */ existing = answerMessage.activities.find(item => item.id === data.id);
            /* 检查条件 existing 是否成立，成立时走这一分支。 */
            if (existing) /* 调用 Object.assign 完成安排处理。 */ Object.assign(existing, data);
            else /* 调用 answerMessage.activities.push 完成push处理。 */ answerMessage.activities.push({ ...data });
          },
          /* 事件发生时规划工作流（onWorkflow）：调用 onWorkflow 完成事件发生时规划工作流处理。 */
          onWorkflow(data) {
            /* 检查条件 String(data.conversationId) !== conversationId 是否成立，成立时走这一分支。 */
            if (String(data.conversationId) !== conversationId) /* 返回空内容，当前函数到此结束。 */ return;
            /* 把表达式 { ...data } 的结果写入 planEvent.value。 */
            planEvent.value = { ...data };
            /* 把真，表示条件成立或开关打开写入 planOpen.value。 */
            planOpen.value = true;
          },
          /* 事件发生时复盘（onReview）：调用 onReview 完成事件发生时复盘处理。 */
          onReview(data) {
            /* 检查条件 String(data.conversationId) !== conversationId 是否成立，成立时走这一分支。 */
            if (String(data.conversationId) !== conversationId) /* 返回空内容，当前函数到此结束。 */ return;
            /* 把表达式 { ...data } 的结果写入 reviewEvent.value。 */
            reviewEvent.value = { ...data };
            /* 把真，表示条件成立或开关打开写入 planOpen.value。 */
            planOpen.value = true;
          },
          /* 事件发生时Delta（onDelta）：调用 onDelta 完成事件发生时Delta处理。 */
          onDelta(delta) {
            /* 更新本次处理状态：answerMessage.content += delta。 */
            answerMessage.content += delta;
          },
          /* 事件发生时结束（onEnd）：调用 onEnd 完成事件发生时结束处理。 */
          onEnd(data) {
            /* 把优先使用前面的有效值，没有时采用后面的备用值：Number(data.turnVersion) || turn.version写入 turn.version。 */
            turn.version = Number(data.turnVersion) || turn.version;
            /* 把条件判断的结果：data.complete === true && data.finishReason === "stop"写入 answerMessage.complete。 */
            answerMessage.complete =
              data.complete === true && data.finishReason === "stop";
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.finishReason || ""写入 answerMessage.finishReason。 */
            answerMessage.finishReason = data.finishReason || "";
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.model || answerMessage.model写入 answerMessage.model。 */
            answerMessage.model = data.model || answerMessage.model;
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.usage || null写入 answerMessage.usage。 */
            answerMessage.usage = data.usage || null;
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.citations || []写入 answerMessage.citations。 */
            answerMessage.citations = data.citations || [];
            /* 把优先使用前面的有效值，没有时采用后面的备用值：data.usedMemoryIds || []写入 answerMessage.usedMemoryIds。 */
            answerMessage.usedMemoryIds = data.usedMemoryIds || [];
            /* 把假，表示条件不成立或开关关闭写入 answerMessage.pending。 */
            answerMessage.pending = false;
            /* 把真，表示条件成立或开关打开写入 ended。 */
            ended = true;
          },
        },
      );
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
      /* 把假，表示条件不成立或开关关闭写入 answerMessage.pending。 */
      answerMessage.pending = false;
      /* 检查条件 controller.signal.aborted || error.name === "AbortError" || error.code === "TURN_CANCELLED" 是否成立，成立时走这一分支。 */
      if (
        controller.signal.aborted ||
        error.name === "AbortError" ||
        error.code === "TURN_CANCELLED"
      ) {
        /* 把真，表示条件成立或开关打开写入 answerMessage.stopped。 */
        answerMessage.stopped = true;
      } else {
        /* 把优先使用前面的有效值，没有时采用后面的备用值：error.message || "提问失败，请稍后重试"写入 answerMessage.error。 */
        answerMessage.error = error.message || "提问失败，请稍后重试";
        /* 把优先使用前面的有效值，没有时采用后面的备用值：error.traceId || ""写入 answerMessage.traceId。 */
        answerMessage.traceId = error.traceId || "";
        /* 检查条件 error.status === 409 是否成立，成立时走这一分支。 */
        if (error.status === 409) /* 等待刷新 页面是否仍处于活动状态（Active）。 */ await refreshActive();
      }
    } finally {
      /* 把假，表示条件不成立或开关关闭写入 answerMessage.pending。 */
      answerMessage.pending = false;
      /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
      for (const /* 声明 业务工具（tool），稍后再填入处理结果。 */ tool of answerMessage.tools || []) {
        /* 检查条件 tool.status === "running" 是否成立，成立时走这一分支。 */
        if (tool.status === "running")
          /* 把根据前面的条件，从两个候选结果中选择一个：answerMessage.stopped ? "cancelled" : "error"写入 tool.status。 */
          tool.status = answerMessage.stopped ? "cancelled" : "error";
      }
      /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
      for (const /* 声明 activity，稍后再填入处理结果。 */ activity of answerMessage.activities || []) {
        /* 检查条件 activity.status === "running" 是否成立，成立时走这一分支。 */
        if (activity.status === "running") /* 把根据前面的条件，从两个候选结果中选择一个：answerMessage.stopped ? "cancelled" : "error"写入 activity.status。 */ activity.status = answerMessage.stopped ? "cancelled" : "error";
      }
      /* 把假，表示条件不成立或开关关闭写入 generating.value。 */
      generating.value = false;
      /* 检查条件 streamController === controller 是否成立，成立时走这一分支。 */
      if (streamController === controller) /* 把空值，表示当前还没有对应内容写入 streamController。 */ streamController = null;
      /* 检查条件 activeTurn === turn 是否成立，成立时走这一分支。 */
      if (activeTurn === turn) /* 把空值，表示当前还没有对应内容写入 activeTurn。 */ activeTurn = null;
      const /* aborted保存表达式 controller.signal.aborted 的结果。 */ aborted = controller.signal.aborted;
      /* 等待刷新 页面是否仍处于活动状态（Active）。 */
      await refreshActive({ /* 恢复（recover）字段使用条件判断的结果：!aborted。 */ recover: !aborted });
      /* 检查条件 ended && activeId.value === conversationId 是否成立，成立时走这一分支。 */
      if (ended && activeId.value === conversationId)
        /* 等待刷新 最新消息列表（LatestMessages）。 */
        await refreshLatestMessages();
      /* 检查条件 activeId.value === conversationId 是否成立，成立时走这一分支。 */
      if (activeId.value === conversationId) /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ planRefreshKey.value++;
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (!disposed) /* 调用 report 完成复盘报告处理。 */ report(error);
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 sending.value。 */
    sending.value = false;
  }
}

/* resetFor会话（resetForSession）：调用 resetForSession 完成resetFor会话处理。 */
function resetForSession() {
  /* 更新本次处理状态：listSequence += 1。 */
  listSequence += 1;
  /* 更新本次处理状态：openSequence += 1。 */
  openSequence += 1;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  listController?.abort();
  /* 把空值，表示当前还没有对应内容写入 listController。 */
  listController = null;
  /* 调用 clearActive 完成清除当前有效处理。 */
  clearActive();
  /* 把一个空列表，用来保存后续多条数据写入 list.rows。 */
  list.rows = [];
  /* 把固定数值 0写入 list.total。 */
  list.total = 0;
  /* 把固定数值 1写入 list.page。 */
  list.page = 1;
  /* 把假，表示条件不成立或开关关闭写入 list.hasMore。 */
  list.hasMore = false;
  /* 把假，表示条件不成立或开关关闭写入 list.loaded。 */
  list.loaded = false;
  /* 把固定文字 ''写入 list.error。 */
  list.error = "";
  /* 把假，表示条件不成立或开关关闭写入 list.busy。 */
  list.busy = false;
  /* 把固定文字 ''写入 filters.keyword。 */
  filters.keyword = "";
  /* 把固定文字 ''写入 draft.value。 */
  draft.value = "";
  /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
  if (!disposed) /* 加载 对话列表（Conversations）。 */ loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
}

/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(authSessionKey, resetForSession);
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => route.query.conversationId, (id) => {
  /* 检查条件 id && String(id) !== activeId.value 是否成立，成立时走这一分支。 */
  if (id && String(id) !== activeId.value) /* 调用 selectConversation 完成选择对话处理。 */ selectConversation(id);
});

/* 读取 单条数据（Item） 的值。 */
onMounted(async () => {
  /* 等待加载 对话列表（Conversations）。 */
  await loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
  /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
  if (disposed || !canList.value) /* 返回空内容，当前函数到此结束。 */ return;
  const /* remembered保存读取 单条数据（Item） 的值。 */ remembered = sessionStorage.getItem(STORAGE_KEY);
  const /* 目标值（target）保存沿 route.query.conversationId || list.rows.find((row) => row.id === remembered)?.id 读取数据；中间对象为空时返回空值。 */ target =
    route.query.conversationId || list.rows.find((row) => row.id === remembered)?.id;
  /* 检查条件 target 是否成立，成立时走这一分支。 */
  if (target) /* 等待调用 openConversation 完成是否展开对话处理。 */ await openConversation(target);
  else /* 检查条件 externalReviewId.value && canCreate.value 是否成立，成立时走这一分支。 */ if (externalReviewId.value && canCreate.value) {
    const /* 创建时（created）保存等待 创建 对话（Conversation） 完成后取得结果。 */ created = await agentApi.createConversation('复盘与计划调整');
    /* 等待加载 对话列表（Conversations）。 */
    await loadConversations({ /* reset字段使用真，表示条件成立或开关打开。 */ reset: true });
    /* 等待调用 openConversation 完成是否展开对话处理。 */
    await openConversation(created.id);
    /* 把真，表示条件成立或开关打开写入 planOpen.value。 */
    planOpen.value = true;
  }
});

/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 把真，表示条件成立或开关打开写入 disposed。 */
  disposed = true;
  /* 更新本次处理状态：listSequence += 1。 */
  listSequence += 1;
  /* 更新本次处理状态：openSequence += 1。 */
  openSequence += 1;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  listController?.abort();
  /* 调用 stopRecovery 完成停止Recovery处理。 */
  stopRecovery();
  /* 检查条件 streamController 是否成立，成立时走这一分支。 */
  if (streamController) {
    const /* 本轮对话（turn）保存当前有效本轮对话（activeTurn）。 */ turn = activeTurn;
    const /* 编号（id）保存表达式 activeId.value 的结果。 */ id = activeId.value;
    /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
    streamController.abort();
    /* 检查条件 turn?.id && id 是否成立，成立时走这一分支。 */
    if (turn?.id && id)
      /* 取消 本轮对话（Turn）。 */
      agentApi.cancelTurn(id, turn.id, turn.version).catch(() => {});
  }
});
</script>

<template>
  <!-- 显示一个有独立含义的内容区；把组件或元素引用保存为 planLayout，供脚本读取；用样式类 agent-page 控制外观；style 取自 { '—agent-plan-width': planWidth + 'px' }；class 取自 { 'has-plan-panel': planOpen, 'is-resizing-plan': resizingPlan }；aria-label 提供无障碍说明 成长助手会话。 -->
  <section ref="planLayout" class="agent-page" :style="{ '--agent-plan-width': planWidth + 'px' }" :class="{ 'has-plan-panel': planOpen, 'is-resizing-plan': resizingPlan }" aria-label="成长助手会话">
    <!-- 显示Teleport组件；启用 defer 属性；to 固定为 #assistant-new-chat。 -->
    <Teleport defer to="#assistant-new-chat">
      <!-- 显示可点击按钮；仅当 canCreate 成立时渲染；类型设为 button；用样式类 sidebar-new-chat 控制外观；disabled 取自 sending || generating；click 事件触发时执行 createConversation。 -->
      <button v-if="canCreate" type="button" class="sidebar-new-chat" :disabled="sending || generating" @click="createConversation"><!-- 显示应用Icon（AppIcon）组件；name 固定为 compose；size 取自 18。 --><AppIcon name="compose" :size="18" /><!-- 显示行内文字区域。 --><span>新对话</span><!-- 显示应用Icon（AppIcon）组件；name 固定为 plus；size 取自 16。 --><AppIcon name="plus" :size="16" /></button>
    </Teleport>
    <!-- 显示Teleport组件；启用 defer 属性；to 固定为 #assistant-conversations。 -->
    <Teleport defer to="#assistant-conversations">
      <!-- 显示成长助手对话列表（AgentConversationList）组件；与 filters.keyword 双向同步（keyword）；rows 取自 list.rows；active-id 取自 activeId；busy 取自 list.busy；error 取自 list.error；loaded 取自 list.loaded；has-more 取自 list.hasMore；total 取自 list.total；can-create 取自 canCreate；can-update 取自 canUpdate；can-delete 取自 canRemove；select 事件触发时执行 selectConversation；create 事件触发时执行 createConversation；refresh 事件触发时执行 loadConversations({ reset: true })；load-more 事件触发时执行 loadMore；rename 事件触发时执行 renameConversation；remove 事件触发时执行 removeConversation。 -->
      <AgentConversationList
        v-model:keyword="filters.keyword"
        :rows="list.rows"
        :active-id="activeId"
        :busy="list.busy"
        :error="list.error"
        :loaded="list.loaded"
        :has-more="list.hasMore"
        :total="list.total"
        :can-create="canCreate"
        :can-update="canUpdate"
        :can-delete="canRemove"
        @select="selectConversation"
        @create="createConversation"
        @refresh="loadConversations({ reset: true })"
        @load-more="loadMore"
        @rename="renameConversation"
        @remove="removeConversation"
      />
    </Teleport>
    <!-- 显示成长助手对话区域（AgentConversationPane）组件；把组件或元素引用保存为 pane，供脚本读取；用样式类 agent-pane 控制外观；与 draft 双向同步（draft）；conversation 取自 conversation；turns 取自 turns；loading 取自 loadingConversation；loading-older 取自 olderBusy；has-more 取自 hasMore；sending 取自 sending；busy 取自 generating || reviewWorking；recovering 取自 recovering；can-send 取自 canSend；can-create 取自 canCreate；can-update 取自 canUpdate；can-delete 取自 canRemove；send 事件触发时执行 send；stop 事件触发时执行 handleStop；retry 事件触发时执行 send；load-older 事件触发时执行 loadOlderMessages；rename 事件触发时执行 renameConversation(activeRow())；remove 事件触发时执行 removeConversation(activeRow())；create 事件触发时执行 createConversation；toggle-list 事件触发时执行 sidebar.toggle。 -->
    <AgentConversationPane
      ref="pane"
      class="agent-pane"
      v-model:draft="draft"
      :conversation="conversation"
      :turns="turns"
      :loading="loadingConversation"
      :loading-older="olderBusy"
      :has-more="hasMore"
      :sending="sending"
      :busy="generating || reviewWorking"
      :recovering="recovering"
      :can-send="canSend"
      :can-create="canCreate"
      :can-update="canUpdate"
      :can-delete="canRemove"
      @send="send"
      @stop="handleStop"
      @retry="send"
      @load-older="loadOlderMessages"
      @rename="renameConversation(activeRow())"
      @remove="removeConversation(activeRow())"
      @create="createConversation"
      @toggle-list="sidebar.toggle"
    >
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 tools 插槽。 -->
      <template #knowledge><KnowledgeScopePicker v-model="draftKnowledgeBaseIds" :conversation="conversation" :loading="loadingConversation"
        @saved="detail => { if (String(detail.id) === activeId) { conversation = detail; updateRow(detail); } }" /></template>
      <template #tools>
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('agent:workflow:read') 成立时渲染；启用 text 属性；aria-expanded 取自 planOpen；click 事件触发时执行 planOpen = !planOpen。 -->
        <el-button v-if="canGrow('agent:workflow:read')" text :aria-expanded="planOpen" @click="planOpen = !planOpen"><!-- 显示应用Icon（AppIcon）组件；name 固定为 goal；size 取自 16。 --><AppIcon name="goal" :size="16" /><!-- 显示行内文字区域。 --><span><!-- 把 planAvailable ? '方案与进度' : '成长方案' 的当前值转成文字显示。 -->{{ planAvailable ? '方案与进度' : '成长方案' }}</span></el-button>
        <!-- 显示Element Plus 的 button 组件；仅当 firstResource 成立时渲染；启用 text 属性；用样式类 agent-resource-trigger 控制外观；click 事件触发时执行 router.push(firstResource)。 -->
        <el-button v-if="firstResource" text class="agent-resource-trigger" @click="router.push(firstResource)"><!-- 显示应用Icon（AppIcon）组件；name 固定为 folder；size 取自 16。 --><AppIcon name="folder" :size="16" /><!-- 显示行内文字区域。 --><span>资料与记录</span></el-button>
      </template>
    </AgentConversationPane>
    <!-- 显示可点击按钮；仅当 planOpen 成立时渲染；类型设为 button；用样式类 plan-panel-backdrop 控制外观；aria-label 提供无障碍说明 收起方案面板；click 事件触发时执行 planOpen = false。 -->
    <button v-if="planOpen" type="button" class="plan-panel-backdrop" aria-label="收起方案面板" @click="planOpen = false"></button>
    <!-- 显示成长助手方案工作区（AgentPlanWorkspace）组件；仅当 canGrow('agent:workflow:read') 成立时渲染；conversation-id 取自 activeId；event 取自 planEvent；review-event 取自 reviewEvent；external-review-id 取自 externalReviewId；refresh-key 取自 planRefreshKey；busy 取自 sending || generating || recovering；open 取自 planOpen；close 事件触发时执行 planOpen = false；available 事件触发时执行 planAvailability；reveal 事件触发时执行 planOpen = true；revise 事件触发时执行 revisePlan；working 事件触发时执行 reviewWorking = $event。 -->
    <AgentPlanWorkspace v-if="canGrow('agent:workflow:read')" :conversation-id="activeId" :event="planEvent" :review-event="reviewEvent" :external-review-id="externalReviewId" :refresh-key="planRefreshKey" :busy="sending || generating || recovering" :open="planOpen" @close="planOpen = false" @available="planAvailability" @reveal="planOpen = true" @revise="revisePlan" @working="reviewWorking = $event">
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 resize 插槽。 -->
      <template #resize><!-- 显示一个布局容器；用样式类 plan-resize-handle 控制外观；role 固定为 separator；aria-label 提供无障碍说明 调整方案面板宽度；aria-orientation 提供无障碍说明 vertical；aria-valuemin 提供无障碍说明 320；aria-valuemax 取自 720；aria-valuenow 取自 planWidth；tabindex 固定为 0；title 固定为 拖动调整方案宽度，双击恢复默认；pointerdown 事件触发时执行 planResizeStart；pointermove 事件触发时执行 planResizeMove；pointerup 事件触发时执行 planResizeEnd；pointercancel 事件触发时执行 planResizeEnd；lostpointercapture 事件触发时执行 planResizeEnd；keydown 事件触发时执行 planResizeKey；dblclick 事件触发时执行 resetPlanWidth。 --><div class="plan-resize-handle" role="separator" aria-label="调整方案面板宽度" aria-orientation="vertical" aria-valuemin="320" :aria-valuemax="720" :aria-valuenow="planWidth" tabindex="0" title="拖动调整方案宽度，双击恢复默认" @pointerdown="planResizeStart" @pointermove="planResizeMove" @pointerup="planResizeEnd" @pointercancel="planResizeEnd" @lostpointercapture="planResizeEnd" @keydown="planResizeKey" @dblclick="resetPlanWidth"></div></template>
    </AgentPlanWorkspace>
    <!-- 显示Element Plus 的 drawer 组件；model-value 取自 resourcesOpen；用样式类 agent-resource-drawer 控制外观；title 固定为 助手空间；size 固定为 min(960px, 100vw)；启用 append-to-body 属性；close 事件触发时执行 closeResources。 -->
    <el-drawer :model-value="resourcesOpen" class="agent-resource-drawer" title="助手空间" size="min(960px, 100vw)" append-to-body @close="closeResources">
      <!-- 显示成长助手工作区Nav（AgentWorkspaceNav）组件。 -->
      <AgentWorkspaceNav />
      <!-- 显示当前路由对应页面的显示位置；key 取自 route.path。 -->
      <RouterView :key="route.path" />
    </el-drawer>
  </section>
</template>

<style scoped>
/* 以下规则应用于 .agent-page 选中的元素。 */
.agent-page {
  /* 决定元素是否参与普通排版，以及按谁定位；取值 relative。 */
  position: relative;
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */
  display: grid;
  /* 设置网格列数和各列宽度；取值 minmax(0, 1fr)。 */
  grid-template-columns: minmax(0, 1fr);
  /* 设置高度；取值 100%。 */
  height: 100%;
  /* 限制最小高度；取值 0。 */
  min-height: 0;
  /* 限制最小宽度；取值 0。 */
  min-width: 0;
}
/* 以下规则应用于 .agent-page.has-plan-panel 选中的元素。 */
.agent-page.has-plan-panel { /* 设置网格列数和各列宽度；取值 minmax(0, 1fr) clamp(320px, var(—agent-plan-width), calc(100% - 360px))。 */ grid-template-columns: minmax(0, 1fr) clamp(320px, var(--agent-plan-width), calc(100% - 360px)); }
/* 以下规则应用于 .plan-resize-handle 选中的元素。 */
.plan-resize-handle { /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 设置左侧定位距离；取值 0。 */ left: 0; /* 设置上侧定位距离；取值 0。 */ top: 0; /* 设置下侧定位距离；取值 0。 */ bottom: 0; /* 设置宽度；取值 8px。 */ width: 8px; /* 设置重叠时的前后层级；取值 4。 */ z-index: 4; /* 设置 CSS 属性 touch-action；取值 none。 */ touch-action: none; /* 设置鼠标移上来时的指针形状；取值 col-resize。 */ cursor: col-resize; }
/* 以下规则应用于 .plan-resize-handle::after 选中的元素。 */
.plan-resize-handle::after { /* 为伪元素提供要显示的内容；取值 ''。 */ content: ''; /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 设置上侧定位距离；取值 0。 */ top: 0; /* 设置下侧定位距离；取值 0。 */ bottom: 0; /* 设置左侧定位距离；取值 0。 */ left: 0; /* 设置宽度；取值 2px。 */ width: 2px; /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; }
/* 以下规则应用于 .plan-resize-handle:hover::after, .plan-resize-handle:focus-visible::after, .is-resizing-plan .plan-resize-handle::after 选中的元素。 */
.plan-resize-handle:hover::after, .plan-resize-handle:focus-visible::after, .is-resizing-plan .plan-resize-handle::after { /* 设置背景颜色、图片或渐变；取值 var(—aw-brand)。 */ background: var(--aw-brand); }
/* 以下规则应用于 .is-resizing-plan, .is-resizing-plan * 选中的元素。 */
.is-resizing-plan, .is-resizing-plan * { /* 设置鼠标移上来时的指针形状；取值 col-resize。 */ cursor: col-resize !important; /* 决定文字能否被拖选；取值 none。 */ user-select: none !important; }
/* 以下规则应用于 .plan-panel-backdrop 选中的元素。 */
.plan-panel-backdrop { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 none。 */ display: none; }
/* 以下规则应用于 .agent-pane 选中的元素。 */
.agent-pane {
  /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */
  display: flex;
  /* 设置 Flex 条目的排列方向；取值 column。 */
  flex-direction: column;
  /* 限制最小高度；取值 0。 */
  min-height: 0;
  /* 设置高度；取值 100%。 */
  height: 100%;
  /* 限制最小宽度；取值 0。 */
  min-width: 0;
  /* 决定内容超出边界时显示、裁切还是滚动；取值 hidden。 */
  overflow: hidden;
  /* 设置背景颜色、图片或渐变；取值 var(—aw-bg)。 */
  background: var(--aw-bg);
}
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(max-width: 1199px)。 */
@media (max-width: 1199px) {
  /* 以下规则应用于 .plan-resize-handle 选中的元素。 */
  .plan-resize-handle { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 none。 */ display: none; }
  /* 以下规则应用于 .agent-page.has-plan-panel 选中的元素。 */
  .agent-page.has-plan-panel { /* 设置网格列数和各列宽度；取值 minmax(0, 1fr)。 */ grid-template-columns: minmax(0, 1fr); }
  /* 以下规则应用于 .plan-panel-backdrop 选中的元素。 */
  .plan-panel-backdrop { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 同时设置四个方向的定位距离；取值 0。 */ inset: 0; /* 设置重叠时的前后层级；取值 510。 */ z-index: 510; /* 设置背景颜色、图片或渐变；取值 var(—aw-overlay)。 */ background: var(--aw-overlay); /* 设置边框宽度、线型与颜色；取值 0。 */ border: 0; }
}
</style>
