-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- W3 后端菜单驱动：sys_menu 补 component/is_public 列 + 全量菜单树种子（幂等）
-- component：前端动态路由的视图路径（ComponentLoader 兼容），代码生成 menu.sql 产物已引用此列
-- is_public：公共菜单（任意登录用户可见：工作台/我的通知/我的消息/待办/审批/个人中心）

ALTER TABLE sys_menu ADD component VARCHAR(128);
ALTER TABLE sys_menu ADD is_public INT DEFAULT 0 NOT NULL;
COMMENT ON COLUMN sys_menu.component IS '前端视图路径（如 /system/user → views/system/user/index.vue；目录为 /index/index）';
COMMENT ON COLUMN sys_menu.is_public IS '是否公共菜单（0 按角色授权 / 1 任意登录可见）';

-- 清理历史 E2E 遗留垃圾菜单
DELETE FROM sys_role_menu WHERE menu_id IN (SELECT id FROM sys_menu WHERE menu_name LIKE 'e2e_menu_%');
DELETE FROM sys_menu WHERE menu_name LIKE 'e2e_menu_%';

-- 系统管理目录自播种（全新库 Flyway 先于 DataInitializer 运行，须自给自足；幂等。
-- 必须先于下方 UPDATE/子菜单插入：否则全新库父级不存在，日志三件套滞留根级）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT 1200000000000000001, 0, '系统管理', '/system', '/index/index', 'M', 2, 'ri:settings-2-line', 0, SYSDATE, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system' AND is_deleted = 0);
-- 用户管理自播种（按钮行（F）按业务键重锚依赖此节点存在）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1200000000000000101, (SELECT id FROM sys_menu WHERE path = '/system' AND is_deleted = 0 LIMIT 1),
	'用户管理', '/system/user', '/system/user', 'C', 'sys:user:list', 1, 'ri:user-line', 0, SYSDATE, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/user' AND is_deleted = 0);

-- 既有节点归位（图标/组件/父级/排序），不动主键（角色授权引用不失效）
UPDATE sys_menu SET icon = 'ri:settings-2-line', component = '/index/index', sort = 2 WHERE path = '/system' AND is_deleted = 0;
UPDATE sys_menu SET icon = 'ri:user-line', component = '/system/user', menu_type = 'C', sort = 1, permission = 'sys:user:list' WHERE path = '/system/user' AND is_deleted = 0;
UPDATE sys_menu SET path = '/system/gen', component = '/system/gen', icon = 'ri:code-box-line', sort = 15
	WHERE permission = 'sys:gen:list' AND is_deleted = 0 AND (path IS NULL OR path = '');
UPDATE sys_menu SET path = '/system/api-log', component = '/system/api-log', icon = 'ri:global-line', sort = 32,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/system' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:api-log:list' AND is_deleted = 0 AND (path IS NULL OR path = '');
UPDATE sys_menu SET path = '/system/error-log', component = '/system/error-log', icon = 'ri:error-warning-line', sort = 33,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/system' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:error-log:list' AND is_deleted = 0 AND (path IS NULL OR path = '');
UPDATE sys_menu SET path = '/system/monitor', component = '/system/monitor', icon = 'ri:line-chart-line', sort = 34,
	parent_id = (SELECT id FROM sys_menu WHERE path = '/system' AND is_deleted = 0 LIMIT 1)
	WHERE permission = 'sys:monitor:list' AND is_deleted = 0 AND (path IS NULL OR path = '');

