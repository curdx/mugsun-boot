-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G38 等保密码与登录安全：历史密码表 + 策略参数（存 sys_param，可后台即时改）
CREATE TABLE sys_password_log (
	id          BIGINT       PRIMARY KEY,
	user_id     BIGINT       NOT NULL,
	password    VARCHAR(100) NOT NULL,
	create_time TIMESTAMP
);
CREATE INDEX idx_pwd_log_user ON sys_password_log (user_id, create_time DESC);
COMMENT ON TABLE sys_password_log IS '历史密码（防重复使用）';

-- 密码/登录安全策略参数（后台参数管理可改，即时生效）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
 (900001, '密码最小长度',       'security.password.min-length',    '8',  '密码最小位数',                   SYSDATE, 0),
 (900002, '密码复杂度校验',     'security.password.complexity',    'true','开启后要求大写/小写/数字/特殊字符至少3类', SYSDATE, 0),
 (900003, '历史密码防重个数',   'security.password.history-count', '3',  '新密码不可与最近N次重复',        SYSDATE, 0),
 (900004, '密码有效期(天)',     'security.password.expire-days',   '0',  '0=永不过期，>0 到期强制改密',    SYSDATE, 0),
 (900005, '登录失败锁定阈值',   'security.login.fail-max',         '5',  '连续失败达此次数锁定',           SYSDATE, 0),
 (900006, '登录锁定时长(分钟)', 'security.login.lock-minutes',     '10', '锁定持续时长',                   SYSDATE, 0);
