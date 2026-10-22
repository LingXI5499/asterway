from pathlib import Path
import re,json,html,hashlib,os
from urllib.parse import unquote
import markdown
from markdown.extensions.toc import slugify_unicode
from html.parser import HTMLParser
from write_modules import OUT,DATA,C,TABLE,NAMES,write,APPENDIX,NFR_CATEGORIES

def esc(x):return html.escape(str(x),quote=True)
def fileid(p):return 'doc-'+hashlib.sha1(p.encode('utf-8')).hexdigest()[:10]

def architecture():
    rows=[('浏览器','Vue / TypeScript · 20 个设计页面'),('Nginx','HTTPS · 静态资源 · 同域 API'),('app','HTTP / 安全 / 应用事务 / 调度 / 启动恢复'),('业务工程 + common','8 个业务工程 · 进程内调用 · 单实例'),('远程 PostgreSQL','17.11 目标基线 · 逻辑关系 · 0 物理外键')]
    s='<svg viewBox="0 0 850 620" role="img" aria-label="模块化单体架构"><defs><marker id="arch-arrow" markerWidth="8" markerHeight="8" refX="6" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8" fill="#286b62"/></marker></defs>'
    for i,(name,desc) in enumerate(rows):
        y=16+i*113
        s+=f'<rect x="30" y="{y}" width="540" height="85" rx="12" fill="#f4faf8" stroke="#94b6ad"/><text x="50" y="{y+29}" font-size="19" fill="#164f47">{esc(name)}</text><text x="50" y="{y+58}" font-size="13" fill="#455d57">{esc(desc)}</text>'
        if i<4:s+=f'<path d="M300,{y+85} V{y+109}" stroke="#286b62" stroke-width="2" marker-end="url(#arch-arrow)"/>'
    s+='<rect x="620" y="353" width="210" height="85" rx="12" fill="#fff8e9" stroke="#c9a76b"/><text x="637" y="386" font-size="16">独立安全账本</text><text x="637" y="414" font-size="12">意图 / 结果 / 撤销核验</text><rect x="620" y="466" width="210" height="85" rx="12" fill="#fff8e9" stroke="#c9a76b"/><text x="637" y="499" font-size="16">独立备份与 WAL</text><text x="637" y="527" font-size="12">恢复点 / 清单 / 时间线</text><path d="M570,395 H618 M570,508 H618" stroke="#b89354" stroke-dasharray="5 4" stroke-width="2"/></svg>'
    return '<figure class="diagram">'+s+'<figcaption>规划运行拓扑。当前仅后端骨架存在；外部账本和备份设施尚未落地。</figcaption></figure>'

