-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- GIS 空间分析菜单 + 底图排序后移；图层 kind 注释扩至 xyz/wms
COMMENT ON COLUMN gis_layer.kind IS 'vector/heatmap/xyz/wms';

UPDATE sys_menu SET sort = 5, menu_name = '底图配置'
	WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, SYSDATE, 0
FROM (
	SELECT 1094000000000000012 AS id, '空间分析' AS name, '/gis/analyze' AS path, '/gis/analyze' AS component, 'gis:analyze:run' AS perm, 4 AS sort, 'ri:shape-line' AS icon FROM DUAL
) v
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, v.parent, v.name, 'F', v.perm, v.sort, SYSDATE, 0
FROM (
	SELECT 1094000000000000013 AS id, 1094000000000000012 AS parent, '执行空间运算' AS name, 'gis:analyze:run' AS perm, 1 AS sort FROM DUAL
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.id = v.id AND x.is_deleted = 0);
