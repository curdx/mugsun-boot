-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_login_log (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	username    VARCHAR(64)  NOT NULL,
	ip          VARCHAR(64),
	status      INT          DEFAULT 1 NOT NULL,
	msg         VARCHAR(255),
	login_time  TIMESTAMP,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE TABLE sys_api_key (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	name        VARCHAR(64)  NOT NULL,
	access_key  VARCHAR(64)  NOT NULL,
	secret_key  VARCHAR(128) NOT NULL,
	scope       VARCHAR(255),
	status      INT          DEFAULT 1 NOT NULL,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_login_log IS '登录日志';
COMMENT ON TABLE sys_api_key IS 'API 密钥';
COMMENT ON COLUMN sys_login_log.status IS '状态：1成功 0失败';
COMMENT ON COLUMN sys_api_key.access_key IS '访问标识 AK';
COMMENT ON COLUMN sys_api_key.secret_key IS '密钥 SK';
COMMENT ON COLUMN sys_api_key.scope IS '作用域（逗号分隔的授权范围）';
COMMENT ON COLUMN sys_api_key.status IS '状态：1启用 0停用';
