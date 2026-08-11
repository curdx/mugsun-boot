-- G104 圈选式可视化埋点：业务库权限锚点（规则表 track_visual_rule 在埋点库，见 T8）
-- 「圈选规则」以 tab 形态挂在「接入管理」页内，不产生新 C 级菜单；此处仅落两个 F 级按钮权限码，
-- parent 锚定接入管理页（path=/track/app），幂等按 permission 查重（同既有 F 种子风格）

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1093000000000000014, p.id, '圈选规则查看', 'F', 'sys:track-visual:list', 10, now(), 0
FROM sys_menu p
WHERE p.path = '/track/app' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'sys:track-visual:list' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1093000000000000015, p.id, '圈选规则管理', 'F', 'sys:track-visual:edit', 11, now(), 0
FROM sys_menu p
WHERE p.path = '/track/app' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'sys:track-visual:edit' AND x.is_deleted = 0);
