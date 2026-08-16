-- GIS 示例中心（独立页，不挤工作台）；工作台仍为业务编辑入口
UPDATE sys_menu SET sort = 3 WHERE path = '/gis/layer' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE path = '/gis/scene' AND is_deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE path = '/gis/analyze' AND is_deleted = 0;
UPDATE sys_menu SET sort = 6, menu_name = '底图配置' WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, now(), 0
FROM (VALUES
	(1094000000000000014, '示例中心', '/gis/lab', '/gis/lab', 'gis:demo:list', 2, 'ri:play-circle-line')
) AS v(id, name, path, component, perm, sort, icon)
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);
