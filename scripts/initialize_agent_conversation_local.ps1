param([switch]$ValidateOnly)

# P1.2 会话三表与六项权限初始化。默认只操作本机 asterway_dev：
# 1) 三表不存在时按已验收 SQL 建表；2) 创建/轮换受限数据库角色并授权；3) 启用六项会话权限（按钮仍隐藏）。
# -ValidateOnly 在随机 schema 中完成准备态、启用态、幂等性和表结构校验后整体回滚。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$privateDirectory = Join-Path $repositoryRoot '.asterway-private/agent-db'
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
$sqlFile = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/agent.sql'
$source = Get-Content -LiteralPath $sqlFile -Raw -Encoding utf8

function Get-Block([string]$name) {
    $match = [regex]::Match($source, "(?s)-- BEGIN $name(.*?)-- END $name")
    if (-not $match.Success) { throw "Missing $name block in agent.sql." }
    return $match.Groups[1].Value
}

function Map-Schema([string]$sql, [string]$schema, [switch]$KeepCreate) {
    $mapped = $sql
    if (-not $KeepCreate) {
        $mapped = $mapped -replace 'CREATE SCHEMA agent;\r?\n', ''
    }
    $mapped = [regex]::Replace($mapped, '(?<![A-Za-z0-9_])agent\.', "${schema}.")
    $mapped = $mapped -replace 'SCHEMA agent\b', "SCHEMA $schema"
    return $mapped
}

function Invoke-Wsl([string[]]$wslArguments) {
    # wsl.exe 会把代理提示写到 stderr；在 Stop 模式下会被当作终止错误。
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & wsl.exe @wslArguments
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Invoke-LocalSql([string]$name, [string]$sql) {
    $path = Join-Path $privateDirectory "$name.sql"
    [System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))
    $linuxPath = (Invoke-Wsl @('-d','Ubuntu','--exec','wslpath','-a',$path) 2>$null).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve local SQL path.' }
    Invoke-Wsl @('-d','Ubuntu','-u','root','--exec','/usr/sbin/runuser','-u','postgres','--','/usr/bin/env','PGCLIENTENCODING=UTF8','/usr/bin/psql','-X','-v','ON_ERROR_STOP=1','-d','asterway_dev','-f',$linuxPath) 1> (Join-Path $privateDirectory "$name.stdout.log") 2> (Join-Path $privateDirectory "$name.stderr.log")
    if ($LASTEXITCODE -ne 0) {
        Get-Content -LiteralPath (Join-Path $privateDirectory "$name.stderr.log") -Tail 10
        throw "SQL failed: $name; the transaction was rolled back."
    }
}

function Invoke-LocalQuery([string]$name, [string]$sql) {
    $path = Join-Path $privateDirectory "$name.sql"
    [System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))
    $linuxPath = (Invoke-Wsl @('-d','Ubuntu','--exec','wslpath','-a',$path) 2>$null).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve local SQL path.' }
    $result = Invoke-Wsl @('-d','Ubuntu','-u','root','--exec','/usr/sbin/runuser','-u','postgres','--','/usr/bin/env','PGCLIENTENCODING=UTF8','/usr/bin/psql','-X','-tA','-v','ON_ERROR_STOP=1','-d','asterway_dev','-f',$linuxPath) 2>$null
    if ($LASTEXITCODE -ne 0) { throw "Query failed: $name" }
    return ($result | Out-String).Trim()
}

$schemaBlock = Get-Block 'AGENT CONVERSATION SCHEMA'
$authBlock = Get-Block 'AGENT CONVERSATION AUTHORIZATION'

$schema = 'agent_conv_check_' + [guid]::NewGuid().ToString('N')
$fixture = @'
BEGIN;
SET LOCAL lock_timeout='5s';
CREATE SCHEMA __SCHEMA__;
REVOKE ALL ON SCHEMA __SCHEMA__ FROM PUBLIC;
SET LOCAL search_path=__SCHEMA__,public;
CREATE TABLE identity_role (LIKE public.identity_role INCLUDING ALL);
CREATE TABLE identity_permission (LIKE public.identity_permission INCLUDING ALL);
CREATE TABLE identity_menu (LIKE public.identity_menu INCLUDING ALL);
CREATE TABLE identity_role_permission (LIKE public.identity_role_permission INCLUDING ALL);
CREATE TABLE identity_role_menu (LIKE public.identity_role_menu INCLUDING ALL);
INSERT INTO identity_role(code,name,is_builtin) VALUES
    ('USER','普通用户',true),('ADMIN','管理员',true),('SUPER_ADMIN','超级管理员',true);
INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled) VALUES
    ('agent:chat:read','访问成长助手','夹具',true,true),
    ('agent:chat:send','发送成长助手提问','夹具',true,true);
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,
    is_visible,is_builtin,is_enabled)
SELECT 'growth_agent',NULL,'成长助手','PAGE','/agent','agent/index',p.id,true,true,true
FROM identity_permission p WHERE p.code='agent:chat:read';
__SCHEMA_BLOCK__
__AUTH_BLOCK__
DO $prepared$
BEGIN
    IF (SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND NOT is_enabled)<>6
        OR (SELECT count(*) FROM identity_menu WHERE code LIKE 'growth_agent_conversation_%'
            AND NOT is_enabled AND NOT is_visible AND menu_type='BUTTON')<>6
        OR to_regclass('__SCHEMA__.conversation') IS NULL
        OR to_regclass('__SCHEMA__.conversation_turn') IS NULL
        OR to_regclass('__SCHEMA__.conversation_message') IS NULL THEN
        RAISE EXCEPTION 'Conversation prepared state or tables mismatch';
    END IF;
END $prepared$;
INSERT INTO identity_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
WHERE r.code='ADMIN' AND p.code LIKE 'agent:conversation:%';
INSERT INTO identity_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
WHERE r.code='SUPER_ADMIN' AND m.code LIKE 'growth_agent_conversation_%';
SET LOCAL asterway.agent_conversation_backend_ready='true';
__AUTH_BLOCK__
DO $enabled$
BEGIN
    IF (SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled)<>6
        OR (SELECT count(*) FROM identity_menu WHERE code LIKE 'growth_agent_conversation_%'
            AND is_enabled AND is_visible)<>0
        OR (SELECT count(*) FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            JOIN identity_permission p ON p.id=rp.permission_id
            WHERE r.code='USER' AND p.code LIKE 'agent:conversation:%')<>6
        OR (SELECT count(*) FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            JOIN identity_menu m ON m.id=rm.menu_id
            WHERE r.code='USER' AND m.code LIKE 'growth_agent_conversation_%')<>6
        OR EXISTS (SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            JOIN identity_permission p ON p.id=rp.permission_id
            WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'agent:conversation:%')
        OR EXISTS (SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            JOIN identity_menu m ON m.id=rm.menu_id
            WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code LIKE 'growth_agent_conversation_%') THEN
        RAISE EXCEPTION 'Conversation authorization mismatch after backend ready';
    END IF;
END $enabled$;
CREATE TEMP TABLE before_repeat AS SELECT
    (SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r) AS roles,
    (SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p) AS permissions,
    (SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m) AS menus;
__AUTH_BLOCK__
DO $idempotent$
BEGIN
    IF EXISTS (SELECT 1 FROM before_repeat b WHERE
        b.roles IS DISTINCT FROM (SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r)
        OR b.permissions IS DISTINCT FROM (SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p)
        OR b.menus IS DISTINCT FROM (SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m)) THEN
        RAISE EXCEPTION 'Repeated conversation authorization must not change versions or timestamps';
    END IF;
END $idempotent$;
ROLLBACK;
SELECT 'AGENT_CONVERSATION_ISOLATION_OK';
'@

$mappedSchema = Map-Schema $schemaBlock $schema
$mappedAuth = Map-Schema $authBlock $schema
$isolated = $fixture.Replace('__SCHEMA__', $schema)
$isolated = $isolated.Replace('__SCHEMA_BLOCK__', $mappedSchema)
$isolated = $isolated.Replace('__AUTH_BLOCK__', $mappedAuth)
Invoke-LocalSql 'conversation-isolation' $isolated
Write-Output 'Agent conversation isolation: PASSED (rolled back).'
if ($ValidateOnly) { return }

