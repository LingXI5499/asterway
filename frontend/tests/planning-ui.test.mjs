// 使用项目已有的 Node 测试工具和 Vue 编译器，隔离网络来验证真实组件逻辑。
import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import * as Vue from "vue";
import { parse, compileScript, compileTemplate } from "@vue/compiler-sfc";

const root = new URL("../src/", import.meta.url);
const hooks = { ...Vue, onBeforeUnmount() {}, onMounted() {}, useId: () => "test-form" };
const date = {
  addDays(value, count) { const d = new Date(value + "T12:00:00Z"); d.setUTCDate(d.getUTCDate() + count); return d.toISOString().slice(0,10); },
  monday(value) { return date.addDays(value, -((new Date(value + "T12:00:00Z").getUTCDay() + 6) % 7)); },
  validDate(value) { return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value)) && new Date(value + "T12:00:00Z").toISOString().slice(0,10) === value; },
  today: () => "2026-11-09", canGrow: () => true,
};
const plain = value => JSON.parse(JSON.stringify(value));
function deferred() { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b; }); return { promise, resolve, reject }; }

async function moduleFor(path, imports = {}, component = false) {
  let source = await readFile(new URL(path, root), "utf8");
  if (component) source = compileScript(parse(source).descriptor, { id: "planning-test" }).content;
  const context = vm.createContext({ AbortController, URLSearchParams, setTimeout, clearTimeout, console });
  const module = new vm.SourceTextModule(source, { context });
  await module.link(async name => {
    assert.ok(imports[name], `Missing test import: ${name}`);
    const values = imports[name];
    return new vm.SyntheticModule(Object.keys(values), function () {
      for (const [key,value] of Object.entries(values)) this.setExport(key,value);
    }, { context });
  });
  await module.evaluate();
  return module.namespace;
}
async function planning(api, allowed = () => true) {
  return moduleFor("api/planning.js", { "./http": { api }, "./growth": { ...date, canGrow: allowed } });
}

test("Enter sends, Ctrl+Enter inserts a newline, composition confirmation stays native", async () => {
  const m = await moduleFor("composables/composerKeyboard.js");
  assert.equal(m.composerEnterAction({ key: "Enter" }), "send");
  assert.equal(m.composerEnterAction({ key: "Enter", ctrlKey: true }), "newline");
  for (const extra of [{ shiftKey:true },{ isComposing:true,ctrlKey:true },{ keyCode:229 },{ altKey:true }]) {
    assert.equal(m.composerEnterAction({ key:"Enter",...extra }),"native");
  }
  assert.deepEqual(plain(m.insertComposerNewline("前后",1,1)),{ value:"前\n后",caret:2 });
  assert.deepEqual(plain(m.insertComposerNewline("前选中文字后",1,5)),{ value:"前\n后",caret:2 });
  assert.deepEqual(plain(m.insertComposerNewline("😀ab",2,3)),{ value:"😀\nb",caret:3 });
});

test("month calendar handles leap days, adjacent months and year changes", async () => {
  const m = await planning(() => {});
  const days = m.monthDates("2028-02-01");
  assert.equal(days.length,42); assert.equal(new Date(days[0]+"T12:00:00Z").getUTCDay(),1);
  assert.ok(days.includes("2028-02-29")); assert.equal(new Set(days).size,42);
  assert.equal(m.shiftMonth("2026-12-01",1),"2027-01-01");
  assert.equal(m.shiftMonth("2027-01-01",-1),"2026-12-01");
  assert.equal(m.pastAssignment({planDate:"2026-11-08"},date.today()),true);
  assert.equal(m.pastAssignment({weekStartDate:"2026-11-09"},date.today()),false);
});

test("legacy entries retain their date, task ID and task management destination", async () => {
  const m = await planning(() => {});
  const route = { query:{date:"2026-12-03"}, params:{id:"9223372036854775000"},hash:"" };
  assert.deepEqual(plain(m.planningRedirect("/planning/daily-plans",route)),{path:"/planning/plans",query:route.query,hash:""});
  assert.equal(m.planningRedirect("/growth/tasks",route).query.manage,"all");
  assert.equal(m.planningRedirect("/planning/pending-tasks",route).query.manage,"overdue");
  assert.equal(m.planningRedirect("/growth/tasks/:id",route).query.task,route.params.id);
  assert.equal(m.planningRedirect("/planning/weekly-plans",{query:{week:"2026-12-07"}}).query.date,"2026-12-07");
  const nav = await moduleFor("composables/workspaceNavigation.js");
  const links = nav.planningNavigation(["/growth/goals","/growth/stages","/growth/tasks","/planning/daily-plans","/planning/weekly-plans","/planning/pending-tasks"].map(routePath => ({routePath})));
  assert.deepEqual(plain(links.map(l => l.routePath)),["/growth/goals","/growth/stages","/planning/plans"]);
});

