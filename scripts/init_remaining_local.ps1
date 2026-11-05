# 仅手工补齐已验收的后续八表与后端动作授权；不随应用启动执行。
# 保留现有主体、安排和执行事实，状态基线与初次提醒按已验收 SQL 初始化；新页面继续隐藏。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$sqlDirectory = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql'
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$approvedReport = Get-Content -LiteralPath (Join-Path $repositoryRoot 'docs/remaining/sql-check-results.json') -Raw | ConvertFrom-Json
if ($approvedReport.status -ne 'PASSED') { throw 'The approved SQL verification report is required.' }
$source = @{}
foreach ($moduleName in @('identity','goal','stage','task','planning','execution','habit','notification')) {
    $relativePath = "backend/app/src/main/resources/sql/$moduleName.sql"
    $actualHash = (Get-FileHash -LiteralPath (Join-Path $repositoryRoot $relativePath) -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $approvedReport.inputSha256.$relativePath) { throw "Approved SQL changed: $moduleName" }
    $source[$moduleName] = Get-Content -LiteralPath (Join-Path $sqlDirectory "$moduleName.sql") -Raw -Encoding utf8
}
function Get-ManualBlock([string]$contents, [string]$marker) {
    $pattern = '(?s)-- BEGIN ' + [regex]::Escape($marker) + '(.*?)-- END ' + [regex]::Escape($marker)
    $match = [regex]::Match($contents, $pattern)
    if (-not $match.Success) { throw "Missing manual SQL block: $marker" }
    return $match.Groups[1].Value
}
function Get-Body([string]$contents) {
    return $contents -replace '(?m)^(BEGIN|COMMIT);[ \t]*\r?$', ''
}
$newTables = @('habit_category','habit','habit_revision','habit_checkin','notification_message','goal_state_history','stage_state_history','task_state_history')
$ddlParts = @(
    (Get-ManualBlock $source.goal 'GOAL STATE HISTORY'),
    (Get-ManualBlock $source.stage 'STAGE STATE HISTORY'),
    (Get-ManualBlock $source.task 'TASK STATE HISTORY'),
    (Get-Body $source.habit),
    (Get-Body $source.notification)
)
$statements = [System.Text.StringBuilder]::new()
[void]$statements.AppendLine(@'
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO public, pg_catalog;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
SET LOCAL asterway.remaining_backend_ready = 'false';
SET LOCAL asterway.remaining_frontend_ready = 'false';
SET LOCAL asterway.remaining_history_bootstrap = 'false';
DO $guard$ BEGIN
    IF current_database()<>'asterway_dev' THEN RAISE EXCEPTION 'Wrong local database'; END IF;
    IF to_regclass('public.execution_segment') IS NULL OR to_regclass('public.task_assignment') IS NULL THEN
        RAISE EXCEPTION 'Existing approved core and Execution tables are required';
    END IF;
END $guard$;
SELECT pg_advisory_xact_lock(hashtextextended('asterway_remaining_initialization',0));
SELECT id FROM identity_user ORDER BY id FOR UPDATE;
CREATE TEMP TABLE remaining_before_facts AS
SELECT 'stage' AS kind,count(*) AS rows,md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) AS checksum FROM stage x
UNION ALL SELECT 'task',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM task x
UNION ALL SELECT 'weekly_plan',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM weekly_plan x
UNION ALL SELECT 'daily_plan',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM daily_plan x
UNION ALL SELECT 'task_assignment',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM task_assignment x
UNION ALL SELECT 'execution_session',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM execution_session x
UNION ALL SELECT 'execution_segment',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM execution_segment x;
'@)
[void]$statements.AppendLine((Get-ManualBlock $source.goal 'GOAL CONDITION UPGRADE'))
[void]$statements.AppendLine(@'
DO $new_tables$
DECLARE present integer;
BEGIN
    SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r'
        AND relname IN('habit_category','habit','habit_revision','habit_checkin','notification_message',
            'goal_state_history','stage_state_history','task_state_history');
    IF present=0 THEN
        EXECUTE $approved_ddl$
'@)
foreach ($part in $ddlParts) { [void]$statements.AppendLine($part) }
[void]$statements.AppendLine(@'
$approved_ddl$;
    ELSIF present<>8 THEN
        RAISE EXCEPTION 'Partial remaining schema exists; review the existing objects before initialization';
    END IF;
END $new_tables$;
'@)
# 逐表比对列名、类型和顺序，已有八表时也必须符合当前已验收定义。
foreach ($part in $ddlParts) {
    foreach ($tableMatch in [regex]::Matches($part, '(?s)CREATE TABLE ([a-z_]+) \((.*?)\r?\n\);')) {
        $tableName = $tableMatch.Groups[1].Value
        $columns = @()
        foreach ($columnMatch in [regex]::Matches($tableMatch.Groups[2].Value, '(?m)^    ([a-z_]+)\s+(bigint|varchar\(\d+\)|integer|smallint|date|time\(0\)|timestamptz\(3\)|boolean|text)')) {
            $columnName = $columnMatch.Groups[1].Value
            $columnType = $columnMatch.Groups[2].Value
            if ($columnType.StartsWith('varchar')) { $columnType = $columnType -replace 'varchar','character varying' }
            if ($columnType -eq 'timestamptz(3)') { $columnType = 'timestamp(3) with time zone' }
            if ($columnType -eq 'time(0)') { $columnType = 'time(0) without time zone' }
            $columns += "'$($columnName):$columnType'"
        }
        if ($columns.Count -eq 0) { throw "Unable to parse approved columns for $tableName" }
        [void]$statements.AppendLine("DO " + '$shape$ BEGIN IF (SELECT array_agg(a.attname || '':'' || format_type(a.atttypid,a.atttypmod) ORDER BY a.attnum) FROM pg_attribute a WHERE a.attrelid=' + "'public.$tableName'::regclass AND a.attnum>0 AND NOT a.attisdropped) IS DISTINCT FROM ARRAY[" + ($columns -join ',') + "]::text[] THEN RAISE EXCEPTION 'Incompatible remaining table: $tableName'; END IF; END " + '$shape$;')
    }
}
[void]$statements.AppendLine("SET LOCAL asterway.remaining_backend_ready = 'true';")
[void]$statements.AppendLine("SET LOCAL asterway.remaining_history_bootstrap = 'true';")
foreach ($moduleName in @('goal','stage','task')) {
    [void]$statements.AppendLine((Get-ManualBlock $source[$moduleName] ("REMAINING " + $moduleName.ToUpperInvariant() + " HISTORY BOOTSTRAP")))
}
[void]$statements.AppendLine((Get-ManualBlock $source.notification 'REMAINING NOTIFICATION BOOTSTRAP'))
[void]$statements.AppendLine((Get-ManualBlock $source.identity 'REMAINING AUTHORIZATION'))
[void]$statements.AppendLine(@'
GRANT SELECT,INSERT,UPDATE ON habit_category,habit,habit_checkin,notification_message TO asterway_dev;
REVOKE DELETE,TRUNCATE ON habit_category,habit,habit_checkin,notification_message FROM asterway_dev;
GRANT SELECT,INSERT ON habit_revision,goal_state_history,stage_state_history,task_state_history TO asterway_dev;
REVOKE UPDATE,DELETE,TRUNCATE ON habit_revision,goal_state_history,stage_state_history,task_state_history FROM asterway_dev;
GRANT USAGE,SELECT ON SEQUENCE habit_category_id_seq,habit_id_seq,habit_revision_id_seq,habit_checkin_id_seq,
    notification_message_id_seq,goal_state_history_id_seq,stage_state_history_id_seq,task_state_history_id_seq TO asterway_dev;
DO $preserved$
BEGIN
    IF EXISTS(
        SELECT 1 FROM remaining_before_facts b JOIN (
            SELECT 'stage' AS kind,count(*) AS rows,md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) AS checksum FROM stage x
            UNION ALL SELECT 'task',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM task x
            UNION ALL SELECT 'weekly_plan',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM weekly_plan x
            UNION ALL SELECT 'daily_plan',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM daily_plan x
            UNION ALL SELECT 'task_assignment',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM task_assignment x
            UNION ALL SELECT 'execution_session',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM execution_session x
            UNION ALL SELECT 'execution_segment',count(*),md5(COALESCE(jsonb_agg(to_jsonb(x) ORDER BY id)::text,'')) FROM execution_segment x
        ) a ON a.kind=b.kind WHERE a.rows<>b.rows OR a.checksum<>b.checksum
    ) THEN RAISE EXCEPTION 'Existing business facts changed during additive initialization'; END IF;
END $preserved$;
SELECT 'REMAINING_INITIALIZATION:' || jsonb_build_object(
    'businessTableCount',(SELECT count(*) FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r'),
    'newPermissionsEnabled',(SELECT count(*) FROM identity_permission WHERE split_part(code,':',1) IN('habit','notification','dashboard','statistics') AND is_enabled),
    'newVisibleMenus',(SELECT count(*) FROM identity_menu WHERE code IN('growth_habits','growth_habit_categories','growth_notifications','growth_dashboard','growth_statistics') AND is_visible),
    'goalBaselines',(SELECT count(*) FROM goal_state_history WHERE event_type='BASELINE'),
    'stageBaselines',(SELECT count(*) FROM stage_state_history WHERE event_type='BASELINE'),
    'taskBaselines',(SELECT count(*) FROM task_state_history WHERE event_type='BASELINE'),
    'taskCompletionsImported',(SELECT count(*) FROM task_state_history WHERE event_type='COMPLETION_IMPORTED'),
    'notificationCount',(SELECT count(*) FROM notification_message),
    'priorFactsPreserved',true
)::text;
COMMIT;
'@)
$scriptPath = Join-Path $runtimeDirectory 'init-remaining-local.sql'
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
$linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the manual SQL path.' }
$stdout = Join-Path $runtimeDirectory 'init-remaining-local.stdout.log'
$stderr = Join-Path $runtimeDirectory 'init-remaining-local.stderr.log'
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> $stdout 2> $stderr
if ($LASTEXITCODE -ne 0) {
    Get-Content -LiteralPath $stderr -Tail 12
    throw 'Remaining initialization failed and the transaction was rolled back.'
}
$output = Get-Content -LiteralPath $stdout -Raw
$summary = [regex]::Match($output, 'REMAINING_INITIALIZATION:(\{[^\r\n]+\})')
if (-not $summary.Success -or $output -notmatch '(?m)^COMMIT\s*$') { throw 'Initialization result is incomplete.' }
$summary.Groups[1].Value | Set-Content -LiteralPath (Join-Path $runtimeDirectory 'remaining-initialization-result.json') -Encoding utf8
Write-Output $summary.Groups[1].Value
