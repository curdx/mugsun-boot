-- G70 租户生命周期：停用开关 + 账号数上限（expire_time 已由 V6 提供）
ALTER TABLE sys_tenant ADD COLUMN status        INT NOT NULL DEFAULT 1;
ALTER TABLE sys_tenant ADD COLUMN account_count INT NOT NULL DEFAULT -1;

COMMENT ON COLUMN sys_tenant.status IS '状态：1正常 0停用（停用后该租户禁止登录与访问）';
COMMENT ON COLUMN sys_tenant.account_count IS '账号数上限（-1 不限制）';
