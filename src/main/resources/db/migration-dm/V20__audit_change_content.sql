-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G42 数据变更记录·字段级 diff：审计表增字段级变更内容
ALTER TABLE sys_data_audit ADD change_content CLOB;
COMMENT ON COLUMN sys_data_audit.change_content IS '字段级变更内容 CLOB：[{label,old,new}]';
