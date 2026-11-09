# 检查复盘执行计时sql相关行为，断言失败时报告具体不符合预期的场景。
# 仅在专用 PostgreSQL 校验库复核 Execution SQL，不连接或部署开发库。
# 复用当前本地 Ubuntu/PostgreSQL 环境；不安装依赖，不修改服务配置。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Join-Path $PSScriptRoot 'review_execution_sql.py' 的结果 保存到 script路径（scriptPath）。
$scriptPath = Join-Path $PSScriptRoot 'review_execution_sql.py'
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $scriptPath).Trim 完成Trim处理 保存到 linuxScript路径（linuxScriptPath）。
$linuxScriptPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Unable to resolve the execution SQL review script path.'
}
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/python3 $linuxScriptPath
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Execution SQL review failed. Inspect the error; no backend or frontend acceptance is implied.'
}
