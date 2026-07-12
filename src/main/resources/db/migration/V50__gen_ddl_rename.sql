-- G78 动态建表：字段改名追踪列 + DDL 执行权限码。
-- column_name_old 记录列的旧名，增量同步据此走 RENAME 而非 DROP+ADD，保数据不丢。
ALTER TABLE gen_column ADD COLUMN column_name_old VARCHAR(128);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted) VALUES
(1075000000000000005, 1075000000000000001, '动态建表', 'F', 'sys:gen:ddl', 4, now(), 0)
ON CONFLICT (id) DO NOTHING;