def ergraph():
    # Each small diagram is an explicit logical relationship from the reviewed dictionary.
    edges=[(1,8,'user_id','1','0..N'),(8,9,'goal_id','1','0..N'),(9,10,'stage_id','1','0..N'),(10,15,'task_id','1','0..1'),(13,14,'weekly_plan_id','1','0..N'),(14,15,'daily_plan_id','0..1','0..N'),(10,16,'task_id','1','0..1'),(10,17,'task_id','1','0..N'),(17,18,'session_id','1','1..N'),(18,21,'segment_id','1','0..N'),(18,20,'segment_id','1','0..N'),(22,23,'habit_id','1','1..N'),(23,24,'rule_id','1','0..N'),(22,26,'habit_id','1','0..N'),(8,12,'goal_id','1','0..N'),(8,27,'goal_id','1','0..N'),(34,35,'session_primary_id','1','0..N')]
    s='<div class="er-grid">'
    for a,b,f,ca,cb in edges:
        pa=TABLE[a-1];ch=TABLE[b-1]
        parfield='primary_id' if a==34 else 'id'
        nf=next((x for x in ch['fields'] if x['name']==f),None)
        typ=nf['type'] if nf else 'uuid'
        s+='<figure class="diagram er"><svg viewBox="0 0 750 180" role="img" aria-label="'+esc(pa['cn']+'与'+ch['cn']+'逻辑关系')+'">'
        for x,tb,key in [(5,pa,parfield),(460,ch,f)]:
            s+=f'<rect x="{x}" y="15" width="285" height="140" rx="8" fill="#f8fbfa" stroke="#9ab7ae"/><text x="{x+12}" y="43" font-size="13" fill="#124b42">{esc(tb["name"])}</text><text x="{x+12}" y="68" font-size="16" fill="#233e36">{esc(tb["cn"])}</text><text x="{x+12}" y="100" font-size="13">{esc(key)} : {esc(typ if x==460 else ("char(36)" if a==34 else "uuid"))}</text><text x="{x+12}" y="128" font-size="12" fill="#62766f">{"PK" if x==5 else "逻辑引用 · 非物理 FK"}</text>'
        s+='<path d="M291,87 H459" stroke="#4b8274" stroke-width="2" stroke-dasharray="5 4"/>'
        s+='<path d="M301,75 V99 M308,75 V99" stroke="#386c5c" stroke-width="2" fill="none"/>' if ca=='1' else '<circle cx="304" cy="87" r="5" fill="white" stroke="#386c5c"/><path d="M314,75 V99" stroke="#386c5c" stroke-width="2"/>'
        if cb.endswith('N'):
            s+='<path d="M440,87 L457,75 M440,87 H457 M440,87 L457,99" stroke="#386c5c" stroke-width="2" fill="none"/>'
            s+=('<circle cx="430" cy="87" r="5" fill="white" stroke="#386c5c"/>' if cb.startswith('0') else '<path d="M429,75 V99" stroke="#386c5c" stroke-width="2"/>')
        else:s+='<circle cx="432" cy="87" r="5" fill="white" stroke="#386c5c"/><path d="M445,75 V99" stroke="#386c5c" stroke-width="2"/>'
        s+=f'<text x="366" y="68" text-anchor="middle" font-size="12">{ca} : {cb}</text><text x="366" y="115" text-anchor="middle" font-size="11">{esc(f)}</text></svg><figcaption>应用事务校验；删除与历史保留遵循 A01，不设数据库级联。</figcaption></figure>'
    return s+'</div>'

