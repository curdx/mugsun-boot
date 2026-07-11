-- G71 数据源隔离系统化：隔离策略 + schema 模式 + 密码密文列宽
ALTER TABLE sys_tenant_datasource ADD COLUMN isolation_type SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE sys_tenant_datasource ADD COLUMN schema_name    VARCHAR(64);
ALTER TABLE sys_tenant_datasource ALTER COLUMN ds_password TYPE VARCHAR(255);

COMMENT ON COLUMN sys_tenant_datasource.isolation_type IS '隔离策略：1独立库(DATASOURCE) 2独立schema(同库search_path)';
COMMENT ON COLUMN sys_tenant_datasource.schema_name IS 'schema 隔离模式的目标 schema（PostgreSQL search_path）';
COMMENT ON COLUMN sys_tenant_datasource.ds_password IS '独立库密码（SM4 密文存储）';
