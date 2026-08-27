-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- GIS 信息架构收口：侧栏只留「地图 / 示例 / 图层 / 底图」。
-- 场景 = 工作台里的文件切换；空间分析 = 工作台「更多」入口。路由保留，仅隐藏菜单。

UPDATE sys_menu SET menu_name = '地图', sort = 1
	WHERE path = '/gis/workspace' AND is_deleted = 0;

UPDATE sys_menu SET menu_name = '示例', sort = 2
	WHERE path = '/gis/lab' AND is_deleted = 0;

UPDATE sys_menu SET menu_name = '图层', sort = 3
	WHERE path = '/gis/layer' AND is_deleted = 0;

UPDATE sys_menu SET menu_name = '底图', sort = 4
	WHERE path = '/gis/provider' AND is_deleted = 0;

UPDATE sys_menu SET is_hide = 1, sort = 8
	WHERE path IN ('/gis/scene', '/gis/analyze') AND is_deleted = 0;
