from write_main import *

APIFILE={'02':'附录/A02-公共接口规范.md','03':'附录/A03-账户接口与对象.md','04':'附录/A04-目标计划接口与对象.md','05':'附录/A05-执行查询接口与对象.md'}
APPENDIX={'01':'附录/A01-完整数据字典.md',**APIFILE,'06':'附录/A09-分层实施细则.md','07':'附录/A06-页面交互.md','08':'附录/A10-专项验收与恢复.md','09':'附录/A11-需求追溯.md','10':'附录/A12-业务全景.md','11':'附录/A07-系统处理.md','12':'附录/A08-技术版本基线.md','00':'09-基线核对与需求追溯.md'}
NAMES=['账户','时区区间','账户并发锁','运行护栏','管理护栏','登录尝试','验证挑战','目标','阶段','任务','目标永久节点','目标结束快照','周计划','日计划','任务当前安排','任务完成事实','专注执行','计时区间','执行控制','时间修正','按日投入归属','习惯','习惯规则版本','规则星期','习惯永久节点','习惯日结果','站内通知','系统配置','审计事件','请求去重','可靠事件','安全变更','周期检查点','框架会话','框架会话属性','迁移历史']
for t,name in zip(TABLE,NAMES):t['cn']=name

def normalize(t):
    pairs={'本轮':'本版','你要求':'项目约定','你的核心约束':'现行约束','继承你的':'继承现行','用户本轮':'现行设计','手写':'实施','讲义':'功能说明','本包':'本设计包','供人工编写':'供开发人员编写','怎样':'如何','到底':'具体','不把':'不得将','不能谎称':'不视为','不伪造':'不生成虚假','假装达标':'认定达标','凑指标':'满足指标','你的':'项目的'}
    for a,b in pairs.items():t=t.replace(a,b)
    # Remove obsolete process status and chat-oriented instructions; preserve business contracts.
    t=re.sub(r'^.*用户已授权连续补齐设计.*\n?', '',t,flags=re.M)
    t=re.sub(r'^>.*(没有创建工程|本版没有|本版提前|本版不创建|本版未生成).*\n?', '',t,flags=re.M)
    t=t.replace('应用工程、依赖锁文件及运行验收尚未建立','后端骨架已建立；前端锁文件及业务运行验收尚未建立')
    t=t.replace('当前不创建任何工程目录或POM。','后端父子工程骨架已存在，业务结构仍为设计落点。')
    t=t.replace('当前尚无前端工程','当前尚无前端工程')
    t=t.replace('本次只修订开发文档和离线阅读页。后续创建 `backend/`、`frontend/` 时再将本文落实到父 POM、前端清单、锁文件和构建环境；本文不表示已经安装或构建这些组件。','后端父子工程与 Java/Boot/MyBatis 版本已写入 POM，当前只通过工程模型校验。前端工程及锁文件尚未建立，前端版本表属于既有设计记录，创建工程前需重新核验可解析版本与兼容关系。')
    t=t.replace('后续创建 `backend/`、`frontend/` 时','后续完善 `backend/`、创建 `frontend/` 时')
    return t

def rewrite(t,current,oldnum):
    def sub(m):
        text,target=m.group(1),m.group(2)
        if target.startswith(('http:','https:','mailto:')):return m.group(0)
        if target.startswith('#'):
            if not current.startswith('模块设计/'):return m.group(0)
            import os
            rel=os.path.relpath(OUT/APPENDIX[oldnum],(OUT/current).parent).replace('\\','/')
            return '['+text+']('+rel+target+')'
        path,sep,anchor=target.partition('#')
        base=Path(path).name
        found=re.match(r'(\d{2})-',base)
        if base=='03-系统设计文档.md':dest=OUT/base
        elif found and found.group(1) in APPENDIX:
            dest=OUT/APPENDIX[found.group(1)]
        elif base in ['业务规则.md','功能需求基线.md','质量需求基线.md']:dest=OUT/'编制源文件'/base
        elif base in ['README.md','index.html']:dest=OUT/'README.md';anchor='';sep=''
        else:return m.group(0)
        import os
        rel=os.path.relpath(dest,(OUT/current).parent).replace('\\','/')
        return '['+text+']('+rel+(('#'+anchor) if sep else '')+')'
    return re.sub(r'\[([^\]]+)\]\(([^\s)]+)\)',sub,normalize(t))

