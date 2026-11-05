# 前端构建、启动完成后手工启用已实现入口；不创建表、不迁移或清理业务数据。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$runtimeDirectory = Join-Path $repositoryRoot '.asterway-private'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$identityPath = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/identity.sql'
$approved = Get-Content -LiteralPath (Join-Path $repositoryRoot 'docs/remaining/sql-check-results.json') -Raw | ConvertFrom-Json
$sourceHash = (Get-FileHash -LiteralPath $identityPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($approved.status -ne 'PASSED' -or $sourceHash -ne $approved.inputSha256.'backend/app/src/main/resources/sql/identity.sql') {
    throw 'The identity SQL must match the approved SQL report.'
}
if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'frontend/dist/index.html'))) {
    throw 'Build the implemented frontend before enabling its menus.'
}
$source = Get-Content -LiteralPath $identityPath -Raw -Encoding utf8
function Get-AuthorizationBlock([string]$marker) {
    $match = [regex]::Match($source, '(?s)-- BEGIN ' + [regex]::Escape($marker) + '(.*?)-- END ' + [regex]::Escape($marker))
    if (-not $match.Success) { throw "Missing approved authorization block: $marker" }
    return $match.Groups[1].Value
}
$statements = [System.Text.StringBuilder]::new()
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
[void]$statements.AppendLine((Get-AuthorizationBlock 'EXECUTION AUTHORIZATION'))
[void]$statements.AppendLine((Get-AuthorizationBlock 'REMAINING AUTHORIZATION'))
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
$scriptPath = Join-Path $runtimeDirectory 'enable-remaining-frontend-local.sql'
[System.IO.File]::WriteAllText($scriptPath, $statements.ToString(), [System.Text.UTF8Encoding]::new($false))
$linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $scriptPath).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the manual SQL path.' }
$stdout = Join-Path $runtimeDirectory 'enable-remaining-frontend.stdout.log'
$stderr = Join-Path $runtimeDirectory 'enable-remaining-frontend.stderr.log'
& wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> $stdout 2> $stderr
if ($LASTEXITCODE -ne 0) {
    Get-Content -LiteralPath $stderr -Tail 16
    throw 'Manual menu initialization failed; the transaction was rolled back.'
}
$resultLine = Get-Content -LiteralPath $stdout | Where-Object { $_.Trim().StartsWith('FRONTEND_MENUS:') } | Select-Object -Last 1
if (-not $resultLine) { throw 'Menu initialization did not return its summary.' }
$result = $resultLine.Trim().Substring('FRONTEND_MENUS:'.Length) | ConvertFrom-Json
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeDirectory 'remaining-frontend-menus.json') -Encoding utf8
$result | ConvertTo-Json
