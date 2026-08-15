-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_form (
	id          BIGINT       PRIMARY KEY,
	name        VARCHAR(64)  NOT NULL,
	form_key    VARCHAR(64)  NOT NULL,
	form_schema CLOB,
	form_option CLOB,
	status      INT          DEFAULT 1 NOT NULL,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_form_key ON sys_form (form_key);

CREATE TABLE sys_form_data (
	id          BIGINT       PRIMARY KEY,
	form_key    VARCHAR(64)  NOT NULL,
	form_data   CLOB,
	submitter   BIGINT,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE INDEX idx_form_data_key ON sys_form_data (form_key);

COMMENT ON TABLE sys_form IS '低代码表单定义（form-create schema）';
COMMENT ON COLUMN sys_form.form_schema IS '表单规则 CLOB（form-create rule）';
COMMENT ON COLUMN sys_form.form_option IS '表单配置 CLOB（form-create option）';
COMMENT ON TABLE sys_form_data IS '低代码表单填报数据';
COMMENT ON COLUMN sys_form_data.form_data IS '填报数据 CLOB';
