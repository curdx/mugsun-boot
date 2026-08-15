-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- 用户状态字典：供数据变更审计将 status 值翻译为中文标签
INSERT INTO sys_dict (id, parent_id, code, dict_key, dict_value, sort, is_sealed, create_time, is_deleted) VALUES
(1050000000000000001, 0,                   'user_status', 'user_status', '用户状态', 0, 1, SYSDATE, 0),
(1050000000000000002, 1050000000000000001, 'user_status', '1',           '正常',     1, 0, SYSDATE, 0),
(1050000000000000003, 1050000000000000001, 'user_status', '0',           '停用',     2, 0, SYSDATE, 0);
