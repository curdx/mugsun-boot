-- G100 开发联调：默认种子应用（mugsun-pc 自监控）开启会话回放
-- dev 自监控全量录（sample_rate=100）便于联调回归；生产部署请按容量自行调低采样率或关闭
-- 幂等：UPDATE 天然幂等，重复迁移/多节点启动安全；仅命中 T2 固定 app_key 种子行，用户自建应用不受影响

UPDATE track_app SET replay_enabled = 1, replay_sample_rate = 100, update_time = now()
WHERE app_key = 'ak_000000000000000000000001' AND is_deleted = 0;
