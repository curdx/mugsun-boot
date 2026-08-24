-- G106 埋点地理：精确坐标成列 + 应用级定位开关
-- 坐标由摄入侧校验后写入（WGS84、4 位小数 ≈ 11m，非法值不落列）；属地分析仍走既有 ip_region
-- 幂等：ADD COLUMN IF NOT EXISTS；分区父表加列自动落到各分区（含未来新分区）

ALTER TABLE track_app ADD COLUMN IF NOT EXISTS geo_enabled INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN track_app.geo_enabled IS '精确位置采集开关（G106：1=SDK 征求定位后随会话上报 geo_lon/geo_lat；默认关）';

ALTER TABLE track_event ADD COLUMN IF NOT EXISTS geo_lon NUMERIC(9, 4);
ALTER TABLE track_event ADD COLUMN IF NOT EXISTS geo_lat NUMERIC(9, 4);
COMMENT ON COLUMN track_event.geo_lon IS 'WGS84 经度（G106：摄入侧校验圆整，非法不落列）';
COMMENT ON COLUMN track_event.geo_lat IS 'WGS84 纬度（G106：摄入侧校验圆整，非法不落列）';

CREATE INDEX IF NOT EXISTS idx_event_geo ON track_event (app_key, received_at)
	WHERE geo_lon IS NOT NULL;
COMMENT ON INDEX idx_event_geo IS '埋点热力点查询（G106）：仅有坐标行，防全分区扫 JSONB';