test("confirmed scheduling reuses existing containers and preserves string IDs", async () => {
  const calls=[];
  const m = await planning(async (path,options={}) => {
    calls.push([path,options]);
    return path.includes("/daily-plans/") ? {id:"9223372036854775002"} : {id:"9223372036854775001"};
  });
  await m.assignNewTask("9223372036854775000","2026-11-10");
  assert.equal(calls.length,3); assert.equal(calls.filter(c => c[1].method==="PUT").length,1);
  assert.deepEqual(plain(calls[2][1].body),{weeklyPlanId:"9223372036854775001",dailyPlanId:"9223372036854775002",assignmentId:null,version:null,sortOrder:0});
});

test("missing containers are created only in confirmed scheduling and respect permissions", async () => {
  const calls=[];
  const m = await planning(async (path,options={}) => { calls.push([path,options.method||"GET"]); return options.method==="PUT" ? {id:path.includes("daily-plans")?"day":"week"} : null; });
  await m.ensurePlan({weekStart:"2026-11-09",planDate:"2026-11-10"});
  assert.deepEqual(calls.map(c => c[1]),["GET","PUT","GET","PUT"]);
  const writes=[];
  const restricted = await planning(async (p,o) => { if(o?.method) writes.push(p); return null; },() => false);
  await assert.rejects(restricted.ensurePlan({weekStart:"2026-11-09"}),/权限/);
  assert.equal(writes.length,0);
  await assert.rejects(m.ensurePlan({weekStart:"2026-11-09",planDate:"2026-11-18"}),/同一周/);
});

async function dayLoader(api, query, allowed=()=>true) {
  const m = await moduleFor("composables/usePlanTasks.js", {vue:hooks,"../api/http":{api},"../api/growth":{...date,query,canGrow:allowed}});
  return m.usePlanTasks();
}
test("date browsing is read-only and paginates the full server result", async () => {
  const calls=[];
  const state = await dayLoader(async (path,options) => { calls.push([path,options]); return {id:path.includes("daily-plans")?"day":"week"}; },async (p,q) => ({items:[{taskId:"page-two"}],total:45,page:q.page,size:q.size}));
  await state.load("2026-11-10",2,20);
  assert.equal(state.total.value,45); assert.equal(state.rows.value[0].taskId,"page-two");
  assert.ok(calls.every(c => !c[1].method)); assert.equal(state.day.value.id,"day");
});
test("an absent week or day is an ordinary empty date", async () => {
  for (const hasWeek of [false,true]) {
    let taskQueries=0;
    const state = await dayLoader(async path => hasWeek && !path.includes("daily-plans") ? {id:"week"} : null,async () => {taskQueries++;});
    await state.load("2026-11-10");
    assert.equal(state.rows.value.length,0); assert.equal(state.total.value,0); assert.equal(state.error.value,""); assert.equal(taskQueries,0);
  }
});
test("a delayed old date cannot replace tasks, metadata, errors or loading of the selected date", async () => {
  const old=deferred();
  const state = await dayLoader(async path => {
    if (path.endsWith("2026-11-10")) return old.promise;
    return {id:path.includes("daily-plans")?"new-day":"week"};
  },async () => ({items:[{taskId:"latest"}],total:1}));
  const previous=state.load("2026-11-10");
  await new Promise(resolve => setTimeout(resolve,0));
  await state.load("2026-11-11");
  old.resolve({id:"old-day"}); await previous;
  assert.equal(state.day.value.id,"new-day"); assert.equal(state.rows.value[0].taskId,"latest"); assert.equal(state.busy.value,false);
});
test("query errors are distinct from rest days and a retry clears the error", async () => {
  let fail=true;
  const state=await dayLoader(async () => {if(fail)throw new Error("连接失败"); return null;},async()=>{});
  await state.load("2026-11-10"); assert.equal(state.error.value,"连接失败");
  fail=false; await state.load("2026-11-10"); assert.equal(state.error.value,""); assert.equal(state.total.value,0);
});
test("missing daily permission keeps weekly metadata available without reading daily tasks",async()=>{
  const paths=[];
  const state=await dayLoader(async path=>{paths.push(path);return {id:"week"};},async()=>assert.fail("Daily tasks must not be queried"),code=>code==="planning:weekly:read");
  await state.load("2026-11-10");
  assert.equal(paths.length,1); assert.equal(state.week.value.id,"week"); assert.equal(state.rows.value.length,0);
  const restricted=await dayLoader(async()=>assert.fail("Weekly data must not be queried"),async()=>{},()=>false);
  await restricted.load("2026-11-10"); assert.equal(restricted.total.value,0);
});

