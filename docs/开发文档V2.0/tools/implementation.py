"""Business-module, Maven-project and delivery-batch correspondence."""
import re
import xml.etree.ElementTree as ET
from write_main import ROOT, MODULES, MID, FR, read, source, section, table_rows, mdtable, link_module

NS={'m':'http://maven.apache.org/POM/4.0.0'}

def engineering_inventory():
    parent=ET.parse(ROOT/'backend/pom.xml').getroot()
    children=[x.text for x in parent.findall('m:modules/m:module',NS)]
    dependencies={}
    for name in children:
        pom=ET.parse(ROOT/'backend'/name/'pom.xml').getroot()
        dependencies[name]=[d.findtext('m:artifactId',namespaces=NS)
                            for d in pom.findall('m:dependencies/m:dependency',NS)
                            if d.findtext('m:groupId',namespaces=NS)=='com.asterway']
    return children,dependencies

def expand_dev(value):
    result=set()
    for first,last in re.findall(r'(?:DEV-)?(\d{3})(?:[～—–-](?:DEV-)?(\d{3}))?',value):
        result.update(f'DEV-{n:03}' for n in range(int(first),int(last or first)+1))
    return result

def dev_batches():
    body=section(read(source('06')),'## 7. 手写业务切片',2)
    rows=[r for r in table_rows(body) if re.match(r'DEV-\d{3} ',r[0])]
    titles={
        'DEV-001':'现有 Maven 工程核验与数据库基线',
        'DEV-005':'目标管理、阶段管理与任务管理主干',
        'DEV-006':'周计划、日计划与任务改期',
        'DEV-008':'任务完成、系统通知与基础统计',
        'DEV-009':'目标/阶段/任务/计划生命周期与历史保留',
        'DEV-010':'目标/计划日期补偿、计时边界与时区变更',
        'DEV-012':'今日看板、历史统计与执行记录查询',
        'DEV-013':'基础管理后台、账户管理与固定配置',
    }
    for row in rows:
        code=row[0].split()[0]
        if code in titles:row[0]=code+' '+titles[code]
        if code=='DEV-001':
            row[1]='01～03、A01/A13 数据库设计、A08 技术基线；先关闭适用 OPEN 项'
            row[2]='核对现有父 POM、10 个子工程和 11 项 Reactor → 核对 JDK 17/Maven 3.9.4/Boot 3.5.16 → 明确 common、8 业务工程、app 的依赖和打包边界 → 按本手册 4.2 实施迁移 → 检查跨模块 XML 与 PostgreSQL 17.11 隔离测试环境'
        elif code=='DEV-008':
            row[2]='growth 完成事实与目标条件 → app 完成用例/事务及 outbox → notification 通知写入、同轮去重、失效和已读 → app 通知查询/处理入口 → insights 基础查询；具体响应按 A04/A05'
            row[3]='完成前逐个结束执行并刷新版本，完成不自动结束目标；通知发送、列表、未读、已读和处理覆盖 FR-NOTICE-001～004；TEST-020～022、043、052～054'
        elif code=='DEV-012':
            row[2]='insights 今日看板与历史统计专用查询模型/XML → 汇总、筛选、未指定日期和待处理分组 → execution 执行/区间与修正历史只读查询 → app 查询 Controller；看板改期复用已完成的任务改期用例'
            row[3]='FR-DASH-001～009、FR-HISTORY-001～011 逐项核对；历史详情复用拥有模块接口，不另写历史事实；执行记录/修正历史、空结果/失败、原日期归属、分页及重度账户；TEST-042～044、058、067、070'
        elif code=='DEV-015':
            row[2]='按下表核对 12 个业务模块 → 按 A02～A05 核对全部接口契约 → 自动化回归 → 真实 API 调试与证据 → 缺陷闭环'
        elif code=='DEV-016':
            row[2]='DEV-015 后，依据 05 前端实施要求与 A06 页面交互制作原型 → 核对 12 模块的入口、20 页面及角色、日期、空态和冲突'
        elif code=='DEV-018':
            row[2]='全功能回归 → 按 06 测试文档与 08 运维恢复要求实测安全、性能、兼容和恢复 → 形成系统验收与 07 发布门槛报告'
        elif code=='DEV-019':
            row[4]='按 07 部署文档选择应用回退或数据恢复，并依 08 运维手册核对权限与历史后开放'
    return rows

