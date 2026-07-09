-- G50 首页工作台：每用户快捷入口持久化
CREATE TABLE sys_workbench_shortcut (
	id          BIGINT PRIMARY KEY,
	user_id     BIGINT NOT NULL,
	config_json TEXT,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_workbench_shortcut_user ON sys_workbench_shortcut (user_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_workbench_shortcut IS '工作台快捷入口（每用户一行，config_json 存 [{name,path}]）';
