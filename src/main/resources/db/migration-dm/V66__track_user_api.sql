-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G102 用户细查：业务库权限锚点与参数种子（时间线索引/应用开关列在 track 库，见 T7）
-- 「用户细查」页挂顶级「埋点分析」目录（/track）下，sort 接续会话回放(6)；
-- 「查看接口响应体」按钮（F，sys:track-user:view-body）挂该页下（最高敏感，读取必留痕审计）

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1093000000000000010, p.id, '用户细查', '/track/user', '/track/user', 'C', 'sys:track-user:list', 7, 'ri:user-search-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/track' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/track/user' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1093000000000000011, 1093000000000000010, '查看接口响应体', 'F', 'sys:track-user:view-body', 1, SYSDATE, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'sys:track-user:view-body' AND x.is_deleted = 0);

-- 响应体参数种子（代码常量兜底默认见 TrackConstants，此处落库支持运行时调整）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900022, '接口响应体上限(字节)', 'track.api-body.max-bytes', '1048576', '单个接口响应体采集上限（安全阀，防大导出响应打爆存储）；解压后口径，超限 413 不采', SYSDATE, 0);
