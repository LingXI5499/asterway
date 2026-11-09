# 按明确的本地路径组织初始化成长助手对话本地操作，调用已有命令完成启动或校验。
# 声明脚本可以接收的开关或参数，后面按参数决定执行范围。
param([switch]$ValidateOnly)

# P1.2 会话三表与六项权限初始化。默认只操作本机 asterway_dev：
# 1) 三表不存在时按已验收 SQL 建表；2) 创建/轮换受限数据库角色并授权；3) 启用六项会话权限（按钮仍隐藏）。
# -ValidateOnly 在随机 schema 中完成准备态、启用态、幂等性和表结构校验后整体回滚。
# 把 固定文字 'Stop' 保存到 错误操作Preference（ErrorActionPreference）。
$ErrorActionPreference = 'Stop'
# 把 表达式 Split-Path $PSScriptRoot -Parent 的结果 保存到 数据仓库根（repositoryRoot）。
$repositoryRoot = Split-Path $PSScriptRoot -Parent
# 把 按表达式计算得到的值：Join-Path $repositoryRoot '.asterway-private/agent-db' 保存到 privateDirectory。
$privateDirectory = Join-Path $repositoryRoot '.asterway-private/agent-db'
# 创建本次运行需要的目录或文件。
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/agent.sql' 保存到 sql文件（sqlFile）。
$sqlFile = Join-Path $repositoryRoot 'backend/app/src/main/resources/sql/agent.sql'
# 把 表达式 Get-Content -LiteralPath $sqlFile -Raw -Encoding utf8 的结果 保存到 来源（source）。
$source = Get-Content -LiteralPath $sqlFile -Raw -Encoding utf8

# 继续执行当前本地步骤：function Get-Block([string]$name) {。
function Get-Block([string]$name) {
    # 把 按表达式计算得到的值：[regex]::Match($source, "(?s)— BEGIN $name(.*?)— END $name") 保存到 match。
    $match = [regex]::Match($source, "(?s)-- BEGIN $name(.*?)-- END $name")
    # 检查条件 -not $match.Success 是否成立；成立时执行对应分支。
    if (-not $match.Success) { throw "Missing $name block in agent.sql." }
    # 结束当前函数或脚本，并按这里的值报告结果。
    return $match.Groups[1].Value
}

# 继续执行当前本地步骤：function Map-Schema([string]$sql, [string]$schema, [switch]$KeepCreate) {。
function Map-Schema([string]$sql, [string]$schema, [switch]$KeepCreate) {
    # 把 sql（$sql） 保存到 mapped。
    $mapped = $sql
    # 检查条件 -not $KeepCreate 是否成立；成立时执行对应分支。
    if (-not $KeepCreate) {
        # 把 表达式 $mapped -replace 'CREATE SCHEMA agent;\r?\n', '' 的结果 保存到 mapped。
        $mapped = $mapped -replace 'CREATE SCHEMA agent;\r?\n', ''
    }
    # 把 条件判断的结果：[regex]::Replace($mapped, '(?<![A-Za-z0-9_])agent\.', "${schema}.") 保存到 mapped。
    $mapped = [regex]::Replace($mapped, '(?<![A-Za-z0-9_])agent\.', "${schema}.")
    # 把 表达式 $mapped -replace 'SCHEMA agent\b', "SCHEMA $schema" 的结果 保存到 mapped。
    $mapped = $mapped -replace 'SCHEMA agent\b', "SCHEMA $schema"
    # 结束当前函数或脚本，并按这里的值报告结果。
    return $mapped
}

# 继续执行当前本地步骤：function Invoke-Wsl([string[]]$wslArguments) {。
function Invoke-Wsl([string[]]$wslArguments) {
    # wsl.exe 会把代理提示写到 stderr；在 Stop 模式下会被当作终止错误。
    # 把 错误操作Preference（$ErrorActionPreference） 保存到 上一次（previous）。
    $previous = $ErrorActionPreference
    # 把 固定文字 'Continue' 保存到 错误操作Preference（ErrorActionPreference）。
    $ErrorActionPreference = 'Continue'
    # 尝试执行这组命令，失败交给 catch，资源清理由 finally 执行。
    try {
        # 调用这里明确指定的已有脚本或程序，并传入后面的参数。
        & wsl.exe @wslArguments
    # 继续执行当前本地步骤：} finally {。
    } finally {
        # 把 上一次（$previous） 保存到 错误操作Preference（ErrorActionPreference）。
        $ErrorActionPreference = $previous
    }
}

