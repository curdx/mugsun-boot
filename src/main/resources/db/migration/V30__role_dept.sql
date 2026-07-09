-- G52 数据权限注解化：角色-自定义部门关联（data_scope=5 时生效）
CREATE TABLE sys_role_dept (
	id          BIGINT PRIMARY KEY,
	role_id     BIGINT NOT NULL,
	dept_id     BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    NOT NULL DEFAULT 0
);
CREATE INDEX idx_role_dept_role ON sys_role_dept (role_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_role_dept IS '角色自定义数据部门（角色 data_scope=5 时的可见部门集合）';
