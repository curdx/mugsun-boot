CREATE TABLE sys_user (
	id          BIGINT       PRIMARY KEY,
	username    VARCHAR(64)  NOT NULL UNIQUE,
	password    VARCHAR(100) NOT NULL,
	nickname    VARCHAR(64),
	status      SMALLINT     NOT NULL DEFAULT 1,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.status IS '状态：1 启用 / 0 停用';
COMMENT ON COLUMN sys_user.is_deleted IS '逻辑删除：0 正常 / 1 删除';
