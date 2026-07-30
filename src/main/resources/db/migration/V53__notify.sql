-- G88 多渠道消息统一调度：统一通知模板 + 渠道配置 + 发送流水；sys_user 补邮箱列（邮件渠道联系方式）

-- 统一通知模板（渠道无关）：subject/content 含 ${key} 占位，required_params 保存期由渲染器抽取落库，
-- channels 为该模板默认投递渠道（逗号分隔渠道编码），发送方未显式指定渠道时按此 fan-out
CREATE TABLE sys_notify_template (
	id              BIGINT       PRIMARY KEY,
	code            VARCHAR(64)  NOT NULL,
	name            VARCHAR(128) NOT NULL,
	subject         VARCHAR(255) NOT NULL,
	content         TEXT         NOT NULL,
	required_params VARCHAR(255),
	channels        VARCHAR(128) NOT NULL DEFAULT 'in_app',
	status          INT          NOT NULL DEFAULT 1,
	remark          VARCHAR(255),
	create_time     TIMESTAMP,
	update_time     TIMESTAMP,
	is_deleted      INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_notify_template_code ON sys_notify_template (code) WHERE is_deleted = 0;
COMMENT ON TABLE sys_notify_template IS '统一通知模板（渠道无关，${key} 占位，缺参发送期 fail-fast）';
COMMENT ON COLUMN sys_notify_template.required_params IS '必传占位参数（保存期正则从 subject/content 抽取，逗号分隔）';
COMMENT ON COLUMN sys_notify_template.channels IS '默认投递渠道（逗号分隔：in_app/mail/sms，wechat_mp 预留）';

-- 渠道配置（平台级基础设施，与 sys_sms 平台级通道先例一致；config JSON 禁类名，按渠道编码映射 Java 配置类）
CREATE TABLE sys_notify_channel (
	id          BIGINT       PRIMARY KEY,
	channel     VARCHAR(16)  NOT NULL,
	name        VARCHAR(64)  NOT NULL,
	status      INT          NOT NULL DEFAULT 0,
	config      TEXT,
	secret      VARCHAR(255),
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_notify_channel ON sys_notify_channel (channel) WHERE is_deleted = 0;
COMMENT ON TABLE sys_notify_channel IS '通知渠道配置（库表驱动热更新；secret 列 SM4 加密存凭据，如 SMTP 密码）';
COMMENT ON COLUMN sys_notify_channel.config IS '渠道非敏感配置 JSON（mail: host/port/username/from；in_app/sms 为空）';

-- 发送流水：一次 fan-out 按（渠道 × 接收人）一行，batch_id 关联同一次业务事件——
-- 重试/回执粒度须精确到单渠道单接收人（邮件 A 成功 B 失败可独立重试），故不采用"一单到底"。
-- append-only 流水不带 is_deleted（无逻辑删除语义，清理走归档/物理删除）
CREATE TABLE sys_notify_record (
	id               BIGINT      PRIMARY KEY,
	batch_id         BIGINT      NOT NULL,
	tenant_id        VARCHAR(12),
	template_code    VARCHAR(64)  NOT NULL,
	channel          VARCHAR(16)  NOT NULL,
	receiver_id      BIGINT,
	receiver_contact VARCHAR(255),
	subject          VARCHAR(255),
	content          TEXT,
	content_summary  VARCHAR(512),
	status           VARCHAR(16)  NOT NULL DEFAULT 'INIT',
	error_msg        VARCHAR(512),
	cost_ms          BIGINT,
	retry_count      INT          NOT NULL DEFAULT 0,
	next_retry_time  TIMESTAMP,
	create_time      TIMESTAMP,
	update_time      TIMESTAMP
);
CREATE INDEX idx_notify_record_batch ON sys_notify_record (batch_id);
CREATE INDEX idx_notify_record_retry ON sys_notify_record (status, next_retry_time);
COMMENT ON TABLE sys_notify_record IS '通知发送流水（租户隔离；状态 INIT/IGNORE/SUCCESS/FAILURE/DEAD，回执异步回填）';
COMMENT ON COLUMN sys_notify_record.receiver_contact IS '发送时联系方式快照（站内信=用户id，邮件=邮箱，短信=手机号）';
COMMENT ON COLUMN sys_notify_record.content_summary IS '渲染后内容摘要（截断 500 字符，列表展示用；重试取 content 全量）';

-- 邮件渠道联系方式：sys_user 补邮箱列（可空，管理端/API 录入，邮件渠道缺邮箱时该接收人记 IGNORE）
ALTER TABLE sys_user ADD COLUMN email VARCHAR(128);
COMMENT ON COLUMN sys_user.email IS '邮箱（通知邮件渠道联系方式）';

-- 种子：welcome 统一模板（自 V26 站内信模板迁移，去掉建用户时未知的 ${role}，仅保留 ${name}；默认仅站内信渠道）
INSERT INTO sys_notify_template (id, code, name, subject, content, required_params, channels, status, remark, create_time, is_deleted) VALUES
(1090000000000000010, 'welcome', '新用户欢迎通知', '欢迎 ${name}',
 '<p>你好 ${name}，欢迎加入 Mugsun 平台！如有疑问可查看右侧帮助文档。</p>',
 'name', 'in_app', 1, '新建用户成功后多渠道触达（V26 sys_message_template welcome 的统一模板化）', now(), 0);

-- 种子：渠道配置——站内信/短信默认启用（短信委托 sys_sms 平台级通道），邮件停用占位（配真实 SMTP 后启用）
INSERT INTO sys_notify_channel (id, channel, name, status, config, remark, create_time, is_deleted) VALUES
(1090000000000000021, 'in_app', '站内信', 1, '{}', '委托消息中心 MessageService（保留实时推送）', now(), 0),
(1090000000000000022, 'mail', '邮件', 0, '{"host":"smtp.example.com","port":465,"username":"","from":"noreply@mugsun.com"}', 'SMTP 凭据密码存 secret 列（SM4 加密）', now(), 0),
(1090000000000000023, 'sms', '短信', 1, '{}', '委托 SmsService（复用 sys_sms 库表热配置，不手写厂商签名）', now(), 0);

-- 种子：失败重试参数（代码常量兜底默认，此处落库支持运行时调整）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900012, '通知重试扫描间隔(毫秒)', 'notify.retry.scan-interval-ms', '60000', '调度按此间隔扫描 FAILURE 且到期的发送流水', now(), 0),
(900013, '通知重试最大次数', 'notify.retry.max-times', '3', '单条流水最大重试次数，达到后转 DEAD 死信', now(), 0),
(900014, '通知重试退避基数(毫秒)', 'notify.retry.backoff-ms', '300000', '线性退避：下次重试时间 = 当前 + 基数 × 已重试次数', now(), 0);
