# 仅在专用 PostgreSQL 校验库复核 Execution SQL，不连接或部署开发库。
# 复用当前本地 Ubuntu/PostgreSQL 环境；不安装依赖，不修改服务配置。
$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'review_execution_sql.py'
$linuxScriptPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to resolve the execution SQL review script path.'
}
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/python3 $linuxScriptPath
if ($LASTEXITCODE -ne 0) {
    throw 'Execution SQL review failed. Inspect the error; no backend or frontend acceptance is implied.'
}
