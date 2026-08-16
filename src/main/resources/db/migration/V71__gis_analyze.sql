-- GIS 空间分析菜单 + 底图排序后移；图层 kind 注释扩至 xyz/wms
COMMENT ON COLUMN gis_layer.kind IS 'vector/heatmap/xyz/wms';

UPDATE sys_menu SET sort = 5, menu_name = '底图配置'
	WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, now(), 0
FROM (VALUES
	(1094000000000000012, '空间分析', '/gis/analyze', '/gis/analyze', 'gis:analyze:run', 4, 'ri:shape-line')
) AS v(id, name, path, component, perm, sort, icon)
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, v.parent, v.name, 'F', v.perm, v.sort, now(), 0
FROM (VALUES
	(1094000000000000013, 1094000000000000012, '执行空间运算', 'gis:analyze:run', 1)
) AS v(id, parent, name, perm, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.id = v.id AND x.is_deleted = 0);
