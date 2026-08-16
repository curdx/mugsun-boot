-- 由 PG V72 转达梦
UPDATE sys_menu SET sort = 3 WHERE path = '/gis/layer' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE path = '/gis/scene' AND is_deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE path = '/gis/analyze' AND is_deleted = 0;
UPDATE sys_menu SET sort = 6, menu_name = '底图配置' WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000014, p.id, '示例中心', '/gis/lab', '/gis/lab', 'C', 'gis:demo:list', 2, 'ri:play-circle-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/gis' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/gis/lab' AND x.is_deleted = 0);
