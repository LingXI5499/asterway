# 按明确的本地路径组织init执行计时本地操作，调用已有命令完成启动或校验。
# 仅手工初始化已确认的执行表和后端授权；保留现有数据，前端入口继续隐藏。
# 不随应用启动执行，不修正旧目标归档安排，不重跑 Identity 或成长建表部分。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 表达式 Join-Path $repositoryRoot '.asterway-private' 的结果 保存到 runtimeDirectory。
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/execution.sql' 保存到 ddl路径（ddlPath）。
$ddlPath = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/execution.sql'
# 把 表达式 Get-Content -LiteralPath $ddlPath -Raw -Encoding utf8 的结果 保存到 ddl。
$ddl = Get-Content -LiteralPath $ddlPath -Raw -Encoding utf8
# 把 按表达式计算得到的值：$ddl -replace '(?m)^(BEGIN|COMMIT);[ \t]*\r?$', '' 保存到 ddl。
$ddl = $ddl -replace '(?m)^(BEGIN|COMMIT);[ \t]*\r?$', ''
# 把 按表达式计算得到的值：Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql') -Raw -Encoding ut… 保存到 账户管理（identity）。
$identity = Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql') -Raw -Encoding utf8
# 把 按表达式计算得到的值：[regex]::Match($identity, '(?s)— BEGIN EXECUTION AUTHORIZATION.*?— END EXECUTION AUTHORIZATION') 保存到 seed。
$seed = [regex]::Match($identity, '(?s)-- BEGIN EXECUTION AUTHORIZATION.*?-- END EXECUTION AUTHORIZATION')
# 检查条件 -not $seed.Success 是否成立；成立时执行对应分支。
if (-not $seed.Success) { throw 'Execution authorization block was not found.' }
# 把 表达式 [System.Text.StringBuilder]::new() 的结果 保存到 statements。
$statements = [System.Text.StringBuilder]::new()
# 继续执行当前本地步骤：[void]$statements.AppendLine('\set ON_ERROR_STOP on')。
[void]$statements.AppendLine('\set ON_ERROR_STOP on')
# 继续执行当前本地步骤：[void]$statements.AppendLine('BEGIN;')。
[void]$statements.AppendLine('BEGIN;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('SET LOCAL search_path TO public, pg_catalog;')。
[void]$statements.AppendLine('SET LOCAL search_path TO public, pg_catalog;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('SET LOCAL lock_timeout = ''5s'';')。
[void]$statements.AppendLine('SET LOCAL lock_timeout = ''5s'';')
# 继续执行当前本地步骤：[void]$statements.AppendLine('DO $guard$ BEGIN IF current_database() <> ''asterway_dev'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $guard$;…。
[void]$statements.AppendLine('DO $guard$ BEGIN IF current_database() <> ''asterway_dev'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $guard$;')
# 继续执行当前本地步骤：[void]$statements.AppendLine("SELECT pg_advisory_xact_lock(hashtextextended('asterway_execution_initialization',0));")。
[void]$statements.AppendLine("SELECT pg_advisory_xact_lock(hashtextextended('asterway_execution_initialization',0));")
# 继续执行当前本地步骤：[void]$statements.AppendLine('DO $create$ DECLARE present integer; BEGIN')。
[void]$statements.AppendLine('DO $create$ DECLARE present integer; BEGIN')
# 继续执行当前本地步骤：[void]$statements.AppendLine("SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname IN ('ex…。
[void]$statements.AppendLine("SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname IN ('execution_session','execution_segment');")
# 继续执行当前本地步骤：[void]$statements.AppendLine('IF present=0 THEN EXECUTE $execution_ddl$')。
[void]$statements.AppendLine('IF present=0 THEN EXECUTE $execution_ddl$')
# 继续执行当前本地步骤：[void]$statements.AppendLine($ddl)。
[void]$statements.AppendLine($ddl)
# 继续执行当前本地步骤：[void]$statements.AppendLine('$execution_ddl$; ELSIF present <> 2 THEN RAISE EXCEPTION ''Partial execution schema exists; manual review is required'';…。
[void]$statements.AppendLine('$execution_ddl$; ELSIF present <> 2 THEN RAISE EXCEPTION ''Partial execution schema exists; manual review is required''; END IF; END $create$;')
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
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
# 继续执行当前本地步骤：[void]$statements.AppendLine($seed.Value)。
[void]$statements.AppendLine($seed.Value)
# 继续执行当前本地步骤：[void]$statements.AppendLine('GRANT SELECT,INSERT,UPDATE ON execution_session,execution_segment TO asterway_dev;')。
[void]$statements.AppendLine('GRANT SELECT,INSERT,UPDATE ON execution_session,execution_segment TO asterway_dev;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('GRANT USAGE,SELECT ON SEQUENCE execution_session_id_seq,execution_segment_id_seq TO asterway_dev;')。
[void]$statements.AppendLine('GRANT USAGE,SELECT ON SEQUENCE execution_session_id_seq,execution_segment_id_seq TO asterway_dev;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('REVOKE DELETE,TRUNCATE ON execution_session,execution_segment FROM asterway_dev;')。
[void]$statements.AppendLine('REVOKE DELETE,TRUNCATE ON execution_session,execution_segment FROM asterway_dev;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('COMMIT;')。
[void]$statements.AppendLine('COMMIT;')
# 把 表达式 Join-Path $runtimeDirectory 'init-execution-local.sql' 的结果 保存到 script路径（scriptPath）。
$scriptPath = Join-Path $runtimeDirectory 'init-execution-local.sql'
# 继续执行当前本地步骤：[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))。
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $scriptPath).Trim 完成Trim处理 保存到 linux路径（linuxPath）。
$linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the execution initialization path.' }
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> (Join-Path $runtimeDirectory 'init-execution-local.stdout.log') 2> (Join-Path $runtimeDirectory 'init-execution-local.stderr.log')
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) {
    # 读取明确指定的本地文件，作为本次操作的配置或输入。
    Get-Content -LiteralPath (Join-Path $runtimeDirectory 'init-execution-local.stderr.log') -Tail 12
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Execution initialization failed and was rolled back.'
}
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Execution tables and seven USER backend permissions initialized; execution page hidden; legacy goal delete retired; existing data preserved.'
