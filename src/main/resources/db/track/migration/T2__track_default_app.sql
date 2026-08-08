-- G99 默认应用种子：mugsun-pc 自埋点（本系统管理台自监控）
-- 固定 app_key 供 mugsun-pc 前端缺省接入（VITE_TRACK_APP_KEY 可覆盖）；appKey 非机密，安全不依赖其保密
-- app_key 格式与 TrackAdminService 生成规则一致：ak_ 前缀 + 24 位 hex（此处为固定常量，便于开发联调零配置）
-- 幂等：命中部分唯一索引 uk_track_app_key（is_deleted = 0）即跳过，重复迁移/多节点启动安全

INSERT INTO track_app (id, app_key, app_name, platform, tenant_id, sample_rate, enabled,
    retention_days, replay_enabled, replay_sample_rate, replay_retention_days, remark,
    create_time, update_time, is_deleted)
VALUES (1899000000000000001, 'ak_000000000000000000000001', 'mugsun-pc 自监控', 'web', '000000', 100, 1,
    90, 0, 10, 14, '默认应用种子：mugsun-pc 自埋点（开发联调固定 app_key，生产环境请新建应用并改用下发配置）',
    now(), now(), 0)
ON CONFLICT (app_key) WHERE is_deleted = 0 DO NOTHING;