# 继续执行当前本地步骤：function Invoke-LocalSql([string]$name, [string]$sql) {。
function Invoke-LocalSql([string]$name, [string]$sql) {
    # 把 表达式 Join-Path $privateDirectory "$name.sql" 的结果 保存到 路径（path）。
    $path = Join-Path $privateDirectory "$name.sql"
    # 继续执行当前本地步骤：[System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))。
    [System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))
    # 把 调用 (Invoke-Wsl @('-d','Ubuntu','—exec','wslpath','-a',$path) 2>$null).Trim 完成Trim处理 保存到 linux路径（linuxPath）。
    $linuxPath = (Invoke-Wsl @('-d','Ubuntu','--exec','wslpath','-a',$path) 2>$null).Trim()
    # 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve local SQL path.' }
    # 继续执行当前本地步骤：Invoke-Wsl @('-d','Ubuntu','-u','root','—exec','/usr/sbin/runuser','-u','postgres','—','/usr/bin/env','PGCLIENTENCODING=UTF8','/usr/bin/psql','-X','…。
    Invoke-Wsl @('-d','Ubuntu','-u','root','--exec','/usr/sbin/runuser','-u','postgres','--','/usr/bin/env','PGCLIENTENCODING=UTF8','/usr/bin/psql','-X','-v','ON_ERROR_STOP=1','-d','asterway_dev','-f',$linuxPath) 1> (Join-Path $privateDirectory "$name.stdout.log") 2> (Join-Path $privateDirectory "$name.stderr.log")
    # 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -ne 0) {
        # 读取明确指定的本地文件，作为本次操作的配置或输入。
        Get-Content -LiteralPath (Join-Path $privateDirectory "$name.stderr.log") -Tail 10
        # 停止脚本并报告这里给出的错误，后续操作不再继续。
        throw "SQL failed: $name; the transaction was rolled back."
    }
}

# 继续执行当前本地步骤：function Invoke-LocalQuery([string]$name, [string]$sql) {。
function Invoke-LocalQuery([string]$name, [string]$sql) {
    # 把 表达式 Join-Path $privateDirectory "$name.sql" 的结果 保存到 路径（path）。
    $path = Join-Path $privateDirectory "$name.sql"
    # 继续执行当前本地步骤：[System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))。
    [System.IO.File]::WriteAllText($path, $sql, [System.Text.UTF8Encoding]::new($false))
    # 把 调用 (Invoke-Wsl @('-d','Ubuntu','—exec','wslpath','-a',$path) 2>$null).Trim 完成Trim处理 保存到 linux路径（linuxPath）。
    $linuxPath = (Invoke-Wsl @('-d','Ubuntu','--exec','wslpath','-a',$path) 2>$null).Trim()
    # 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve local SQL path.' }
    # 把 按表达式计算得到的值：Invoke-Wsl @('-d','Ubuntu','-u','root','—exec','/usr/sbin/runuser','-u','postgres','—','/usr/bin/env','PGCLIENTENCODIN… 保存到 结果（result）。
    $result = Invoke-Wsl @('-d','Ubuntu','-u','root','--exec','/usr/sbin/runuser','-u','postgres','--','/usr/bin/env','PGCLIENTENCODING=UTF8','/usr/bin/psql','-X','-tA','-v','ON_ERROR_STOP=1','-d','asterway_dev','-f',$linuxPath) 2>$null
    # 检查条件 $LASTEXITCODE -ne 0 是否成立；成立时执行对应分支。
    if ($LASTEXITCODE -ne 0) { throw "Query failed: $name" }
    # 结束当前函数或脚本，并按这里的值报告结果。
    return ($result | Out-String).Trim()
}