CSS='''
:root{--ink:#20342f;--muted:#61746d;--line:#dce5df;--paper:#fff;--bg:#f3f5ef;--green:#145b4d;--soft:#e6f0e9;--gold:#967135;--nav:292px}*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.85 "Segoe UI","Microsoft YaHei",sans-serif}button,input{font:inherit}button,a{touch-action:manipulation}a{color:var(--green);text-underline-offset:4px}button{cursor:pointer;border:1px solid var(--line);background:#fff;color:var(--ink);padding:7px 13px;border-radius:7px}button:hover{background:var(--soft)}button:focus-visible,a:focus-visible,input:focus-visible{outline:3px solid #c0994d;outline-offset:2px}.sidebar{width:var(--nav);position:fixed;inset:0 auto 0 0;background:#123e34;color:#e5efe8;padding:24px 18px;overflow:auto;z-index:5}.brand{font:600 26px/1.3 Georgia,serif;letter-spacing:1px;color:#fff}.edition{font-size:12px;color:#b9d1c5;margin:8px 0 23px;letter-spacing:1px}.nav-group{margin-top:23px;font-size:11px;letter-spacing:2px;color:#a9c5b7}.sidebar a{display:block;color:#d9e8de;font-size:13px;text-decoration:none;padding:6px 9px;margin:2px 0;border-radius:5px}.sidebar a:hover,.sidebar a.active{background:#2b5a4c;color:#fff}.sidebar .nav-search{width:100%;background:#204b3e;border:1px solid #527365;border-radius:6px;color:white;padding:8px 10px}.sidebar .nav-search::placeholder{color:#b4c7bb}.side-note{font-size:11px;color:#a9c5b7;margin:25px 9px}.workspace{margin-left:var(--nav)}.toolbar{position:sticky;top:0;background:#f6f8f2f5;backdrop-filter:blur(12px);border-bottom:1px solid var(--line);padding:13px 30px;display:flex;gap:10px;align-items:center;z-index:4}.toolbar input{min-width:0;flex:1;border:1px solid #ccd9d0;background:#fff;border-radius:7px;padding:8px 12px}.toolbar button{font-size:12px;white-space:nowrap}.content{max-width:1220px;margin:auto;padding:30px 42px 80px}.cover{background:#174f40;color:#edf5ed;border-radius:12px;padding:35px 38px;margin-bottom:25px;position:relative;overflow:hidden}.cover:after{content:"";position:absolute;right:-70px;top:-100px;width:360px;height:360px;border:1px solid #ffffff1c;border-radius:50%;box-shadow:0 0 0 42px #ffffff05,0 0 0 83px #ffffff04;pointer-events:none}.cover .eyebrow{font-size:11px;color:#b8d0bc;letter-spacing:3px}.cover h1{font:600 34px/1.4 "Segoe UI","Microsoft YaHei",sans-serif;margin:13px 0}.cover p{max-width:680px;font-size:14px;color:#cadbd0}.badge{display:inline-block;border:1px solid #c7d8cb;border-radius:20px;padding:1px 10px;font-size:11px;color:#456253;background:#eff5ef;margin:0 6px 7px 0}.cover .badge{color:#e5eee4;background:#ffffff12;border-color:#729580}.metrics{display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin-bottom:26px}.metric{background:#fff;border:1px solid var(--line);border-radius:8px;padding:16px}.metric b{font:600 26px/1.2 Georgia,serif;color:var(--green);display:block;margin-bottom:7px}.metric span{font-size:12px;color:var(--muted)}.boundary{border-left:3px solid #b88d4d;background:#faf5e9;padding:12px 17px;font-size:13px;margin:18px 0 25px}.catalog-panel{background:white;border:1px solid var(--line);padding:20px;border-radius:9px;margin-bottom:25px}.catalog-controls{display:flex;gap:8px;flex-wrap:wrap;align-items:center}.catalog-controls button[aria-pressed=true]{background:var(--green);color:#fff}.catalog-controls input{border:1px solid var(--line);padding:6px 10px;border-radius:6px;flex:1;min-width:130px}.catalog-body{max-height:400px;overflow:auto;margin-top:12px}.catalog-body a{display:block;border-bottom:1px solid #edf0eb;text-decoration:none;padding:8px;font-size:13px}.catalog-body a small{display:block;color:var(--muted)}.catalog-body a code{color:var(--green)}.search-results{display:none;background:white;border:1px solid var(--line);padding:20px;border-radius:9px;margin-bottom:25px}.search-results.show{display:block}.search-results a{display:block;padding:9px;border-bottom:1px solid var(--line);text-decoration:none;font-size:13px}.search-results small{display:block;color:var(--muted)}.article-wrap{display:grid;grid-template-columns:minmax(0,1fr) 180px;gap:26px;align-items:start}.document{min-width:0;background:#fff;padding:33px 34px 45px;border:1px solid var(--line);border-radius:9px}.document[hidden]{display:none}.document h1{font-size:27px;line-height:1.5;margin:0 0 25px;color:#163d30}.document h2{font-size:21px;margin:40px 0 18px;padding-top:6px;border-top:1px solid var(--line);color:#174f40}.document h3{font-size:17px;margin-top:30px}.document h4{font-size:15px;margin-top:25px}.document p{margin:14px 0}.document blockquote{margin:20px 0;border-left:3px solid #6f9b85;padding:8px 15px;background:#f3f7f1;color:var(--muted);font-size:12px}.document ul,.document ol{padding-left:25px}.table-scroll{overflow:auto;width:100%;margin:18px 0;border:1px solid var(--line);border-radius:6px}table{border-collapse:collapse;width:100%;font-size:12px;line-height:1.7}th{background:#eaf1e9;color:#2a5140;text-align:left;font-weight:600}td,th{padding:10px 12px;vertical-align:top;border-bottom:1px solid var(--line);min-width:92px;overflow-wrap:anywhere}tr:last-child td{border-bottom:0}tbody tr:nth-child(even){background:#fafbf7}code{font-family:Consolas,"Microsoft YaHei",monospace;font-size:.9em;background:#edf3ee;padding:1px 4px;border-radius:3px;overflow-wrap:anywhere}pre{background:#122e26;color:#deeadf;border-radius:8px;padding:18px;overflow:auto;font:12px/1.85 Consolas,"Microsoft YaHei",monospace;position:relative}pre code{background:transparent;padding:0;color:inherit;white-space:pre;overflow-wrap:normal}details{margin:16px 0;border:1px solid var(--line);border-radius:7px;padding:10px 13px;background:#fcfdf9}summary{cursor:pointer;font-weight:600;color:#285842;font-size:13px}details[open]>summary{margin-bottom:12px}.section-index{position:sticky;top:85px;font-size:11px;max-height:75vh;overflow:auto}.section-index strong{display:block;margin-bottom:10px;color:var(--muted)}.section-index a{display:block;padding:4px 0;color:var(--muted);text-decoration:none;line-height:1.6}.section-index a:hover{color:var(--green)}.diagram{margin:25px 0;border:1px solid var(--line);border-radius:8px;padding:14px;background:#fff;overflow:auto;break-inside:avoid}.diagram svg{display:block;width:100%;height:auto;min-width:420px;max-width:850px;margin:auto}.diagram svg text{font-family:"Segoe UI","Microsoft YaHei",sans-serif;fill:#244735}figcaption{font-size:11px;line-height:1.65;color:var(--muted);margin-top:10px}.er-grid{display:grid;grid-template-columns:1fr;gap:6px}.er{margin:8px 0}.footnav{display:flex;justify-content:space-between;gap:20px;margin-top:25px}.footnav a{font-size:13px;text-decoration:none}.footer{font-size:11px;color:var(--muted);margin-top:24px}.print-directory{display:none}.no-results{color:var(--muted);padding:15px}.doc-label{color:var(--muted);font-size:11px;letter-spacing:1px;margin-bottom:12px}.copy{position:absolute;right:7px;top:7px;font-size:10px;padding:1px 7px;background:#254b3c;color:#dfeade;border-color:#4d6e5d}.noscript{padding:18px;background:#fff3d8}body.nojs .document{display:block!important}.table-filter{display:block;width:100%;max-width:320px;border:1px solid var(--line);padding:5px 9px;border-radius:5px;margin-top:12px;font-size:12px}.mobile-toggle{display:none}
@media(max-width:1250px){.article-wrap{grid-template-columns:minmax(0,1fr)}.section-index{position:static;max-height:220px;grid-row:1;background:#edf2e9;padding:15px;border-radius:7px;columns:2}.section-index strong{column-span:all}.content{padding:25px}.metrics{grid-template-columns:repeat(3,1fr)}}
@media(max-width:850px){:root{--nav:0px}.sidebar{width:275px;transform:translateX(-100%);transition:transform .2s}.sidebar.open{transform:translateX(0)}.mobile-toggle{display:block}.toolbar{padding:10px}.toolbar button{padding:6px}.toolbar .print-all{display:none}.content{padding:15px}.cover{padding:24px}.cover h1{font-size:28px}.document{padding:22px 18px}.metrics{gap:7px}.metric{padding:12px}.metric b{font-size:23px}td,th{min-width:82px;padding:8px}.section-index{columns:1}.cover:after{display:none}}
@media print{@page{size:A4;margin:14mm 12mm}html{scroll-behavior:auto}body{font:9pt/1.6 "Microsoft YaHei",sans-serif;background:#fff;color:#000}.sidebar,.toolbar,.catalog-panel,.search-results,.section-index,.footnav,.footer,.copy,.cover,.metrics,.boundary,.table-filter{display:none!important}.workspace{margin:0}.content{padding:0;max-width:none}.article-wrap{display:block}.document{padding:0;border:0;border-radius:0;page-break-before:always}.document[hidden]{display:none}.print-all-mode .document[hidden]{display:block}.print-directory{display:block;page-break-after:always}.print-directory h1{font-size:22pt}.print-directory ul{columns:2;font-size:8pt;padding-left:15px}.print-directory a{color:#000;text-decoration:none}.document h1{font-size:20pt}.document h2{font-size:14pt;page-break-after:avoid}.document h3{font-size:11pt;page-break-after:avoid}.table-scroll{overflow:visible;border-radius:0}table{font-size:7.5pt;line-height:1.5;table-layout:auto}td,th{min-width:0;padding:5px;word-break:break-word}thead{display:table-header-group}tr{break-inside:avoid}pre{white-space:pre-wrap;overflow:visible;font-size:7.5pt;background:#f2f4ef;color:#000}pre code{white-space:pre-wrap}details{background:white;padding:6px;border-color:#ccc}summary{font-size:9pt}.diagram svg{min-width:0;max-width:100%}.diagram{break-inside:avoid}a{color:#000}blockquote{font-size:8pt!important}body.print-all-mode .print-only-current{display:none}}
'''

