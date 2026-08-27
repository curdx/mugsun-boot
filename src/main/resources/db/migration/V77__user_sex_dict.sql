-- 用户性别字典（V76 误用 id 与 notice_category 冲突，ON CONFLICT 跳过；此处用空闲 id 补种）
INSERT INTO sys_dict (id, parent_id, code, dict_key, dict_value, sort, is_sealed, create_time, is_deleted) VALUES
(1050000000000000151, 0,                   'user_sex', 'user_sex', '用户性别', 0, 1, now(), 0),
(1050000000000000152, 1050000000000000151, 'user_sex', '0',        '未知',     1, 0, now(), 0),
(1050000000000000153, 1050000000000000151, 'user_sex', '1',        '男',       2, 0, now(), 0),
(1050000000000000154, 1050000000000000151, 'user_sex', '2',        '女',       3, 0, now(), 0)
ON CONFLICT (id) DO NOTHING;