def appendices():
    for n,filename in APPENDIX.items():
        if n=='00':continue
        t=read(source(n))
        t=re.sub(r'^# .*\n', '# '+Path(filename).stem.replace('-', ' ',1)+'\n',t,count=1)
        first=re.search(r'^## ',t,re.M)
        if first:t=t[:t.index('\n')]+f'\n\n> 文档版本 V2.0；核对日期 {DATE}。继承现行契约和业务规则。架构细化为本地设计审阅稿。后端业务尚未实现；工作树已有最小账户表 V001，用户说明已执行，真实数据库和迁移历史未核实。\n\n'+t[first.start():]
        if n=='09':
            t=t.split('## 5.')[0]+'\n## 当前基线\n\n历史变更不作为当前运行状态。本版基点、真实工程清单、静态差异及验证状态统一见 09 基线核对与需求追溯。\n'
        if n=='06':
            t=t.replace('## 2. 未来Maven父子工程与目录','## 2. Maven父子工程与规划目录')
            t=t.replace('工程目录与POM按DEV-001实施。','父 POM 与 10 个子工程已存在；DEV-001 先核验骨架再补迁移与资源，不重复建一套工程。12 个业务模块与 10 个子工程的完整映射见 [05 编码实施手册](../05-编码实施手册.md)。')
            t=t.replace('未来模块/位置','工程位置（骨架已存在）')
            t=t.replace('`backend/pom.xml` 与Maven Wrapper','`backend/pom.xml` 与 Wrapper 版本属性')
            t=t.replace('Wrapper发行版固定3.9.4','Wrapper 属性指定发行版 3.9.4；尚无 mvnw/mvnw.cmd，当前使用已安装 Maven')
            t=t.replace('先读10明确需求与业务，再按01编写并验证数据库迁移，按02～05及11编写后端和接口/内部用例测试，完成后端阶段验收后制作07的页面原型，再写Vue 3 + TypeScript前端并联调，最后按08完整验收与准备发布。',
                        '先读 01/02 需求与 A12 业务全景，再按 A01/A13 编写并验证数据库迁移，按 A02～A05 接口及 A07 系统处理实施后端和内部用例测试；完成 05 的 DEV-015 后制作 07/A06 的页面原型，再写前端并联调，最后按 06 测试与 07 部署文档验收和准备发布。')
            # The appendix and main handbook share the same corrected DEV catalog.
            from implementation import dev_batches
            plan='\n以 [05 编码实施手册](../05-编码实施手册.md) 的十二模块覆盖矩阵核对完整性；DEV 为实施批次，不是 Maven 子工程。以下 19 批次和主手册共用同一数据源，业务状态均为 NOT_VERIFIED。\n\n'
            plan+=mdtable(['编号','依赖','先改哪里、后改哪里','交付与重点验证','失败时如何回退'],dev_batches())+'\n'
            t=re.sub(r'(## 7\. 手写业务切片\n).*?(?=^## 8\.)',lambda m:m.group(1)+plan,t,flags=re.S|re.M)
            t=re.sub(r'(## 9\. 当前交付状态\n).*?(?=^## 10\.)',r'\1\n后端骨架、统一响应、公共错误与 MVC 异常/追踪基础已存在，多模块编译和 6 项基础契约测试通过。正式账户业务、Mapper XML、完整迁移及前端尚未建立；下文业务类与方法均为规划落点，业务运行状态为 NOT_VERIFIED。\n\n',t,flags=re.S|re.M)
        if n=='12':
            t=t.replace('2026-10-19 已核对上述包的 npm 正式发布信息、`engines` 和 `peerDependencies`。','2026-10-19 的既有版本册记录了下列前端版本与兼容说明。本次未重新从 npm 仓库验证这些候选值；不能据此声明依赖已安装或可构建。')
            t=t.replace('DEV-001 按本文创建统一父 POM、10 个子模块和 app 配置','DEV-001 按本文复核已存在的父 POM、10 个子模块和 app 配置')
            t=t.replace('| 直接执行 `java -version` | Java 21.0.10 | 当前 PATH 中的 Java 与 Maven 不一致；开发终端需要切换到 JDK 17 |','| 2026-10-19 直接执行 `java -version` 的历史输出 | Java 21.0.10 | 与当时 Maven 使用的 JDK 不一致；本次未重新核对全局 PATH，开发前需确认 |')
        write(filename,rewrite(t,filename,n))
    # Expanded columns make all common fields explicit and independently reviewable.
    t=meta('A13 展开字段字典')+'共 36 张设计表，公共字段展开后共 '+str(sum(len(t['fields']) for t in TABLE))+' 列定义。工作树已有 1 份 V001 最小账户迁移，定义 1 表9列；用户说明已执行，实际数据库和历史未核验。420 是设计列数，账户完整设计为19列；保护 V001 后追加迁移。物理外键设计数为 0，图示关联均为应用维护的逻辑关系。\n\n'
    for tb in TABLE:
        t+=f'## {tb["id"]} {tb["name"]}（{tb["cn"]}）\n\n'
        t+=mdtable(['字段','PostgreSQL 类型','长度/精度','可空','默认/生成规则','键或业务约束及含义','来源'],[[f['name'],f['type'],re.search(r'\(([^)]+)\)',f['type']).group(1) if '(' in f['type'] else '由类型定义',f['nullable'],f['default'],f['meaning'],f['origin']] for f in tb['fields']])
    write('附录/A13-展开字段字典.md',t)

