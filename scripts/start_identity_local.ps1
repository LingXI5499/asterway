param([switch]$Stop)
$ErrorActionPreference = 'Stop'
$runtimeDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) '.asterway-private'
$stateFile = Join-Path $runtimeDirectory 'identity-runtime.json'

if ($Stop) {
    # 仅结束本脚本的命名保持进程；不 terminate Ubuntu，不停止其他项目的 WSL 服务。
    & wsl.exe -d Ubuntu --exec pkill -f '^asterway-identity-runtime infinity$'
    if ($LASTEXITCODE -notin 0,1) { throw 'Unable to stop the local runtime holder.' }
    if (Test-Path -LiteralPath $stateFile) { Remove-Item -LiteralPath $stateFile }
    Write-Output 'Local runtime holder stopped. Database files are preserved.'
    exit
}

$holder = $null
if (Test-Path -LiteralPath $stateFile) {
    $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    $candidate = Get-Process -Id $state.processId -ErrorAction SilentlyContinue
    if ($candidate -and $candidate.ProcessName -eq 'wsl' -and
        [string]$candidate.StartTime.ToUniversalTime().Ticks -eq $state.startedTicks) {
        $holder = $candidate
    }
}
if (-not $holder) {
    New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
    $holder = Start-Process -FilePath 'wsl.exe' -ArgumentList @('-d','Ubuntu','--exec','bash','-c','"exec -a asterway-identity-runtime sleep infinity"') -WindowStyle Hidden -PassThru
    @{processId=$holder.Id;startedTicks=[string]$holder.StartTime.ToUniversalTime().Ticks} |
        ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding utf8
}
& wsl.exe -d Ubuntu --exec systemctl is-active postgresql redis-server
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL or Redis is not running in Ubuntu.' }
foreach ($port in @(5432,6379)) {
    $ready = $false
    for ($attempt=0; $attempt -lt 30; $attempt++) {
        $client = [System.Net.Sockets.TcpClient]::new()
        try { $client.Connect('127.0.0.1',$port); $ready=$true; break }
        catch { Start-Sleep -Milliseconds 200 }
        finally { $client.Dispose() }
    }
    if (-not $ready) { throw "Localhost port $port is unavailable." }
}
Write-Output 'Local PostgreSQL and Redis are ready on localhost. WSL runtime holder is active.'
