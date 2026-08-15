-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_report (
	id          BIGINT       PRIMARY KEY,
	report_name VARCHAR(128) NOT NULL,
	report_key  VARCHAR(64)  NOT NULL,
	chart_type  VARCHAR(16),
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_report IS '报表定义';
COMMENT ON COLUMN sys_report.report_key IS '内置数据集标识（user_status/dept_user 等）';
COMMENT ON COLUMN sys_report.chart_type IS '展示类型 table/bar/pie/line';
