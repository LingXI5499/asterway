# 仅在专用校验库的随机 schema 复核 P1.2 SQL；不连接或部署开发库。
$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'review_agent_sql.py'
$linuxScriptPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve Agent SQL review script.' }
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/python3 $linuxScriptPath
if ($LASTEXITCODE -ne 0) { throw 'Agent SQL review failed; no backend or HTTP acceptance is implied.' }
