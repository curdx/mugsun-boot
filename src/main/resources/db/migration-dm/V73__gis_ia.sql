-- 由 PG V73 转达梦
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
