-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G67 登录客户端差异化策略 + 登录日志增强
-- 登录客户端：平台级配置（无 tenant_id，规避 Flex 租户隔离，登录前无租户上下文即可加载）
CREATE TABLE sys_client (
	id              BIGINT PRIMARY KEY,
	client_id       VARCHAR(64) NOT NULL,
	client_name     VARCHAR(64) NOT NULL,
	captcha_enabled INT DEFAULT 1 NOT NULL,       -- 图形验证码开关（1 开 / 0 关）
	max_online      INT DEFAULT 0 NOT NULL,        -- 单账号最大在线终端数（0 = 不限）
	token_timeout   INT DEFAULT 2592000 NOT NULL,  -- 令牌有效期（秒）
	status          INT DEFAULT 1 NOT NULL,
	remark          VARCHAR(255),
	create_time     TIMESTAMP,
	update_time     TIMESTAMP,
	is_deleted      INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uk_sys_client_id ON sys_client (client_id);
COMMENT ON TABLE sys_client IS '登录客户端差异化策略（验证码开关/并发在线数/令牌有效期，一 client 一套）';

-- 内置客户端：web 管理后台（验证码开、不限在线、30 天）
INSERT INTO sys_client (id, client_id, client_name, captcha_enabled, max_online, token_timeout, status, create_time, is_deleted)
VALUES (6700000000000001, 'web', '管理后台', 1, 0, 2592000, 1, SYSDATE, 0);

-- 登录日志增强：User-Agent / 设备（tenant_id 列已存在，实体直接映射记录）
ALTER TABLE sys_login_log ADD user_agent VARCHAR(512);
ALTER TABLE sys_login_log ADD device     VARCHAR(32);