JS=r'''
const docs=[...document.querySelectorAll('.document')],nav=[...document.querySelectorAll('.file-nav')],sectionBox=document.querySelector('.section-index');let active=docs[0];
function reveal(el){for(let p=el;p;p=p.parentElement){if(p.tagName==='DETAILS')p.open=true;}}
function show(id,scroll=true){let el=document.getElementById(id);if(!el)return;let doc=el.closest('.document');if(!doc)return;docs.forEach(d=>d.hidden=d!==doc);active=doc;nav.forEach(a=>a.classList.toggle('active',a.hash==='#'+doc.id));sectionBox.replaceChildren();let h=document.createElement('strong');h.textContent='本册目录';sectionBox.append(h);doc.querySelectorAll(doc.dataset.title.startsWith('02-')?'h2,h3,h4':'h2,h3').forEach(x=>{let a=document.createElement('a');a.href='#'+x.id;a.textContent=x.textContent;sectionBox.append(a);});document.querySelector('.current-title').textContent=doc.dataset.title;reveal(el);if(scroll)requestAnimationFrame(()=>el.scrollIntoView({block:'start'}));document.querySelector('.sidebar').classList.remove('open');}
window.addEventListener('hashchange',()=>show(decodeURIComponent(location.hash.slice(1))));
document.querySelector('.mobile-toggle').onclick=()=>document.querySelector('.sidebar').classList.toggle('open');
const q=document.querySelector('#global-search'),rs=document.querySelector('.search-results');let timer;
q.addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(()=>{let term=q.value.trim().toLowerCase();rs.replaceChildren();rs.classList.toggle('show',!!term);if(!term)return;let title=document.createElement('strong');title.textContent='全文搜索';rs.append(title);let count=0;for(let d of docs){if(!d.textContent.toLowerCase().includes(term))continue;let hit=[...d.querySelectorAll('h2,h3,h4,p,td,summary')].find(x=>x.textContent.toLowerCase().includes(term));let a=document.createElement('a');let nearest=hit?.closest('details')?.querySelector('summary');let target=(hit?.id?hit:nearest?.id?nearest:d);a.href='#'+target.id;a.textContent=d.dataset.title;let small=document.createElement('small');small.textContent=(hit?.textContent||d.textContent).trim().replace(/\s+/g,' ').slice(0,170);a.append(small);a.onclick=()=>{show(target.id);if(hit){reveal(hit);setTimeout(()=>hit.scrollIntoView({block:'center'}),60);}};rs.append(a);if(++count>=60)break;}if(!count){let p=document.createElement('p');p.textContent='没有匹配项，请尝试功能编号、接口路径或表名。';rs.append(p);}},180);});
document.querySelector('.nav-search').oninput=e=>{let v=e.target.value.trim().toLowerCase();nav.forEach(a=>a.hidden=!a.textContent.toLowerCase().includes(v));};
const cats=CATALOG_DATA;let catType='api';const catBody=document.querySelector('.catalog-body'),catQ=document.querySelector('#catalog-search');
function drawCat(){let term=catQ.value.toLowerCase().trim();catBody.replaceChildren();let items=cats[catType].filter(x=>(x.title+' '+x.desc).toLowerCase().includes(term));items.slice(0,200).forEach(x=>{let a=document.createElement('a');a.href='#'+x.target;let b=document.createElement('code');b.textContent=x.title;a.append(b);let s=document.createElement('small');s.textContent=x.desc;a.append(s);catBody.append(a);});if(!items.length){let p=document.createElement('p');p.textContent='没有匹配项';p.className='no-results';catBody.append(p);}}
document.querySelectorAll('[data-cat]').forEach(b=>b.onclick=()=>{catType=b.dataset.cat;document.querySelectorAll('[data-cat]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));drawCat();});catQ.oninput=drawCat;drawCat();
document.querySelector('#expand').onclick=()=>active.querySelectorAll('details').forEach(d=>d.open=true);document.querySelector('#collapse').onclick=()=>active.querySelectorAll('details').forEach(d=>d.open=false);
document.querySelector('#print').onclick=()=>{document.body.classList.remove('print-all-mode');window.print();};document.querySelector('#print-all').onclick=()=>{document.body.classList.add('print-all-mode');window.print();};let detailState;
let rowState;
window.addEventListener('beforeprint',()=>{detailState=[...document.querySelectorAll('details')].map(d=>[d,d.open]);rowState=[...document.querySelectorAll('tbody tr')].map(r=>[r,r.hidden]);document.querySelectorAll('details').forEach(d=>d.open=true);rowState.forEach(([r])=>r.hidden=false);});window.addEventListener('afterprint',()=>{detailState?.forEach(([d,o])=>d.open=o);rowState?.forEach(([r,h])=>r.hidden=h);document.body.classList.remove('print-all-mode');});
document.querySelectorAll('table').forEach(table=>{if(table.tBodies[0]?.rows.length<10)return;let inp=document.createElement('input');inp.className='table-filter';inp.type='search';inp.placeholder='筛选本表内容';inp.setAttribute('aria-label','筛选本表内容');table.closest('.table-scroll').before(inp);inp.oninput=()=>{let v=inp.value.toLowerCase();[...table.tBodies[0].rows].forEach(r=>r.hidden=!r.textContent.toLowerCase().includes(v));};});
document.querySelectorAll('pre').forEach(p=>{let b=document.createElement('button');b.className='copy';b.textContent='复制';b.onclick=async()=>{try{await navigator.clipboard.writeText(p.querySelector('code')?.textContent||p.textContent);b.textContent='已复制';setTimeout(()=>b.textContent='复制',1500);}catch{b.textContent='请选择文本复制';}};p.append(b);});
document.body.classList.remove('nojs');show(decodeURIComponent(location.hash.slice(1))||docs[0].id,false);
'''

