-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_attach (
	id           BIGINT       PRIMARY KEY,
	tenant_id    VARCHAR(12),
	name         VARCHAR(255),
	url          VARCHAR(512),
	path         VARCHAR(255),
	filename     VARCHAR(255),
	ext          VARCHAR(32),
	content_type VARCHAR(128),
	size         BIGINT,
	platform     VARCHAR(32),
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          DEFAULT 0 NOT NULL
);

CREATE TABLE sys_sms_code (
	id          BIGINT      PRIMARY KEY,
	phone       VARCHAR(20) NOT NULL,
	code        VARCHAR(8)  NOT NULL,
	expire_time TIMESTAMP   NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_attach IS '附件登记';
COMMENT ON TABLE sys_sms_code IS '短信验证码';
COMMENT ON COLUMN sys_attach.name IS '原始文件名';
COMMENT ON COLUMN sys_attach.filename IS '存储文件名';
