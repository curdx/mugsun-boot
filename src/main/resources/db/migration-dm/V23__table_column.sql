-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- 表格自定义列持久化：每用户 + 每表格一条列配置（顺序/显隐/列宽 CLOB）
CREATE TABLE sys_table_column (
	id          BIGINT       PRIMARY KEY,
	user_id     BIGINT       NOT NULL,
	table_key   VARCHAR(128) NOT NULL,
	config_json CLOB,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uk_table_column_user ON sys_table_column (user_id, table_key);

COMMENT ON TABLE sys_table_column IS '表格自定义列配置（每用户每表一条）';
COMMENT ON COLUMN sys_table_column.table_key IS '表格标识：前端页面唯一 key';
COMMENT ON COLUMN sys_table_column.config_json IS '列配置 CLOB：[{key,visible,width}] 顺序即数组序';
