CREATE TABLE sys_dept (
	id          BIGINT      PRIMARY KEY,
	parent_id   BIGINT      NOT NULL DEFAULT 0,
	dept_name   VARCHAR(64) NOT NULL,
	sort        INT         NOT NULL DEFAULT 0,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         NOT NULL DEFAULT 0
);

CREATE TABLE sys_post (
	id          BIGINT      PRIMARY KEY,
	post_code   VARCHAR(64) NOT NULL,
	post_name   VARCHAR(64) NOT NULL,
	sort        INT         NOT NULL DEFAULT 0,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT         NOT NULL DEFAULT 0
);

ALTER TABLE sys_user ADD COLUMN dept_id BIGINT;
ALTER TABLE sys_user ADD COLUMN post_id BIGINT;

COMMENT ON TABLE sys_dept IS '部门';
COMMENT ON TABLE sys_post IS '岗位';
