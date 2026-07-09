CREATE TABLE sys_oauth_client (
	id                    BIGINT       PRIMARY KEY,
	tenant_id             VARCHAR(12),
	name                  VARCHAR(64)  NOT NULL,
	client_id             VARCHAR(64)  NOT NULL,
	client_secret         VARCHAR(128) NOT NULL,
	grant_types           VARCHAR(128) NOT NULL DEFAULT 'client_credentials',
	scopes                VARCHAR(255),
	redirect_uri          VARCHAR(255),
	access_token_validity INT          NOT NULL DEFAULT 7200,
	status                INT          NOT NULL DEFAULT 1,
	remark                VARCHAR(255),
	create_time           TIMESTAMP,
	update_time           TIMESTAMP,
	is_deleted            INT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_oauth_client_id ON sys_oauth_client (client_id) WHERE is_deleted = 0;

CREATE TABLE sys_oauth_log (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	client_id   VARCHAR(64)  NOT NULL,
	api_path    VARCHAR(255),
	scope       VARCHAR(128),
	status      INT          NOT NULL DEFAULT 1,
	ip          VARCHAR(64),
	msg         VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_oauth_log_client ON sys_oauth_log (client_id);

COMMENT ON TABLE sys_oauth_client IS 'OAuth2 客户端（开放平台）';
COMMENT ON COLUMN sys_oauth_client.client_id IS '客户端标识';
COMMENT ON COLUMN sys_oauth_client.client_secret IS '客户端密钥';
COMMENT ON COLUMN sys_oauth_client.grant_types IS '授权类型（逗号分隔：client_credentials,authorization_code）';
COMMENT ON COLUMN sys_oauth_client.scopes IS '可授权的接口范围（逗号分隔）';
COMMENT ON COLUMN sys_oauth_client.redirect_uri IS '授权码回调地址';
COMMENT ON COLUMN sys_oauth_client.access_token_validity IS '令牌有效期（秒）';
COMMENT ON COLUMN sys_oauth_client.status IS '状态：1启用 0停用';
COMMENT ON TABLE sys_oauth_log IS '开放接口调用日志';
COMMENT ON COLUMN sys_oauth_log.status IS '状态：1放行 0拒绝';
