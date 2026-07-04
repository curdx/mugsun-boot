CREATE TABLE sys_role (
	id          BIGINT      PRIMARY KEY,
	role_name   VARCHAR(64) NOT NULL,
	role_code   VARCHAR(64) NOT NULL,
	sort        INT         NOT NULL DEFAULT 0,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         NOT NULL DEFAULT 0
);

CREATE TABLE sys_menu (
	id          BIGINT       PRIMARY KEY,
	parent_id   BIGINT       NOT NULL DEFAULT 0,
	menu_name   VARCHAR(64)  NOT NULL,
	path        VARCHAR(128),
	component   VARCHAR(128),
	menu_type   VARCHAR(8),
	permission  VARCHAR(128),
	sort        INT          NOT NULL DEFAULT 0,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE TABLE sys_role_menu (
	id          BIGINT PRIMARY KEY,
	role_id     BIGINT NOT NULL,
	menu_id     BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    NOT NULL DEFAULT 0
);

CREATE TABLE sys_user_role (
	id          BIGINT PRIMARY KEY,
	user_id     BIGINT NOT NULL,
	role_id     BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_role IS '角色';
COMMENT ON TABLE sys_menu IS '菜单/按钮权限';
COMMENT ON COLUMN sys_menu.menu_type IS 'M 菜单 / B 按钮';
COMMENT ON COLUMN sys_menu.permission IS '权限标识码';
