# 按明确的本地路径组织初始化成长助手本地操作，调用已有命令完成启动或校验。
# 声明脚本可以接收的开关或参数，后面按参数决定执行范围。
param([switch]$ValidateOnly)

# P1.1 仅初始化两个权限和两个菜单节点；不创建持久化 Agent 表，不改旧业务事实。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 按表达式计算得到的值：Join-Path $repositoryRoot '.asterway-private/agent-tool-calling' 保存到 privateDirectory。
$privateDirectory = Join-Path $repositoryRoot '.asterway-private/agent-tool-calling'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
# 把 按表达式计算得到的值：Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/agent.sql') -Raw -Encoding utf8 保存到 来源（source）。
$source = Get-Content -LiteralPath (Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/agent.sql') -Raw -Encoding utf8
# 把 按表达式计算得到的值：[regex]::Match($source, '(?s)— BEGIN AGENT AUTHORIZATION(.*?)— END AGENT AUTHORIZATION') 保存到 match。
$match = [regex]::Match($source, '(?s)-- BEGIN AGENT AUTHORIZATION(.*?)-- END AGENT AUTHORIZATION')
# 检查条件 -not $match.Success 是否成立；成立时执行对应分支。
if (-not $match.Success) { throw 'Missing Agent authorization block.' }
# 把 表达式 $match.Groups[1].Value 的结果 保存到 block。
$block = $match.Groups[1].Value

# 继续执行当前本地步骤：function Invoke-LocalSql([string]$name, [string]$sql) {。
function Invoke-LocalSql([string]$name, [string]$sql) {
    # 把 表达式 Join-Path $privateDirectory "$name.sql" 的结果 保存到 路径（path）。
    $path = Join-Path $privateDirectory "$name.sql"
    # 继续执行当前本地步骤：[System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))。
    [System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))
    # 把 调用 (& wsl.exe -d Ubuntu —exec wslpath -a $path).Trim 完成Trim处理 保存到 linux路径（linuxPath）。
    $linuxPath = (& wsl.exe -d Ubuntu --exec wslpath -a $path).Trim()
    # 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve local SQL path.' }
    # 调用这里明确指定的已有脚本或程序，并传入后面的参数。
    & wsl.exe -d Ubuntu -u root --exec /usr/sbin/runuser -u postgres -- /usr/bin/psql -X -v ON_ERROR_STOP=1 -d asterway_dev -f $linuxPath 1> (Join-Path $privateDirectory "$name.stdout.log") 2> (Join-Path $privateDirectory "$name.stderr.log")
    # 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -ne 0) {
        # 读取明确指定的本地文件，作为本次操作的配置或输入。
        Get-Content -LiteralPath (Join-Path $privateDirectory "$name.stderr.log") -Tail 8
        # 停止脚本并报告这里给出的错误，后续操作不再继续。
        throw 'Agent authorization SQL failed; the transaction was rolled back.'
    }
}

# 把 调用 'agent_check_' + [guid]::NewGuid().ToString 完成To文字处理 保存到 数据库分区（schema）。
$schema = 'agent_check_' + [guid]::NewGuid().ToString('N')
# 把 表达式 @' 的结果 保存到 isolated。
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
# 继续执行当前本地步骤：Invoke-LocalSql 'authorization-isolation' ($isolated.Replace('__SCHEMA__', $schema).Replace('__BLOCK__', $block))。
Invoke-LocalSql 'authorization-isolation' ($isolated.Replace('__SCHEMA__', $schema).Replace('__BLOCK__', $block))
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Agent authorization isolation: PASSED (rolled back).'
# 检查条件 $ValidateOnly 是否成立；成立时执行对应分支。
if ($ValidateOnly) { return }

# 检查条件 (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'backend/agent/target/agent-0.1.0-SNAPSHOT.jar')) -or 是否成立；成立时执行对应分支。
if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'backend/agent/target/agent-0.1.0-SNAPSHOT.jar')) -or
    # 核对指定文件或目录是否实际存在，再决定后续处理。
    -not (Test-Path -LiteralPath (Join-Path $repositoryRoot 'frontend/dist/index.html'))) {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Build the Agent backend and frontend before enabling the page.'
}
# 把 表达式 @' 的结果 保存到 本地（local）。
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
# 继续执行当前本地步骤：Invoke-LocalSql 'authorization-local' ($local.Replace('__BLOCK__', $block))。
Invoke-LocalSql 'authorization-local' ($local.Replace('__BLOCK__', $block))
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Agent permissions and page initialized locally; existing business facts retained.'
