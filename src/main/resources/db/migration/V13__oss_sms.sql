CREATE TABLE sys_oss (
	id           BIGINT       PRIMARY KEY,
	tenant_id    VARCHAR(12),
	name         VARCHAR(64)  NOT NULL,
	oss_code     VARCHAR(32)  NOT NULL,
	category     VARCHAR(32)  NOT NULL DEFAULT 'local',
	endpoint     VARCHAR(255),
	access_key   VARCHAR(255),
	secret_key   VARCHAR(255),
	bucket_name  VARCHAR(128),
	domain       VARCHAR(255),
	storage_path VARCHAR(255),
	status       INT          NOT NULL DEFAULT 0,
	remark       VARCHAR(255),
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          NOT NULL DEFAULT 0
);

CREATE TABLE sys_sms (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	name        VARCHAR(64)  NOT NULL,
	sms_code    VARCHAR(32)  NOT NULL,
	category    VARCHAR(32)  NOT NULL DEFAULT 'alibaba',
	access_key  VARCHAR(255),
	secret_key  VARCHAR(255),
	signature   VARCHAR(64),
	template_id VARCHAR(64),
	status      INT          NOT NULL DEFAULT 0,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_oss IS '对象存储配置';
COMMENT ON TABLE sys_sms IS '短信平台配置';
COMMENT ON COLUMN sys_oss.oss_code IS '存储平台唯一标识';
COMMENT ON COLUMN sys_oss.category IS '存储类型：local/minio/aliyun 等';
COMMENT ON COLUMN sys_oss.storage_path IS '本地存储绝对路径（local 类型用）';
COMMENT ON COLUMN sys_oss.status IS '状态：1启用 0禁用（同租户仅一个启用）';
COMMENT ON COLUMN sys_sms.sms_code IS '短信配置唯一标识';
COMMENT ON COLUMN sys_sms.category IS '供应商：alibaba/tencent 等';
COMMENT ON COLUMN sys_sms.status IS '状态：1启用 0禁用（同租户仅一个启用）';
