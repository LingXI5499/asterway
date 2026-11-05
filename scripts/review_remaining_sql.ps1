# 仅连接专用校验库，随机 schema 回滚或清理；不连接、部署或清理开发库。
$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'review_remaining_sql.py'
$linuxScriptPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the remaining SQL review script path.' }
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/python3 $linuxScriptPath
if ($LASTEXITCODE -ne 0) { throw 'Remaining SQL review failed; inspect the reported SQL assertion.' }
