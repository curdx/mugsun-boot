-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G50 首页工作台：每用户快捷入口持久化
CREATE TABLE sys_workbench_shortcut (
	id          BIGINT PRIMARY KEY,
	user_id     BIGINT NOT NULL,
	config_json CLOB,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uk_workbench_shortcut_user ON sys_workbench_shortcut (user_id);
COMMENT ON TABLE sys_workbench_shortcut IS '工作台快捷入口（每用户一行，config_json 存 [{name,path}]）';
