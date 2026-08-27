-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- GIS 页面全部关闭 keep-alive：地图实例不宜缓存，避免切页后空白 / 路由监听互抢。
UPDATE sys_menu SET is_keep_alive = 0
	WHERE path LIKE '/gis/%' AND is_deleted = 0;