def backend_structure():
    children,deps=engineering_inventory()
    business=list(dict.fromkeys(m[2] for m in MODULES))
    t='### 5.1 业务模块、Maven 子工程与构建项目的区别\n\n'
    t+=mdtable(['口径','数量','计算方式'],[
        ['业务模块',len(MODULES),'MOD-001～MOD-012，按产品需求划分'],
        ['Maven 业务子工程',len(business),'identity、growth、planning、execution、habit、insights、notification、administration'],
        ['Maven 全部子工程',len(children),'8 个业务工程 + common 公共能力 + app 应用入口'],
        ['Maven Reactor 项目',len(children)+1,'backend 父工程 1 项 + 子工程 10 项；validate 会显示 [1/11]～[11/11]']])
    t+='\n工程数量根据当前 `backend/pom.xml` 的 `<modules>` 和各子 POM 生成。业务模块采用多对一映射；不需要为每个 MOD 新建一个 Maven 工程，也不能把 DEV 实施批次误当成模块数。\n\n'
    t+=mdtable(['业务模块','需求组/数量','Maven 业务工程','app 接入位置（规划）','业务设计'],[
        [MID[g]+' '+name,f'FR-{g} / '+str(sum(f['group']==g for f in FR)),f'`backend/{eng}/`',f'`app/web/{eng}/`；跨模块用例在 `app/application/`',link_module(g)]
        for g,name,eng,*_ in MODULES])
    t+='\n### 5.2 现有 Maven 工程目录与依赖\n\n下列目录与 POM 已存在；各业务模块目前只有包说明，不能把规划的 Service、Mapper、Controller 或 XML 当作已经实现。\n\n```text\nbackend/                         # 聚合父工程，不生成可执行 JAR\n  pom.xml                        # 聚合 10 个子工程，统一版本\n  .mvn/wrapper/                  # 只有发行版本属性，尚无完整启动脚本\n'
    names={m[2]:'、'.join(x[1] for x in MODULES if x[2]==m[2]) for m in MODULES}
    names.update(common='公共身份标识、错误、时间、锁、幂等与事件机制',app='唯一启动应用；HTTP、安全、协调、调度、迁移与集成测试')
    for name in children:t+=f'  {name+"/":<30} # {names[name]}；已有 pom.xml\n'
    t+='```\n\n'
    t+=mdtable(['现有子工程','内部直接依赖（由 POM 读取）','产物/边界'],[
        [f'`backend/{name}/`','、'.join('`'+d+'`' for d in deps[name]) or '无内部模块依赖',
         '唯一可执行 JAR；依赖所有业务工程并负责装配' if name=='app' else '普通 JAR；不反向依赖 app，不启用可执行重打包']
        for name in children])
    t+='\n`<modules>` 表示聚合，`<dependencies>` 表示代码依赖；两者不能互相替代。app 直接依赖 common 和全部 8 个业务工程；各业务工程只依赖 common；common 不依赖业务或 app。业务间调用由 app 协调，禁止兄弟工程互相依赖。\n\n'
    t+='Java 17、Boot 3.5.16、MyBatis Starter 3.0.5 已在 POM 声明。只有 app 启用 Spring Boot 重打包。业务 SQL 的目标位置是各工程 `src/main/resources/mapper/<工程>/`；app 配置使用 `classpath*:mapper/**/*.xml`，迁移集中在 `app/src/main/resources/db/migration/`。业务 XML 尚未编写；工作树已有最小账户表 V001，用户说明已执行，真实历史未核实，不能重用或改写该版本。完整分层职责见 [A09 分层实施细则](附录/A09-分层实施细则.md)。\n\n'
    t+='### 5.3 工程核验、构建与启动顺序\n\n依次实现安全过滤链和响应空值策略、统一错误和 traceId、DTO 校验、账户锁、幂等及 outbox、受控时钟、框架会话适配、独立安全账本和启动恢复，再实现首条业务接口。\n\n'
    return t

