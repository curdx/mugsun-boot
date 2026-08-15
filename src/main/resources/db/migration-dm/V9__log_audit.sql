-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_oper_log (
	id             BIGINT       PRIMARY KEY,
	title          VARCHAR(128),
	method         VARCHAR(255),
	request_method VARCHAR(16),
	request_uri    VARCHAR(255),
	operator       VARCHAR(64),
	ip             VARCHAR(64),
	params         CLOB,
	duration       BIGINT,
	status         INT,
	error_msg      CLOB,
	create_time    TIMESTAMP,
	update_time    TIMESTAMP,
	is_deleted     INT          DEFAULT 0 NOT NULL
);

CREATE TABLE sys_data_audit (
	id          BIGINT      PRIMARY KEY,
	biz_table   VARCHAR(64),
	biz_id      VARCHAR(64),
	before_data CLOB,
	after_data  CLOB,
	operator    VARCHAR(64),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_oper_log IS '操作日志';
COMMENT ON TABLE sys_data_audit IS '数据变更审计（前后镜像）';
COMMENT ON COLUMN sys_oper_log.duration IS '耗时（毫秒）';
COMMENT ON COLUMN sys_oper_log.status IS '状态 1 成功 0 失败';
