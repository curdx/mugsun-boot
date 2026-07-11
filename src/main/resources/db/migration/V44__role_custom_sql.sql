-- G73 数据权限：角色自定义 SQL 规则（data_scope=6 时按此 SQL 片段过滤）
ALTER TABLE sys_role ADD COLUMN custom_sql VARCHAR(512);
COMMENT ON COLUMN sys_role.custom_sql IS '自定义数据权限 SQL 片段（data_scope=6 生效，平台管理员配置）';
