-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_dept (
	id          BIGINT      PRIMARY KEY,
	parent_id   BIGINT      DEFAULT 0 NOT NULL,
	dept_name   VARCHAR(64) NOT NULL,
	sort        INT         DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         DEFAULT 0 NOT NULL
);

CREATE TABLE sys_post (
	id          BIGINT      PRIMARY KEY,
	post_code   VARCHAR(64) NOT NULL,
	post_name   VARCHAR(64) NOT NULL,
	sort        INT         DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         DEFAULT 0 NOT NULL
);

ALTER TABLE sys_user ADD dept_id BIGINT;
ALTER TABLE sys_user ADD post_id BIGINT;

COMMENT ON TABLE sys_dept IS '部门';
COMMENT ON TABLE sys_post IS '岗位';
