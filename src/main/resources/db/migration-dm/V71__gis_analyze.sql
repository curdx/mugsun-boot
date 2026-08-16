-- 由 PG V71 转达梦（无 VALUES 行构造器 / now()）
COMMENT ON COLUMN gis_layer.kind IS 'vector/heatmap/xyz/wms';

UPDATE sys_menu SET sort = 5, menu_name = '底图配置'
	WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000012, p.id, '空间分析', '/gis/analyze', '/gis/analyze', 'C', 'gis:analyze:run', 4, 'ri:shape-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/gis' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/gis/analyze' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1094000000000000013, 1094000000000000012, '执行空间运算', 'F', 'gis:analyze:run', 1, SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'gis:analyze:run' AND x.id = 1094000000000000013 AND x.is_deleted = 0);
