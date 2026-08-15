-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G103 漏斗分析 + 留存分析：业务库权限锚点（两能力走埋点库明细限窗即席查询，无新表）
-- 「漏斗分析」「留存分析」页挂顶级「埋点分析」目录（/track）下，sort 接续用户细查(7)

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1093000000000000012, p.id, '漏斗分析', '/track/funnel', '/track/funnel', 'C', 'sys:track-funnel:list', 8, 'ri:filter-3-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/track' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/track/funnel' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1093000000000000013, p.id, '留存分析', '/track/retention', '/track/retention', 'C', 'sys:track-retention:list', 9, 'ri:user-heart-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/track' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/track/retention' AND x.is_deleted = 0);
