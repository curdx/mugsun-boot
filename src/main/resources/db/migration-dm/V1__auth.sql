-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_user (
	id          BIGINT       PRIMARY KEY,
	username    VARCHAR(64)  NOT NULL UNIQUE,
	password    VARCHAR(100) NOT NULL,
	nickname    VARCHAR(64),
	status      SMALLINT     DEFAULT 1 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.status IS '状态：1 启用 / 0 停用';
COMMENT ON COLUMN sys_user.is_deleted IS '逻辑删除：0 正常 / 1 删除';
