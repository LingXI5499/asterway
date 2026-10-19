# 迁移脚本目录

Flyway 迁移脚本集中放在本目录（`app/src/main/resources/db/migration/`）。

- 命名规范：`V<版本>__<描述>.sql`，例如 `V1__baseline.sql`。
- 约束：**所有表不建物理外键、不做数据库级联、不用触发器模拟外键**（RULE-001）。
- 依赖 `btree_gist` 扩展用于 UUID 等值与时间范围排除约束，迁移前按 12 册在目标库核验并 `CREATE EXTENSION`。
- 第一批迁移依据 01 册数据字典手写，属 DEV-001 出口（TEST-001、002、059）。

> 当前为占位说明，正式迁移脚本待按 01 册生成。
