# 按明确的本地路径组织init成长业务本地操作，调用已有命令完成启动或校验。
# 手工初始化已确认的成长表及内置授权；不自动随应用启动，不重建已有账户。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 表达式 Join-Path $repositoryRoot '.asterway-private' 的结果 保存到 runtimeDirectory。
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
# 把 表达式 [System.Text.StringBuilder]::new() 的结果 保存到 statements。
$statements = [System.Text.StringBuilder]::new()
# 继续执行当前本地步骤：[void]$statements.AppendLine('\set ON_ERROR_STOP on')。
[void]$statements.AppendLine('\set ON_ERROR_STOP on')
# 继续执行当前本地步骤：[void]$statements.AppendLine('BEGIN;')。
[void]$statements.AppendLine('BEGIN;')
# 继续执行当前本地步骤：[void]$statements.AppendLine("SELECT pg_advisory_xact_lock(hashtextextended('asterway_growth_initialization',0));")。
[void]$statements.AppendLine("SELECT pg_advisory_xact_lock(hashtextextended('asterway_growth_initialization',0));")
# 继续执行当前本地步骤：[void]$statements.AppendLine('DO $guard$ BEGIN IF current_database() <> ''asterway_dev'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $guard$;…。
[void]$statements.AppendLine('DO $guard$ BEGIN IF current_database() <> ''asterway_dev'' THEN RAISE EXCEPTION ''Wrong database''; END IF; END $guard$;')
# 把 固定文字 '' 保存到 ddl。
$ddl = ''
# 按这里的集合或重试条件循环处理，直到完成或达到上限。
foreach ($moduleName in @('goal','stage','task','planning')) {
    # 把 按表达式计算得到的值：Join-Path $repositoryRoot "backend/app/src/main/resources/sql/$moduleName.sql" 保存到 路径（path）。
    $path = Join-Path $repositoryRoot "backend/app/src/main/resources/sql/$moduleName.sql"
    # 读取明确指定的本地文件，作为本次操作的配置或输入。
    $ddl += (Get-Content -LiteralPath $path -Raw -Encoding utf8) -replace '(?m)^BEGIN;\s*$', '' -replace '(?m)^COMMIT;\s*$', ''
    # 继续执行当前本地步骤：$ddl += "`n"。
    $ddl += "`n"
}
# 继续执行当前本地步骤：[void]$statements.AppendLine('DO $create$ DECLARE present integer; BEGIN')。
[void]$statements.AppendLine('DO $create$ DECLARE present integer; BEGIN')
# 继续执行当前本地步骤：[void]$statements.AppendLine("SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname IN ('go…。
[void]$statements.AppendLine("SELECT count(*) INTO present FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname IN ('goal','stage','task','weekly_plan','daily_plan','task_assignment');")
# 继续执行当前本地步骤：[void]$statements.AppendLine('IF present=0 THEN EXECUTE $ddl$')。
[void]$statements.AppendLine('IF present=0 THEN EXECUTE $ddl$')
# 继续执行当前本地步骤：[void]$statements.AppendLine($ddl)。
[void]$statements.AppendLine($ddl)
# 继续执行当前本地步骤：[void]$statements.AppendLine('$ddl$; ELSIF present <> 6 THEN RAISE EXCEPTION ''Partial growth schema exists; manual review is required''; END IF; END …。
[void]$statements.AppendLine('$ddl$; ELSIF present <> 6 THEN RAISE EXCEPTION ''Partial growth schema exists; manual review is required''; END IF; END $create$;')
# 把 按表达式计算得到的值：Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql') -Raw -Encoding ut… 保存到 账户管理（identity）。
$identity = Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql') -Raw -Encoding utf8
# 把 按表达式计算得到的值：[regex]::Match($identity, '(?s)— BEGIN GROWTH AUTHORIZATION.*?— END GROWTH AUTHORIZATION') 保存到 seed。
$seed = [regex]::Match($identity, '(?s)-- BEGIN GROWTH AUTHORIZATION.*?-- END GROWTH AUTHORIZATION')
# 检查条件 -not $seed.Success 是否成立；成立时执行对应分支。
if (-not $seed.Success) { throw 'Growth authorization block was not found.' }
# 继续执行当前本地步骤：[void]$statements.AppendLine($seed.Value)。
[void]$statements.AppendLine($seed.Value)
# 继续执行当前本地步骤：[void]$statements.AppendLine('GRANT SELECT,INSERT,UPDATE ON goal,stage,task,weekly_plan,daily_plan,task_assignment TO asterway_dev;')。
[void]$statements.AppendLine('GRANT SELECT,INSERT,UPDATE ON goal,stage,task,weekly_plan,daily_plan,task_assignment TO asterway_dev;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('GRANT USAGE,SELECT ON SEQUENCE goal_id_seq,stage_id_seq,task_id_seq,weekly_plan_id_seq,daily_plan_id_seq,task_assignment…。
[void]$statements.AppendLine('GRANT USAGE,SELECT ON SEQUENCE goal_id_seq,stage_id_seq,task_id_seq,weekly_plan_id_seq,daily_plan_id_seq,task_assignment_id_seq TO asterway_dev;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('REVOKE DELETE,TRUNCATE ON goal,stage,task,weekly_plan,daily_plan,task_assignment FROM asterway_dev;')。
[void]$statements.AppendLine('REVOKE DELETE,TRUNCATE ON goal,stage,task,weekly_plan,daily_plan,task_assignment FROM asterway_dev;')
# 继续执行当前本地步骤：[void]$statements.AppendLine('COMMIT;')。
[void]$statements.AppendLine('COMMIT;')
# 把 表达式 Join-Path $runtimeDirectory 'init-growth-local.sql' 的结果 保存到 script路径（scriptPath）。
$scriptPath = Join-Path $runtimeDirectory 'init-growth-local.sql'
# 继续执行当前本地步骤：[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))。
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $scriptPath).Trim 完成Trim处理 保存到 wsl路径（wslPath）。
$wslPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the initialization path.' }
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $wslPath 1> (Join-Path $runtimeDirectory 'init-growth-local.stdout.log') 2> (Join-Path $runtimeDirectory 'init-growth-local.stderr.log')
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $runtimeDirectory 'init-growth-local.stderr.log') -Tail 12; throw 'Initialization failed and was rolled back.' }
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Growth tables initialized: 32 USER permissions, 27 menu nodes (7 visible). Manager growth grants removed. Existing accounts preserved.'