def associated(g):
    if g=='AUTH':return [a for a in API if a['id'].split('-')[1] in ['AUTH','PUB']]
    if g in ['WEEK','DAY']:
        refs=set(re.findall(r'API-[A-Z]+-\d{3}',' '.join(f['operations'] for f in FR if f['group']==g)))
        return [a for a in API if a['id'] in refs or (a['id'].startswith('API-PLAN') and g=='WEEK')]
    return [a for a in API if a['id'].split('-')[1]==g or a['id'] in set(re.findall(r'API-[A-Z]+-\d{3}',' '.join(f['operations'] for f in FR if f['group']==g)))]

def function_flow(f,apis):
    refs=re.findall(r'API-[A-Z]+-\d{3}',f['operations'])
    match=next((a for aid in refs for a in apis if a['id']==aid),None)
    jobs=re.findall(r'JOB-\d{3}',f['operations'])
    if jobs:match=None
    b=f'### {f["id"]} {f["name"]}\n\n'
    if match:
        b+='**触发、发起者与前置：**\n\n'+normalize(match['analysis'])+'\n\n'
        b+='**输入：**采用本章第三部分 '+match['id']+' 的路径、查询或请求体；身份取服务端会话，不能以客户端归属字段授权。\n\n'
        b+='**事件流：**\n\n'+normalize(match['flow'])+'\n\n'
        b+='**数据与业务规则：**同动作的字段、版本、日期和唯一性约束见第三部分及其完整对象字典；事务内重读、校验归属及父链。\n\n'
        b+='**异常、取消与执行结果：**参数、权限、版本及状态拒绝不产生部分业务写入；成功以提交后的权威状态为准。需要先独立持久化到期补偿的动作遵循 A07；已提交的前序结束执行不随后续拒绝回滚。请求发送前取消不写入；提交后取消不能撤销已提交事实。各动作特有错误如下节接口定义。\n\n'
        b+='**验收入口：**'+f['tests']+'；本接口功能测试：\n\n'+normalize(match['tests'])+'\n\n'
    else:
        b+='**触发：**'+('调度、访问前补偿或内部事件' if f['name'].startswith('系统') else '用户在对应入口操作')+'。**发起者：**'+('系统' if f['name'].startswith('系统') else '当前用户')+'。\n\n'
        b+='**前置：**本人有效身份或受控系统上下文；日期、对象和状态满足现行业务规则。\n\n**输入：**当前状态、权威时刻和有效业务日，不接收客户端虚构完成事实。\n\n'
        b+='**事件流：**核对身份和对象 → 按 A07 先补偿到期事实 → 执行 '+f['operations']+' → 形成结果或标准拒绝 → 留存必要事实及可追踪记录。\n\n'
        b+='**规则与异常：**'+scenes[f['group']][4]+' '+scenes[f['group']][5]+' 系统处理使用原业务身份去重，失败不提前推进检查点。\n\n**执行结果：**'+f['name']+'；无权或依赖失败不伪装成空结果。\n\n**关联验证：**'+f['tests']+'。\n\n'
    if jobs:
        text=read(source('11'))
        for job in jobs:
            jm=re.search(r'^## [^\n]*'+job+r'[^\n]*\n(.*?)(?=^## |\Z)',text,re.M|re.S)
            if jm:
                jobbody=jm.group(1)
                flow=section(jobbody,'### 业务流程',3) or section(jobbody,'### 业务流程：习惯日结算',3)
                if flow:b+='**'+job+' 详细事件流：**\n\n'+normalize(flow)+'\n\n'
    b+='**关联功能/处理：**'+f['operations']+'；**开发批次：**'+f['dev']+'。\n\n'
    return b

