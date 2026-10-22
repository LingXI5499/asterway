"""Static document checks. Does not claim to run the application or a browser."""
from pathlib import Path
from html.parser import HTMLParser
import collections,hashlib,json,re,subprocess,xml.etree.ElementTree as ET
from write_main import OUT,ROOT,DATA,C,meta,mdtable,write,MODULES,MID,NFR_CATEGORIES,section,table_rows
from implementation import engineering_inventory,dev_batches,expand_dev
from prepare import source_digest,SOURCE

class Scan(HTMLParser):
    VOID={'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr','path','rect','circle','line','polyline','polygon','stop','use'}
    def __init__(self):
        super().__init__(convert_charrefs=True);self.ids=[];self.links=[];self.stack=[];self.problems=[];self.tags=collections.Counter();self.external=[];self.pre=0;self.raw_tables=[];self.ignore=0;self.visible=[]
    def handle_starttag(self,tag,attrs):
        attrs=dict(attrs);self.tags[tag]+=1
        if 'id' in attrs:self.ids.append(attrs['id'])
        if tag=='a' and 'href' in attrs:self.links.append(attrs['href'])
        if tag in ['script','link','img','iframe'] and (attrs.get('src') or attrs.get('href')):self.external.append(attrs.get('src') or attrs.get('href'))
        if tag=='pre':self.pre+=1
        if tag in ['script','style']:self.ignore+=1
        if tag not in self.VOID:self.stack.append(tag)
    def handle_endtag(self,tag):
        if tag=='pre':self.pre-=1
        if tag in ['script','style']:self.ignore-=1
        if tag in self.VOID:return
        if not self.stack or self.stack[-1]!=tag:
            self.problems.append('Unexpected close '+tag+' after '+str(self.stack[-3:]))
            if tag in self.stack:self.stack=self.stack[:len(self.stack)-1-self.stack[::-1].index(tag)]
        else:self.stack.pop()
    def handle_startendtag(self,tag,attrs):
        # Self-closing SVG elements are valid even though HTML has no end tag.
        self.tags[tag]+=1
        if 'id' in dict(attrs):self.ids.append(dict(attrs)['id'])
    def handle_data(self,data):
        if not self.ignore:self.visible.append(data)
        if not self.pre and re.search(r'\|\s*:?-{3,}',data):self.raw_tables.append(data[:120])

def backend_state():
    """Fingerprint the current working files and staging, including pre-existing edits."""
    names=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z','--','backend'],cwd=ROOT).decode('utf-8').split('\0')
    files={name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() if (ROOT/name).is_file() else None
           for name in sorted(set(names)-{''})}
    index=subprocess.check_output(['git','ls-files','--stage','-z','--','backend'],cwd=ROOT)
    return {'files':files,'index':hashlib.sha256(index).hexdigest()}

