-- G104 圈选式可视化埋点：圈选规则表（TRACK-PLAN §21.1）
-- G105 遗留收口：回放会话事件打点索引（TRACK-PLAN §22.3，按会话查事件走此索引免全分区扫描）

CREATE TABLE track_visual_rule (
    id          BIGINT PRIMARY KEY,                  -- 雪花
    app_key     VARCHAR(32)  NOT NULL,
    event_name  VARCHAR(64)  NOT NULL,               -- 命中后上报的自定义事件名（须过 CUSTOM_EVENT_NAME 正则，$ 前缀必拒）
    selector    VARCHAR(512) NOT NULL,               -- 圈选生成的 CSS selector（SDK 端已验唯一）
    route_path  VARCHAR(255),                        -- 路由模板限定；NULL = 全站生效
    match_text  VARCHAR(128),                        -- 元素文本包含匹配；NULL = 不限
    status      INT          NOT NULL DEFAULT 1,     -- 1 启用 / 0 停用（停用 = 不下发不命中）
    source      VARCHAR(16)  NOT NULL DEFAULT 'visual', -- 规则来源（当前恒 visual；留列防未来手工规则混入）
    tenant_id   VARCHAR(12),                          -- 服务端裁定（令牌归属），禁止客户端上报
    remark      VARCHAR(255),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_deleted  INT          NOT NULL DEFAULT 0
);
-- 重复圈选（同应用+事件名+selector+路由+文本）= 更新而非堆行；NULL 维经 coalesce 归一参与唯一约束
CREATE UNIQUE INDEX uk_visual_rule ON track_visual_rule
    (app_key, event_name, selector, coalesce(route_path, ''), coalesce(match_text, '')) WHERE is_deleted = 0;
-- config 下发查询（status=1 启用集）与按应用分页双命中
CREATE INDEX idx_visual_rule_app ON track_visual_rule (app_key, status) WHERE is_deleted = 0;
COMMENT ON TABLE track_visual_rule IS '圈选式可视化埋点规则（G104：inspect 圈选→草稿确认→/track/config 下发→SDK 命中上报自定义事件）';

-- 回放会话事件时间轴打点：按 (app_key, session_id, received_at) 查会话事件流（分区表建索引自动传播到各叶子分区）
CREATE INDEX idx_event_session ON track_event (app_key, session_id, received_at);
COMMENT ON INDEX idx_event_session IS 'G105：回放打点/会话事件流查询（会话墙钟窗裁剪分区后命中本索引）';