# 把 表达式 Get-Block 'AGENT CONVERSATION SCHEMA' 的结果 保存到 数据库分区Block（schemaBlock）。
$schemaBlock = Get-Block 'AGENT CONVERSATION SCHEMA'
# 把 表达式 Get-Block 'AGENT CONVERSATION AUTHORIZATION' 的结果 保存到 认证Block（authBlock）。
$authBlock = Get-Block 'AGENT CONVERSATION AUTHORIZATION'

# 把 调用 'agent_conv_check_' + [guid]::NewGuid().ToString 完成To文字处理 保存到 数据库分区（schema）。
$schema = 'agent_conv_check_' + [guid]::NewGuid().ToString('N')
# 把 表达式 @' 的结果 保存到 fixture。
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

# 把 表达式 Map-Schema $schemaBlock $schema 的结果 保存到 mapped数据库分区（mappedSchema）。
$mappedSchema = Map-Schema $schemaBlock $schema
# 把 表达式 Map-Schema $authBlock $schema 的结果 保存到 mapped认证（mappedAuth）。
$mappedAuth = Map-Schema $authBlock $schema
# 把 调用 $fixture.Replace 完成整体替换处理 保存到 isolated。
$isolated = $fixture.Replace('__SCHEMA__', $schema)
# 把 调用 $isolated.Replace 完成整体替换处理 保存到 isolated。
$isolated = $isolated.Replace('__SCHEMA_BLOCK__', $mappedSchema)
# 把 调用 $isolated.Replace 完成整体替换处理 保存到 isolated。
$isolated = $isolated.Replace('__AUTH_BLOCK__', $mappedAuth)
# 继续执行当前本地步骤：Invoke-LocalSql 'conversation-isolation' $isolated。
Invoke-LocalSql 'conversation-isolation' $isolated
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Agent conversation isolation: PASSED (rolled back).'
# 检查条件 $ValidateOnly 是否成立；成立时执行对应分支。
if ($ValidateOnly) { return }

# 开发库只允许本机 asterway_dev；不重建已有表，不修改业务事实。
# 把 表达式 @' 的结果 保存到 当前状态Sql（stateSql）。
$stateSql = @'
SELECT coalesce(to_regclass('agent.conversation')::text,'')
    || '|' || coalesce(to_regclass('agent.conversation_turn')::text,'')
    || '|' || coalesce(to_regclass('agent.conversation_message')::text,'');
'@
# 把 表达式 Invoke-LocalQuery 'conversation-state' $stateSql 的结果 保存到 当前状态（state）。
$state = Invoke-LocalQuery 'conversation-state' $stateSql
# 把 表达式 ($state -split '\|' | Where-Object { $_ -ne '' }).Count 的结果 保存到 present。
$present = ($state -split '\|' | Where-Object { $_ -ne '' }).Count
# 检查条件 $present -eq 0 是否成立；成立时执行对应分支。
if ($present -eq 0) {
    # 把 固定文字 'BEGIN;`nSET LOCAL lock_timeout=\'5s\';`n" + $schemaBlock + "`nCOMMIT;`n' 保存到 apply数据库分区（applySchema）。
    $applySchema = "BEGIN;`nSET LOCAL lock_timeout='5s';`n" + $schemaBlock + "`nCOMMIT;`n"
    # 继续执行当前本地步骤：Invoke-LocalSql 'conversation-schema-local' $applySchema。
    Invoke-LocalSql 'conversation-schema-local' $applySchema
    # 把操作结果输出给调用方，方便确认当前执行到了哪一步。
    Write-Output 'Agent conversation tables created locally.'
# 继续执行当前本地步骤：} elseif ($present -eq 3) {。
} elseif ($present -eq 3) {
    # 把操作结果输出给调用方，方便确认当前执行到了哪一步。
    Write-Output 'Agent conversation tables already exist; skipped DDL.'
# 继续执行当前本地步骤：} else {。
} else {
    # 停止脚本并报告这里给出的错误，后续操作不再继续。
    throw 'Agent conversation tables are partially present; review the schema manually before continuing.'
}

