from pathlib import Path
import hashlib,json,re,subprocess,sys

ROOT=Path(__file__).resolve().parents[3]
OUT=Path(__file__).resolve().parents[1]
DATA=OUT/'data'
SOURCE=OUT/'编制源文件'
DATA.mkdir(exist_ok=True)
def read(p): return p.read_text(encoding='utf-8-sig')
def source_digest(p): return hashlib.sha256(read(p).encode('utf-8')).hexdigest()
def source(n): return next(SOURCE.glob(n+'-*.md'))
def table_rows(t):
    return [[x.strip() for x in l.strip().strip('|').split('|')] for l in t.splitlines() if l.startswith('|') and not re.match(r'^\|[\s:|-]+$',l)]
def section(t, heading, endlevel=2):
    m=re.search(r'^'+re.escape(heading)+r'\s*$',t,re.M)
    if not m:return ''
    end=re.search(r'^#{1,'+str(endlevel)+r'} ',t[m.end():],re.M)
    return t[m.end():m.end()+end.start() if end else len(t)].strip()

docs={n:read(source(n)) for n in [f'{x:02}' for x in range(1,13)]}
inventory=[]
source_paths={source(n) for n in docs}
source_paths.update(SOURCE/name for name in ['业务规则.md','功能需求基线.md','质量需求基线.md'])
for p in sorted(source_paths):
    t=read(p)
    inventory.append(dict(path=p.relative_to(ROOT).as_posix(),sha256=source_digest(p),bytes=len(t.encode('utf-8')),
        headings=re.findall(r'^#{1,3} (.+)',t,re.M),kind='V2.0 工程设计输入' if re.match(r'\d{2}-',p.name) else 'V2.0 业务需求输入'))
if '--refresh-sources' in sys.argv or not (DATA/'source-inventory.json').exists():
    (DATA/'source-inventory.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
apis=[]
for n in ['02','03','04','05']:
    t=docs[n]; matches=list(re.finditer(r'^### (API-[A-Z]+-\d{3}) (.+)$',t,re.M))
    for i,m in enumerate(matches):
        end=matches[i+1].start() if i+1<len(matches) else len(t)
        b=t[m.end():end]
        # Do not absorb the following top-level section into an endpoint.
        stop=re.search(r'^## ',b,re.M)
        if stop:b=b[:stop.start()]
        route=re.search(r'\b(GET|POST|PUT|PATCH|DELETE)\s+(/api/v1/[^\s*`<]+)',b)
        if not route: raise ValueError(m.group(1)+' missing route')
        apis.append(dict(id=m.group(1),name=m.group(2),method=route.group(1),path=route.group(2),body=b.strip(),source=n,
           fr=sorted(set(re.findall(r'FR-[A-Z]+-\d{3}',b))),
           flow=section(b,'#### 业务流程',4),analysis=section(b,'#### 需求分析',4),
           contract=section(b,'#### 接口设计',4),operations=section(b,'#### 数据操作',4),
           implementation=section(b,'#### 分层实现步骤',4),tests=section(b,'#### 功能测试',4)))
frs=[]
for row in table_rows(docs['09']):
    if re.fullmatch(r'FR-[A-Z]+-\d{3}',row[0]):
        frs.append(dict(id=row[0],name=row[1],operations=row[2],tables=row[3],dev=row[4],tests=row[5],group=row[0].split('-')[1]))
nfrs=[]
for row in table_rows(read(SOURCE/'质量需求基线.md')):
    if re.fullmatch(r'NFR-[A-Z0-9]+-\d{3}',row[0]):nfrs.append(dict(id=row[0],target=row[1],verification=row[2],group=row[0].split('-')[1]))
tests=[]
for row in table_rows(docs['08']):
    if re.fullmatch(r'TEST-\d{3}',row[0]):tests.append(dict(id=row[0],input=row[1],steps=row[2],expected=row[3],evidence=row[4],status=row[5]))
tables=[]
matches=list(re.finditer(r'^### (DB-\d{2}) (\S+)\s*$',docs['01'],re.M))
for i,m in enumerate(matches):
    b=docs['01'][m.end():matches[i+1].start() if i+1<len(matches) else len(docs['01'])]
    stop=re.search(r'^## ',b,re.M)
    if stop:b=b[:stop.start()]
    fields=[]
    for row in table_rows(b):
        if len(row)==5 and row[0] not in ['字段','字段名'] and re.fullmatch(r'[a-z_]+',row[0]):
            fields.append(dict(name=row[0],type=row[1],nullable=row[2],default=row[3],meaning=row[4],origin='专用字段'))
    common=re.search(r'公共字段[：:]([^。\n]+)',b)
    cs=re.findall(r'\b[ICUM]\b',common.group(1)) if common else []
    sets={'I':[('id','uuid','否','gen_random_uuid()','主键')], 'C':[('created_at','timestamptz(6)','否','clock_timestamp()','真实持久化时刻')],
          'U':[('user_id','uuid','否','无默认值','本人所有者，由服务端身份确定')],
          'M':[('updated_at','timestamptz(6)','否','clock_timestamp()','更新时显式赋值'),('version','bigint','否','0','非负；条件更新后递增')]}
    extra=[]
    for s in cs:
        for name,typ,nullable,default,meaning in sets[s]:
            if not any(f['name']==name for f in fields):extra.append(dict(name=name,type=typ,nullable=nullable,default=default,meaning=meaning,origin='公共字段'+s))
    fields=extra+fields
    tables.append(dict(id=m.group(1),name=m.group(2),body=b.strip(),fields=fields))
pages=[]
for row in table_rows(docs['07']):
    if re.fullmatch(r'PAGE-\d{3}',row[0]):pages.append(dict(id=row[0],name=row[1],route=row[2],permission=row[3]))
baseline=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip()
if (DATA/'catalog.json').exists():
    previous=json.loads((DATA/'catalog.json').read_text(encoding='utf-8')).get('commit')
    if previous and subprocess.run(['git','diff','--quiet',previous,'--','backend'],cwd=ROOT).returncode==0:
        baseline=previous
state=dict(commit=baseline,branch=subprocess.check_output(['git','branch','--show-current'],cwd=ROOT).decode().strip(),
           api=apis,fr=frs,nfr=nfrs,tests=tests,tables=tables,pages=pages)
(DATA/'catalog.json').write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(dict(source_docs=len(inventory),api=len(apis),fr=len(frs),nfr=len(nfrs),tests=len(tests),tables=len(tables),columns=sum(len(t['fields']) for t in tables),pages=len(pages)),ensure_ascii=False))
for a in apis:
    if not a['contract'] or not a['flow']: print('Special structure:',a['id'])
