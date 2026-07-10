-- G59 字典运行时体系：为状态类字典项补标签颜色 + 新增登录结果字典
-- 用户状态标签着色（正常绿 / 停用红），供前端 ArtDictTag 纯字典驱动着色
UPDATE sys_dict SET color = '#67C23A' WHERE code = 'user_status' AND dict_key = '1';
UPDATE sys_dict SET color = '#F56C6C' WHERE code = 'user_status' AND dict_key = '0';

-- 登录结果字典（1 成功 / 0 失败），供登录日志页 ArtStatusTag 使用
INSERT INTO sys_dict (id, parent_id, code, dict_key, dict_value, sort, is_sealed, color, create_time, is_deleted) VALUES
(1050000000000000010, 0,                   'login_result', 'login_result', '登录结果', 0, 1, NULL,      now(), 0),
(1050000000000000011, 1050000000000000010, 'login_result', '1',            '成功',     1, 0, '#67C23A', now(), 0),
(1050000000000000012, 1050000000000000010, 'login_result', '0',            '失败',     2, 0, '#F56C6C', now(), 0)
ON CONFLICT (id) DO NOTHING;
