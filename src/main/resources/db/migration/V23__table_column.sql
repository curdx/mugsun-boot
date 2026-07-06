-- 表格自定义列持久化：每用户 + 每表格一条列配置（顺序/显隐/列宽 JSON）
CREATE TABLE sys_table_column (
	id          BIGINT       PRIMARY KEY,
	user_id     BIGINT       NOT NULL,
	table_key   VARCHAR(128) NOT NULL,
	config_json TEXT,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_table_column_user ON sys_table_column (user_id, table_key) WHERE is_deleted = 0;

COMMENT ON TABLE sys_table_column IS '表格自定义列配置（每用户每表一条）';
COMMENT ON COLUMN sys_table_column.table_key IS '表格标识：前端页面唯一 key';
COMMENT ON COLUMN sys_table_column.config_json IS '列配置 JSON：[{key,visible,width}] 顺序即数组序';
