# 按明确的本地路径组织开始账户管理本地操作，调用已有命令完成启动或校验。
# 声明脚本可以接收的开关或参数，后面按参数决定执行范围。
param([switch]$Stop)
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Join-Path (Split-Path $PSScriptRoot -Parent) '.asterway-private' 的结果 保存到 runtimeDirectory。
$runtimeDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) '.asterway-private'
# 把 表达式 Join-Path $runtimeDirectory 'identity-runtime.json' 的结果 保存到 当前状态文件（stateFile）。
$stateFile = Join-Path $runtimeDirectory 'identity-runtime.json'

# 检查条件 $Stop 是否成立；成立时执行对应分支。
if ($Stop) {
    # 仅结束本脚本的命名保持进程；不 terminate Ubuntu，不停止其他项目的 WSL 服务。
    # 调用这里明确指定的已有脚本或程序，并传入后面的参数。
    & wsl.exe -d Ubuntu --exec pkill -f '^asterway-identity-runtime infinity$'
    # 检查条件 $LASTEXITCODE -notin 0,1 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -notin 0,1) { throw 'Unable to stop the local runtime holder.' }
    # 检查条件 Test-Path -LiteralPath $stateFile 是否成立；成立时执行对应分支。
    if (Test-Path -LiteralPath $stateFile) { Remove-Item -LiteralPath $stateFile }
    # 把操作结果输出给调用方，方便确认当前执行到了哪一步。
    Write-Output 'Local runtime holder stopped. Database files are preserved.'
    # 结束当前函数或脚本，并按这里的值报告结果。
    exit
}

# 把 空值（$null） 保存到 holder。
$holder = $null
# 检查条件 Test-Path -LiteralPath $stateFile 是否成立；成立时执行对应分支。
if (Test-Path -LiteralPath $stateFile) {
    # 把 表达式 Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json 的结果 保存到 当前状态（state）。
    $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    # 把 表达式 Get-Process -Id $state.processId -ErrorAction SilentlyContinue 的结果 保存到 候选（candidate）。
    $candidate = Get-Process -Id $state.processId -ErrorAction SilentlyContinue
    # 检查条件 ($candidate -and $candidate.ProcessName -eq 'wsl' -and 是否成立；成立时执行对应分支。
    if ($candidate -and $candidate.ProcessName -eq 'wsl' -and
        # 继续执行当前本地步骤：[string]$candidate.StartTime.ToUniversalTime().Ticks -eq $state.startedTicks) {。
        [string]$candidate.StartTime.ToUniversalTime().Ticks -eq $state.startedTicks) {
        # 把 候选（$candidate） 保存到 holder。
        $holder = $candidate
    }
}
# 检查条件 -not $holder 是否成立；成立时执行对应分支。
if (-not $holder) {
    # 创建本次运行需要的目录或文件。
    New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
    # 把 表达式 Start-Process -FilePath 'wsl.exe' -ArgumentList @('-d','Ubuntu','—exec','bash','-c','"exec -a asterway-identity-runtime… 的结果 保存到 holder。
    $holder = Start-Process -FilePath 'wsl.exe' -ArgumentList @('-d','Ubuntu','--exec','bash','-c','"exec -a asterway-identity-runtime sleep infinity"') -WindowStyle Hidden -PassThru
    # 继续执行当前本地步骤：@{processId=$holder.Id;startedTicks=[string]$holder.StartTime.ToUniversalTime().Ticks} |。
    @{processId=$holder.Id;startedTicks=[string]$holder.StartTime.ToUniversalTime().Ticks} |
        # 把本次检查或运行结果编码为 JSON，便于后续核对。
        ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding utf8
}
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu --exec systemctl is-active postgresql redis-server
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL or Redis is not running in Ubuntu.' }
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($port in @(5432,6379)) {
    # 把 不成立（$false） 保存到 已就绪（ready）。
    $ready = $false
    # 按这里的集合或重试条件循环处理，直到完成或达到上限。
    for ($attempt=0; $attempt -lt 30; $attempt++) {
        # 把 表达式 [System.Net.Sockets.TcpClient]::new() 的结果 保存到 客户端（client）。
        $client = [System.Net.Sockets.TcpClient]::new()
        # 尝试执行这组命令，失败交给 catch，资源清理由 finally 执行。
        try { $client.Connect('127.0.0.1',$port); $ready=$true; break }
        # 捕获命令失败原因，按下面规则报告或继续恢复。
        catch { Start-Sleep -Milliseconds 200 }
        # 无论前面是否成功，都释放本次打开的临时资源。
        finally { $client.Dispose() }
    }
    # 检查条件 -not $ready 是否成立；成立时执行对应分支。
    if (-not $ready) { throw "Localhost port $port is unavailable." }
}
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Local PostgreSQL and Redis are ready on localhost. WSL runtime holder is active.'
