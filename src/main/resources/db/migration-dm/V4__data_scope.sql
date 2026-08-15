-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

ALTER TABLE sys_role ADD data_scope INT DEFAULT 1 NOT NULL;

COMMENT ON COLUMN sys_role.data_scope IS '数据范围：1 全部 / 2 本部门 / 3 仅本人';
