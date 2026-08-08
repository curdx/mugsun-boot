-- G99 埋点：业务库权限锚点与参数种子（埋点业务表在独立 track 库，见 db/track/migration/T1 起）
-- 菜单按 V60 全量路由风格（path/component/icon 驱动前端动态路由），挂 /system 目录下，sort 接续既有子菜单（已用至 42）

-- 埋点分析五页（C）：概览/事件/性能/错误/接入
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, now(), 0
FROM (VALUES
	(1093000000000000001, '埋点概览', '/system/track-overview', '/system/track-overview', 'sys:track-overview:list', 43, 'ri:bar-chart-box-line'),
	(1093000000000000002, '事件分析', '/system/track-event',    '/system/track-event',    'sys:track-event:list',    44, 'ri:cursor-line'),
	(1093000000000000003, '性能分析', '/system/track-perf',     '/system/track-perf',     'sys:track-perf:list',     45, 'ri:speed-line'),
	(1093000000000000004, '错误监控', '/system/track-error',    '/system/track-error',    'sys:track-error:list',    46, 'ri:bug-line'),
	(1093000000000000005, '埋点接入', '/system/track-app',      '/system/track-app',      'sys:track-app:list',      47, 'ri:plug-line')
) AS v(id, name, path, component, perm, sort, icon)
JOIN sys_menu p ON p.path = '/system' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

-- 埋点接入按钮权限（F，挂「埋点接入」页下；回放查看为 G100 预留）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, 1093000000000000005, v.name, 'F', v.perm, v.sort, now(), 0
FROM (VALUES
	(1093000000000000006, '新增应用', 'sys:track-app:add',      1),
	(1093000000000000007, '编辑应用', 'sys:track-app:edit',     2),
	(1093000000000000008, '回放查看', 'sys:track-replay:view',  3)
) AS v(id, name, perm, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.is_deleted = 0);

-- 埋点参数种子（代码常量兜底默认见 TrackConstants，此处落库支持运行时调整）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900018, '埋点摄入限流(次/分/IP)', 'track.collect.rate-limit', '600', 'collect 端点单 IP 滑窗限流', now(), 0),
(900019, '埋点单批最大事件数', 'track.collect.batch-max', '100', '超过截断并计数', now(), 0),
(900020, '埋点明细保留天数(默认)', 'track.retention-days', '90', '新应用默认值；分区清理依据', now(), 0)
ON CONFLICT (id) DO NOTHING;
