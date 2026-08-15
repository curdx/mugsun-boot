-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G74 列级/字段级权限：用户管理下敏感字段的查看/明文按钮权限（menu_type=F），供角色按字段授权
-- 挂在「用户管理」菜单（id=102821679786000104）下，StpInterface 由角色→菜单派生 permission 进 buttons，
-- RoleAwareMaskProcessor 按这些权限码裁决 明文/脱敏/不可见。
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted) VALUES
(1074000000000000001, 102821679786000104, '查看手机号', 'F', 'sys:user:phone',        10, SYSDATE, 0),
(1074000000000000002, 102821679786000104, '手机号明文', 'F', 'sys:user:phone:plain',  11, SYSDATE, 0),
(1074000000000000003, 102821679786000104, '查看身份证', 'F', 'sys:user:idcard',       12, SYSDATE, 0),
(1074000000000000004, 102821679786000104, '身份证明文', 'F', 'sys:user:idcard:plain', 13, SYSDATE, 0);

-- datatest 角色（id=102852046756000121）仅授予「查看手机号（脱敏）」：演示非超管按字段权限
-- 手机号见脱敏、身份证不可见（无 idcard 权），与超管全明文形成对照
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, is_deleted) VALUES
(1074000000000000101, 102852046756000121, 1074000000000000001, SYSDATE, 0);
