-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_dict (
	id          BIGINT       PRIMARY KEY,
	parent_id   BIGINT       DEFAULT 0 NOT NULL,
	code        VARCHAR(64)  NOT NULL,
	dict_key    VARCHAR(64)  NOT NULL,
	dict_value  VARCHAR(128) NOT NULL,
	sort        INT          DEFAULT 0 NOT NULL,
	remark      VARCHAR(255),
	is_sealed   INT          DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE TABLE sys_dict_biz (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	parent_id   BIGINT       DEFAULT 0 NOT NULL,
	code        VARCHAR(64)  NOT NULL,
	dict_key    VARCHAR(64)  NOT NULL,
	dict_value  VARCHAR(128) NOT NULL,
	sort        INT          DEFAULT 0 NOT NULL,
	remark      VARCHAR(255),
	is_sealed   INT          DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE TABLE sys_param (
	id          BIGINT       PRIMARY KEY,
	param_name  VARCHAR(128) NOT NULL,
	param_key   VARCHAR(128) NOT NULL,
	param_value VARCHAR(255) NOT NULL,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_dict IS '系统字典';
COMMENT ON TABLE sys_dict_biz IS '业务字典（租户隔离）';
COMMENT ON TABLE sys_param IS '系统参数';
COMMENT ON COLUMN sys_dict.code IS '字典编码（分类标识，如 sex）';
COMMENT ON COLUMN sys_dict.dict_key IS '字典键';
COMMENT ON COLUMN sys_dict.dict_value IS '字典值/名称';
COMMENT ON COLUMN sys_dict.is_sealed IS '是否封存 0 否 1 是';
COMMENT ON COLUMN sys_dict_biz.tenant_id IS '租户编号（多租户隔离）';
