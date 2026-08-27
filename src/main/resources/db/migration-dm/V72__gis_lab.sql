-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- GIS 示例中心（独立页，不挤工作台）；工作台仍为业务编辑入口
UPDATE sys_menu SET sort = 3 WHERE path = '/gis/layer' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE path = '/gis/scene' AND is_deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE path = '/gis/analyze' AND is_deleted = 0;
UPDATE sys_menu SET sort = 6, menu_name = '底图配置' WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, SYSDATE, 0
FROM (
	SELECT 1094000000000000014 AS id, '示例中心' AS name, '/gis/lab' AS path, '/gis/lab' AS component, 'gis:demo:list' AS perm, 2 AS sort, 'ri:play-circle-line' AS icon FROM DUAL
) v
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);