def architecture_overview():
    children,deps=engineering_inventory()
    t='''### 3.3 Maven 多模块与模块内部的分层

`backend/pom.xml` 聚合 10 个 Maven 子工程，app 启动整个后端。模块按业务分工，分层按代码职责分工；request/response 是包和数据类，不新增 Maven 工程。

| 放在哪里 | 写什么 | 以创建目标为例 |
|---|---|---|
| app/web/growth | Controller、request 请求 DTO、response 响应 VO | 接收前端 JSON，校验允许字段，返回安全结果 |
| app/application/goal | 需要锁、幂等、事务或跨模块协调的应用用例 | 组织本次创建，调用目标公开能力 |
| growth/api、api/model | 模块间 Java 调用接口和输入/结果 | GoalApi 声明可调用的目标业务方法 |
| growth/service | 目标、阶段、任务的业务规则 | GoalService 实现资格和状态校验 |
| growth/mapper、resources/mapper/growth | Mapper 接口与 XML SQL | 绑定参数，查询或保存目标 |
| growth/model | 内部存储/查询模型 | 接收表行，不直接作为 HTTP 返回 |
| common | 通用错误、游标、上下文与机制端口 | 不放 Controller、领域规则和具体 Web 响应 |

请求 JSON → app 请求 DTO → Controller → 应用用例（需要时）→ 业务 Api/Service → Mapper/XML → 业务结果 → app 响应 VO → ApiResponse.data → 响应 JSON。公开 Api 由具体 Service 实现，不为单实现机械加 ServiceImpl。单模块只读可直接调用公开能力，涉及到期补偿时仍先执行受控补偿。

### 3.4 app、common 与业务工程的明确边界

采用 app 完整组装方案：所有 HTTP Controller、请求 DTO、响应 VO、统一响应和异常、安全/会话适配、跨模块用例、启动恢复、调度和集中迁移均归 app。Goal/Task/Habit 状态规则归各自业务工程，app 不直接取得业务 Mapper 写表。common 的公共锁、幂等和事件只保留中立契约/端口，具体持久实现落 app/infrastructure；这属于本版待审细化，不表示已经有实现需要搬迁。

只保留两类明确的跨域只读表投影：insights 的看板/历史聚合、administration 的授权账户摘要。它们不取得其他工程写权，也不形成兄弟工程 Java 依赖；账户写入仍归 identity。详细包树、MVC/Security 异常链和具体文件落点见 05 与 A09；每个模块第四部分同步同一套命名。

### 3.5 真实依赖矩阵与推荐依赖图

<!-- DIAGRAM:modules -->

行依赖列；1 为当前 POM 的直接内部依赖，— 为无依赖。父工程聚合/继承关系不计入这些边；当前与推荐方向一致。

'''
    t+=mdtable(['依赖方 \\ 被依赖方',*children],[[name,*['1' if col in deps[name] else '—' for col in children]] for name in children])
    edges=sum(map(len,deps.values()))
    t+=f'\n共 {edges} 条直接内部依赖；app 9 条、8 个业务工程各 1 条，无业务环。\n\n```mermaid\nflowchart TB\n'
    for name in children:t+=f'    {name}["{name}"]\n'
    for name in children:
        for dep in deps[name]:t+=f'    {name} --> {dep}\n'
    return t+'```\n\n'

def implementation_plan():
    t='### 7.2 十二个业务模块的实施覆盖矩阵\n\n先按业务模块核对需求覆盖，再按 DEV 依赖顺序编码。12 是业务模块数，19 是实施批次数；一个批次可以跨多个模块，一个模块也可以分多批完成。下表从 132 项 FR 的开发关联汇总，页面实现统一补充 DEV-017，后端统一在 DEV-015 检查，系统统一在 DEV-018 验收。\n\n'
    rows=[]
    for g,name,eng,*_ in MODULES:
        items=[f for f in FR if f['group']==g]
        batches=sorted(set().union(*(expand_dev(f['dev']) for f in items))|{'DEV-017'})
        tests='；'.join(dict.fromkeys(f['tests'] for f in items))
        rows.append([MID[g]+' '+name,f'FR-{g} / {len(items)} 项',f'`backend/{eng}/`','、'.join(batches),tests,link_module(g)])
    t+=mdtable(['业务模块','需求覆盖','Maven 工程','实施批次','综合验证关联','完整规则与接口'],rows)
    t+='\n### 7.3 十九个实施批次与跨模块依赖\n\n这些批次描述将来按什么顺序开发，当前业务状态均为 `NOT_VERIFIED`。测试编号引用本版 06 中同号 TC；数据库字段看 A01/A13，接口看 A02～A05，系统处理看 A07，技术基线看 A08。每批先选用例再编码，各阶段只验证当时已具备前置条件的子项，完整用例必须等全部适用子项都有证据后才通过。\n\n'
    rows=[]
    for row in dev_batches():
        code=row[0].split()[0]
        covered=[MID[g]+' '+name for g,name,*_ in MODULES if any(code in expand_dev(f['dev']) for f in FR if f['group']==g)]
        if code=='DEV-017':covered=['全部 12 个业务模块及公共交互']
        label='；'.join(covered) or '公共支撑或全系统阶段门禁，不单独计入业务模块'
        rows.append([row[0],label,*row[1:]])
    t+=mdtable(['批次','业务模块对应','前置依赖','先改哪里、后改哪里','交付与重点验证','失败时如何回退'],rows)
    t+='\nDEV-008 只交付任务完成、通知与基础查询的首条后端闭环；今日看板与历史统计的完整功能必须继续完成 DEV-012。系统通知的完整 API 与发送、去重、处理和已读要求不能用 outbox 基础机制替代。DEV-015 验收前逐行核对上面的 12 模块覆盖矩阵，不能以 8 个业务工程的目录已建齐认定业务完成。\n\n'
    return t
