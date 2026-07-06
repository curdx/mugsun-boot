-- 消息中心/站内信：消息主体 + 收件人未读态 + 占位模板
CREATE TABLE sys_message (
	id          BIGINT       PRIMARY KEY,
	title       VARCHAR(255) NOT NULL,
	content     TEXT,
	type        VARCHAR(16)  NOT NULL DEFAULT 'system',
	sender_id   BIGINT,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_message IS '站内信消息主体';
COMMENT ON COLUMN sys_message.type IS '类型：system系统/notice通知/todo待办';

CREATE TABLE sys_message_user (
	id          BIGINT    PRIMARY KEY,
	message_id  BIGINT    NOT NULL,
	user_id     BIGINT    NOT NULL,
	is_read     INT       NOT NULL DEFAULT 0,
	read_time   TIMESTAMP,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_message_user_unread ON sys_message_user (user_id, is_read) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_message_user ON sys_message_user (message_id, user_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_message_user IS '站内信收件人（每收件人一条读未读状态）';

CREATE TABLE sys_message_template (
	id          BIGINT       PRIMARY KEY,
	code        VARCHAR(64)  NOT NULL,
	title       VARCHAR(255) NOT NULL,
	content     TEXT,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_message_template_code ON sys_message_template (code) WHERE is_deleted = 0;
COMMENT ON TABLE sys_message_template IS '站内信模板（title/content 含 ${key} 占位）';

-- 模板种子（${} 占位）
INSERT INTO sys_message_template (id, code, title, content, create_time) VALUES
(1090000000000000001, 'welcome', '欢迎 ${name}',
 '<p>你好 ${name}，欢迎加入 Mugsun 平台！你的角色是 ${role}，如有疑问可查看右侧帮助文档。</p>', now());
