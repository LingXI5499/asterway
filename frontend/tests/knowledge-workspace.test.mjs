// 用隔离接口检查队列并发、目标库固定与重试，开发模板也保留注释编译。
import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import * as Vue from "vue";
import { parse, compileScript, compileTemplate } from "@vue/compiler-sfc";

const root = new URL("../src/", import.meta.url);
const capabilities = { maxFileBytes:25*1024*1024,maxBatchFiles:20,uploadConcurrency:2,formats:[".txt",".md",".pdf",".docx"].map(extension=>({extension})) };
const file = (name="a.txt",size=20) => ({name,size,type:"text/plain"});
const tick = () => new Promise(resolve=>setTimeout(resolve,0));
function deferred() { let resolve,reject; const promise=new Promise((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject}; }
async function queueModule(workspaceApi) {
  const source=await readFile(new URL("composables/useKnowledgeImport.js",root),"utf8");
  const dispose=[],events=new Map(); let number=0;
  const context=vm.createContext({AbortController,crypto:{randomUUID:()=>String(++number)},document:{hidden:false},window:{addEventListener:(n,f)=>events.set(n,f),removeEventListener:n=>events.delete(n)},setInterval:()=>1,clearInterval(){}});
  const mod=new vm.SourceTextModule(source,{context});
  const imports={vue:{...Vue,onBeforeUnmount:fn=>dispose.push(fn)},"../api/agentWorkspace":{workspaceApi,query:q=>"?"+new URLSearchParams(q)}};
  await mod.link(async name=>{const values=imports[name];assert.ok(values,name);return new vm.SyntheticModule(Object.keys(values),function(){for(const[k,v]of Object.entries(values))this.setExport(k,v);},{context});});
  await mod.evaluate();
  return {m:mod.namespace,dispose:()=>dispose.forEach(fn=>fn()),events};
}

async function scopeModule(props, workspaceApi, agentApi) {
  const source = await readFile(new URL("components/agent/KnowledgeScopePicker.vue", root), "utf8");
  const body = parse(source).descriptor.scriptSetup.content.replace(/^import .*;\s*$/gm, "");
  const emitted = [], dispose = [];
  const context = vm.createContext({
    ...Vue, onBeforeUnmount: fn => dispose.push(fn),
    defineProps: () => props, defineEmits: () => (...event) => emitted.push(event),
    canGrow: () => true, workspaceApi, agentApi,
  });
  const mod = new vm.SourceTextModule(body + "\nexport {show, load, save, open, selected, loaded, error};", {context});
  await mod.link(() => { throw new Error("unexpected import"); });
  await mod.evaluate();
  return {m: mod.namespace, emitted, dispose: () => dispose.forEach(fn => fn())};
}

test("conversation scope preserves filled choices after saving fails and blocks a failed initial read", async () => {
  const props = Vue.reactive({conversation: {id: "10"}, modelValue: []});
  let readsFail = true, savesFail = true, posts = 0;
  const fixture = await scopeModule(props, {get: async () => ({items: [{id: "7"}, {id: "8"}]})}, {
    conversation: async () => { if (readsFail) throw new Error("读取失败"); return {id: "10", version: 4, knowledgeBaseIds: ["7"]}; },
    knowledgeBases: async (id, ids, version) => {
      posts++; assert.equal(id, "10"); assert.equal(version, 4); assert.deepEqual(Array.from(ids), ["7", "8"]);
      if (savesFail) throw new Error("保存失败"); return {id, version: 5, knowledgeBaseIds: ids};
    },
  });
  await fixture.m.show(); await fixture.m.save();
  assert.equal(posts, 0); assert.equal(fixture.m.loaded.value, false);
  readsFail = false; await fixture.m.load(); fixture.m.selected.value.push("8"); await fixture.m.save();
  assert.equal(fixture.m.open.value, true); assert.match(fixture.m.error.value, /保存失败/);
  assert.deepEqual(Array.from(fixture.m.selected.value), ["7", "8"]);
  savesFail = false; await fixture.m.save();
  assert.equal(posts, 2); assert.equal(fixture.m.open.value, false); assert.equal(fixture.emitted.length, 1);
  fixture.dispose();
});

test("late scope reads and saves from a previous conversation cannot alter the new conversation", async () => {
  const oldRead = deferred(), oldSave = deferred();
  const props = Vue.reactive({conversation: {id: "old"}, modelValue: []});
  const fixture = await scopeModule(props, {get: async () => ({items: [{id: "7"}, {id: "8"}]})}, {
    conversation: id => id === "old" ? oldRead.promise : Promise.resolve({id, version: 2, knowledgeBaseIds: ["8"]}),
    knowledgeBases: () => oldSave.promise,
  });
  const reading = fixture.m.show();
  props.conversation = {id: "next"}; await Vue.nextTick(); await fixture.m.show();
  oldRead.resolve({id: "old", version: 0, knowledgeBaseIds: ["7"]}); await reading;
  assert.deepEqual(Array.from(fixture.m.selected.value), ["8"]);
  const saving = fixture.m.save();
  props.conversation = {id: "third"}; await Vue.nextTick();
  oldSave.resolve({id: "next", knowledgeBaseIds: ["8"]}); await saving;
  assert.equal(fixture.emitted.length, 0); assert.equal(fixture.m.open.value, false);
  fixture.dispose();
});
test("file validation and document states distinguish upload, parsing and index readiness",async()=>{
  const {m}=await queueModule({});
  assert.equal(m.validateImportFile(file("hello.MD"),capabilities),"");
  assert.match(m.validateImportFile(file("a.doc"),capabilities),/格式/);
  assert.match(m.validateImportFile(file("a.txt",0),capabilities),/为空/);
  assert.match(m.validateImportFile(file("a.txt",capabilities.maxFileBytes+1),capabilities),/超过/);
  assert.equal(m.documentState({status:"UPLOADED"}),"WAIT_PARSE");
  assert.equal(m.documentState({status:"PARSING"}),"PARSING");
  assert.equal(m.documentState({status:"READY",indexStatus:"PENDING"}),"INDEXING");
  assert.equal(m.documentState({status:"READY",indexStatus:"FAILED"}),"FAILED");
  assert.equal(m.documentState({status:"READY",indexStatus:"READY"}),"READY");
});
test("batch uploading limits concurrency and pins the target library at enqueue time",async()=>{
  const requests=[];
  const fixture=await queueModule({upload:(file,options)=>{const d=deferred();requests.push({file,options,...d});return d.promise;}});
  const queue=fixture.m.useKnowledgeImport(Vue.ref(capabilities));
  const base={id:"9223372036854775000",name:"英语"};
  queue.enqueue([file("1.txt"),file("2.txt"),file("3.txt")],base);
  base.id="other";base.name="Java";
  assert.equal(requests.length,2); assert.equal(queue.items.value[2].state,"WAITING");
  requests[0].resolve({created:true,document:{id:"doc1",status:"UPLOADED"}}); await tick();
  assert.equal(requests.length,3);
  assert.ok(requests.every(r=>r.options.knowledgeBaseId==="9223372036854775000"));
  requests[1].reject(new Error("单文件失败"));
  requests[2].resolve({created:false,document:{id:"doc3",status:"READY",indexStatus:"READY"}});await tick();
  assert.equal(queue.items.value[0].state,"WAIT_PARSE");
  assert.equal(queue.items.value[1].state,"FAILED");assert.equal(queue.items.value[2].state,"DUPLICATE");
  fixture.dispose();
});
test("invalid files do not stop valid files and oversized batches are rejected before uploading",async()=>{
  let uploads=0;
  const fixture=await queueModule({upload:async()=>{uploads++;return {created:true,document:{id:"a",status:"UPLOADED"}};}});
  const queue=fixture.m.useKnowledgeImport(Vue.ref(capabilities));
  queue.enqueue([file("bad.exe"),file("empty.txt",0),file("good.txt")],{id:"1",name:"英语"});await tick();
  assert.equal(uploads,1);assert.equal(queue.items.value[0].state,"INVALID");assert.equal(queue.items.value[2].state,"WAIT_PARSE");
  assert.throws(()=>queue.enqueue(Array.from({length:21},()=>file()),{id:"1",name:"英语"}),/最多/);
  assert.equal(uploads,1);fixture.dispose();
});
test("backend processing retry refreshes the version and never uploads a second copy",async()=>{
  let uploads=0,reads=0;const posts=[];
  const fixture=await queueModule({
    upload:async()=>{uploads++;return {created:true,document:{id:"doc",status:"READY",indexStatus:"FAILED",version:1}};},
    get:async()=>++reads===1?{id:"doc",version:7,status:"READY",indexStatus:"FAILED"}:{id:"doc",version:8,status:"READY",indexStatus:"READY"},
    post:async path=>posts.push(path),
  });
  const queue=fixture.m.useKnowledgeImport(Vue.ref(capabilities));queue.enqueue([file()],{id:"1",name:"英语"});await tick();
  await queue.retry(queue.items.value[0]);
  assert.equal(uploads,1);assert.deepEqual(posts,["knowledge/doc/retry?version=7"]);assert.equal(queue.items.value[0].state,"READY");fixture.dispose();
});
test("upload failures retry the retained file while closing progress does not cancel the queue",async()=>{
  let count=0;const fixture=await queueModule({upload:async()=>{if(++count===1)throw new Error("连接失败");return {created:true,document:{id:"doc",status:"UPLOADED"}};}});
  const queue=fixture.m.useKnowledgeImport(Vue.ref(capabilities));queue.enqueue([file()],{id:"1",name:"英语"});await tick();
  queue.open.value=false;await queue.retry(queue.items.value[0]);await tick();
  assert.equal(count,2);assert.equal(queue.items.value[0].state,"WAIT_PARSE");fixture.dispose();
});
test("pending files guard navigation and disposal aborts only browser transfers",async()=>{
  const requests=[];const fixture=await queueModule({upload:(f,o)=>{requests.push(o);return new Promise(()=>{});}});
  const queue=fixture.m.useKnowledgeImport(Vue.ref(capabilities));queue.enqueue([file(),file(),file()],{id:"1",name:"英语"});
  let prevented=false;fixture.events.get("beforeunload")({preventDefault(){prevented=true;}});assert.equal(prevented,true);
  queue.remove(queue.items.value[2]);assert.equal(queue.items.value.length,2);
  fixture.dispose();assert.ok(requests.every(r=>r.signal.aborted));assert.equal(fixture.events.size,0);
});
test("knowledge templates compile in development with comments enabled",async()=>{
  for(const name of ["views/agent/AgentKnowledgeView.vue","views/agent/AgentKnowledgeDetailView.vue","views/agent/AgentChatView.vue","components/agent/KnowledgeScopePicker.vue","components/agent/KnowledgeFormatHelp.vue","components/agent/AgentMessageItem.vue","components/agent/AgentConversationPane.vue"]){
    const source=await readFile(new URL(name,root),"utf8"),descriptor=parse(source).descriptor;
    const script=compileScript(descriptor,{id:name});
    const template=compileTemplate({source:descriptor.template.content,filename:name,id:name,isProd:false,compilerOptions:{comments:true,bindingMetadata:script.bindings}});
    assert.deepEqual(template.errors,[],name);
  }
});