def class_design(g,eng,stem,scope):
    """Keep HTTP objects and business-layer responsibilities consistent across all modules."""
    usecase_package,usecase={'AUTH':('identity','AccountUseCase'),'GOAL':('goal','GoalLifecycleUseCase'),'STAGE':('goal','StageUseCase'),'TASK':('task','TaskUseCase'),'WEEK':('planning','PlanChangeUseCase'),'DAY':('planning','PlanChangeUseCase'),'FOCUS':('execution','ExecutionCommandUseCase'),'HABIT':('habit','HabitCommandUseCase'),'DASH':('insights','DashboardQueryUseCase'),'HISTORY':('insights','HistoryQueryUseCase'),'NOTICE':('notification','NotificationUseCase'),'ADMIN':('administration','AdminAccountUseCase')}[g]
    service_role='查询口径与只读投影，不写业务事实' if eng=='insights' else '规则与本模块拥有数据的写入'
    t=f'''```text
backend/{eng}/src/main/java/com/asterway/{eng}/
  api/{stem}Api.java           模块之间调用的 Java 公开能力
  api/model/                  公开 Command / Result / Snapshot，按用例创建
  api/error/                  本业务错误码和必要的带类型异常
  service/{stem}Service.java   {service_role}
  mapper/{stem}Mapper.java     本模块数据访问或明确允许的只读投影
  model/                      内部存储/查询模型，不直接作为 HTTP 响应
backend/{eng}/src/main/resources/mapper/{eng}/{stem}Mapper.xml
backend/app/src/main/java/com/asterway/app/
  web/{eng}/{stem}Controller.java
  web/{eng}/request/           请求 DTO：前端提交的允许字段
  web/{eng}/response/          响应 VO：返回前端的安全字段
  application/{usecase_package}/{usecase}.java   需要协调时创建
```

`request` 和 `response` 是 Java 包，分别存请求 DTO 和响应 VO；DTO/VO 是数据传输对象，不是额外 Maven 工程。请求 JSON 绑定为请求 DTO，Controller 调用用例/公开能力，业务结果映射为响应 VO，再放入统一 `ApiResponse.data`。`api` 指模块间 Java 调用契约，HTTP 路由集中在 app 的 Controller。

### 4.2 类清单

'''
    t+=mdtable(['包','类/接口','类型','职责','依赖','关键技术'],[
        [f'com.asterway.app.web.{eng}',stem+'Controller','Controller','路由、校验、DTO 映射','应用用例/公开能力','Spring MVC'],
        ['com.asterway.app.application.'+usecase_package,usecase,'应用用例（按需）','锁、幂等、补偿及跨模块协调',eng+'公开能力、common','受代理的事务入口'],
        [f'com.asterway.{eng}.api',stem+'Api','公开业务契约','声明有类型的公开输入与结果','common；api/model','Java 进程内调用'],
        [f'com.asterway.{eng}.service',stem+'Service','业务服务',service_role,'本模块 Mapper','实现公开能力；不机械加 Impl'],
        [f'com.asterway.{eng}.mapper',stem+'Mapper','Mapper','数据访问；insights/admin 仅按明确投影例外跨表只读','XML','统一扫描；不写 SQL 注解'],
        [f'com.asterway.app.web.{eng}.request',stem+'Request','请求 DTO','仅接收允许字段','接口字段契约','jakarta.validation'],
        [f'com.asterway.app.web.{eng}.response',stem+'Response / View','响应 VO','仅输出安全字段','接口响应契约','放入 ApiResponse.data']])
    t+=f'\n### 4.3 关键类设计\n\n`{usecase}` 是需要应用协调时的落点示例，不要求为每条只读接口建立转发类。写命令涉及统一锁、幂等、补偿和跨模块原子性时由应用用例协调；具体 Service 实现公开能力，不再机械增加同义 Service 接口和 ServiceImpl。公开输入/结果在 api/model，内部存储模型在 model。\n\n'
    if g=='TASK':t+='任务完成的协调另用 `app/application/task/TaskCompletionUseCase.java`：先确认所有执行已结束，再原子保存完成事实、撤销安排和写可靠事件；普通任务管理不全部塞进完成用例。\n\n'
    return t

