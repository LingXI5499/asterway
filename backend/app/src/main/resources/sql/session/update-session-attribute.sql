-- 参数依次为属性内容、父会话主键、属性名；先锁定父会话，再更新属性。
WITH value AS (
    SELECT ?::bytea AS bytes, ?::char(36) AS parent_id, ?::varchar AS name
), parent AS (
    SELECT s.PRIMARY_ID FROM %TABLE_NAME% s, value v
    WHERE s.PRIMARY_ID = v.parent_id FOR UPDATE OF s
)
UPDATE %TABLE_NAME%_ATTRIBUTES a SET ATTRIBUTE_BYTES = v.bytes
FROM parent p, value v
WHERE a.SESSION_PRIMARY_ID = p.PRIMARY_ID AND a.ATTRIBUTE_NAME = v.name