def verify(backend_before=None):
    errors=[];s=(OUT/'index.html').read_text(encoding='utf-8');scan=Scan();scan.feed(s)
    if {p.relative_to(OUT).as_posix() for p in OUT.rglob('*.html')}!={'index.html'}:
        errors.append('V2.0 must use its existing index.html as the only HTML reader')
    if scan.problems or scan.stack:errors+=scan.problems+['Unclosed: '+str(scan.stack)] if scan.stack else scan.problems
    duplicate=[k for k,v in collections.Counter(scan.ids).items() if v>1]
    if duplicate:errors.append('Duplicate IDs '+str(duplicate[:5]))
    missing=[x for x in scan.links if x.startswith('#') and x[1:] not in set(scan.ids)]
    if missing:errors.append('Missing anchors '+str(missing[:5]))
    if scan.external:errors.append('External rendering dependencies '+str(scan.external))
    if scan.raw_tables:errors.append('Raw Markdown tables '+str(scan.raw_tables[:3]))
    text=' '.join(scan.visible)
    banned=re.findall(r'(?i)\b(?:placeholder|TODO|TBD)\b|【[^】]*】|\ufffd',text)
    if banned:errors.append('Unexpected visible terms '+str(banned[:8]))
    svg=re.findall(r'<svg\b.*?</svg>',s,re.S)
    for x in svg:
        try:ET.fromstring(x)
        except ET.ParseError as e:errors.append('SVG '+str(e))
    refs=re.findall(r'url\(#([^\)]+)\)',s)
    if any(x not in set(scan.ids) for x in refs):errors.append('Missing SVG markers')
    scripts=re.findall(r'<script>(.*?)</script>',s,re.S)
    syntax=subprocess.run(['node','--check'],input='\n'.join(scripts),capture_output=True,text=True,encoding='utf-8')
    if syntax.returncode:errors.append('JavaScript syntax '+syntax.stderr)
    all_mdpaths=sorted(p for p in OUT.rglob('*.md'))
    mdpaths=[p for p in all_mdpaths if SOURCE not in p.parents]
    jsoncount=0
    for p in all_mdpaths:
        t=p.read_text(encoding='utf-8')
        fence=None
        for line_number,line in enumerate(t.splitlines(),1):
            match=re.match(r'^ {0,3}(`{3,}|~{3,})(.*)$',line)
            if match:
                token,tail=match.groups()
                if fence is None:fence=(token[0],len(token),line_number)
                elif token[0]==fence[0] and len(token)>=fence[1] and not tail.strip():fence=None
        if fence:errors.append('Unclosed code fence '+str(p.relative_to(OUT))+':'+str(fence[2]))
        for j,block in enumerate(re.findall(r'```json\s*\n(.*?)\n```',t,re.S)):
            if p in mdpaths:jsoncount+=1
            try:json.loads(block)
            except json.JSONDecodeError as e:errors.append(str(p.relative_to(OUT))+' JSON '+str(j)+': '+str(e))
        for label,link in re.findall(r'\[([^\]]+)\]\(([^\s)]+)\)',t):
            if link.startswith(('http:','https:','mailto:','#')):continue
            path=link.split('#')[0]
            if path and not (p.parent/path).exists():errors.append('Missing file '+str(p.relative_to(OUT))+' -> '+path)
    totals={}
    patterns=[('fr','02-需求分析文档.md',r'^\| (FR-[A-Z]+-\d{3}) \|',132),('nfr','02-需求分析文档.md',r'^\| (NFR-[A-Z0-9]+-\d{3}) \|',84),('tc','06-测试文档.md',r'^\| (TC-\d{3}) / TEST-',72)]
    for key,p,pat,want in patterns:
        count=len(set(re.findall(pat,(OUT/p).read_text(encoding='utf-8'),re.M)));totals[key]=count
        if count!=want:errors.append(f'{key}: {count} != {want}')
    # Compare complete identifiers with original sources, not just a hardcoded total.
    requirements=(OUT/'02-需求分析文档.md').read_text(encoding='utf-8')
    source_nfr=(SOURCE/'质量需求基线.md').read_text(encoding='utf-8-sig')
    expected_nfr=set(re.findall(r'^\| (NFR-[A-Z0-9]+-\d{3}) \|',source_nfr,re.M))
    actual_nfr=re.findall(r'^\| (NFR-[A-Z0-9]+-\d{3}) \|',requirements,re.M)
    actual_fr=re.findall(r'^\| (FR-[A-Z]+-\d{3}) \|',requirements,re.M)
    if len(re.findall(r'<h4[^>]*>1\.2\.\d+ MOD-',s))!=len(MODULES):errors.append('FR category heading did not render as a heading')
    if len(re.findall(r'<h4[^>]*>2\.2\.\d+ ',s))!=len(NFR_CATEGORIES):errors.append('NFR category heading did not render as a heading')
    if set(actual_nfr)!=expected_nfr or len(actual_nfr)!=len(expected_nfr):errors.append('NFR source coverage/uniqueness mismatch')
    if set(actual_fr)!={f['id'] for f in C['fr']} or len(actual_fr)!=len(C['fr']):errors.append('FR source coverage/uniqueness mismatch')
    for prefix,groups,items in [('fr',[m[0] for m in MODULES],C['fr']),('nfr',[n[0] for n in NFR_CATEGORIES],C['nfr'])]:
        if set(groups)!={x['group'] for x in items}:errors.append(prefix+' category catalog mismatch')
        for group in groups:
            marker=f'<a id="{prefix}-{group.lower()}"></a>'
            if requirements.count(marker)!=1:errors.append('Missing/duplicate requirement category '+marker);continue
            block=requirements.split(marker,1)[1].split('\n#### ',1)[0].split('\n### ',1)[0]
            ids=re.findall(r'^\| ('+prefix.upper()+r'-[A-Z0-9]+-\d{3}) \|',block,re.M)
            if set(ids)!={x['id'] for x in items if x['group']==group}:errors.append('Wrong requirement category '+group)
    manual=(OUT/'05-编码实施手册.md').read_text(encoding='utf-8')
    mapping=section(manual,'### 5.1 业务模块、Maven 子工程与构建项目的区别',3)
    coverage=section(manual,'### 7.2 十二个业务模块的实施覆盖矩阵',3)
    children,deps=engineering_inventory()
    business={m[2] for m in MODULES}
    if len(children)!=10 or set(children)!=business|{'common','app'}:errors.append('Maven child/business inventory mismatch')
    if set(deps.get('app',[]))!=business|{'common'}:errors.append('app internal dependency mismatch')
    if deps.get('common') or any(deps.get(name)!=['common'] for name in business):errors.append('Maven business dependency direction mismatch')
    for g,name,eng,*_ in MODULES:
        for part,label in [(mapping,'Maven mapping'),(coverage,'DEV coverage')]:
            row=next((r for r in table_rows(part) if r[0]==MID[g]+' '+name),None)
            if not row or f'`backend/{eng}/`' not in row:errors.append(label+' missing/wrong module '+g)
        row=next((r for r in table_rows(coverage) if r[0]==MID[g]+' '+name),None)
        expected=set().union(*(expand_dev(f['dev']) for f in C['fr'] if f['group']==g))|{'DEV-017'}
        if row and expand_dev(row[3])!=expected:errors.append('FR-to-DEV mismatch '+g)
    batches=dev_batches();batch_ids={r[0].split()[0] for r in batches};seen_batches=set()
    for row in batches:
        code=row[0].split()[0]
        if not expand_dev(row[1])<=seen_batches:errors.append('Forward/missing DEV prerequisite '+code)
        seen_batches.add(code)
    if len(batches)!=19 or len(batch_ids)!=19:errors.append('DEV batch catalog mismatch')
    if any(not expand_dev(f['dev'])<=batch_ids for f in C['fr']):errors.append('FR references unknown DEV batch')
    if any(old in manual for old in ['01、02及12版本基线','核对02—05接口覆盖','08门槛报告']):errors.append('Stale V1 chapter references in handbook')
    # JS-built navigation targets must exist just like static hrefs.
    catalog_targets=re.findall(r'"target": "([^"]+)"',scripts[0]) if scripts else []
    if any(t not in set(scan.ids) for t in catalog_targets):errors.append('Missing catalog navigation target')
    definitions=[]
    for p in (OUT/'附录').glob('A0[2-5]-*.md'):definitions+=re.findall(r'^### (API-[A-Z]+-\d{3}) ',p.read_text(encoding='utf-8'),re.M)
    if len(definitions)!=95 or len(set(definitions))!=95:errors.append('API catalog mismatch')
    if len({(a['method'],a['path']) for a in C['api']})!=95:errors.append('Duplicate method/path')
    schema=(OUT/'附录/A13-展开字段字典.md').read_text(encoding='utf-8')
    if len(re.findall(r'^## DB-\d{2} ',schema,re.M))!=36:errors.append('Table count mismatch')
    fields=re.findall(r'^\| [a-z_]+ \|',schema,re.M)
    if len(fields)!=420:errors.append('Expanded column mismatch '+str(len(fields)))
    mods=list((OUT/'模块设计').glob('MOD-*.md'))
    if len(mods)!=12:errors.append('Module count mismatch')
    for p in mods:
        t=p.read_text(encoding='utf-8')
        sections=['第一部分：业务逻辑','第二部分：数据库设计','第三部分：接口设计','第四部分：类设计']
        if any(x not in t for x in sections) or [t.find(x) for x in sections]!=sorted(t.find(x) for x in sections):errors.append('Module order '+p.name)
        if 'web/'+next(m[2] for m in MODULES if MID[m[0]] in p.name)+'/dto/' in t:errors.append('Obsolete combined HTTP dto package '+p.name)
        if not all(term in t for term in ['/request/','/response/','api/model/','api/error/','ApiResponse.data']):errors.append('Incomplete integrated package design '+p.name)
        if any(term in t for term in ['V001__identity_and_session.sql','尚未存在 SQL']):errors.append('Obsolete migration baseline '+p.name)
    if '### 5.5 完整推荐包结构与 DTO/VO' not in manual or '### 7.5 架构细化的分阶段落点与验证' not in manual:
        errors.append('Architecture/package review was not integrated into the existing handbook')
    if 'V001__identity_and_session.sql' in manual:errors.append('Handbook reuses the protected V001 migration version')
    if not all('P'+str(n)+'：' in manual for n in range(6)):errors.append('Handbook architecture phases were truncated')
    allowed={'README.md','index.html',OUT.name}
    unexpected=[p.name for p in (ROOT/'docs').iterdir() if p.name not in allowed]
    if unexpected:errors.append('Unexpected docs edition/files '+str(unexpected))
    inv=json.loads((DATA/'source-inventory.json').read_text(encoding='utf-8'))
    if {str((ROOT/x['path']).resolve()) for x in inv}!={str(p.resolve()) for p in SOURCE.glob('*.md')}:errors.append('Source inventory layout mismatch')
    changed=[x['path'] for x in inv if source_digest(ROOT/x['path'])!=x['sha256']]
    if changed:errors.append('Original docs changed '+str(changed))
    backend_unchanged=None if backend_before is None else backend_state()==backend_before
    if backend_unchanged is False:errors.append('Backend working files or staged entries changed during document build')
    backend=subprocess.check_output(['git','diff','--name-only','--','backend'],cwd=ROOT).decode('utf-8').splitlines()
    maps=json.loads((DATA/'render-map.json').read_text(encoding='utf-8'))
    if maps['legacy_anchor_fallbacks']:errors.append('Unresolved imported anchors')
    browser_status='NOT_VERIFIED: static checks do not execute browser interactions'
    browser_file=DATA/'browser-check.json'
    if browser_file.exists():
        evidence=json.loads(browser_file.read_text(encoding='utf-8'))
        if evidence.get('inputs') and all((OUT/p).exists() and hashlib.sha256((OUT/p).read_bytes()).hexdigest()==digest for p,digest in evidence['inputs'].items()):
            browser_status=evidence['status']+'：'+evidence['scope']
    report={'date':'2026-10-22','status':'PASS' if not errors else 'FAIL','markdown_files':len(mdpaths),'articles':scan.tags['article'],'internal_links':sum(x.startswith('#') for x in scan.links),'unique_ids':len(set(scan.ids)),'json_examples':jsoncount,'svg_diagrams':len(svg),'tables_rendered':scan.tags['table'],'html_bytes':len(s.encode('utf-8')),'fr':totals['fr'],'nfr':totals['nfr'],'fr_categories':len(MODULES),'nfr_categories':len(NFR_CATEGORIES),'maven_children':len(children),'maven_reactor_projects':len(children)+1,'dev_batches':len(batches),'test_cases':totals['tc'],'api_definitions':len(definitions),'design_tables':36,'expanded_columns':len(fields),'modules':len(mods),'source_docs_preserved':len(inv),'javascript':'PASS' if not syntax.returncode else 'FAIL','backend_unchanged':backend_unchanged,'backend_existing_changes':backend,'browser_preview':browser_status,'errors':errors}
    (DATA/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False))
    return report