async function editorFixture(assignNewTask) {
  const calls=[],events=[];
  const api=async (p,o) => {calls.push([p,o]); return {id:"created-task",version:0};};
  const mod=await moduleFor("components/growth/GrowthEditor.vue",{
    vue:hooks,"../../api/http":{api},"../../api/growth":{...date,getResource:()=>{},resources:{task:"tasks"},permission:(k,a)=>`${k}:${k}:${a}`},
    "../../api/planning":{assignNewTask},"./GrowthSelector.vue":{default:{}},
  },true);
  const editor=mod.default.setup({kind:"task",planDate:"2026-11-10"},{expose(){},emit:(...event)=>events.push(event)});
  Object.assign(editor.form.value,{name:"测试任务",weight:1}); editor.stageId.value="stage"; editor.loading.value=false;
  return {editor,calls,events};
}
test("a created task survives assignment failure and retries scheduling without a duplicate POST",async()=>{
  let attempts=0;
  const {editor,calls,events}=await editorFixture(async()=>{if(++attempts===1)throw new Error("安排失败");});
  await editor.save("day");
  assert.equal(calls.length,1); assert.equal(editor.createdTask.value.id,"created-task"); assert.match(editor.error.value,/任务已创建/);
  assert.equal(events.filter(e=>e[0]==="created").length,1); assert.equal(events.filter(e=>e[0]==="saved").length,0);
  await editor.save("day");
  assert.equal(calls.length,1); assert.equal(attempts,2); assert.equal(events.filter(e=>e[0]==="saved").length,1);
});
test("saving a new task to the pool does not create a date arrangement",async()=>{
  let assignments=0;
  const {editor,calls,events}=await editorFixture(async()=>{assignments++;});
  await editor.save("pool"); assert.equal(assignments,0); assert.equal(calls.length,1); assert.equal(events.at(-1)[0],"saved");
});
test("duplicate confirmation clicks only schedule once while saving",async()=>{
  const wait=deferred(); let attempts=0;
  const {editor,calls}=await editorFixture(async()=>{attempts++;await wait.promise;});
  const first=editor.save("day"); await new Promise(resolve=>setTimeout(resolve,0));
  await editor.save("day"); wait.resolve(); await first;
  assert.equal(calls.length,1); assert.equal(attempts,1);
});

