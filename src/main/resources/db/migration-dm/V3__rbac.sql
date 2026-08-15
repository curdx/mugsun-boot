-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_role (
	id          BIGINT      PRIMARY KEY,
	role_name   VARCHAR(64) NOT NULL,
	role_code   VARCHAR(64) NOT NULL,
	sort        INT         DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         DEFAULT 0 NOT NULL
);

CREATE TABLE sys_menu (
	id          BIGINT       PRIMARY KEY,
	parent_id   BIGINT       DEFAULT 0 NOT NULL,
	menu_name   VARCHAR(64)  NOT NULL,
	path        VARCHAR(128),
	component   VARCHAR(128),
	menu_type   VARCHAR(8),
	permission  VARCHAR(128),
	sort        INT          DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE TABLE sys_role_menu (
	id          BIGINT PRIMARY KEY,
	role_id     BIGINT NOT NULL,
	menu_id     BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    DEFAULT 0 NOT NULL
);

CREATE TABLE sys_user_role (
	id          BIGINT PRIMARY KEY,
	user_id     BIGINT NOT NULL,
	role_id     BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_role IS '角色';
COMMENT ON TABLE sys_menu IS '菜单/按钮权限';
COMMENT ON COLUMN sys_menu.menu_type IS 'M 菜单 / B 按钮';
COMMENT ON COLUMN sys_menu.permission IS '权限标识码';
