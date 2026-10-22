"""Rebuild the V2.0 document set and self-contained reader."""
from write_main import main01,main02,main03,meta,write
from write_modules import run
from write_delivery import main05,main06,main07,main08,audit,readme
from render import render
from verify import verify,verification_doc

main01();main02();main03();run()
main05();main06();main07();main08();audit();readme()
# Establish the file before the first link check; final content is replaced below.
write('10-文档校验记录.md',meta('10 文档校验记录')+'校验记录由结构检查结果更新。')
render()
r=verify()
if r['errors']:raise SystemExit(1)
verification_doc(r)
render()
r=verify()
if r['errors']:raise SystemExit(1)
# Refresh numeric self-inventory until the record no longer changes the link count.
verification_doc(r)
render()
r=verify()
if r['errors']:raise SystemExit(1)
