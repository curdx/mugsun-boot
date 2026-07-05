CREATE TABLE sys_login_log (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	username    VARCHAR(64)  NOT NULL,
	ip          VARCHAR(64),
	status      INT          NOT NULL DEFAULT 1,
	msg         VARCHAR(255),
	login_time  TIMESTAMP,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE TABLE sys_api_key (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	name        VARCHAR(64)  NOT NULL,
	access_key  VARCHAR(64)  NOT NULL,
	secret_key  VARCHAR(128) NOT NULL,
	scope       VARCHAR(255),
	status      INT          NOT NULL DEFAULT 1,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_login_log IS '登录日志';
COMMENT ON TABLE sys_api_key IS 'API 密钥';
COMMENT ON COLUMN sys_login_log.status IS '状态：1成功 0失败';
COMMENT ON COLUMN sys_api_key.access_key IS '访问标识 AK';
COMMENT ON COLUMN sys_api_key.secret_key IS '密钥 SK';
COMMENT ON COLUMN sys_api_key.scope IS '作用域（逗号分隔的授权范围）';
COMMENT ON COLUMN sys_api_key.status IS '状态：1启用 0停用';
