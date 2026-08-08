-- G100 补丁：回放会话时长改墙钟口径（首末 rrweb 事件时间戳极差），与播放器时间轴一致
-- 原 duration_ms 为逐块时长累加（只计活跃段，含静止间隙的墙钟跨度对不上播放器）

ALTER TABLE track_replay ADD COLUMN IF NOT EXISTS first_event_ts BIGINT;
ALTER TABLE track_replay ADD COLUMN IF NOT EXISTS last_event_ts BIGINT;

COMMENT ON COLUMN track_replay.first_event_ts IS '会话内首个 rrweb 事件时间戳（epoch 毫秒，upsert 取 LEAST）';
COMMENT ON COLUMN track_replay.last_event_ts IS '会话内末个 rrweb 事件时间戳（epoch 毫秒，upsert 取 GREATEST）';

-- 存量行回填：duration_ms 已是累加口径，无法精确还原墙钟，以 start_time + duration 近似末时刻
UPDATE track_replay SET first_event_ts = EXTRACT(EPOCH FROM start_time) * 1000,
	last_event_ts = (EXTRACT(EPOCH FROM start_time) * 1000)::BIGINT + duration_ms
	WHERE first_event_ts IS NULL;
