-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G70 租户生命周期：停用开关 + 账号数上限（expire_time 已由 V6 提供）
ALTER TABLE sys_tenant ADD status        INT DEFAULT 1 NOT NULL;
ALTER TABLE sys_tenant ADD account_count INT DEFAULT -1 NOT NULL;

COMMENT ON COLUMN sys_tenant.status IS '状态：1正常 0停用（停用后该租户禁止登录与访问）';
COMMENT ON COLUMN sys_tenant.account_count IS '账号数上限（-1 不限制）';
