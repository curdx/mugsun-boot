-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G100 会话回放：业务库权限锚点与参数种子（回放元数据/本体在 track 库与对象存储，见 T3 起）
-- 「会话回放」页挂 V64 提升后的顶级「埋点分析」目录（/track）下，sort 接续接入管理(5)；
-- V63 预留的「回放查看」按钮（F，sys:track-replay:view）改挂该页下，权限码不变（角色授权不失效）

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1093000000000000009, p.id, '会话回放', '/track/replay', '/track/replay', 'C', 'sys:track-replay:list', 6, 'ri:play-circle-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/track' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/track/replay' AND x.is_deleted = 0);

UPDATE sys_menu SET parent_id = 1093000000000000009
WHERE id = 1093000000000000008 AND permission = 'sys:track-replay:view' AND is_deleted = 0;

-- 回放参数种子（代码常量兜底默认见 TrackConstants，此处落库支持运行时调整）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900021, '回放单会话累计上限(字节)', 'track.replay.session-max-bytes', '20971520', '解压后口径；超限 413 并封禁该会话后续块', SYSDATE, 0);
