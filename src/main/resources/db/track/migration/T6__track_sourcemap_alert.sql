-- G101 错误监控增强：sourcemap 元数据（堆栈还原支撑）+ 应用级错误告警配置
-- sourcemap 本体存对象存储私有区（x-file-storage），本表仅元数据；告警状态走 Redis，不落表
-- 幂等：全量 IF NOT EXISTS / ADD COLUMN IF NOT EXISTS，重复迁移/多节点启动安全

-- 应用级错误告警配置（消费侧对 $error 评估告警规则的依据；告警状态在 Redis，见 TrackConstants alert-* 键）
ALTER TABLE track_app ADD COLUMN IF NOT EXISTS alert_enabled INT NOT NULL DEFAULT 0;
ALTER TABLE track_app ADD COLUMN IF NOT EXISTS alert_threshold INT NOT NULL DEFAULT 10;
COMMENT ON COLUMN track_app.alert_enabled IS '错误告警开关（G101：1=消费侧对 $error 评估新指纹/频次阈值规则并站内信告警）';
COMMENT ON COLUMN track_app.alert_threshold IS '同指纹告警频次阈值（次/10 分钟窗；规则 B 触发线，1..1000）';

-- sourcemap 元数据（.map 本体在对象存储；storage_* 记录写入时平台坐标，读取/删除按原坐标重建 FileInfo）
CREATE TABLE IF NOT EXISTS track_sourcemap (
    id                BIGINT PRIMARY KEY,
    app_key           VARCHAR(32)  NOT NULL,
    release           VARCHAR(128) NOT NULL,
    filename          VARCHAR(255) NOT NULL,
    storage_key       VARCHAR(512) NOT NULL,
    storage_platform  VARCHAR(64)  NOT NULL,
    storage_base_path VARCHAR(255) NOT NULL DEFAULT '',
    size_bytes        BIGINT       NOT NULL,
    tenant_id         VARCHAR(12),
    create_by         BIGINT,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP,
    is_deleted        INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_track_sourcemap ON track_sourcemap (app_key, release, filename) WHERE is_deleted = 0;
COMMENT ON TABLE track_sourcemap IS 'sourcemap 元数据（G101：.map 本体在对象存储私有区，唯一键 app_key+release+filename）';
COMMENT ON COLUMN track_sourcemap.id IS '雪花主键';
COMMENT ON COLUMN track_sourcemap.app_key IS '接入应用标识';
COMMENT ON COLUMN track_sourcemap.release IS '发布版本号（与 $error props.release 对齐，堆栈还原选图依据）';
COMMENT ON COLUMN track_sourcemap.filename IS '原始 .map 文件名（同时作对象键文件名段）';
COMMENT ON COLUMN track_sourcemap.storage_key IS '对象存储完整对象键（含平台 basePath 前缀；管理端不下发）';
COMMENT ON COLUMN track_sourcemap.storage_platform IS '写入时的 x-file-storage 平台名（读取/删除按原平台寻址）';
COMMENT ON COLUMN track_sourcemap.storage_base_path IS '写入时平台的 basePath（FileInfo 重建坐标；storage_key 含此前缀）';
COMMENT ON COLUMN track_sourcemap.size_bytes IS '文件大小（字节）';
COMMENT ON COLUMN track_sourcemap.tenant_id IS '归属租户（服务端裁定，取上传操作人会话租户）';
COMMENT ON COLUMN track_sourcemap.create_by IS '上传操作人用户 id';
COMMENT ON COLUMN track_sourcemap.create_time IS '创建时间';
COMMENT ON COLUMN track_sourcemap.update_time IS '更新时间';
COMMENT ON COLUMN track_sourcemap.is_deleted IS '逻辑删除（0 正常 / 1 删除）';
