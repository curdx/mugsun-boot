-- G99 埋点库首版迁移（track 独立数据源，版本序列独立于主库 V 序列）
-- 物理隔离：本脚本在 mugsun_track 库执行（TrackFlywayConfig 建库 + 独立 Flyway），业务库零 track_* 表
-- 锁定 PostgreSQL（分区/DETACH/ON CONFLICT/JSONB/BRIN 均为 PG 特化，不做多方言改写）
-- 流水表（track_event/track_event_data）为亿级不可变追加流：豁免 is_deleted，清理走 DROP 分区；可变表保留逻辑删除 + 审计时间
-- 幂等：全量 IF NOT EXISTS；月分区经 DO 块动态命名（含当月 + 次月预建）

-- 4.1 接入应用（appKey/采样/开关/保留期/回放配置；配置下发数据源）
CREATE TABLE IF NOT EXISTS track_app (
    id                 BIGINT PRIMARY KEY,
    app_key            VARCHAR(32)  NOT NULL,
    app_name           VARCHAR(64)  NOT NULL,
    platform           VARCHAR(16)  NOT NULL DEFAULT 'web',
    tenant_id          VARCHAR(12),
    sample_rate        INT          NOT NULL DEFAULT 100,
    enabled            INT          NOT NULL DEFAULT 1,
    mask_selectors     VARCHAR(1024),
    retention_days     INT          NOT NULL DEFAULT 90,
    replay_enabled     INT          NOT NULL DEFAULT 0,
    replay_sample_rate INT          NOT NULL DEFAULT 10,
    replay_retention_days INT       NOT NULL DEFAULT 14,
    remark             VARCHAR(255),
    create_time        TIMESTAMP,
    update_time        TIMESTAMP,
    is_deleted         INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_track_app_key ON track_app (app_key) WHERE is_deleted = 0;
COMMENT ON TABLE track_app IS '埋点接入应用（appKey/采样/开关/保留期/回放配置；配置下发数据源）';
COMMENT ON COLUMN track_app.id IS '雪花主键';
COMMENT ON COLUMN track_app.app_key IS '接入标识（浏览器可见，非机密，仅作应用标识+限流维度）';
COMMENT ON COLUMN track_app.app_name IS '应用显示名';
COMMENT ON COLUMN track_app.platform IS '平台：web/android/ios（跨端预留）';
COMMENT ON COLUMN track_app.tenant_id IS '归属租户（服务端映射，禁止客户端上报）';
COMMENT ON COLUMN track_app.sample_rate IS '事件采样率 %';
COMMENT ON COLUMN track_app.enabled IS '总开关（0=采集端直接拒收）';
COMMENT ON COLUMN track_app.mask_selectors IS '前端屏蔽选择器，逗号分隔，配置下发';
COMMENT ON COLUMN track_app.retention_days IS '明细保留天数（分区清理依据）';
COMMENT ON COLUMN track_app.replay_enabled IS '会话回放开关（G100）';
COMMENT ON COLUMN track_app.replay_sample_rate IS '回放会话采样率 %（回放重，单独采样）';
COMMENT ON COLUMN track_app.replay_retention_days IS '回放保留天数（远短于事件）';
COMMENT ON COLUMN track_app.create_time IS '创建时间';
COMMENT ON COLUMN track_app.update_time IS '更新时间';
COMMENT ON COLUMN track_app.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

-- 4.2 事件流水（按 received_at 月分区；fillfactor=100 追加不更新）
-- 三时间戳：client_ts 客户端原始时间（永不改写）/ ts 校时修正后发生时间（仅展示下钻）/ received_at 服务端接收时间（分区键 + rollup 分窗基准，单调）
-- 幂等三段式：event_id 客户端稳定幂等键 → Redis SETNX 跨重发幂等 → UNIQUE(event_id, received_at) 同接收窗兜底
CREATE TABLE IF NOT EXISTS track_event (
    id              BIGINT       NOT NULL,
    event_id        VARCHAR(36)  NOT NULL,
    app_key         VARCHAR(32)  NOT NULL,
    event_name      VARCHAR(64)  NOT NULL,
    client_ts       TIMESTAMPTZ  NOT NULL,
    ts              TIMESTAMPTZ  NOT NULL,
    received_at     TIMESTAMPTZ  NOT NULL,
    clock_skewed    INT          NOT NULL DEFAULT 0,
    distinct_id     VARCHAR(64)  NOT NULL,
    user_id         BIGINT,
    session_id      VARCHAR(36)  NOT NULL,
    tenant_id       VARCHAR(12)  NOT NULL,
    url_path        VARCHAR(512),
    route_path      VARCHAR(255),
    page_title      VARCHAR(255),
    referrer_domain VARCHAR(255),
    utm_source      VARCHAR(255),
    utm_medium      VARCHAR(255),
    utm_campaign    VARCHAR(255),
    browser         VARCHAR(32),
    os              VARCHAR(32),
    device          VARCHAR(16),
    ip              VARCHAR(64),
    ip_region       VARCHAR(64),
    duration_ms     INT,
    error_fingerprint VARCHAR(64),
    props           JSONB,
    create_time     TIMESTAMP    DEFAULT now(),
    PRIMARY KEY (id, received_at),
    UNIQUE (event_id, received_at)
) PARTITION BY RANGE (received_at);
-- fillfactor=100（追加不更新）：PG 不允许分区父表携带存储参数，只能设到叶子分区（DEFAULT 分区与 DO 块月分区均带 WITH）
-- 明细下钻（app+事件+接收时间范围，分区裁剪）
CREATE INDEX IF NOT EXISTS idx_event_query ON track_event (app_key, event_name, received_at);
-- received_at 单调，BRIN 仅 KB 级；rollup 增量扫窗用
CREATE INDEX IF NOT EXISTS idx_event_brin ON track_event USING brin (received_at) WITH (pages_per_range = 32);
-- 兜底分区，防"缺分区插入报错"；默认分区有数据即说明预建失败（监控告警）
CREATE TABLE IF NOT EXISTS track_event_default PARTITION OF track_event DEFAULT WITH (fillfactor = 100);
COMMENT ON TABLE track_event IS '埋点事件流水（按 received_at 月分区；热点属性成列，长尾 props jsonb）';
COMMENT ON COLUMN track_event.id IS '雪花主键（分区表复合主键含 received_at）';
COMMENT ON COLUMN track_event.event_id IS '客户端 UUID，跨重发幂等键（配合 Redis SETNX）';
COMMENT ON COLUMN track_event.app_key IS '接入应用标识';
COMMENT ON COLUMN track_event.event_name IS '事件名（$pageview/$click 等内置或自定义）';
COMMENT ON COLUMN track_event.client_ts IS '客户端原始时间（不改写；幂等判定 + 校时诊断）';
COMMENT ON COLUMN track_event.ts IS '分析用发生时间（校时修正后；仅供展示/下钻，不参与聚合分窗）';
COMMENT ON COLUMN track_event.received_at IS '服务端接收时间（分区键 + rollup 分窗基准，单调）';
COMMENT ON COLUMN track_event.clock_skewed IS '1=发生校时修正';
COMMENT ON COLUMN track_event.distinct_id IS '匿名 ID（anonymous_id）';
COMMENT ON COLUMN track_event.user_id IS '服务端裁定的登录用户（非客户端直采；统计唯一事实源走 track_identity 归并）';
COMMENT ON COLUMN track_event.session_id IS '会话 ID';
COMMENT ON COLUMN track_event.tenant_id IS '归属租户（恒非空：从 app_key 服务端映射，客户端传了也丢弃）';
COMMENT ON COLUMN track_event.url_path IS '原始路径（明细展示用）';
COMMENT ON COLUMN track_event.route_path IS '路由模板（如 /user/:id/detail），page 维度聚合用它防高基数';
COMMENT ON COLUMN track_event.page_title IS '页面标题';
COMMENT ON COLUMN track_event.referrer_domain IS '来源域名';
COMMENT ON COLUMN track_event.utm_source IS 'UTM 来源';
COMMENT ON COLUMN track_event.utm_medium IS 'UTM 媒介';
COMMENT ON COLUMN track_event.utm_campaign IS 'UTM 活动';
COMMENT ON COLUMN track_event.browser IS '浏览器';
COMMENT ON COLUMN track_event.os IS '操作系统';
COMMENT ON COLUMN track_event.device IS '设备类型：desktop/mobile/tablet';
COMMENT ON COLUMN track_event.ip IS '客户端 IP（可配匿名化截断）';
COMMENT ON COLUMN track_event.ip_region IS 'IP 归属地';
COMMENT ON COLUMN track_event.duration_ms IS '时长（$pageleave/计时事件）';
COMMENT ON COLUMN track_event.error_fingerprint IS '错误指纹（仅 $error 有值：message+首帧 hash，错误分组聚合用）';
COMMENT ON COLUMN track_event.props IS '长尾自定义属性（截断：键≤64/值≤1024/总量≤16KB/深度≤3）';
COMMENT ON COLUMN track_event.create_time IS '落库时间';

-- 4.3 会话物化表（事件流 upsert 维护；乱序安全：LEAST/GREATEST/累加/置位，绝不用裸 EXCLUDED 覆盖）
CREATE TABLE IF NOT EXISTS track_session (
    id              BIGINT PRIMARY KEY,
    session_id      VARCHAR(36)  NOT NULL,
    app_key         VARCHAR(32)  NOT NULL,
    tenant_id       VARCHAR(12),
    distinct_id     VARCHAR(64)  NOT NULL,
    user_id         BIGINT,
    start_time      TIMESTAMP    NOT NULL,
    end_time        TIMESTAMP    NOT NULL,
    duration_ms     INT          NOT NULL DEFAULT 0,
    pageviews       INT          NOT NULL DEFAULT 0,
    event_count     INT          NOT NULL DEFAULT 0,
    is_bounce       INT          NOT NULL DEFAULT 0,
    entry_path      VARCHAR(512),
    exit_path       VARCHAR(512),
    referrer_domain VARCHAR(255),
    utm_source      VARCHAR(255),
    browser         VARCHAR(32),
    os              VARCHAR(32),
    device          VARCHAR(16),
    ip_region       VARCHAR(64),
    has_error       INT          NOT NULL DEFAULT 0,
    has_replay      INT          NOT NULL DEFAULT 0,
    settled         INT          NOT NULL DEFAULT 0,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    is_deleted      INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_track_session_sid ON track_session (session_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_session_query ON track_session (app_key, start_time);
-- 部分索引：结算任务只扫未结会话
CREATE INDEX IF NOT EXISTS idx_session_settle ON track_session (end_time) WHERE settled = 0 AND is_deleted = 0;
COMMENT ON TABLE track_session IS '会话物化表（事件流 upsert 维护；会话指标查询行数比事件少1-2个数量级）';
COMMENT ON COLUMN track_session.id IS '雪花主键';
COMMENT ON COLUMN track_session.session_id IS '会话 ID';
COMMENT ON COLUMN track_session.app_key IS '接入应用标识';
COMMENT ON COLUMN track_session.tenant_id IS '归属租户';
COMMENT ON COLUMN track_session.distinct_id IS '匿名 ID';
COMMENT ON COLUMN track_session.user_id IS '登录用户（服务端裁定）';
COMMENT ON COLUMN track_session.start_time IS '会话开始时间（upsert 取 LEAST）';
COMMENT ON COLUMN track_session.end_time IS '会话末事件时间（upsert 取 GREATEST）';
COMMENT ON COLUMN track_session.duration_ms IS '会话时长（结算定稿）';
COMMENT ON COLUMN track_session.pageviews IS '页面浏览数（累加）';
COMMENT ON COLUMN track_session.event_count IS '事件数（累加）';
COMMENT ON COLUMN track_session.is_bounce IS '1=单 PV 跳出会话';
COMMENT ON COLUMN track_session.entry_path IS '入口路径（仅更早事件到达时更新）';
COMMENT ON COLUMN track_session.exit_path IS '出口路径（仅更晚事件到达时更新）';
COMMENT ON COLUMN track_session.referrer_domain IS '来源域名';
COMMENT ON COLUMN track_session.utm_source IS 'UTM 来源';
COMMENT ON COLUMN track_session.browser IS '浏览器';
COMMENT ON COLUMN track_session.os IS '操作系统';
COMMENT ON COLUMN track_session.device IS '设备类型';
COMMENT ON COLUMN track_session.ip_region IS 'IP 归属地';
COMMENT ON COLUMN track_session.has_error IS '1=会话内发生过 $error（回放筛选用）';
COMMENT ON COLUMN track_session.has_replay IS '1=有回放数据（G100）';
COMMENT ON COLUMN track_session.settled IS '1=会话已结算定稿（结算任务扫描依据）';
COMMENT ON COLUMN track_session.create_time IS '创建时间';
COMMENT ON COLUMN track_session.update_time IS '更新时间';
COMMENT ON COLUMN track_session.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

-- 4.4 rollup 窄表与游标（分窗基准一律 received_at；写入幂等 = 窗口全量重算 + SET 覆盖，禁止累加）
CREATE TABLE IF NOT EXISTS track_stats_5m (
    id            BIGINT PRIMARY KEY,
    app_key       VARCHAR(32)  NOT NULL,
    bucket_time   TIMESTAMP    NOT NULL,
    dim_type      VARCHAR(16)  NOT NULL,
    dim_key       VARCHAR(255) NOT NULL,
    tenant_id     VARCHAR(12),
    pv            BIGINT       NOT NULL DEFAULT 0,
    event_count   BIGINT       NOT NULL DEFAULT 0,
    session_count BIGINT       NOT NULL DEFAULT 0,
    duration_sum  BIGINT       NOT NULL DEFAULT 0,
    create_time   TIMESTAMP,
    is_deleted    INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stats_5m ON track_stats_5m (app_key, dim_type, dim_key, bucket_time);
COMMENT ON TABLE track_stats_5m IS '5 分钟窗口聚合（只放可加和指标；去重类只进 day 表；page 维度 dim_key 用路由模板防高基数）';
COMMENT ON COLUMN track_stats_5m.id IS '雪花主键';
COMMENT ON COLUMN track_stats_5m.app_key IS '接入应用标识';
COMMENT ON COLUMN track_stats_5m.bucket_time IS '5 分钟窗口起点（按 received_at 分窗）';
COMMENT ON COLUMN track_stats_5m.dim_type IS '维度类型：event/page/referrer/device';
COMMENT ON COLUMN track_stats_5m.dim_key IS '维度值：事件名/路由模板/域名/设备类型';
COMMENT ON COLUMN track_stats_5m.tenant_id IS '归属租户';
COMMENT ON COLUMN track_stats_5m.pv IS '页面浏览数';
COMMENT ON COLUMN track_stats_5m.event_count IS '事件数';
COMMENT ON COLUMN track_stats_5m.session_count IS '窗口内活跃会话（去重类，采样时仅标注口径不外推）';
COMMENT ON COLUMN track_stats_5m.duration_sum IS '时长合计（毫秒）';
COMMENT ON COLUMN track_stats_5m.create_time IS '创建时间';
COMMENT ON COLUMN track_stats_5m.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

CREATE TABLE IF NOT EXISTS track_stats_day (
    id            BIGINT PRIMARY KEY,
    app_key       VARCHAR(32)  NOT NULL,
    stat_date     DATE         NOT NULL,
    dim_type      VARCHAR(16)  NOT NULL,
    dim_key       VARCHAR(255) NOT NULL,
    tenant_id     VARCHAR(12),
    pv            BIGINT       NOT NULL DEFAULT 0,
    uv            BIGINT       NOT NULL DEFAULT 0,
    session_count BIGINT       NOT NULL DEFAULT 0,
    bounce_count  BIGINT       NOT NULL DEFAULT 0,
    duration_sum  BIGINT       NOT NULL DEFAULT 0,
    event_count   BIGINT       NOT NULL DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    is_deleted    INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stats_day ON track_stats_day (app_key, dim_type, dim_key, stat_date);
COMMENT ON TABLE track_stats_day IS '天级聚合（UV 不可从子窗口相加，必须从明细经 track_identity 归并精确算；CH 阶段改 uniqState 可合并）';
COMMENT ON COLUMN track_stats_day.id IS '雪花主键';
COMMENT ON COLUMN track_stats_day.app_key IS '接入应用标识';
COMMENT ON COLUMN track_stats_day.stat_date IS '统计日（按 received_at 分日）';
COMMENT ON COLUMN track_stats_day.dim_type IS '维度类型：overview/event/page/referrer/device';
COMMENT ON COLUMN track_stats_day.dim_key IS '维度值（page 维度用路由模板）';
COMMENT ON COLUMN track_stats_day.tenant_id IS '归属租户';
COMMENT ON COLUMN track_stats_day.pv IS '页面浏览数';
COMMENT ON COLUMN track_stats_day.uv IS '独立访客（精确去重：count(distinct coalesce(user_id, distinct_id))）';
COMMENT ON COLUMN track_stats_day.session_count IS '会话数';
COMMENT ON COLUMN track_stats_day.bounce_count IS '跳出会话数';
COMMENT ON COLUMN track_stats_day.duration_sum IS '时长合计（毫秒）';
COMMENT ON COLUMN track_stats_day.event_count IS '事件数';
COMMENT ON COLUMN track_stats_day.create_time IS '创建时间';
COMMENT ON COLUMN track_stats_day.update_time IS '更新时间';
COMMENT ON COLUMN track_stats_day.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

CREATE TABLE IF NOT EXISTS track_stats_vitals (
    id          BIGINT PRIMARY KEY,
    app_key     VARCHAR(32)  NOT NULL,
    stat_date   DATE         NOT NULL,
    metric      VARCHAR(16)  NOT NULL,
    url_path    VARCHAR(512),
    bucket      INT          NOT NULL,
    cnt         BIGINT       NOT NULL DEFAULT 0,
    tenant_id   VARCHAR(12),
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stats_vitals ON track_stats_vitals (app_key, stat_date, metric, url_path, bucket);
COMMENT ON TABLE track_stats_vitals IS 'Web Vitals 分位直方图预聚合（桶计数增量累加；看板插值 p50/p75/p95，替实时 percentile）';
COMMENT ON COLUMN track_stats_vitals.id IS '雪花主键';
COMMENT ON COLUMN track_stats_vitals.app_key IS '接入应用标识';
COMMENT ON COLUMN track_stats_vitals.stat_date IS '统计日（按 received_at 分日）';
COMMENT ON COLUMN track_stats_vitals.metric IS '指标：lcp/inp/cls/fcp/ttfb（桶定义按 metric 独立，入 TrackConstants）';
COMMENT ON COLUMN track_stats_vitals.url_path IS '可选按页维度（路由模板）';
COMMENT ON COLUMN track_stats_vitals.bucket IS '直方图桶序号（值域已知，对数桶）';
COMMENT ON COLUMN track_stats_vitals.cnt IS '桶计数（增量累加）';
COMMENT ON COLUMN track_stats_vitals.tenant_id IS '归属租户';
COMMENT ON COLUMN track_stats_vitals.create_time IS '创建时间';
COMMENT ON COLUMN track_stats_vitals.update_time IS '更新时间';

CREATE TABLE IF NOT EXISTS track_rollup_cursor (
    id          BIGINT PRIMARY KEY,
    job_key     VARCHAR(32) NOT NULL,
    app_key     VARCHAR(32) NOT NULL,
    last_bucket TIMESTAMP   NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_rollup_cursor ON track_rollup_cursor (job_key, app_key);
COMMENT ON TABLE track_rollup_cursor IS 'rollup 游标：任务从游标后一窗口补扫至当前窗口，跳窗/宕机不永久缺数（配合幂等重算）';
COMMENT ON COLUMN track_rollup_cursor.id IS '雪花主键';
COMMENT ON COLUMN track_rollup_cursor.job_key IS '任务键：stats_5m / stats_day / stats_vitals';
COMMENT ON COLUMN track_rollup_cursor.app_key IS '接入应用标识';
COMMENT ON COLUMN track_rollup_cursor.last_bucket IS '已聚合到的窗口（含）';
COMMENT ON COLUMN track_rollup_cursor.create_time IS '创建时间';
COMMENT ON COLUMN track_rollup_cursor.update_time IS '更新时间';

-- 4.7 匿名↔登录身份映射（identify 落库；user_id 首绑写入后绝不覆盖，重复 identify 只刷 last_seen_time）
CREATE TABLE IF NOT EXISTS track_identity (
    id              BIGINT PRIMARY KEY,
    app_key         VARCHAR(32) NOT NULL,
    distinct_id     VARCHAR(64) NOT NULL,
    user_id         BIGINT      NOT NULL,
    tenant_id       VARCHAR(12) NOT NULL,
    first_bind_time TIMESTAMP   NOT NULL,
    last_seen_time  TIMESTAMP,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    is_deleted      INT         NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_identity ON track_identity (app_key, distinct_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_identity_user ON track_identity (app_key, user_id);
COMMENT ON TABLE track_identity IS '匿名ID↔登录用户映射（identify 落库；UV/留存去重的归并依据：coalesce(user_id, distinct_id)）';
COMMENT ON COLUMN track_identity.id IS '雪花主键';
COMMENT ON COLUMN track_identity.app_key IS '接入应用标识';
COMMENT ON COLUMN track_identity.distinct_id IS '匿名 ID（anonymous_id）';
COMMENT ON COLUMN track_identity.user_id IS 'identify() 绑定的登录用户（首绑写入后绝不覆盖，防共享设备串号归并）';
COMMENT ON COLUMN track_identity.tenant_id IS '归属租户（恒非空）';
COMMENT ON COLUMN track_identity.first_bind_time IS '首次绑定时间';
COMMENT ON COLUMN track_identity.last_seen_time IS '最近出现时间（重复 identify 只刷本列）';
COMMENT ON COLUMN track_identity.create_time IS '创建时间';
COMMENT ON COLUMN track_identity.update_time IS '更新时间';
COMMENT ON COLUMN track_identity.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

-- 4.5 事件元数据治理（采集端自动注册 first/last_seen，管理端认领补充；停用=采集端拒收）
CREATE TABLE IF NOT EXISTS track_event_def (
    id              BIGINT PRIMARY KEY,
    app_key         VARCHAR(32) NOT NULL,
    event_name      VARCHAR(64) NOT NULL,
    display_name    VARCHAR(64),
    description     VARCHAR(255),
    status          INT         NOT NULL DEFAULT 1,
    owner           VARCHAR(64),
    first_seen_time TIMESTAMP,
    last_seen_time  TIMESTAMP,
    tenant_id       VARCHAR(12),
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    is_deleted      INT         NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_event_def ON track_event_def (app_key, event_name) WHERE is_deleted = 0;
COMMENT ON TABLE track_event_def IS '事件元数据治理（自动注册 + 认领；事件名白名单与属性"可分析"标记的载体）';
COMMENT ON COLUMN track_event_def.id IS '雪花主键';
COMMENT ON COLUMN track_event_def.app_key IS '接入应用标识';
COMMENT ON COLUMN track_event_def.event_name IS '事件名';
COMMENT ON COLUMN track_event_def.display_name IS '显示名（管理端认领补充）';
COMMENT ON COLUMN track_event_def.description IS '事件说明';
COMMENT ON COLUMN track_event_def.status IS '状态：1 启用 / 0 停用（停用=采集端拒收）';
COMMENT ON COLUMN track_event_def.owner IS '负责人';
COMMENT ON COLUMN track_event_def.first_seen_time IS '首次采集时间（自动注册）';
COMMENT ON COLUMN track_event_def.last_seen_time IS '最近采集时间';
COMMENT ON COLUMN track_event_def.tenant_id IS '归属租户';
COMMENT ON COLUMN track_event_def.create_time IS '创建时间';
COMMENT ON COLUMN track_event_def.update_time IS '更新时间';
COMMENT ON COLUMN track_event_def.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

-- 4.6 回放元数据（G100；rrweb 本体存对象存储私有桶压缩块，绝不进数据库事实表）
CREATE TABLE IF NOT EXISTS track_replay (
    id            BIGINT PRIMARY KEY,
    session_id    VARCHAR(36)  NOT NULL,
    app_key       VARCHAR(32)  NOT NULL,
    tenant_id     VARCHAR(12),
    distinct_id   VARCHAR(64)  NOT NULL,
    user_id       BIGINT,
    start_time    TIMESTAMP    NOT NULL,
    duration_ms   INT          NOT NULL DEFAULT 0,
    page_count    INT          NOT NULL DEFAULT 0,
    rrweb_events  INT          NOT NULL DEFAULT 0,
    size_bytes    BIGINT       NOT NULL DEFAULT 0,
    has_error     INT          NOT NULL DEFAULT 0,
    entry_path    VARCHAR(512),
    storage_key   VARCHAR(255),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    is_deleted    INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_replay_session ON track_replay (session_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_replay_query ON track_replay (app_key, start_time);
COMMENT ON TABLE track_replay IS '会话回放元数据（rrweb 本体存对象存储压缩块；短保留期）';
COMMENT ON COLUMN track_replay.id IS '雪花主键';
COMMENT ON COLUMN track_replay.session_id IS '会话 ID';
COMMENT ON COLUMN track_replay.app_key IS '接入应用标识';
COMMENT ON COLUMN track_replay.tenant_id IS '归属租户';
COMMENT ON COLUMN track_replay.distinct_id IS '匿名 ID';
COMMENT ON COLUMN track_replay.user_id IS '登录用户';
COMMENT ON COLUMN track_replay.start_time IS '会话开始时间';
COMMENT ON COLUMN track_replay.duration_ms IS '回放时长（毫秒）';
COMMENT ON COLUMN track_replay.page_count IS '页面数';
COMMENT ON COLUMN track_replay.rrweb_events IS 'rrweb 事件条数';
COMMENT ON COLUMN track_replay.size_bytes IS '压缩后体积（字节）';
COMMENT ON COLUMN track_replay.has_error IS '1=会话内发生过 $error';
COMMENT ON COLUMN track_replay.entry_path IS '入口路径';
COMMENT ON COLUMN track_replay.storage_key IS '对象存储对象键（私有桶）';
COMMENT ON COLUMN track_replay.create_time IS '创建时间';
COMMENT ON COLUMN track_replay.update_time IS '更新时间';
COMMENT ON COLUMN track_replay.is_deleted IS '逻辑删除（0 正常 / 1 删除）';

-- 4.8 长尾属性 EAV（按需启用：仅 track_event_def 标记"可分析"的属性才拆入，供属性分布聚合；分区键跟随主表）
CREATE TABLE IF NOT EXISTS track_event_data (
    id          BIGINT       NOT NULL,
    event_id    VARCHAR(36)  NOT NULL,
    app_key     VARCHAR(32)  NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL,
    prop_key    VARCHAR(64)  NOT NULL,
    str_value   VARCHAR(512),
    num_value   NUMERIC,
    tenant_id   VARCHAR(12)  NOT NULL,
    PRIMARY KEY (id, received_at)
) PARTITION BY RANGE (received_at);
CREATE INDEX IF NOT EXISTS idx_event_data_key ON track_event_data (app_key, prop_key, received_at);
-- 兜底分区（同主表）
CREATE TABLE IF NOT EXISTS track_event_data_default PARTITION OF track_event_data DEFAULT;
COMMENT ON TABLE track_event_data IS '长尾自定义属性 EAV（仅标记"可分析"的属性才拆入，供属性分布聚合；主表 JSONB 不建 GIN）';
COMMENT ON COLUMN track_event_data.id IS '雪花主键（分区表复合主键含 received_at）';
COMMENT ON COLUMN track_event_data.event_id IS '关联事件 event_id';
COMMENT ON COLUMN track_event_data.app_key IS '接入应用标识';
COMMENT ON COLUMN track_event_data.received_at IS '服务端接收时间（跟随主表分区键）';
COMMENT ON COLUMN track_event_data.prop_key IS '属性键';
COMMENT ON COLUMN track_event_data.str_value IS '字符串值';
COMMENT ON COLUMN track_event_data.num_value IS '数值值';
COMMENT ON COLUMN track_event_data.tenant_id IS '归属租户（恒非空）';

-- 预建当月与次月分区（动态分区名；维护任务每月 25 日预建次月，此处保证全新库启动即可写）
DO $$
DECLARE
	month_start DATE;
	month_end   DATE;
	part_name   TEXT;
BEGIN
	FOR i IN 0..1 LOOP
		month_start := (date_trunc('month', now()) + (i || ' month')::interval)::date;
		-- 上界 = 次月 1 日（date + interval 须显式转 date；month_start + 1 是加 1 天，经典坑）
		month_end := (month_start + interval '1 month')::date;
		part_name := 'track_event_' || to_char(month_start, 'YYYY_MM');
		EXECUTE format(
			'CREATE TABLE IF NOT EXISTS %I PARTITION OF track_event FOR VALUES FROM (%L) TO (%L) WITH (fillfactor = 100)',
			part_name, month_start, month_end);
		part_name := 'track_event_data_' || to_char(month_start, 'YYYY_MM');
		EXECUTE format(
			'CREATE TABLE IF NOT EXISTS %I PARTITION OF track_event_data FOR VALUES FROM (%L) TO (%L)',
			part_name, month_start, month_end);
	END LOOP;
END $$;
