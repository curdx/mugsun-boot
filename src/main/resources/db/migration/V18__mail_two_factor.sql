-- G39 双因子登录 + 邮件模板
CREATE TABLE sys_mail_template (
	id          BIGINT       PRIMARY KEY,
	code        VARCHAR(64)  NOT NULL,
	name        VARCHAR(128) NOT NULL,
	subject     VARCHAR(255) NOT NULL,
	content     TEXT         NOT NULL,
	status      INT          NOT NULL DEFAULT 1,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_mail_template IS '邮件模板（${key} 占位）';

-- 内置登录双因子验证码邮件模板
INSERT INTO sys_mail_template (id, code, name, subject, content, status, create_time, is_deleted) VALUES
 (910001, 'login_2fa', '登录双因子验证码', '【Mugsun】登录验证码', '您的登录验证码是 ${code}，5 分钟内有效，请勿泄露。', 1, NOW(), 0);

-- 双因子登录策略参数（默认关闭，不影响现有登录）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
 (900007, '双因子登录开关',   'security.login.two-factor',         'false', '开启后登录需二次验证码',        NOW(), 0),
 (900008, '双因子验证渠道',   'security.login.two-factor-channel', 'email', 'email=邮箱 / sms=短信',          NOW(), 0);
