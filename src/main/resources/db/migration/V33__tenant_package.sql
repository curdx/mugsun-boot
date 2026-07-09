CREATE TABLE sys_tenant_package (
	id          BIGINT       PRIMARY KEY,
	name        VARCHAR(64)  NOT NULL,
	menu_keys   TEXT,
	status      INT          NOT NULL DEFAULT 1,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

ALTER TABLE sys_tenant ADD COLUMN package_id BIGINT;

COMMENT ON TABLE sys_tenant_package IS '租户套餐（限定可用功能菜单）';
COMMENT ON COLUMN sys_tenant_package.menu_keys IS '套餐内可用菜单标识（前端路由 name，逗号分隔）';
COMMENT ON COLUMN sys_tenant_package.status IS '状态：1启用 0停用';
COMMENT ON COLUMN sys_tenant.package_id IS '所属套餐（NULL 表示不限功能）';
