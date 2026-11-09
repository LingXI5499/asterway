# 按明确的本地路径组织enable剩余frontend本地操作，调用已有命令完成启动或校验。
# 前端构建、启动完成后手工启用已实现入口；不创建表、不迁移或清理业务数据。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 表达式 Join-Path $repositoryRoot '.asterway-private' 的结果 保存到 runtimeDirectory。
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql' 保存到 账户管理路径（identityPath）。
$identityPath = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql'
# 把 按表达式计算得到的值：Get-Content -LiteralPath (Join-Path $repositoryRoot 'docs/remaining/sql-check-results.json') -Raw | ConvertFrom-Json 保存到 approved。
$approved = Get-Content -LiteralPath (Join-Path $repositoryRoot 'docs/remaining/sql-check-results.json') -Raw | ConvertFrom-Json
# 把 调用 (Get-FileHash -LiteralPath $identityPath -Algorithm SHA256).Hash.ToLowerInvariant 完成ToLowerInvariant处理 保存到 来源摘要值（sourceHash）。
$sourceHash = (Get-FileHash -LiteralPath $identityPath -Algorithm SHA256).Hash.ToLowerInvariant()
# 检查条件 $approved.status -ne 'PASSED' -or $sourceHash -ne $approved.inputSha256.'backend/app/src/main/resources/sql/identity.sql' 是否成立；成立时执行对应分支。
if ($approved.status -ne 'PASSED' -or $sourceHash -ne $approved.inputSha256.'backend/app/src/main/resources/sql/identity.sql') {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'The identity SQL must match the approved SQL report.'
}
# 检查条件 -not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'frontend/dist/index.html')) 是否成立；成立时执行对应分支。
if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'frontend/dist/index.html'))) {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Build the implemented frontend before enabling its menus.'
}
# 把 表达式 Get-Content -LiteralPath $identityPath -Raw -Encoding utf8 的结果 保存到 来源（source）。
$source = Get-Content -LiteralPath $identityPath -Raw -Encoding utf8
# 继续执行当前本地步骤：function Get-AuthorizationBlock([string]$marker) {。
function Get-AuthorizationBlock([string]$marker) {
    # 把 按表达式计算得到的值：[regex]::Match($source, '(?s)— BEGIN ' + [regex]::Escape($marker) + '(.*?)— END ' + [regex]::Escape($marker)) 保存到 match。
    $match = [regex]::Match($source, '(?s)-- BEGIN ' + [regex]::Escape($marker) + '(.*?)-- END ' + [regex]::Escape($marker))
    # 检查条件 -not $match.Success 是否成立；成立时执行对应分支。
    if (-not $match.Success) { throw "Missing approved authorization block: $marker" }
    # 结束当前函数或脚本，并按这里的值报告结果。
    return $match.Groups[1].Value
}
# 把 表达式 [System.Text.StringBuilder]::new() 的结果 保存到 statements。
$statements = [System.Text.StringBuilder]::new()
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
[void]$statements.AppendLine(@'
BEGIN;
SET LOCAL search_path=public;
SET LOCAL lock_timeout='5s';
DO $development_only$
BEGIN
    IF current_database()<>'asterway_dev' THEN
        RAISE EXCEPTION 'Frontend menus may only be enabled in the local development database';
    END IF;
END $development_only$;
SELECT pg_advisory_xact_lock(hashtext('asterway-local-frontend-menus'));
SET LOCAL asterway.execution_backend_ready='true';
SET LOCAL asterway.execution_frontend_ready='true';
SET LOCAL asterway.remaining_backend_ready='true';
SET LOCAL asterway.remaining_frontend_ready='true';
'@)
# 继续执行当前本地步骤：[void]$statements.AppendLine((Get-AuthorizationBlock 'EXECUTION AUTHORIZATION'))。
[void]$statements.AppendLine((Get-AuthorizationBlock 'EXECUTION AUTHORIZATION'))
# 继续执行当前本地步骤：[void]$statements.AppendLine((Get-AuthorizationBlock 'REMAINING AUTHORIZATION'))。
[void]$statements.AppendLine((Get-AuthorizationBlock 'REMAINING AUTHORIZATION'))
# 继续执行当前本地步骤：[void]$statements.AppendLine(@'。
[void]$statements.AppendLine(@'
DO $implemented_menus$
BEGIN
    IF (SELECT count(*) FROM identity_menu
        WHERE code IN ('growth_executions','growth_habits','growth_habit_categories',
            'growth_notifications','growth_dashboard','growth_statistics')
            AND is_enabled AND is_visible)<>6 THEN
        RAISE EXCEPTION 'All six implemented page menus must be enabled';
    END IF;
    IF EXISTS (SELECT 1 FROM identity_role_menu rm
        JOIN identity_role r ON r.id=rm.role_id JOIN identity_menu m ON m.id=rm.menu_id
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code IN
            ('growth_executions','growth_habits','growth_habit_categories',
             'growth_notifications','growth_dashboard','growth_statistics')) THEN
        RAISE EXCEPTION 'Management roles must not receive personal business menus';
    END IF;
END $implemented_menus$;
SELECT 'FRONTEND_MENUS:' || jsonb_build_object(
    'enabledVisiblePages',(SELECT count(*) FROM identity_menu WHERE
        code IN ('growth_executions','growth_habits','growth_habit_categories',
            'growth_notifications','growth_dashboard','growth_statistics')
        AND is_enabled AND is_visible),
    'enabledPermissions',(SELECT count(*) FROM identity_permission
        WHERE split_part(code,':',1) IN ('execution','habit','notification','dashboard','statistics') AND is_enabled),
    'ordinaryRolePageGrants',(SELECT count(*) FROM identity_role_menu rm
        JOIN identity_role r ON r.id=rm.role_id JOIN identity_menu m ON m.id=rm.menu_id
        WHERE r.code='USER' AND m.code IN ('growth_executions','growth_habits','growth_habit_categories',
            'growth_notifications','growth_dashboard','growth_statistics')),
    'managementPersonalPermissionGrants',(SELECT count(*) FROM identity_role_permission rp
        JOIN identity_role r ON r.id=rp.role_id JOIN identity_permission p ON p.id=rp.permission_id
        WHERE r.code IN ('ADMIN','SUPER_ADMIN')
            AND split_part(p.code,':',1) IN ('goal','stage','task','planning','execution','habit','notification','dashboard','statistics')),
    'goalDeleteEnabled',(SELECT is_enabled FROM identity_permission WHERE code='goal:goal:delete'),
    'businessDdlExecuted',false
)::text;
COMMIT;
'@)
# 把 表达式 Join-Path $runtimeDirectory 'enable-remaining-frontend-local.sql' 的结果 保存到 script路径（scriptPath）。
$scriptPath = Join-Path $runtimeDirectory 'enable-remaining-frontend-local.sql'
# 继续执行当前本地步骤：[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))。
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
# 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $scriptPath).Trim 完成Trim处理 保存到 linux路径（linuxPath）。
$linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the manual SQL path.' }
# 把 表达式 Join-Path $runtimeDirectory 'enable-remaining-frontend.stdout.log' 的结果 保存到 stdout。
$stdout = Join-Path $runtimeDirectory 'enable-remaining-frontend.stdout.log'
# 把 表达式 Join-Path $runtimeDirectory 'enable-remaining-frontend.stderr.log' 的结果 保存到 stderr。
$stderr = Join-Path $runtimeDirectory 'enable-remaining-frontend.stderr.log'
# 调用这里明确指定的已有脚本或程序，并传入后面的参数。
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> $stdout 2> $stderr
# 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
if ($LASTEXITCODE -ne 0) {
    # 读取明确指定的本地文件，作为本次操作的配置或输入。
    Get-Content -LiteralPath $stderr -Tail 16
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Manual menu initialization failed; the transaction was rolled back.'
}
# 把 调用 Get-Content -LiteralPath $stdout | Where-Object { $_.Trim 完成Trim处理 保存到 结果Line（resultLine）。
$resultLine = Get-Content -LiteralPath $stdout | Where-Object { $_.Trim().StartsWith('FRONTEND_MENUS:') } | Select-Object -Last 1
# 检查条件 -not $resultLine 是否成立；成立时执行对应分支。
if (-not $resultLine) { throw 'Menu initialization did not return its summary.' }
# 把 调用 $resultLine.Trim 完成Trim处理 保存到 结果（result）。
$result = $resultLine.Trim().Substring('FRONTEND_MENUS:'.Length) | ConvertFrom-Json
# 把本次检查或运行结果编码为 JSON，便于后续核对。
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeDirectory 'remaining-frontend-menus.json') -Encoding utf8
# 把本次检查或运行结果编码为 JSON，便于后续核对。
$result | ConvertTo-Json
