# 仅手工初始化已确认的执行表和后端授权；保留现有数据，前端入口继续隐藏。
# 不随应用启动执行，不修正旧目标归档安排，不重跑 Identity 或成长建表部分。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$ddlPath = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/execution.sql'
$ddl = Get-Content -LiteralPath $ddlPath -Raw -Encoding utf8
$ddl = $ddl -replace '(?m)^(BEGIN|COMMIT);[ \t]*\r?$', ''
$identity = Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql') -Raw -Encoding utf8
$seed = [regex]::Match($identity, '(?s)-- BEGIN EXECUTION AUTHORIZATION.*?-- END EXECUTION AUTHORIZATION')
if (-not $seed.Success) { throw 'Execution authorization block was not found.' }
$statements = [System.Text.StringBuilder]::new()
[void]$statements.AppendLine('\set ON_ERROR_STOP on')
[void]$statements.AppendLine('BEGIN;')
[void]$statements.AppendLine('SET LOCAL search_path TO public, pg_catalog;')
[void]$statements.AppendLine('SET LOCAL lock_timeout = ''5s'';')
[void]$statements.AppendLine('DO $guard$ BEGIN IF current_database() <> ''asterway_dev'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $guard$;')
[void]$statements.AppendLine("SELECT pg_advisory_xact_lock(hashtextextended('asterway_execution_initialization',0));")
[void]$statements.AppendLine('DO $create$ DECLARE present integer; BEGIN')
[void]$statements.AppendLine("SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname IN ('execution_session','execution_segment');")
[void]$statements.AppendLine('IF present=0 THEN EXECUTE $execution_ddl$')
[void]$statements.AppendLine($ddl)
[void]$statements.AppendLine('$execution_ddl$; ELSIF present <> 2 THEN RAISE EXCEPTION ''Partial execution schema exists; manual review is required''; END IF; END $create$;')
[void]$statements.AppendLine(@'
DO $shape$ BEGIN
    IF (SELECT count(*) FROM information_schema.columns WHERE table_schema='public'
        AND ((table_name='execution_session' AND column_name IN ('id','user_id','task_id','status','started_at',
            'finished_at','last_heartbeat_at','controller_session_id','finish_reason','version','created_at','updated_at'))
        OR (table_name='execution_segment' AND column_name IN ('id','execution_id','assignment_id','segment_no','started_at',
            'ended_at','end_reason','eligible_until','time_zone','heartbeat_timeout_seconds','created_at','updated_at')))) <> 24
       OR (SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname IN (
            'uk_execution_session_user_running','uk_execution_session_user_task_unfinished',
            'ix_execution_session_user_page','ix_execution_session_user_task_page','ix_execution_session_running_heartbeat',
            'uk_execution_segment_sequence','uk_execution_segment_open','ix_execution_segment_open_deadline',
            'ix_execution_segment_assignment')) <> 9 THEN
        RAISE EXCEPTION 'Existing execution schema does not match the approved columns and indexes';
    END IF;
END $shape$;
SET LOCAL asterway.execution_backend_ready = 'true';
SET LOCAL asterway.execution_frontend_ready = 'false';
'@)
[void]$statements.AppendLine($seed.Value)
[void]$statements.AppendLine('GRANT SELECT,INSERT,UPDATE ON execution_session,execution_segment TO asterway_dev;')
[void]$statements.AppendLine('GRANT USAGE,SELECT ON SEQUENCE execution_session_id_seq,execution_segment_id_seq TO asterway_dev;')
[void]$statements.AppendLine('REVOKE DELETE,TRUNCATE ON execution_session,execution_segment FROM asterway_dev;')
[void]$statements.AppendLine('COMMIT;')
$scriptPath = Join-Path $runtimeDirectory 'init-execution-local.sql'
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
$linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the execution initialization path.' }
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> (Join-Path $runtimeDirectory 'init-execution-local.stdout.log') 2> (Join-Path $runtimeDirectory 'init-execution-local.stderr.log')
if ($LASTEXITCODE -ne 0) {
    Get-Content -LiteralPath (Join-Path $runtimeDirectory 'init-execution-local.stderr.log') -Tail 12
    throw 'Execution initialization failed and was rolled back.'
}
Write-Output 'Execution tables and seven USER backend permissions initialized; execution page hidden; legacy goal delete retired; existing data preserved.'