-- 工作台分组（console 公共：任何登录用户落点）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, v.pid, v.name, v.path, v.component, v.mtype, v.sort, v.icon, v.pub, SYSDATE, 0
FROM (
	SELECT 1200000000000000010 AS id, 0 AS pid, '工作台' AS name, '/dashboard' AS path, '/index/index' AS component, 'M' AS mtype, 1 AS sort, 'ri:dashboard-line' AS icon, 0 AS pub FROM DUAL
	UNION ALL
	SELECT 1200000000000000011, 1200000000000000010, '工作台', '/dashboard/console', '/dashboard/console', 'C', 1, 'ri:dashboard-line', 1 FROM DUAL
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

-- 系统管理子菜单（parent 按 /system 解析；user/gen/api-log/error-log/monitor 已存在不重复插）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.sort, v.icon, v.pub, SYSDATE, 0
FROM (
	SELECT 1200000000000000102 AS id, '角色管理' AS name, '/system/role' AS path, '/system/role' AS component, 2 AS sort, 'ri:user-settings-line' AS icon, 0 AS pub FROM DUAL
	UNION ALL
	SELECT 1200000000000000103, '部门管理', '/system/dept', '/system/dept', 3, 'ri:organization-chart', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000104, '岗位管理', '/system/post', '/system/post', 4, 'ri:contacts-book-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000105, '参数管理', '/system/param', '/system/param', 5, 'ri:settings-3-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000106, '接口加解密', '/system/crypto', '/system/crypto', 6, 'ri:shield-keyhole-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000107, '邮件模板', '/system/mail-template', '/system/mail-template', 7, 'ri:mail-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000108, '字典管理', '/system/dict', '/system/dict', 8, 'ri:book-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000109, '业务字典', '/system/dict-biz', '/system/dict-biz', 9, 'ri:book-marked-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000110, '通知公告', '/system/notice', '/system/notice', 10, 'ri:notification-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000111, '我的通知', '/system/my-notice', '/system/my-notice', 11, 'ri:mail-open-line', 1 FROM DUAL
	UNION ALL
	SELECT 1200000000000000112, '附件管理', '/system/attach', '/system/attach', 12, 'ri:folder-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000113, '存储配置', '/system/oss', '/system/oss', 13, 'ri:cloud-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000114, '短信配置', '/system/sms', '/system/sms', 14, 'ri:message-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000116, '在线表单', '/system/online-form', '/system/online-form', 16, 'ri:table-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000117, '动态建表', '/system/gen-modeling', '/system/gen-modeling', 17, 'ri:database-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000118, '表单设计', '/system/form-designer', '/system/form-designer', 18, 'ri:file-edit-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000119, '流程定义', '/system/flow-def', '/system/flow-def', 19, 'ri:git-branch-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000120, '待办工作台', '/system/flow-todo', '/system/flow-todo', 20, 'ri:task-line', 1 FROM DUAL
	UNION ALL
	SELECT 1200000000000000121, '审批中心', '/system/flow-center', '/system/flow-center', 21, 'ri:inbox-archive-line', 1 FROM DUAL
	UNION ALL
	SELECT 1200000000000000122, '流程设计', '/system/flow-graph', '/system/flow-graph', 22, 'ri:node-tree', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000123, '定时任务', '/system/job', '/system/job', 23, 'ri:timer-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000124, '报表管理', '/system/report', '/system/report', 24, 'ri:bar-chart-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000125, '登录日志', '/system/login-log', '/system/login-log', 25, 'ri:shield-keyhole-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000126, '在线会话', '/system/online', '/system/online', 26, 'ri:computer-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000127, '登录客户端', '/system/client', '/system/client', 27, 'ri:device-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000128, '行政区划', '/system/region', '/system/region', 28, 'ri:map-pin-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000129, '操作日志', '/system/log', '/system/log', 29, 'ri:file-list-3-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000130, '变更记录', '/system/data-audit', '/system/data-audit', 30, 'ri:history-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000131, '帮助文档', '/system/help-doc', '/system/help-doc', 31, 'ri:question-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000135, '更新日志', '/system/changelog', '/system/changelog', 35, 'ri:git-branch-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000136, '意见反馈', '/system/feedback', '/system/feedback', 36, 'ri:feedback-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000137, '我的消息', '/system/message', '/system/message', 37, 'ri:notification-2-line', 1 FROM DUAL
	UNION ALL
	SELECT 1200000000000000138, '发送站内信', '/system/message-send', '/system/message-send', 38, 'ri:send-plane-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000139, '消息模板', '/system/message-template', '/system/message-template', 39, 'ri:mail-settings-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000140, '缓存管理', '/system/cache', '/system/cache', 40, 'ri:database-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000141, '个人中心', '/system/user-center', '/system/user-center', 41, 'ri:user-line', 1 FROM DUAL
	UNION ALL
	SELECT 1200000000000000142, '菜单管理', '/system/menu', '/system/menu', 42, 'ri:menu-line', 0 FROM DUAL
) v
JOIN sys_menu p ON p.path = '/system' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

-- 个人中心默认隐藏（经头像菜单进入，不占侧边栏）
UPDATE sys_menu SET is_hide = 1 WHERE path = '/system/user-center' AND is_deleted = 0;

-- 租户运营分组
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, v.pid, v.name, v.path, v.component, v.mtype, v.sort, v.icon, v.pub, SYSDATE, 0
FROM (
	SELECT 1200000000000000020 AS id, 0 AS pid, '租户运营' AS name, '/saas' AS path, '/index/index' AS component, 'M' AS mtype, 3 AS sort, 'ri:community-line' AS icon, 0 AS pub FROM DUAL
	UNION ALL
	SELECT 1200000000000000201, 1200000000000000020, '租户管理', '/saas/tenant', '/system/tenant', 'C', 1, 'ri:building-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000202, 1200000000000000020, '租户套餐', '/saas/tenant-package', '/system/tenant-package', 'C', 2, 'ri:price-tag-3-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000203, 1200000000000000020, '租户数据源', '/saas/tenant-datasource', '/system/tenant-datasource', 'C', 3, 'ri:database-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000204, 1200000000000000020, '客户管理', '/saas/customer', '/system/customer', 'C', 4, 'ri:contacts-book-line', 0 FROM DUAL
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

-- 开放平台分组
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, v.pid, v.name, v.path, v.component, v.mtype, v.sort, v.icon, v.pub, SYSDATE, 0
FROM (
	SELECT 1200000000000000030 AS id, 0 AS pid, '开放平台' AS name, '/open-platform' AS path, '/index/index' AS component, 'M' AS mtype, 4 AS sort, 'ri:apps-2-line' AS icon, 0 AS pub FROM DUAL
	UNION ALL
	SELECT 1200000000000000031, 1200000000000000030, 'API密钥', '/open-platform/api-key', '/system/api-key', 'C', 1, 'ri:key-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000032, 1200000000000000030, '客户端管理', '/open-platform/oauth-client', '/system/oauth-client', 'C', 2, 'ri:apps-2-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000033, 1200000000000000030, '接口调试', '/open-platform/oauth-debug', '/system/oauth-debug', 'C', 3, 'ri:terminal-box-line', 0 FROM DUAL
	UNION ALL
	SELECT 1200000000000000034, 1200000000000000030, '调用日志', '/open-platform/oauth-log', '/system/oauth-log', 'C', 4, 'ri:file-list-3-line', 0 FROM DUAL
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);
