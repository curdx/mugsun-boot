-- 埋点看板菜单升级：「埋点分析」从系统管理子菜单提升为顶级菜单（与工作台/系统管理同级，幂等）
-- 新结构：埋点分析(M /track) ├ 数据概览 /track/overview ├ 事件分析 /track/event
--         ├ 性能分析 /track/perf ├ 错误监控 /track/error └ 接入管理 /track/app
-- 会话回放页（/track/replay）为后续波次预留：页面未建不播菜单，避免菜单指向 404

-- 埋点分析顶级目录自播种（全新库 Flyway 先于 DataInitializer 运行，须自给自足；幂等。
-- 必须先于下方子菜单 UPDATE：否则全新库父级不存在，看板页滞留原目录）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT 1093100000000000001, 0, '埋点分析', '/track', '/index/index', 'M', 2, 'ri:line-chart-line', 0, now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/track' AND is_deleted = 0);

-- 根级排序归位：埋点分析紧跟工作台(1)之后，系统管理/租户运营/开放平台顺移一位（只动 sort，不动主键）
UPDATE sys_menu SET menu_name = '埋点分析', component = '/index/index', menu_type = 'M', parent_id = 0, sort = 2, icon = 'ri:line-chart-line'
	WHERE path = '/track' AND is_deleted = 0;
UPDATE sys_menu SET sort = 3 WHERE path = '/system' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE path = '/saas' AND is_deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE path = '/open-platform' AND is_deleted = 0;

-- 看板五页（C）迁入埋点分析目录：path/component/parent/sort/名称归位，按权限业务键锚定，主键不动（角色授权不失效）
UPDATE sys_menu SET menu_name = '数据概览', path = '/track/overview', component = '/track/overview', sort = 1,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/track' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:track-overview:list' AND is_deleted = 0;
UPDATE sys_menu SET menu_name = '事件分析', path = '/track/event', component = '/track/event', sort = 2,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/track' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:track-event:list' AND is_deleted = 0;
UPDATE sys_menu SET menu_name = '性能分析', path = '/track/perf', component = '/track/perf', sort = 3,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/track' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:track-perf:list' AND is_deleted = 0;
UPDATE sys_menu SET menu_name = '错误监控', path = '/track/error', component = '/track/error', sort = 4,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/track' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:track-error:list' AND is_deleted = 0;
UPDATE sys_menu SET menu_name = '接入管理', path = '/track/app', component = '/track/app', sort = 5,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/track' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:track-app:list' AND is_deleted = 0;

-- 接入管理页按钮（F：sys:track-app:add/edit、sys:track-replay:view）parent 仍锚接入管理页主键（未变），无需调整
