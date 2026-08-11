-- G102 用户细查 + 接口监控 + 响应体采集：时间线索引 + 应用级三开关与 body 保留期
-- 响应体本体存对象存储私有区（x-file-storage，键 api-body/{app_key}/{yyyyMM}/{event_id}.json.gz），
-- 不落任何元数据表：读取/清理一律按 track_event.props->>'body_ref'（= 事件 event_id）+ 事件 received_at 纯推导
-- 幂等：全量 IF NOT EXISTS / ADD COLUMN IF NOT EXISTS，重复迁移/多节点启动安全

-- 按人查时间线索引：分区父表建索引自动落各分区（含未来新分区）；范围硬限 ≤7 天由 API 层强制
CREATE INDEX IF NOT EXISTS idx_event_user_timeline ON track_event (app_key, user_id, received_at);
COMMENT ON INDEX idx_event_user_timeline IS '用户细查时间线（G102）：按 app+user+接收时间倒序游标分页，防全分区扫描';
-- 访客直查同思路（distinct_id 匿名行为时间线）
CREATE INDEX IF NOT EXISTS idx_event_distinct_timeline ON track_event (app_key, distinct_id, received_at);
COMMENT ON INDEX idx_event_distinct_timeline IS '用户细查时间线（G102）：按 app+distinct+接收时间倒序游标分页（访客口径）';

-- 应用级三开关 + body 保留期（默认全关：接口元数据/响应体/业务字段脱敏；开关经 /track/config 下发 SDK）
ALTER TABLE track_app ADD COLUMN IF NOT EXISTS api_monitor_enabled INT NOT NULL DEFAULT 0;
ALTER TABLE track_app ADD COLUMN IF NOT EXISTS api_body_enabled INT NOT NULL DEFAULT 0;
ALTER TABLE track_app ADD COLUMN IF NOT EXISTS api_body_mask_enabled INT NOT NULL DEFAULT 0;
ALTER TABLE track_app ADD COLUMN IF NOT EXISTS api_body_retention_days INT NOT NULL DEFAULT 7;
COMMENT ON COLUMN track_app.api_monitor_enabled IS '接口元数据采集开关（G102：1=SDK 包装 fetch/XHR 上报 api_request 事件）';
COMMENT ON COLUMN track_app.api_body_enabled IS '接口响应体采集开关（G102：1=SDK 经独立通道 /track/api-body 上传响应体）';
COMMENT ON COLUMN track_app.api_body_mask_enabled IS '响应体业务字段脱敏开关（G102：默认关；凭证端点硬屏蔽不可关，此为业务字段附加脱敏）';
COMMENT ON COLUMN track_app.api_body_retention_days IS '响应体保留天数（G102：远短于事件明细；清理任务到期线，1..30）';
