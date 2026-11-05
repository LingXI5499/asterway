# 只在专用校验库中建立隔离 schema，全部 DDL 和检查最终 ROLLBACK。
# 不连接 asterway_dev，不清理 public，不修改既有 Identity 数据。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$schemaName = 'asterway_growth_review_' + [guid]::NewGuid().ToString('N')
$sqlFile = Join-Path $runtimeDirectory 'growth-review.sql'
$stdoutFile = Join-Path $runtimeDirectory 'growth-review.stdout.log'
$stderrFile = Join-Path $runtimeDirectory 'growth-review.stderr.log'
$moduleNames = @('goal', 'stage', 'task', 'planning')
$hashes = [ordered]@{}
$statements = [System.Text.StringBuilder]::new()
[void]$statements.AppendLine('\set ON_ERROR_STOP on')
[void]$statements.AppendLine('BEGIN;')
[void]$statements.AppendLine('DO $$ BEGIN IF current_database() <> ''asterway_identity_test'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $$;')
[void]$statements.AppendLine("CREATE SCHEMA $schemaName;")
[void]$statements.AppendLine("SET LOCAL search_path TO $schemaName, pg_catalog;")
foreach ($moduleName in $moduleNames) {
    $relativePath = "backend/app/src/main/resources/sql/$moduleName.sql"
    $modulePath = Join-Path $repositoryRoot $relativePath
    $ddl = Get-Content -LiteralPath $modulePath -Raw -Encoding utf8
    $hashes[$relativePath] = (Get-FileHash -LiteralPath $modulePath -Algorithm SHA256).Hash.ToLowerInvariant()
    # 合并四份原始 DDL 到同一个外层回滚事务，不复制维护另一套建表定义。
    $ddl = $ddl -replace '(?m)^BEGIN;\s*$', '' -replace '(?m)^COMMIT;\s*$', ''
    [void]$statements.AppendLine($ddl)
}
$checksPath = Join-Path $repositoryRoot 'docs/growth/SQL层校验.sql'
[void]$statements.AppendLine((Get-Content -LiteralPath $checksPath -Raw -Encoding utf8))
[void]$statements.AppendLine("SELECT 'POSTGRES_VERSION:' || current_setting('server_version');")
[void]$statements.AppendLine('ROLLBACK;')
[void]$statements.AppendLine("SELECT 'SCHEMA_ROLLED_BACK:' || (NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname='$schemaName'))::text;")
[System.IO.File]::WriteAllText($sqlFile, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
$wslSqlPath = (& wsl.exe -d Ubuntu --exec wslpath -a $sqlFile).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the SQL review path in Ubuntu.' }
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_identity_test -f $wslSqlPath 1> $stdoutFile 2> $stderrFile
if ($LASTEXITCODE -ne 0) {
    Get-Content -LiteralPath $stderrFile -Tail 12
    throw 'SQL review failed; the connection was closed and the transaction rolled back.'
}
$output = Get-Content -LiteralPath $stdoutFile -Raw
$resultMatch = [regex]::Match($output, 'GROWTH_RESULT:(\{[^\r\n]+\})')
if (-not $resultMatch.Success -or $output -notmatch 'SCHEMA_ROLLED_BACK:true') {
    throw 'SQL review output is incomplete.'
}
$result = $resultMatch.Groups[1].Value | ConvertFrom-Json
$versionMatch = [regex]::Match($output, 'POSTGRES_VERSION:([^\r\n]+)')
$report = [ordered]@{
    verifiedAt = [DateTimeOffset]::Now.ToString('o')
    database = 'asterway_identity_test'
    postgresqlVersion = $versionMatch.Groups[1].Value.Trim()
    scope = 'SQL-only isolated schema verification; not Service, API or concurrent-application acceptance'
    checks = $result.checks
    passed = $result.passed
    schemaRolledBack = $true
    developmentDatabaseChanged = $false
    sqlSha256 = $hashes
    cases = $result.cases
}
$reportPath = Join-Path $repositoryRoot 'docs/growth/sql-check-results.json'
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding utf8
Write-Output ("SQL checks passed: {0}/{1}. Isolated schema rolled back. Development database unchanged." -f $result.passed, $result.checks)
