-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_tenant (
	id            BIGINT      PRIMARY KEY,
	tenant_code   VARCHAR(12) NOT NULL,
	tenant_name   VARCHAR(64) NOT NULL,
	contact_user  VARCHAR(32),
	contact_phone VARCHAR(20),
	expire_time   TIMESTAMP,
	create_time   TIMESTAMP,
	update_time   TIMESTAMP,
	is_deleted    INT         DEFAULT 0 NOT NULL
);

ALTER TABLE sys_user ADD tenant_id VARCHAR(12);
ALTER TABLE sys_user DROP CONSTRAINT  sys_user_username_key;
CREATE UNIQUE INDEX uk_user_tenant_username ON sys_user (tenant_id, username);
ALTER TABLE sys_dept ADD tenant_id VARCHAR(12);
ALTER TABLE sys_post ADD tenant_id VARCHAR(12);
ALTER TABLE sys_role ADD tenant_id VARCHAR(12);

COMMENT ON TABLE sys_tenant IS '租户';
COMMENT ON COLUMN sys_tenant.tenant_code IS '租户编号（业务表 tenant_id 隔离列取值来源）';
COMMENT ON COLUMN sys_user.tenant_id IS '租户编号（字段隔离）';
