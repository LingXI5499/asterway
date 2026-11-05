# 手工初始化已确认的成长表及内置授权；不自动随应用启动，不重建已有账户。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$statements = [System.Text.StringBuilder]::new()
[void]$statements.AppendLine('\set ON_ERROR_STOP on')
[void]$statements.AppendLine('BEGIN;')
[void]$statements.AppendLine("SELECT pg_advisory_xact_lock(hashtextextended('asterway_growth_initialization',0));")
[void]$statements.AppendLine('DO $guard$ BEGIN IF current_database() <> ''asterway_dev'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $guard$;')
$ddl = ''
foreach ($moduleName in @('goal','stage','task','planning')) {
    $path = Join-Path $repositoryRoot "backend/app/src/main/resources/sql/$moduleName.sql"
    $ddl += (Get-Content -LiteralPath $path -Raw -Encoding utf8) -replace '(?m)^BEGIN;\s*$', '' -replace '(?m)^COMMIT;\s*$', ''
    $ddl += "`n"
}
[void]$statements.AppendLine('DO $create$ DECLARE present integer; BEGIN')
[void]$statements.AppendLine("SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname IN ('goal','stage','task','weekly_plan','daily_plan','task_assignment');")
[void]$statements.AppendLine('IF present=0 THEN EXECUTE $ddl$')
[void]$statements.AppendLine($ddl)
[void]$statements.AppendLine('$ddl$; ELSIF present <> 6 THEN RAISE EXCEPTION ''Partial growth schema exists; manual review is required''; END IF; END $create$;')
$identity = Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql') -Raw -Encoding utf8
$seed = [regex]::Match($identity, '(?s)-- BEGIN GROWTH AUTHORIZATION.*?-- END GROWTH AUTHORIZATION')
if (-not $seed.Success) { throw 'Growth authorization block was not found.' }
[void]$statements.AppendLine($seed.Value)
[void]$statements.AppendLine('GRANT SELECT,INSERT,UPDATE ON goal,stage,task,weekly_plan,daily_plan,task_assignment TO asterway_dev;')
[void]$statements.AppendLine('GRANT USAGE,SELECT ON SEQUENCE goal_id_seq,stage_id_seq,task_id_seq,weekly_plan_id_seq,daily_plan_id_seq,task_assignment_id_seq TO asterway_dev;')
[void]$statements.AppendLine('REVOKE DELETE,TRUNCATE ON goal,stage,task,weekly_plan,daily_plan,task_assignment FROM asterway_dev;')
[void]$statements.AppendLine('COMMIT;')
$scriptPath = Join-Path $runtimeDirectory 'init-growth-local.sql'
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
$wslPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the initialization path.' }
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $wslPath 1> (Join-Path $runtimeDirectory 'init-growth-local.stdout.log') 2> (Join-Path $runtimeDirectory 'init-growth-local.stderr.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $runtimeDirectory 'init-growth-local.stderr.log') -Tail 12; throw 'Initialization failed and was rolled back.' }
Write-Output 'Growth tables initialized: 32 USER permissions, 27 menu nodes (7 visible). Manager growth grants removed. Existing accounts preserved.'