def render():
    paths=[OUT/'README.md']+sorted(p for p in OUT.glob('*.md') if p.name!='README.md')+sorted((OUT/'模块设计').glob('*.md'))+sorted((OUT/'附录').glob('*.md'))
    mapping={p.relative_to(OUT).as_posix():fileid(p.relative_to(OUT).as_posix()) for p in paths}
    rendered={}; anchors={}; targets={}
    for p in paths:
        rel=p.relative_to(OUT).as_posix();fid=mapping[rel];t=p.read_text(encoding='utf-8')
        # Explicit stable anchors for domain identifiers, independent of heading wording.
        t=re.sub(r'^(#{2,4}) (API-[A-Z]+-\d{3}|DB-\d{2}|PAGE-\d{3}|FR-[A-Z]+-\d{3})([^\n]*)$',lambda m:m.group(0)+' {#'+m.group(2).lower()+'}',t,flags=re.M)
        t=re.sub(r'<details(?:\s[^>]*)?>','<details markdown="1">',t)
        t=re.sub(r'<summary>(.*?)</summary>',r'<summary markdown="span">\1</summary>',t,flags=re.S)
        chunks=re.split(r'(```.*?```|`[^`]*`)',t,flags=re.S)
        for ci in range(0,len(chunks),2):
            chunks[ci]=re.sub(r'<([A-Z][A-Za-z0-9_,?. ]*)>',lambda m:'&lt;'+m.group(1)+'&gt;',chunks[ci])
        t=''.join(chunks)
        h=markdown.markdown(t,extensions=['extra','toc','sane_lists','md_in_html'],extension_configs={'toc':{'slugify':slugify_unicode}})
        # Mermaid source remains available as details; the offline SVG is the reading surface.
        h=re.sub(r'<pre><code class="language-mermaid">(.*?)</code></pre>',r'<details><summary>关系结构文本</summary><pre><code>\1</code></pre></details>',h,flags=re.S)
        if rel=='03-系统设计文档.md':h=h.replace('<h2 id="三总体架构">','<h2 id="三总体架构">')+architecture()
        if rel=='附录/A13-展开字段字典.md':h=h[:h.index('<h2')]+ '<h2 id="logical-relations">核心逻辑关系图</h2><p>鸟足标记：竖线表示一，圆圈表示可选，分叉表示多；虚线均为应用逻辑关系，不是数据库外键。完整关系及删除行为以 A01 的 REL 目录为准，以下展示主要业务链。</p>'+ergraph()+h[h.index('<h2'):]
        ids=re.findall(r'\bid="([^"]+)"',h);anchors[rel]=set(ids)
        for id in ids:
            if re.match(r'(api|db|page|fr|nfr)-',id) and id not in targets:targets[id]=fid+'--'+id
        # Number suffixes handle any repeated raw anchor inside an imported contract.
        seen={}
        def pre(m):
            key=m.group(1);seen[key]=seen.get(key,0)+1
            return 'id="'+fid+'--'+key+(('-repeat-'+str(seen[key])) if seen[key]>1 else '')+'"'
        h=re.sub(r'\bid="([^"]+)"',pre,h)
        h=re.sub(r'url\(#([^\)]+)\)',lambda m:'url(#'+fid+'--'+m.group(1)+')',h)
        rendered[rel]=h
    broken=[]
    for rel,h in list(rendered.items()):
        fid=mapping[rel]
        def href(m):
            raw=html.unescape(m.group(1));target=unquote(raw)
            if target.startswith(('https:','http:','mailto:')):return m.group(0)
            path,sep,anchor=target.partition('#')
            if not path:dest=rel
            else:dest=Path(os.path.normpath(str(Path(rel).parent/path))).as_posix()
            # Links to original evidence remain within the standalone evidence register.
            if dest not in mapping:
                if path.endswith('.md') or path.endswith('.json'):dest='09-基线核对与需求追溯.md';anchor='';sep=''
                elif path=='index.html':dest='README.md';anchor='';sep=''
                else:broken.append((rel,raw));return m.group(0)
            if anchor not in anchors.get(dest,set()):
                if anchor in targets:return 'href="#'+targets[anchor]+'"'
                # Legacy Markdown references occasionally spell punctuation differently.
                key=re.sub(r'[^\w\u4e00-\u9fff]','',anchor)
                match=next((x for x in anchors[dest] if re.sub(r'[^\w\u4e00-\u9fff]','',x)==key),None)
                if match:anchor=match
                elif anchor:
                    broken.append((rel,raw));anchor=''
            final=mapping[dest]+(('--'+anchor) if anchor else '')
            return 'href="#'+final+'"'
        rendered[rel]=re.sub(r'href="([^"]+)"',href,h)
        rendered[rel]=re.sub(r'<table>(.*?)</table>',r'<div class="table-scroll"><table>\1</table></div>',rendered[rel],flags=re.S)
    # Prefer complete appendix contracts over copied module excerpts for API navigation.
    for a in C['api']:
        rel=APPENDIX[a['source']];targets[a['id'].lower()]=mapping[rel]+'--'+a['id'].lower()
    cat={
      'api':[dict(title=a['id']+' '+a['method']+' '+a['path'],desc=a['name'],target=targets[a['id'].lower()]) for a in C['api']],
      'table':[dict(title=t['id']+' '+t['name'],desc=NAMES[int(t['id'][3:])-1]+' · '+str(len(t['fields']))+' 列设计',target=mapping['附录/A13-展开字段字典.md']+'--'+t['id'].lower()) for t in C['tables']],
      'page':[dict(title=p['id']+' '+p['route'],desc=p['name']+' · '+p['permission'],target=mapping['附录/A06-页面交互.md']) for p in C['pages']],
      'fr':[dict(title=f['id'],desc=f['name'],target=mapping['02-需求分析文档.md']+'--fr-'+f['group'].lower()) for f in C['fr']],
      'nfr':[dict(title=n['id'],desc=next(x[1] for x in NFR_CATEGORIES if x[0]==n['group'])+' · '+n['target'],target=mapping['02-需求分析文档.md']+'--nfr-'+n['group'].lower()) for n in C['nfr']],
      'class':[dict(title=p,desc='真实文件 · 后端骨架',target=mapping['09-基线核对与需求追溯.md']) for p in ['backend/app/src/main/java/com/asterway/app/AsterwayApplication.java']+[f'backend/{m}/src/main/java/com/asterway/{m}/package-info.java' for m in ['common','identity','growth','planning','execution','habit','insights','notification','administration']]]}
    nav='';articles='';directory='';lastgroup=''
    for p in paths:
        rel=p.relative_to(OUT).as_posix();fid=mapping[rel];group='模块设计' if rel.startswith('模块设计/') else '附录规范' if rel.startswith('附录/') else '开发主文档';title='阅读说明' if p.name=='README.md' else p.stem
        if group!=lastgroup:nav+='<div class="nav-group">'+group+'</div>';lastgroup=group
        nav+='<a class="file-nav" href="#'+fid+'">'+esc(title)+'</a>'
        directory+='<li><a href="#'+fid+'">'+esc(title)+'</a></li>'
        articles+='<article class="document" id="'+fid+'" data-title="'+esc(title)+'"><div class="doc-label">ASTERWAY / '+esc(group)+' / V2.0</div>'+rendered[rel]+'</article>'
    cards=''.join('<div class="metric"><b>'+str(n)+'</b><span>'+label+'</span></div>' for n,label in [(12,'业务模块'),(132,'功能需求'),(len(C['nfr']),'质量需求'),(95,'接口设计'),(36,'设计数据表'),(20,'页面设计')])
    effective_css=CSS+'\n.document [id]{scroll-margin-top:90px}[hidden]{display:none!important}@media print{.print-all-mode .document[hidden]{display:block!important}}'
    page='''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="description" content="Asterway 全栈项目开发文档 V2.0：从需求、业务、数据、接口与类设计到测试、部署和运维。"><title>Asterway · 全栈开发文档 V2.0</title><style>'''+effective_css+'''</style></head><body class="nojs"><aside class="sidebar"><div class="brand">Asterway</div><div class="edition">全栈开发文档 · V2.0</div><input class="nav-search" aria-label="筛选目录" placeholder="筛选目录">'''+nav+'''<div class="side-note">文档日期 2026.10.22<br>需求 → 设计 → 实施 → 验证<br>离线阅读 · 无外部展示依赖</div></aside><div class="workspace"><div class="toolbar"><button class="mobile-toggle" aria-label="打开目录">目录</button><input id="global-search" type="search" placeholder="全文搜索：功能编号、接口、表名或业务关键词" aria-label="全文搜索"><button id="expand">展开本册</button><button id="collapse">收起本册</button><button id="print">打印本册</button><button id="print-all" class="print-all">完整打印</button></div><main class="content"><section class="cover"><div class="eyebrow">PROJECT DEVELOPMENT HANDBOOK / 2026</div><h1>从目标规划，到真实执行</h1><p>个人成长管理系统的完整开发规范。以使用者场景为起点，逐步明确业务逻辑、数据、接口与实现职责，贯通测试、部署和运维。</p><span class="badge">V2.0 设计审阅稿</span><span class="badge">Java 17 · 模块化单体</span><span class="badge">2026.10.22</span></section><div class="metrics">'''+cards+'''</div><div class="boundary"><strong>当前工程边界：</strong>已建立后端骨架，Maven 工程模型校验通过；业务代码、数据库迁移、前端及运行验收尚未完成。目录数量均为设计范围。</div><section class="catalog-panel"><div class="catalog-controls"><button data-cat="api" aria-pressed="true">接口 95</button><button data-cat="table" aria-pressed="false">数据表 36</button><button data-cat="page" aria-pressed="false">页面 20</button><button data-cat="fr" aria-pressed="false">功能需求 132</button><button data-cat="nfr" aria-pressed="false">质量需求 84</button><button data-cat="class" aria-pressed="false">真实类文件 10</button><input id="catalog-search" type="search" placeholder="筛选本目录" aria-label="筛选分类目录"></div><div class="catalog-body"></div></section><section class="search-results" aria-live="polite"></section><section class="print-directory"><h1>Asterway 开发文档 V2.0</h1><p>目录索引 · 2026-10-22 · 设计审阅稿</p><ul>'''+directory+'''</ul><h2>业务模块与物理表目录</h2><p>'''+esc('；'.join(t['id']+' '+t['name'] for t in TABLE))+'''</p></section><noscript><p class="noscript">脚本未启用：全部正文可顺序阅读；可使用浏览器查找和打印。</p></noscript><div class="current-title doc-label"></div><div class="article-wrap"><div class="articles">'''+articles+'''</div><nav class="section-index" aria-label="本册章节目录"></nav></div><div class="footer">Asterway · 全栈开发文档 V2.0 · 文档与运行状态分别留证</div></main></div><script>'''+JS.replace('CATALOG_DATA',json.dumps(cat,ensure_ascii=False).replace('</','<\/'))+'''</script></body></html>'''
    (OUT/'index.html').write_text(page,encoding='utf-8',newline='\n')
    (DATA/'render-map.json').write_text(json.dumps(dict(files=mapping,targets=targets,legacy_anchor_fallbacks=broken),ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(dict(documents=len(paths),html_bytes=len(page.encode('utf-8')),legacy_anchor_fallbacks=len(broken)),ensure_ascii=False))
    return broken

if __name__=='__main__':render()
