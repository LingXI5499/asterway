param([switch]$ValidateOnly)

# P1.1 仅初始化两个权限和两个菜单节点；不创建持久化 Agent 表，不改旧业务事实。
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$privateDirectory = Join-Path $repositoryRoot '.asterway-private/agent-tool-calling'
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
$source = Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/agent.sql') -Raw -Encoding utf8
$match = [regex]::Match($source, '(?s)-- BEGIN AGENT AUTHORIZATION(.*?)-- END AGENT AUTHORIZATION')
if (-not $match.Success) { throw 'Missing Agent authorization block.' }
$block = $match.Groups[1].Value

function Invoke-LocalSql([string]$name, [string]$sql) {
    $path = Join-Path $privateDirectory "$name.sql"
    [System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))
    $linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $path).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve local SQL path.' }
    & wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> (Join-Path $privateDirectory "$name.stdout.log") 2> (Join-Path $privateDirectory "$name.stderr.log")
    if ($LASTEXITCODE -ne 0) {
        Get-Content -LiteralPath (Join-Path $privateDirectory "$name.stderr.log") -Tail 8
        throw 'Agent authorization SQL failed; the transaction was rolled back.'
    }
}

$schema = 'agent_check_' + [guid]::NewGuid().ToString('N')
$isolated = @'
BEGIN;
SET LOCAL lock_timeout='5s';
CREATE SCHEMA __SCHEMA__;
SET LOCAL search_path=__SCHEMA__,public;
CREATE TABLE identity_role (LIKE public.identity_role INCLUDING ALL);
CREATE TABLE identity_permission (LIKE public.identity_permission INCLUDING ALL);
CREATE TABLE identity_menu (LIKE public.identity_menu INCLUDING ALL);
CREATE TABLE identity_role_permission (LIKE public.identity_role_permission INCLUDING ALL);
CREATE TABLE identity_role_menu (LIKE public.identity_role_menu INCLUDING ALL);
INSERT INTO identity_role(code,name,is_builtin) VALUES
    ('USER','普通用户',true),('ADMIN','管理员',true),('SUPER_ADMIN','超级管理员',true);
INSERT INTO identity_menu(code,name,menu_type,route_path,is_builtin)
VALUES('growth_root','成长规划','DIRECTORY','/growth',true);
__BLOCK__
DO $prepared$
BEGIN
    IF (SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:%' AND NOT is_enabled)<>2
        OR (SELECT count(*) FROM identity_menu WHERE code LIKE 'growth_agent%' AND NOT is_enabled AND NOT is_visible)<>2 THEN
        RAISE EXCEPTION 'Prepared-state authorization must remain disabled and hidden';
    END IF;
END $prepared$;
-- 模拟遗留误授权；再次运行时必须只移除管理身份的 Agent 授权。
INSERT INTO identity_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p WHERE r.code='ADMIN';
INSERT INTO identity_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m WHERE r.code='SUPER_ADMIN' AND m.code LIKE 'growth_agent%';
SET LOCAL asterway.agent_backend_ready='true';
SET LOCAL asterway.agent_frontend_ready='true';
__BLOCK__
DO $enabled$
BEGIN
    IF (SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:%' AND is_enabled)<>2
        OR (SELECT count(*) FROM identity_menu WHERE code='growth_agent' AND is_enabled AND is_visible)<>1
        OR (SELECT count(*) FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            WHERE r.code='USER')<>2
        OR (SELECT count(*) FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            WHERE r.code='USER')<>2
        OR EXISTS (SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id WHERE r.code<>'USER')
        OR EXISTS (SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id WHERE r.code<>'USER') THEN
        RAISE EXCEPTION 'Agent role and page authorization mismatch';
    END IF;
END $enabled$;
CREATE TEMP TABLE before_repeat AS SELECT
    (SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r) AS roles,
    (SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p) AS permissions,
    (SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m) AS menus;
__BLOCK__
DO $idempotent$
BEGIN
    IF EXISTS (SELECT 1 FROM before_repeat b WHERE
        b.roles IS DISTINCT FROM (SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r)
        OR b.permissions IS DISTINCT FROM (SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p)
        OR b.menus IS DISTINCT FROM (SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m)) THEN
        RAISE EXCEPTION 'Repeated authorization must not change versions or timestamps';
    END IF;
END $idempotent$;
ROLLBACK;
SELECT 'AGENT_AUTHORIZATION_ISOLATION_OK';
'@
Invoke-LocalSql 'authorization-isolation' ($isolated.Replace('__SCHEMA__', $schema).Replace('__BLOCK__', $block))
Write-Output 'Agent authorization isolation: PASSED (rolled back).'
if ($ValidateOnly) { return }

if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'backend/agent/target/agent-0.1.0-SNAPSHOT.jar')) -or
    -not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'frontend/dist/index.html'))) {
    throw 'Build the Agent backend and frontend before enabling the page.'
}
$local = @'
BEGIN;
SET LOCAL search_path=public;
SET LOCAL lock_timeout='5s';
DO $development_only$
BEGIN
    IF current_database()<>'asterway_dev' THEN
        RAISE EXCEPTION 'Agent menus may only be enabled in the local development database';
    END IF;
END $development_only$;
SELECT pg_advisory_xact_lock(hashtext('asterway-agent-authorization'));
SET LOCAL asterway.agent_backend_ready='true';
SET LOCAL asterway.agent_frontend_ready='true';
__BLOCK__
SELECT 'AGENT_AUTHORIZATION_LOCAL_OK';
COMMIT;
'@
Invoke-LocalSql 'authorization-local' ($local.Replace('__BLOCK__', $block))
Write-Output 'Agent permissions and page initialized locally; existing business facts retained.'
