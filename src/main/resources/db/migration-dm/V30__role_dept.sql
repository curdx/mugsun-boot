-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G52 数据权限注解化：角色-自定义部门关联（data_scope=5 时生效）
CREATE TABLE sys_role_dept (
	id          BIGINT PRIMARY KEY,
	role_id     BIGINT NOT NULL,
	dept_id     BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    DEFAULT 0 NOT NULL
);
CREATE INDEX idx_role_dept_role ON sys_role_dept (role_id);
COMMENT ON TABLE sys_role_dept IS '角色自定义数据部门（角色 data_scope=5 时的可见部门集合）';
