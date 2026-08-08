-- G100 回放元数据补列：块序号上限 + 存储坐标（storage_key 只存首块对象键，块清单按 seq 推导需 last_seq；
-- 读取/删除重建 FileInfo 需写入时的平台与 basePath，防默认存储平台配置变更后读不到旧对象）

ALTER TABLE track_replay ADD COLUMN IF NOT EXISTS last_seq INT NOT NULL DEFAULT -1;
ALTER TABLE track_replay ADD COLUMN IF NOT EXISTS storage_platform VARCHAR(64);
ALTER TABLE track_replay ADD COLUMN IF NOT EXISTS storage_base_path VARCHAR(255);

COMMENT ON COLUMN track_replay.last_seq IS '已持久化的最大块序号（seq 自 0 连续递增；块键清单纯推导：dir(storage_key)+seq+".gz"，个别被拒/丢失的 seq 读取时 404 由前端跳过；-1=尚无块）';
COMMENT ON COLUMN track_replay.storage_platform IS '首块写入的 x-file-storage 平台名（读取/删除按原平台寻址，不随默认平台切换漂移）';
COMMENT ON COLUMN track_replay.storage_base_path IS '首块写入时平台的 basePath（FileInfo 重建坐标；storage_key 含此前缀）';
