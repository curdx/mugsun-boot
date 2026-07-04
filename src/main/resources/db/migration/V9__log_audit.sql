CREATE TABLE sys_oper_log (
	id             BIGINT       PRIMARY KEY,
	title          VARCHAR(128),
	method         VARCHAR(255),
	request_method VARCHAR(16),
	request_uri    VARCHAR(255),
	operator       VARCHAR(64),
	ip             VARCHAR(64),
	params         TEXT,
	duration       BIGINT,
	status         INT,
	error_msg      TEXT,
	create_time    TIMESTAMP,
	update_time    TIMESTAMP,
	is_deleted     INT          NOT NULL DEFAULT 0
);

CREATE TABLE sys_data_audit (
	id          BIGINT      PRIMARY KEY,
	biz_table   VARCHAR(64),
	biz_id      VARCHAR(64),
	before_data TEXT,
	after_data  TEXT,
	operator    VARCHAR(64),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_oper_log IS '操作日志';
COMMENT ON TABLE sys_data_audit IS '数据变更审计（前后镜像）';
COMMENT ON COLUMN sys_oper_log.duration IS '耗时（毫秒）';
COMMENT ON COLUMN sys_oper_log.status IS '状态 1 成功 0 失败';
