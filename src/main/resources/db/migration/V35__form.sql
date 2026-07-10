CREATE TABLE sys_form (
	id          BIGINT       PRIMARY KEY,
	name        VARCHAR(64)  NOT NULL,
	form_key    VARCHAR(64)  NOT NULL,
	form_schema TEXT,
	form_option TEXT,
	status      INT          NOT NULL DEFAULT 1,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_form_key ON sys_form (form_key) WHERE is_deleted = 0;

CREATE TABLE sys_form_data (
	id          BIGINT       PRIMARY KEY,
	form_key    VARCHAR(64)  NOT NULL,
	form_data   TEXT,
	submitter   BIGINT,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_form_data_key ON sys_form_data (form_key);

COMMENT ON TABLE sys_form IS '低代码表单定义（form-create schema）';
COMMENT ON COLUMN sys_form.form_schema IS '表单规则 JSON（form-create rule）';
COMMENT ON COLUMN sys_form.form_option IS '表单配置 JSON（form-create option）';
COMMENT ON TABLE sys_form_data IS '低代码表单填报数据';
COMMENT ON COLUMN sys_form_data.form_data IS '填报数据 JSON';
