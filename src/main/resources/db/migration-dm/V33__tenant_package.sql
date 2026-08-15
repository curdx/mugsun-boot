-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_tenant_package (
	id          BIGINT       PRIMARY KEY,
	name        VARCHAR(64)  NOT NULL,
	menu_keys   CLOB,
	status      INT          DEFAULT 1 NOT NULL,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

ALTER TABLE sys_tenant ADD package_id BIGINT;

COMMENT ON TABLE sys_tenant_package IS '租户套餐（限定可用功能菜单）';
COMMENT ON COLUMN sys_tenant_package.menu_keys IS '套餐内可用菜单标识（前端路由 name，逗号分隔）';
COMMENT ON COLUMN sys_tenant_package.status IS '状态：1启用 0停用';
COMMENT ON COLUMN sys_tenant.package_id IS '所属套餐（NULL 表示不限功能）';
