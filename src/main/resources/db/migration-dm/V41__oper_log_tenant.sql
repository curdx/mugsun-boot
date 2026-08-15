-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G68 操作日志租户化：@Async 落库经 TenantTaskDecorator 透传租户，验证异步不丢隔离
ALTER TABLE sys_oper_log ADD tenant_id VARCHAR(12);
-- 存量日志回填默认租户，避免租户过滤后管理端视图为空
UPDATE sys_oper_log SET tenant_id = '000000' WHERE tenant_id IS NULL;
