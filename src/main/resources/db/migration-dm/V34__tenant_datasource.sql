-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_tenant_datasource (
	id          BIGINT       PRIMARY KEY,
	tenant_code VARCHAR(12)  NOT NULL,
	ds_url      VARCHAR(255) NOT NULL,
	ds_username VARCHAR(64)  NOT NULL,
	ds_password VARCHAR(128) NOT NULL,
	status      INT          DEFAULT 1 NOT NULL,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_tenant_ds_code ON sys_tenant_datasource (tenant_code);

CREATE TABLE biz_customer (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	name        VARCHAR(64)  NOT NULL,
	phone       VARCHAR(32),
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_tenant_datasource IS '租户独立数据源配置（动态多源隔离）';
COMMENT ON COLUMN sys_tenant_datasource.tenant_code IS '租户编号';
COMMENT ON COLUMN sys_tenant_datasource.ds_url IS '独立库 JDBC URL';
COMMENT ON COLUMN sys_tenant_datasource.status IS '状态：1启用 0停用';
COMMENT ON TABLE biz_customer IS '演示业务实体（客户）——按租户独立数据源路由';