def module_doc(i,m):
    g,name,eng,scope,db,entry,dep=m; mid=MID[g];fs=[f for f in FR if f['group']==g];apis=associated(g)
    filename=f'模块设计/{mid}-{name}.md'
    t=meta(f'04 {mid} {name}模块设计')+f'> 对应功能：{", ".join(f["id"] for f in fs)}\n> 工程归属：`backend/{eng}`；HTTP 和跨模块应用用例归 `backend/app`。\n\n'
    t+='## 一、模块范围\n\n### 1.1 模块职责\n\n'+scope+'。'+scenes[g][4]+'\n\n### 1.2 对应功能清单\n\n'+mdtable(['功能','名称','本模块承担部分'],[[f['id'],f['name'],f['operations']] for f in fs])
    t+='\n### 1.3 模块依赖\n\n'+mdtable(['依赖','使用原因','交互内容','失败处理'],[[dep,'核对业务资格及形成统一结果','由 app 在同一上下文协调业务公开能力','依赖不可用按接口错误反馈；核心事务整体回滚'],['common','身份、时间、锁、幂等、错误及事件机制','明确上下文与固定锁序','恢复门禁未开放时拒绝相关写入'],['PostgreSQL','持久化与约束','本人数据和唯一性/排除约束','提交未知则保留原意图核实']])
    t+='\n## 第一部分：业务逻辑\n\n'
    for f in fs:t+=function_flow(f,apis)
    t+='### 状态变化\n\n'
    state={
      'GOAL':[['待开始','达到开始日且未主动归档','ACTIVE','JOB-001；不创建计划'],['ACTIVE','本人确认结束且执行已结束','已结束','冻结快照；不代完成任务'],['未结束','归档且执行已结束','归档标记','不能执行；恢复不抹历史']],
      'STAGE':[['可推进','暂停','暂停','可规划，不允许执行'],['暂停','恢复','可推进','重验父目标'],['可推进','当前有效未完成任务为零且确认完成','已完成','阶段可重新打开，任务完成不撤销']],
      'TASK':[['未完成','合法完成确认','DONE','唯一完成事实；不可恢复未完成'],['未完成','重新安排','原任务状态','只改当前安排'],['未完成/已完成','删除且全部执行已结束','移出当前范围','有历史保留身份']],
      'FOCUS':[['无执行','开始','RUNNING','创建新区间'],['RUNNING','暂停或超时','PAUSED','终点按可信点'],['PAUSED','继续','RUNNING','本次累计延续'],['RUNNING/PAUSED','结束','ENDED','新一次执行另建身份']],
      'HABIT':[['有效','暂停','暂停','保留已完成结果'],['暂停','恢复','有效','按当前规则及日期重验'],['有效/暂停','结束或删除','结束/删除','过去不重算；删除不可恢复']],
      'NOTICE':[['条件成立','可靠消费','待确认','同轮唯一'],['待确认','阅读','已读','不代表目标结束'],['待确认/已读','条件失效或目标关闭','已失效/已确认','重验轮次，不重复结束']]}
    t+=mdtable(['当前状态','触发','目标状态','约束/失败反馈'],state.get(g,[[scenes[g][3],'本章动作','已提交结果','重验版本、归属和日期；只读查询不改变业务事实']]))
    t+='''
### 事务、并发与重复请求

核心事实与必要审计、去重结果及 outbox 同库提交。锁序遵循运行护栏、管理锁（如适用）、按稳定顺序排列的账户锁；锁后重验当前时刻、归属、父链和 expectedVersion。昂贵密码计算、外部网络和重度统计不占用账户短事务。读请求复用必要到期补偿，但补偿与随后可拒绝业务的提交边界必须明确。

同一次意图使用同一 Idempotency-Key；不同参数重用旧键返回冲突。响应丢失不新建意图自动重做。安全账本采用先独立意图、同库提交、再同步结果的协议，结果无法核实保持隔离，不声称跨存储原子提交。

## 第二部分：数据库设计

### 2.1 数据表清单

'''
    owned=[tb for tb in TABLE if int(tb['id'][3:]) in db]
    t+=mdtable(['表','中文名','所属工程','用途','主键'],[[tb['name'],tb['cn'],eng,tb['body'].split('\n')[0],', '.join(f['name'] for f in tb['fields'] if 'PK' in f['meaning'] or f['name']=='id') or '见字段约束'] for tb in owned]) if owned else '本模块不拥有写入表。只读聚合其他领域的既有事实，不另建一套可漂移的完成或投入真相。\n'
    t+='\n### 2.2 表间关系\n\n本模块引用及跨域依赖见 A01 第 10 章的完整逻辑关系目录和删除检查。所有关系由应用事务检查，物理外键为 0，无数据库级联；历史身份保留规则不能由通用删除替代。HTML 数据关系图以虚线表示逻辑关系，字段明细中的关联标识不表示已建 FK。\n\n'
    t+='### 2.3 表字段设计\n\n'
    for tb in owned:
        t+=f'#### {tb["id"]} `{tb["name"]}`（{tb["cn"]}）\n\n'
        t+=mdtable(['字段','类型','长度/精度','可空','默认/生成','注释、键和规则'],[[f['name'],f['type'],re.search(r'\(([^)]+)\)',f['type']).group(1) if '(' in f['type'] else '按类型',f['nullable'],f['default'],f['meaning']] for f in tb['fields']])
        # Keep exact per-table prose on uniqueness, null combinations, lifecycle and deletion.
        lines=tb['body'].splitlines(); prose=[l for l in lines if not l.startswith('|') and not l.startswith('公共字段')]
        t+='\n'+normalize('\n'.join(prose)).strip()+'\n\n'
    if not owned:t+='不适用新增字段：查询模型及响应对象按第三部分和 A03～A05 定义；来源字段从 A13 可逐列查阅。\n\n'
    t+='### 2.4 约束与索引\n\n本模块的主键、唯一、行内 CHECK、时间排除和查询索引必须同时落实。精确约束清单集中在 A01 第 11～12 章，不以简写替换索引名称和字段顺序；模块字段表说明不可空、生成列和状态。当前尚无 SQL 迁移，不能将设计索引数量视为已安装数量。\n\n'
    t+='### 2.5 数据字典与规范\n\n枚举采用 A01 的稳定值；时刻用 UTC 和 timestamptz，业务日期按有效 IANA 时区冻结；版本和时长通过 HTTP 传输为字符串。金额不适用，本期没有支付。软删除及历史保留以动作定义为准，不统一物理删除。记录永久业务事实时保留完整正文，技术载荷限制不截断结束/完成快照。\n\n'
    t+='### 2.6 数据库脚本/迁移文件\n\n集中目录为 `backend/app/src/main/resources/db/migration/`。工作树已有 `V001__create_app_user.sql` 最小账户表脚本，用户说明已执行，真实数据库及 Flyway 历史未核实。该脚本不改写、删除或重命名；当前暂存为空，提交前须核对实际执行原文。后续按 MG-01～MG-07 设计内容组规划，MG 编号不等于 Flyway 版本；真实文件版本须核实历史后分配未占用编号，不能再次建立另一份 V001。完整分组与前置见 05。\n\n'
    t+='## 第三部分：接口设计\n\n### 3.1 公共接口约定\n\n统一继承 A02：`/api/v1`、JSON、服务端会话、写入 CSRF、必要 Idempotency-Key、统一错误、分页与安全归属。接口示例中的凭据是说明性合成值，不是实际秘密。响应对象包含嵌套字段、null、数组及分页外壳的完整字典见 A03～A05；不得只复制顶层字段。\n\n### 3.2 接口清单\n\n'
    t+=mdtable(['编号','方法','路径','名称','权限/用途'],[[a['id'],a['method'],a['path'],a['name'],'遵守该接口认证、本人归属或后台角色限制'] for a in apis])
    for a in apis:
        t+=f'\n### {a["id"]} {a["name"]}\n\n'+rewrite(a['contract'],filename,a['source'])+'\n\n'
        t+='**完整字段与功能契约：**见 ['+a['id']+' 对象及示例](../'+APIFILE[a['source']]+'#'+a['id'].lower()+')。\n\n'
    t+='## 第四部分：类设计\n\n### 4.1 包结构\n\n现有工程仅含包说明；以下为本模块的规划类，尚未实现。\n\n'
    stem={'AUTH':'Account','GOAL':'Goal','STAGE':'Stage','TASK':'Task','WEEK':'WeeklyPlan','DAY':'DailyPlan','FOCUS':'Focus','HABIT':'Habit','DASH':'Dashboard','HISTORY':'History','NOTICE':'Notification','ADMIN':'Administration'}[g]
    t+=class_design(g,eng,stem,scope)
    t+=mdtable(['契约入口','输入','输出','公开职责与边界'],[[a['id'],a['method']+' '+a['path'],'第三部分成功响应模型',a['name']+'；授权、日期、状态、版本及异常按本节契约重验。'] for a in apis])
    for a in apis:
        t+=f'\n#### {a["id"]} 数据操作与实现职责\n\n'+normalize(a['operations'])+'\n\n'+normalize(a['implementation'])+'\n\n'
    t+='''### 4.4 层次职责与调用链

Controller → HTTP DTO 校验/转换 → app 应用用例 → 领域公开能力/Service → 本模块 Mapper/XML → PostgreSQL → 业务结果 → 安全 VO。业务异常在服务层产生，由 app 的全局异常处理映射到 A02 的 HTTP 和业务码；数据库约束冲突只转换为可披露的标准错误，不回传 SQL、内部路径或堆栈。

Controller 不发起独立通知或跨域直接写表；业务模块不引用兄弟模块 Mapper。只保留 insights 聚合事实、administration 授权账户摘要两类明确的只读投影例外，不取得其他模块写权。账户写入由 identity 处理，app 协调管理权限。

## 五、模块设计一致性检查

功能编号、业务步骤、表字段、接口及类职责已按本模块对应。实现落地、真实迁移、接口执行、并发和页面验收仍为 NOT_VERIFIED。09 记录编码前差异，06 定义执行证据和退出标准。
'''
    write(filename,t)

