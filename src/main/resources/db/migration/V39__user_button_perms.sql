-- G61 权限双通道：用户管理下按钮权限（menu_type=F）+ 授予 datatest 角色部分按钮，供非超管按钮门控实测
-- 按钮权限挂在「用户管理」菜单（id=102821679786000104）下，StpInterface 由角色→菜单派生 permission 进 buttons
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted) VALUES
(1061000000000000001, 102821679786000104, '新增用户', 'F', 'sys:user:add',    1, now(), 0),
(1061000000000000002, 102821679786000104, '编辑用户', 'F', 'sys:user:edit',   2, now(), 0),
(1061000000000000003, 102821679786000104, '删除用户', 'F', 'sys:user:remove', 3, now(), 0),
(1061000000000000004, 102821679786000104, '用户授权', 'F', 'sys:user:grant',  4, now(), 0),
(1061000000000000005, 102821679786000104, '重置密码', 'F', 'sys:user:reset',  5, now(), 0)
ON CONFLICT (id) DO NOTHING;

-- datatest 角色（id=102852046756000121）仅授予「新增用户」，演示非超管按真实权限码显隐按钮
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, is_deleted) VALUES
(1061000000000000101, 102852046756000121, 1061000000000000001, now(), 0)
ON CONFLICT (id) DO NOTHING;
