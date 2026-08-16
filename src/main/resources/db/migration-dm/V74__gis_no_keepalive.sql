-- 由 PG V74 转达梦
UPDATE sys_menu SET is_keep_alive = 0
	WHERE path LIKE '/gis/%' AND is_deleted = 0;