async function managerFixture(api, query, initialTab="week") {
  const mod=await moduleFor("components/growth/TaskManagerDialog.vue",{
    vue:hooks,"../../api/http":{api},"../../api/growth":{...date,query,states:{},priorities:{},positions:{}},
    "../../api/planning":{pastAssignment:()=>false},"./GrowthSelector.vue":{default:{}},"./PlanTaskCard.vue":{default:{}},
  },true);
  const manager=mod.default.setup({weekStart:"2026-11-09",initialTab},{expose(){},emit(){}});
  await new Promise(resolve=>setTimeout(resolve,0));
  return manager;
}
test("management loads on opening and the undated filter keeps server pagination and totals",async()=>{
  const queries=[];
  const manager=await managerFixture(async()=>({id:"week"}),async(p,q)=>{queries.push([p,plain(q)]);return {items:[{taskId:"row"}],total:47};});
  assert.equal(queries.length,1); assert.equal(manager.total.value,47);
  manager.undated.value=true; manager.page.value=2; manager.filters.value.priority="HIGH";
  await manager.load();
  assert.equal(queries.at(-1)[1].assignmentStatus,"WEEK");
  assert.equal(queries.at(-1)[1].page,2); assert.equal(queries.at(-1)[1].priority,"HIGH");
  assert.equal(manager.rows.value.length,1); assert.equal(manager.total.value,47);
  manager.tab.value="pool"; await Vue.nextTick(); await new Promise(resolve=>setTimeout(resolve,0));
  assert.equal(queries.at(-1)[0],"/tasks"); assert.equal(queries.at(-1)[1].assignmentStatus,"POOL");
  manager.tab.value="overdue"; await Vue.nextTick(); await new Promise(resolve=>setTimeout(resolve,0));
  assert.equal(queries.at(-1)[0],"/planning/pending-tasks");
});
test("changing management tabs cannot reveal a delayed result from the previous tab",async()=>{
  const old=deferred();
  const manager=await managerFixture(async()=>({id:"week"}),async p=>p.startsWith("/weekly-plans")?old.promise:{items:[{id:"pool-task"}],total:1});
  manager.tab.value="pool"; await Vue.nextTick(); await new Promise(resolve=>setTimeout(resolve,0));
  old.resolve({items:[{taskId:"old-task"}],total:30});
  await new Promise(resolve=>setTimeout(resolve,0));
  assert.equal(manager.rows.value[0].id,"pool-task"); assert.equal(manager.total.value,1); assert.equal(manager.busy.value,false);
});
test("removing the last task from a later page returns to the last existing page",async()=>{
  let remaining=21; const pages=[];
  const manager=await managerFixture(async()=>({id:"week"}),async(p,q)=>{pages.push(q.page);return {items:[{taskId:"remaining"}],total:remaining};});
  manager.page.value=2; remaining=9; await manager.load();
  assert.equal(manager.page.value,1); assert.equal(manager.total.value,9); assert.equal(pages.at(-1),1);
});
test("rescheduling reads on opening and submits the current assignment version only on confirmation",async()=>{
  const mounted=[],calls=[],containerCalls=[];
  const mod=await moduleFor("components/growth/TaskAssignmentDialog.vue",{
    vue:{...hooks,onMounted:fn=>mounted.push(fn)},
    "../../api/http":{api:async(p,o={})=>{calls.push([p,o]);return o.method?{}:{id:"old-assignment",version:4,sortOrder:3};}},
    "../../api/growth":date,"../../api/planning":{ensurePlan:async args=>{containerCalls.push(args);return {week:{id:"new-week"},day:{id:"new-day"}};}},
  },true);
  const dialog=mod.default.setup({taskId:"original-task",weekStart:"2026-11-09",planDate:"2026-11-10"},{expose(){},emit(){}});
  await mounted[0](); assert.equal(calls.length,1); assert.equal(containerCalls.length,0); assert.equal(calls[0][1].method,undefined);
  await dialog.save();
  assert.equal(containerCalls.length,1);
  assert.deepEqual(plain(calls[1][1].body),{weeklyPlanId:"new-week",dailyPlanId:"new-day",assignmentId:"old-assignment",version:4,sortOrder:3});
  dialog.dayDate.value="2026-11-08"; await dialog.save(); assert.equal(calls.length,2); assert.match(dialog.error.value,/有效安排日期/);
});

test("all touched Vue templates compile with development comments enabled", async () => {
  for(const file of ["views/WorkspaceView.vue","views/growth/PlansView.vue","components/growth/PlanTaskCard.vue","components/growth/TaskManagerDialog.vue","components/growth/TaskDetailsDialog.vue","components/growth/GrowthActions.vue","components/growth/GrowthEditor.vue","components/growth/TaskAssignmentDialog.vue","components/growth/AssignmentHistory.vue","components/growth/TaskPicker.vue","components/growth/OrderDialog.vue","components/personal/GrowthEvidence.vue","components/personal/StateHistory.vue","components/personal/ExecutionDetails.vue","components/agent/AgentConversationPane.vue"]) {
    const source=await readFile(new URL(file,root),"utf8"), descriptor=parse(source).descriptor;
    const script=compileScript(descriptor,{id:file});
    const compiled=compileTemplate({source:descriptor.template.content,filename:file,id:file,isProd:false,compilerOptions:{comments:true,bindingMetadata:script.bindings}});
    assert.deepEqual(compiled.errors,[],file);
  }
});
