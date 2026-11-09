# 检查复盘成长业务sql相关行为，断言失败时报告具体不符合预期的场景。
# 只在专用校验库中建立隔离 schema，全部 DDL 和检查最终 ROLLBACK。
# 不连接 asterway_dev，不清理 public，不修改既有 Identity 数据。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 表达式 Join-Path $repositoryRoot '.asterway-private' 的结果 保存到 runtimeDirectory。
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
# 把 调用 'asterway_growth_review_' + [guid]::NewGuid().ToString 完成To文字处理 保存到 数据库分区名称（schemaName）。
$schemaName = 'asterway_growth_review_' + [guid]::NewGuid().ToString('N')
# 把 表达式 Join-Path $runtimeDirectory 'growth-review.sql' 的结果 保存到 sql文件（sqlFile）。
$sqlFile = Join-Path $runtimeDirectory 'growth-review.sql'
# 把 表达式 Join-Path $runtimeDirectory 'growth-review.stdout.log' 的结果 保存到 stdout文件（stdoutFile）。
$stdoutFile = Join-Path $runtimeDirectory 'growth-review.stdout.log'
# 把 表达式 Join-Path $runtimeDirectory 'growth-review.stderr.log' 的结果 保存到 stderr文件（stderrFile）。
$stderrFile = Join-Path $runtimeDirectory 'growth-review.stderr.log'
# 把 表达式 @('goal', 'stage', 'task', 'planning') 的结果 保存到 moduleNames。
$moduleNames = @('goal', 'stage', 'task', 'planning')
# 把 表达式 [ordered]@{} 的结果 保存到 hashes。
$hashes = [ordered]@{}
# 把 表达式 [System.Text.StringBuilder]::new() 的结果 保存到 statements。
$statements = [System.Text.StringBuilder]::new()
# 继续执行当前本地步骤：[void]$statements.AppendLine('\set ON_ERROR_STOP on')。
[void]$statements.AppendLine('\set ON_ERROR_STOP on')
# 继续执行当前本地步骤：[void]$statements.AppendLine('BEGIN;')。
[void]$statements.AppendLine('BEGIN;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('DO $$ BEGIN IF current_database() <> ''asterway_identity_test'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $$;…。
[void]$statements.AppendLine('DO $$ BEGIN IF current_database() <> ''asterway_identity_test'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $$;')
# 继续执行当前本地步骤：[void]$statements.AppendLine("CREATE SCHEMA $schemaName;")。
[void]$statements.AppendLine("CREATE SCHEMA $schemaName;")
# 继续执行当前本地步骤：[void]$statements.AppendLine("SET LOCAL search_path TO $schemaName, pg_catalog;")。
[void]$statements.AppendLine("SET LOCAL search_path TO $schemaName, pg_catalog;")
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($moduleName in $moduleNames) {
    # 把 固定文字 'backend/app/src/main/resources/sql/$moduleName.sql' 保存到 relative路径（relativePath）。
    $relativePath = "backend/app/src/main/resources/sql/$moduleName.sql"
    # 把 表达式 Join-Path $repositoryRoot $relativePath 的结果 保存到 module路径（modulePath）。
    $modulePath = Join-Path $repositoryRoot $relativePath
    # 把 表达式 Get-Content -LiteralPath $modulePath -Raw -Encoding utf8 的结果 保存到 ddl。
    $ddl = Get-Content -LiteralPath $modulePath -Raw -Encoding utf8
    # 继续执行当前本地步骤：$hashes[$relativePath] = (Get-FileHash -LiteralPath $modulePath -Algorithm SHA256).Hash.ToLowerInvariant()。
    $hashes[$relativePath] = (Get-FileHash -LiteralPath $modulePath -Algorithm SHA256).Hash.ToLowerInvariant()
    # 合并四份原始 DDL 到同一个外层回滚事务，不复制维护另一套建表定义。
    # 把 按表达式计算得到的值：$ddl -replace '(?m)^BEGIN;\s*$', '' -replace '(?m)^COMMIT;\s*$', '' 保存到 ddl。
    $ddl = $ddl -replace '(?m)^BEGIN;\s*$', '' -replace '(?m)^COMMIT;\s*$', ''
    # 继续执行当前本地步骤：[void]$statements.AppendLine($ddl)。
    [void]$statements.AppendLine($ddl)
}
# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'docs/growth/SQL层校验.sql' 保存到 checks路径（checksPath）。
$checksPath = Join-Path $repositoryRoot 'docs/growth/SQL层校验.sql'
# 读取明确指定的本地文件，作为本次操作的配置或输入。
[void]$statements.AppendLine((Get-Content -LiteralPath $checksPath -Raw -Encoding utf8))
# 继续执行当前本地步骤：[void]$statements.AppendLine("SELECT 'POSTGRES_VERSION:' || current_setting('server_version');")。
[void]$statements.AppendLine("SELECT 'POSTGRES_VERSION:' || current_setting('server_version');")
# 继续执行当前本地步骤：[void]$statements.AppendLine('ROLLBACK;')。
[void]$statements.AppendLine('ROLLBACK;')
# 继续执行当前本地步骤：[void]$statements.AppendLine("SELECT 'SCHEMA_ROLLED_BACK:' || (NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname='$schemaName'))::text;")。
[void]$statements.AppendLine("SELECT 'SCHEMA_ROLLED_BACK:' || (NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname='$schemaName'))::text;")
# 继续执行当前本地步骤：[System.IO.File]::WriteAllText($sqlFile, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))。
[System.IO.File]::WriteAllText($sqlFile, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $sqlFile).Trim 完成Trim处理 保存到 wslSql路径（wslSqlPath）。
$wslSqlPath = (& wsl.exe -d Ubuntu --exec wslpath -a $sqlFile).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the SQL review path in Ubuntu.' }
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_identity_test -f $wslSqlPath 1> $stdoutFile 2> $stderrFile
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) {
    # 读取明确指定的本地文件，作为本次操作的配置或输入。
    Get-Content -LiteralPath $stderrFile -Tail 12
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'SQL review failed; the connection was closed and the transaction rolled back.'
}
# 把 表达式 Get-Content -LiteralPath $stdoutFile -Raw 的结果 保存到 输出（output）。
$output = Get-Content -LiteralPath $stdoutFile -Raw
# 把 按表达式计算得到的值：[regex]::Match($output, 'GROWTH_RESULT:(\{[^\r\n]+\})') 保存到 结果Match（resultMatch）。
$resultMatch = [regex]::Match($output, 'GROWTH_RESULT:(\{[^\r\n]+\})')
# 检查条件 -not $resultMatch.Success -or $output -notmatch 'SCHEMA_ROLLED_BACK:true' 是否成立；成立时执行对应分支。
if (-not $resultMatch.Success -or $output -notmatch 'SCHEMA_ROLLED_BACK:true') {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'SQL review output is incomplete.'
}
# 把 表达式 $resultMatch.Groups[1].Value | ConvertFrom-Json 的结果 保存到 结果（result）。
$result = $resultMatch.Groups[1].Value | ConvertFrom-Json
# 把 按表达式计算得到的值：[regex]::Match($output, 'POSTGRES_VERSION:([^\r\n]+)') 保存到 版本号Match（versionMatch）。
$versionMatch = [regex]::Match($output, 'POSTGRES_VERSION:([^\r\n]+)')
# 把 表达式 [ordered]@{ 的结果 保存到 复盘报告（report）。
$report = [ordered]@{
    # 继续执行当前本地步骤：verifiedAt = [DateTimeOffset]::Now.ToString('o')。
    verifiedAt = [DateTimeOffset]::Now.ToString('o')
    # 继续执行当前本地步骤：database = 'asterway_identity_test'。
    database = 'asterway_identity_test'
    # 继续执行当前本地步骤：postgresqlVersion = $versionMatch.Groups[1].Value.Trim()。
    postgresqlVersion = $versionMatch.Groups[1].Value.Trim()
    # 继续执行当前本地步骤：scope = 'SQL-only isolated schema verification; not Service, API or concurrent-application acceptance'。
    scope = 'SQL-only isolated schema verification; not Service, API or concurrent-application acceptance'
    # 继续执行当前本地步骤：checks = $result.checks。
    checks = $result.checks
    # 继续执行当前本地步骤：passed = $result.passed。
    passed = $result.passed
    # 继续执行当前本地步骤：schemaRolledBack = $true。
    schemaRolledBack = $true
    # 继续执行当前本地步骤：developmentDatabaseChanged = $false。
    developmentDatabaseChanged = $false
    # 继续执行当前本地步骤：sqlSha256 = $hashes。
    sqlSha256 = $hashes
    # 继续执行当前本地步骤：cases = $result.cases。
    cases = $result.cases
}
# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'docs/growth/sql-check-results.json' 保存到 复盘报告路径（reportPath）。
$reportPath = Join-Path $repositoryRoot 'docs/growth/sql-check-results.json'
# 把本次检查或运行结果编码为 JSON，便于后续核对。
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding utf8
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output ("SQL checks passed: {0}/{1}. Isolated schema rolled back. Development database unchanged." -f $result.passed, $result.checks)