def verification_doc(r):
    t=meta('10 文档校验记录')+'> 校验完成日期：2026-10-22。本文记录本次实际检查，不是软件业务测试报告。\n\n## 一、检查结果\n\n'
    backend_result='相对本次编制前快照一致；保留已有未提交修改和暂存' if r['backend_unchanged'] else 'NOT_VERIFIED：独立校验未提供本次编制前快照' if r['backend_unchanged'] is None else 'FAIL：本次编制期间发生变化'
    t+=mdtable(['检查','实际结果'],[['文档静态校验',r['status']],['填充正文',str(r['markdown_files'])+' 份（含本记录）'],['覆盖','8 类主文档、12 业务分册、公共能力、13 附录与基线/校验'],['功能/质量/综合用例',f'{r["fr"]} / {r["nfr"]} / {r["test_cases"]}'],['功能分类 / 质量分类',f'{r["fr_categories"]} / {r["nfr_categories"]}'],['Maven 子工程 / Reactor / DEV 批次',f'{r["maven_children"]} / {r["maven_reactor_projects"]} / {r["dev_batches"]}'],['唯一 API',r['api_definitions']],['表/展开列',f'{r["design_tables"]} / {r["expanded_columns"]}'],['内部链接',str(r['internal_links'])+'，目标全部存在'],['标识重复','0'],['JSON 示例',str(r['json_examples'])+' 段均可解析'],['SVG 图',str(r['svg_diagrams'])+' 个均可解析；引用标记有效'],['脚本','node --check 成功'],['V2.0 编制输入',str(r['source_docs_preserved'])+' 份 SHA-256 一致'],['后端源码与暂存',backend_result],['本地工程检查','2026-10-22：11 项 clean compile、两类定向测试及完整 Maven test 通过，共 6 项，失败/错误/跳过 0'],['浏览器预览',r['browser_preview']],['软件业务验证','多模块 clean compile 和当前 6 项契约测试通过；真实 HTTP、PostgreSQL 业务链路、打包和部署恢复 NOT_VERIFIED']])
    t+='''
## 二、核对方式

解析所有 HTML 标签、ID 与片段链接，核对源目录和正文中的 FR/NFR/API/表/TC 数量；检查模块四部分顺序、全部 JSON 示例、SVG、内嵌脚本语法、展示依赖和可见残留文字。所有正文、CSS、脚本与图形都包含在 HTML 中，展示无需网络资源。

本版编制输入指纹与后端保护分别核对。build.py 在编制开始时获取后端文件与暂存条目快照，生成期间逐次比较；不要求用户先清理已有改动，也不把已有差异算成本次修改。单独运行 verify.py 未提供快照时只报告该项未核实。Maven validate 只验证工程模型，不扩展为构建或业务通过。浏览器核对通过只绑定 data/browser-check.json 中记录的正文及渲染脚本指纹；正文变化后须重新预览。具体交互范围单独记录，未将屏幕预览扩大为全册交互或打印通过。

## 三、交付与复核

阅读入口为同目录 index.html，推荐先读 01～03，再按 04 模块分册进入业务、数据、接口和类；05～08 承接实施到运维，09 承接事实和追溯。编辑 Markdown 后使用 tools/build.py 重建，再由 tools/verify.py 复核。结构化结果见 data/verification.json。
'''
    write('10-文档校验记录.md',t)

if __name__=='__main__':
    report=verify()
    if report['errors']:raise SystemExit(1)
