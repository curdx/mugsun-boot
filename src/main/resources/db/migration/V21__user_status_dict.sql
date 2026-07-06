-- 用户状态字典：供数据变更审计将 status 值翻译为中文标签
INSERT INTO sys_dict (id, parent_id, code, dict_key, dict_value, sort, is_sealed, create_time, is_deleted) VALUES
(1050000000000000001, 0,                   'user_status', 'user_status', '用户状态', 0, 1, now(), 0),
(1050000000000000002, 1050000000000000001, 'user_status', '1',           '正常',     1, 0, now(), 0),
(1050000000000000003, 1050000000000000001, 'user_status', '0',           '停用',     2, 0, now(), 0)
ON CONFLICT (id) DO NOTHING;