def common_doc():
    a=next(a for a in API if a['id']=='API-COMMON-001')
    t=meta('04 公共能力与跨模块契约')+'''## 一、模块范围

这是工程支撑规范，不增加第十三个产品模块。common 提供中立机制，app 处理 HTTP、安全装配、调度及跨业务协调；八个业务工程保持数据写入所有权。

## 第一部分：业务逻辑

请求进入 → 认证/CSRF → 必要运行与恢复门禁 → 到期补偿 → 原意图结果核实 → 锁后版本、父链、归属和资格重验 → 核心事务 → 权威结果。重复请求不重复效果，未知结果不新建操作重做。系统处理 JOB-001～007 的完整流程见 A07。

## 第二部分：数据库设计

DB-03～05、DB-29～31、DB-33 是公共机制表：中立契约与端口归 common，具体锁、去重、outbox、审计和检查点持久适配归 app/infrastructure，迁移仍集中 app。框架会话与迁移历史 DB-34～36 分别由 app 的 Session 适配和 Flyway 管理，业务代码不手改历史。身份拥有 DB-32 安全变化本地依据，独立账本在数据库之外。完整字段、约束和删除规则见 A01/A13，不建立物理外键。

## 第三部分：接口设计

除业务接口外，公共结果查询是第 95 个唯一 HTTP 契约；完整请求、响应和错误如下。

'''+rewrite(a['body'],'模块设计/公共能力.md','02')+'''

## 第四部分：类设计

| 规划位置 | 类/职责 | 依赖与边界 |
|---|---|---|
| common/context | 身份与业务日上下文 | 不承载领域对象 |
| common/error、exception、pagination | ErrorCode、CommonErrorCode、BusinessException、CursorPage | 中立契约，不依赖 Web/Security 或业务工程 |
| common/concurrency | 固定锁序及护栏端口 | 不代业务资格，不承载具体 SQL |
| common/idempotency | 原意图和结果的中立契约/端口 | 不保存凭据或正文 |
| common/event | 中立事件契约/端口 | 不决定目标结束，持久适配归 app |
| app/infrastructure | 护栏、去重、outbox、审计、检查点适配 | 只访问自身机制表，不直接写领域表 |
| app/web/contract | ApiResponse、ErrorData、Violation | HTTP 外壳与安全数据，data 承载响应 VO |
| app/web/error、web/filter | Advice、ApiErrorFactory/Writer、TraceIdFilter | 与安全入口共享响应契约，分别处理边界 |
| app/application | 跨模块命令与读前补偿 | 取得稳定上下文后调用公开能力 |
| app/security | 过滤链、CSRF、授权版本、会话适配 | 框架事务内无外键清理 |
| app/bootstrap | 运行代次和恢复门禁 | 未核实安全依据不开放 |
| app/scheduling | JOB 调度与补偿 | 重用业务用例，不拼跨域 SQL |

规划类尚未存在。现有真实启动类、POM 和配置见 09 的文件清单。核心事务和锁序细节、框架会话适配测试见 A09/A10。

## 五、模块设计一致性检查

逐请求授权、稳定锁序、原意图查询、核心提交、可靠消费和恢复门禁必须共同验证；单一机制通过不能代表整条业务链通过。
'''
    write('模块设计/公共能力.md',t)

def run():
    appendices()
    for i,m in enumerate(MODULES,1):module_doc(i,m)
    common_doc()
    write('04-模块设计文档.md',meta('04 模块设计文档')+'本册是十二个业务分册与公共规范的索引。每个业务分册完整采用“业务逻辑→数据库设计→接口设计→类设计”的顺序。响应对象、公共字段和系统处理在附录集中维护，以保持唯一口径。\n\n'+mdtable(['编号','模块','工程','功能数','分册'],[[MID[g],name,eng,sum(f['group']==g for f in FR),link_module(g)] for g,name,eng,*_ in MODULES])+'\n[公共能力与跨模块契约](模块设计/公共能力.md)\n\n接口引用可能跨模块；全局接口总数按“HTTP 方法+路径”的唯一集合统计为 95，不将重复引用重复计数。36 张表的每列定义已在 A13 展开；工作树 V001 仅定义最小账户表，用户说明已执行，完整设计迁移和真实数据库核实尚未完成。\n')
    print('Written 12 module documents, shared contract and 13 appendices')

if __name__=='__main__':run()
