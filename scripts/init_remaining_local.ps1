# 按明确的本地路径组织init剩余本地操作，调用已有命令完成启动或校验。
# 仅手工补齐已验收的后续八表与后端动作授权；不随应用启动执行。
# 保留现有主体、安排和执行事实，状态基线与初次提醒按已验收 SQL 初始化；新页面继续隐藏。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'backend/app/src/main/resources/sql' 保存到 sqlDirectory。
$sqlDirectory = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql'
# 把 表达式 Join-Path $repositoryRoot '.asterway-private' 的结果 保存到 runtimeDirectory。
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
# 把 按表达式计算得到的值：Get-Content -LiteralPath (Join-Path $repositoryRoot 'docs/remaining/sql-check-results.json') -Raw | ConvertFrom-Json 保存到 approved复盘报告（approvedReport）。
$approvedReport = Get-Content -LiteralPath (Join-Path $repositoryRoot 'docs/remaining/sql-check-results.json') -Raw | ConvertFrom-Json
# 检查条件 $approvedReport.status -ne 'PASSED' 是否成立；成立时执行对应分支。
if ($approvedReport.status -ne 'PASSED') { throw 'The approved SQL verification report is required.' }
# 把 表达式 @{} 的结果 保存到 来源（source）。
$source = @{}
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($moduleName in @('identity','goal','stage','task','planning','execution','habit','notification')) {
    # 把 固定文字 'backend/app/src/main/resources/sql/$moduleName.sql' 保存到 relative路径（relativePath）。
    $relativePath = "backend/app/src/main/resources/sql/$moduleName.sql"
    # 把 调用 (Get-FileHash -LiteralPath (Join-Path $repositoryRoot $relativePath) -Algorithm SHA256).Hash.ToLowerInvariant 完成ToLowerInvariant处理 保存到 实际摘要值（actualHash）。
    $actualHash = (Get-FileHash -LiteralPath (Join-Path $repositoryRoot $relativePath) -Algorithm SHA256).Hash.ToLowerInvariant()
    # 检查条件 $actualHash -ne $approvedReport.inputSha256.$relativePath 是否成立；成立时执行对应分支。
    if ($actualHash -ne $approvedReport.inputSha256.$relativePath) { throw "Approved SQL changed: $moduleName" }
    # 读取明确指定的本地文件，作为本次操作的配置或输入。
    $source[$moduleName] = Get-Content -LiteralPath (Join-Path $sqlDirectory "$moduleName.sql") -Raw -Encoding utf8
}
# 继续执行当前本地步骤：function Get-ManualBlock([string]$contents, [string]$marker) {。
function Get-ManualBlock([string]$contents, [string]$marker) {
    # 把 按表达式计算得到的值：'(?s)— BEGIN ' + [regex]::Escape($marker) + '(.*?)— END ' + [regex]::Escape($marker) 保存到 匹配规则（pattern）。
    $pattern = '(?s)-- BEGIN ' + [regex]::Escape($marker) + '(.*?)-- END ' + [regex]::Escape($marker)
    # 把 表达式 [regex]::Match($contents, $pattern) 的结果 保存到 match。
    $match = [regex]::Match($contents, $pattern)
    # 检查条件 -not $match.Success 是否成立；成立时执行对应分支。
    if (-not $match.Success) { throw "Missing manual SQL block: $marker" }
    # 结束当前函数或脚本，并按这里的值报告结果。
    return $match.Groups[1].Value
}
# 继续执行当前本地步骤：function Get-Body([string]$contents) {。
function Get-Body([string]$contents) {
    # 结束当前函数或脚本，并按这里的值报告结果。
    return $contents -replace '(?m)^(BEGIN|COMMIT);[ \t]*\r?$', ''
}
# 把 表达式 @('habit_category','habit','habit_revision','habit_checkin','notification_message','goal_state_history','stage_state_his… 的结果 保存到 新Tables（newTables）。
$newTables = @('habit_category','habit','habit_revision','habit_checkin','notification_message','goal_state_history','stage_state_history','task_state_history')
# 把 表达式 @( 的结果 保存到 ddlParts。
$ddlParts = @(
    # 继续执行当前本地步骤：(Get-ManualBlock $source.goal 'GOAL STATE HISTORY'),。
    (Get-ManualBlock $source.goal 'GOAL STATE HISTORY'),
    # 继续执行当前本地步骤：(Get-ManualBlock $source.stage 'STAGE STATE HISTORY'),。
    (Get-ManualBlock $source.stage 'STAGE STATE HISTORY'),
    # 继续执行当前本地步骤：(Get-ManualBlock $source.task 'TASK STATE HISTORY'),。
    (Get-ManualBlock $source.task 'TASK STATE HISTORY'),
    # 继续执行当前本地步骤：(Get-Body $source.habit),。
    (Get-Body $source.habit),
    # 继续执行当前本地步骤：(Get-Body $source.notification)。
    (Get-Body $source.notification)
)
# 把 表达式 [System.Text.StringBuilder]::new() 的结果 保存到 statements。
$statements = [System.Text.StringBuilder]::new()
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
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
# 继续执行当前本地步骤：[void]$statements.AppendLine((Get-ManualBlock $source.goal 'GOAL CONDITION UPGRADE'))。
[void]$statements.AppendLine((Get-ManualBlock $source.goal 'GOAL CONDITION UPGRADE'))
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
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
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($part in $ddlParts) { [void]$statements.AppendLine($part) }
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
[void]$statements.AppendLine(@'
$approved_ddl$;
    ELSIF present<>8 THEN
        RAISE EXCEPTION 'Partial remaining schema exists; review the existing objects before initialization';
    END IF;
END $new_tables$;
'@)
# 逐表比对列名、类型和顺序，已有八表时也必须符合当前已验收定义。
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($part in $ddlParts) {
    # 按这里的集合或重试条件循环处理，直到完成或达到上限。
    foreach ($tableMatch in [regex]::Matches($part, '(?s)CREATE TABLE ([a-z_]+) \((.*?)\r?\n\);')) {
        # 把 表达式 $tableMatch.Groups[1].Value 的结果 保存到 表名称（tableName）。
        $tableName = $tableMatch.Groups[1].Value
        # 把 表达式 @() 的结果 保存到 columns。
        $columns = @()
        # 按这里的集合或重试条件循环处理，直到完成或达到上限。
        foreach ($columnMatch in [regex]::Matches($tableMatch.Groups[2].Value, '(?m)^    ([a-z_]+)\s+(bigint|varchar\(\d+\)|integer|smallint|date|time\(0\)|timestamptz\(3\)|boolean|text)')) {
            # 把 表达式 $columnMatch.Groups[1].Value 的结果 保存到 字段名称（columnName）。
            $columnName = $columnMatch.Groups[1].Value
            # 把 表达式 $columnMatch.Groups[2].Value 的结果 保存到 字段类型（columnType）。
            $columnType = $columnMatch.Groups[2].Value
            # 检查条件 $columnType.StartsWith('varchar') 是否成立；成立时执行对应分支。
            if ($columnType.StartsWith('varchar')) { $columnType = $columnType -replace 'varchar','character varying' }
            # 检查条件 $columnType -eq 'timestamptz(3)' 是否成立；成立时执行对应分支。
            if ($columnType -eq 'timestamptz(3)') { $columnType = 'timestamp(3) with time zone' }
            # 检查条件 $columnType -eq 'time(0)' 是否成立；成立时执行对应分支。
            if ($columnType -eq 'time(0)') { $columnType = 'time(0) without time zone' }
            # 继续执行当前本地步骤：$columns += "'$($columnName):$columnType'"。
            $columns += "'$($columnName):$columnType'"
        }
        # 检查条件 $columns.Count -eq 0 是否成立；成立时执行对应分支。
        if ($columns.Count -eq 0) { throw "Unable to parse approved columns for $tableName" }
        # 继续执行当前本地步骤：[void]$statements.AppendLine("DO " + '$shape$ BEGIN IF (SELECT array_agg(a.attname || '':'' || format_type(a.atttypid,a.atttypmod) ORDER BY a.attnum) …。
        [void]$statements.AppendLine("DO " + '$shape$ BEGIN IF (SELECT array_agg(a.attname || '':'' || format_type(a.atttypid,a.atttypmod) ORDER BY a.attnum) FROM pg_attribute a WHERE a.attrelid=' + "'public.$tableName'::regclass AND a.attnum>0 AND NOT a.attisdropped) IS DISTINCT FROM ARRAY[" + ($columns -join ',') + "]::text[] THEN RAISE EXCEPTION 'Incompatible remaining table: $tableName'; END IF; END " + '$shape$;')
    }
}
# 继续执行当前本地步骤：[void]$statements.AppendLine("SET LOCAL asterway.remaining_backend_ready = 'true';")。
[void]$statements.AppendLine("SET LOCAL asterway.remaining_backend_ready = 'true';")
# 继续执行当前本地步骤：[void]$statements.AppendLine("SET LOCAL asterway.remaining_history_bootstrap = 'true';")。
[void]$statements.AppendLine("SET LOCAL asterway.remaining_history_bootstrap = 'true';")
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($moduleName in @('goal','stage','task')) {
    # 继续执行当前本地步骤：[void]$statements.AppendLine((Get-ManualBlock $source[$moduleName] ("REMAINING " + $moduleName.ToUpperInvariant() + " HISTORY BOOTSTRAP")))。
    [void]$statements.AppendLine((Get-ManualBlock $source[$moduleName] ("REMAINING " + $moduleName.ToUpperInvariant() + " HISTORY BOOTSTRAP")))
}
# 继续执行当前本地步骤：[void]$statements.AppendLine((Get-ManualBlock $source.notification 'REMAINING NOTIFICATION BOOTSTRAP'))。
[void]$statements.AppendLine((Get-ManualBlock $source.notification 'REMAINING NOTIFICATION BOOTSTRAP'))
# 继续执行当前本地步骤：[void]$statements.AppendLine((Get-ManualBlock $source.identity 'REMAINING AUTHORIZATION'))。
[void]$statements.AppendLine((Get-ManualBlock $source.identity 'REMAINING AUTHORIZATION'))
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
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
# 把 表达式 Join-Path $runtimeDirectory 'init-remaining-local.sql' 的结果 保存到 script路径（scriptPath）。
$scriptPath = Join-Path $runtimeDirectory 'init-remaining-local.sql'
# 继续执行当前本地步骤：[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))。
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $scriptPath).Trim 完成Trim处理 保存到 linux路径（linuxPath）。
$linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the manual SQL path.' }
# 把 表达式 Join-Path $runtimeDirectory 'init-remaining-local.stdout.log' 的结果 保存到 stdout。
$stdout = Join-Path $runtimeDirectory 'init-remaining-local.stdout.log'
# 把 表达式 Join-Path $runtimeDirectory 'init-remaining-local.stderr.log' 的结果 保存到 stderr。
$stderr = Join-Path $runtimeDirectory 'init-remaining-local.stderr.log'
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> $stdout 2> $stderr
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) {
    # 读取明确指定的本地文件，作为本次操作的配置或输入。
    Get-Content -LiteralPath $stderr -Tail 12
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Remaining initialization failed and the transaction was rolled back.'
}
# 把 表达式 Get-Content -LiteralPath $stdout -Raw 的结果 保存到 输出（output）。
$output = Get-Content -LiteralPath $stdout -Raw
# 把 按表达式计算得到的值：[regex]::Match($output, 'REMAINING_INITIALIZATION:(\{[^\r\n]+\})') 保存到 汇总（summary）。
$summary = [regex]::Match($output, 'REMAINING_INITIALIZATION:(\{[^\r\n]+\})')
# 检查条件 -not $summary.Success -or $output -notmatch '(?m)^COMMIT\s*$' 是否成立；成立时执行对应分支。
if (-not $summary.Success -or $output -notmatch '(?m)^COMMIT\s*$') { throw 'Initialization result is incomplete.' }
# 把当前结果写入明确指定的输出文件。
$summary.Groups[1].Value | Set-Content -LiteralPath (Join-Path $runtimeDirectory 'remaining-initialization-result.json') -Encoding utf8
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output $summary.Groups[1].Value