# 开发库只允许本机 asterway_dev；不重建已有表，不修改业务事实。
$stateSql = @'
SELECT coalesce(to_regclass('agent.conversation')::text,'')
    || '|' || coalesce(to_regclass('agent.conversation_turn')::text,'')
    || '|' || coalesce(to_regclass('agent.conversation_message')::text,'');
'@
$state = Invoke-LocalQuery 'conversation-state' $stateSql
$present = ($state -split '\|' | Where-Object { $_ -ne '' }).Count
if ($present -eq 0) {
    $applySchema = "BEGIN;`nSET LOCAL lock_timeout='5s';`n" + $schemaBlock + "`nCOMMIT;`n"
    Invoke-LocalSql 'conversation-schema-local' $applySchema
    Write-Output 'Agent conversation tables created locally.'
} elseif ($present -eq 3) {
    Write-Output 'Agent conversation tables already exist; skipped DDL.'
} else {
    throw 'Agent conversation tables are partially present; review the schema manually before continuing.'
}

$localAuth = @'
BEGIN;
SET LOCAL search_path=public;
SET LOCAL lock_timeout='5s';
DO $development_only$
BEGIN
    IF current_database()<>'asterway_dev' THEN
        RAISE EXCEPTION 'Conversation permissions may only be enabled in the local development database';
    END IF;
END $development_only$;
SELECT pg_advisory_xact_lock(hashtext('asterway-agent-authorization'));
SET LOCAL asterway.agent_conversation_backend_ready='true';
__AUTH_BLOCK__
SELECT 'AGENT_CONVERSATION_AUTHORIZATION_LOCAL_OK';
COMMIT;
'@
Invoke-LocalSql 'conversation-authorization-local' ($localAuth.Replace('__AUTH_BLOCK__', $authBlock))
Write-Output 'Agent conversation permissions enabled locally (buttons stay hidden until frontend).'

# 受限角色只访问 agent schema；密码只写入未跟踪的私密目录与 .env。
$password = [guid]::NewGuid().ToString('N')
$roleSql = @"
BEGIN;
SET LOCAL lock_timeout='5s';
DO `$role`$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='asterway_agent') THEN
        CREATE ROLE asterway_agent LOGIN;
    END IF;
END `$role`$;
ALTER ROLE asterway_agent WITH LOGIN PASSWORD '$password';
GRANT CONNECT ON DATABASE asterway_dev TO asterway_agent;
GRANT USAGE ON SCHEMA agent TO asterway_agent;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA agent TO asterway_agent;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA agent TO asterway_agent;
ALTER ROLE asterway_agent SET search_path = agent, public;
COMMIT;
"@
Invoke-LocalSql 'conversation-role-local' $roleSql

$credentials = @{
    host = '127.0.0.1'
    port = 5432
    dbname = 'asterway_dev'
    user = 'asterway_agent'
    password = $password
}
$credentials | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $privateDirectory 'credentials.json') -Encoding utf8
$dsn = "postgresql+asyncpg://asterway_agent:$password@127.0.0.1:5432/asterway_dev"

$envPath = Join-Path $repositoryRoot 'agent/.env'
if (Test-Path -LiteralPath $envPath) {
    $lines = @(Get-Content -LiteralPath $envPath -Encoding utf8)
} else {
    $lines = @(Get-Content -LiteralPath (Join-Path $repositoryRoot 'agent/.env.example') -Encoding utf8)
}
$found = $false
$updated = foreach ($line in $lines) {
    if ($line -match '^AGENT_DATABASE_URL=') {
        $found = $true
        "AGENT_DATABASE_URL=$dsn"
    } else {
        $line
    }
}
if (-not $found) { $updated = @($updated) + "AGENT_DATABASE_URL=$dsn" }
[System.IO.File]::WriteAllLines($envPath, @($updated), [System.Text.UTF8Encoding]::new($false))
Write-Output 'Agent database role initialized; AGENT_DATABASE_URL written to agent/.env (private, git-ignored).'
