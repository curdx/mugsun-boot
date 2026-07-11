-- G67 登录客户端差异化策略 + 登录日志增强
-- 登录客户端：平台级配置（无 tenant_id，规避 Flex 租户隔离，登录前无租户上下文即可加载）
CREATE TABLE sys_client (
	id              BIGINT PRIMARY KEY,
	client_id       VARCHAR(64) NOT NULL,
	client_name     VARCHAR(64) NOT NULL,
	captcha_enabled INT NOT NULL DEFAULT 1,       -- 图形验证码开关（1 开 / 0 关）
	max_online      INT NOT NULL DEFAULT 0,        -- 单账号最大在线终端数（0 = 不限）
	token_timeout   INT NOT NULL DEFAULT 2592000,  -- 令牌有效期（秒）
	status          INT NOT NULL DEFAULT 1,
	remark          VARCHAR(255),
	create_time     TIMESTAMP,
	update_time     TIMESTAMP,
	is_deleted      INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_client_id ON sys_client (client_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_client IS '登录客户端差异化策略（验证码开关/并发在线数/令牌有效期，一 client 一套）';

-- 内置客户端：web 管理后台（验证码开、不限在线、30 天）
INSERT INTO sys_client (id, client_id, client_name, captcha_enabled, max_online, token_timeout, status, create_time, is_deleted)
VALUES (6700000000000001, 'web', '管理后台', 1, 0, 2592000, 1, now(), 0);

-- 登录日志增强：User-Agent / 设备（tenant_id 列已存在，实体直接映射记录）
ALTER TABLE sys_login_log ADD COLUMN user_agent VARCHAR(512);
ALTER TABLE sys_login_log ADD COLUMN device     VARCHAR(32);
