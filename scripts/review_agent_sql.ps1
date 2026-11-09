# 检查复盘成长助手sql相关行为，断言失败时报告具体不符合预期的场景。
# 仅在专用校验库的随机 schema 复核 P1.2 SQL；不连接或部署开发库。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Join-Path $PSScriptRoot 'review_agent_sql.py' 的结果 保存到 script路径（scriptPath）。
$scriptPath = Join-Path $PSScriptRoot 'review_agent_sql.py'
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $scriptPath).Trim 完成Trim处理 保存到 linuxScript路径（linuxScriptPath）。
$linuxScriptPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve Agent SQL review script.' }
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/python3 $linuxScriptPath
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Agent SQL review failed; no backend or HTTP acceptance is implied.' }