# 把 表达式 @' 的结果 保存到 本地认证（localAuth）。
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
# 继续执行当前本地步骤：Invoke-LocalSql 'conversation-authorization-local' ($localAuth.Replace('__AUTH_BLOCK__', $authBlock))。
Invoke-LocalSql 'conversation-authorization-local' ($localAuth.Replace('__AUTH_BLOCK__', $authBlock))
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Agent conversation permissions enabled locally (buttons stay hidden until frontend).'

# 受限角色只访问 agent schema；密码只写入未跟踪的私密目录与 .env。
# 把 调用 [guid]::NewGuid().ToString 完成To文字处理 保存到 密码（password）。
$password = [guid]::NewGuid().ToString('N')
# 把 表达式 @" 的结果 保存到 角色Sql（roleSql）。
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
# 继续执行当前本地步骤：Invoke-LocalSql 'conversation-role-local' $roleSql。
Invoke-LocalSql 'conversation-role-local' $roleSql

# 把 表达式 @{ 的结果 保存到 credentials。
$credentials = @{
    # 继续执行当前本地步骤：host = '127.0.0.1'。
    host = '127.0.0.1'
    # 继续执行当前本地步骤：port = 5432。
    port = 5432
    # 继续执行当前本地步骤：dbname = 'asterway_dev'。
    dbname = 'asterway_dev'
    # 继续执行当前本地步骤：user = 'asterway_agent'。
    user = 'asterway_agent'
    # 继续执行当前本地步骤：password = $password。
    password = $password
}
# 把本次检查或运行结果编码为 JSON，便于后续核对。
$credentials | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $privateDirectory 'credentials.json') -Encoding utf8
# 把 固定文字 'postgresql+asyncpg://asterway_agent:$password@127.0.0.1:5432/asterway_dev' 保存到 dsn。
$dsn = "postgresql+asyncpg://asterway_agent:$password@127.0.0.1:5432/asterway_dev"

# 把 按表达式计算得到的值：Join-Path $repositoryRoot 'agent/.env' 保存到 env路径（envPath）。
$envPath = Join-Path $repositoryRoot 'agent/.env'
# 检查条件 Test-Path -LiteralPath $envPath 是否成立；成立时执行对应分支。
if (Test-Path -LiteralPath $envPath) {
    # 把 表达式 @(Get-Content -LiteralPath $envPath -Encoding utf8) 的结果 保存到 lines。
    $lines = @(Get-Content -LiteralPath $envPath -Encoding utf8)
# 继续执行当前本地步骤：} else {。
} else {
    # 把 按表达式计算得到的值：@(Get-Content -LiteralPath (Join-Path $repositoryRoot 'agent/.env.example') -Encoding utf8) 保存到 lines。
    $lines = @(Get-Content -LiteralPath (Join-Path $repositoryRoot 'agent/.env.example') -Encoding utf8)
}
# 把 不成立（$false） 保存到 found。
$found = $false
# 把 表达式 foreach ($line in $lines) { 的结果 保存到 更新时（updated）。
$updated = foreach ($line in $lines) {
    # 检查条件 $line -match '^AGENT_DATABASE_URL=' 是否成立；成立时执行对应分支。
    if ($line -match '^AGENT_DATABASE_URL=') {
        # 把 成立（$true） 保存到 found。
        $found = $true
        # 继续执行当前本地步骤："AGENT_DATABASE_URL=$dsn"。
        "AGENT_DATABASE_URL=$dsn"
    # 继续执行当前本地步骤：} else {。
    } else {
        # 继续执行当前本地步骤：$line。
        $line
    }
}
# 检查条件 -not $found 是否成立；成立时执行对应分支。
if (-not $found) { $updated = @($updated) + "AGENT_DATABASE_URL=$dsn" }
# 继续执行当前本地步骤：[System.IO.File]::WriteAllLines($envPath, @($updated), [System.Text.UTF8Encoding]::new($false))。
[System.IO.File]::WriteAllLines($envPath, @($updated), [System.Text.UTF8Encoding]::new($false))
# 把操作结果输出给调用方，方便确认当前执行到了哪一步。
Write-Output 'Agent database role initialized; AGENT_DATABASE_URL written to agent/.env (private, git-ignored).'
