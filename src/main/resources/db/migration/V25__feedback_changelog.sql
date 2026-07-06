-- 意见反馈（含附件引用）+ 版本更新记录（类型分类富文本）
CREATE TABLE sys_feedback (
	id          BIGINT       PRIMARY KEY,
	content     TEXT         NOT NULL,
	contact     VARCHAR(64),
	attach_id   BIGINT,
	attach_name VARCHAR(255),
	attach_url  VARCHAR(512),
	user_id     BIGINT,
	status      INT          NOT NULL DEFAULT 0,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_feedback IS '意见反馈';
COMMENT ON COLUMN sys_feedback.status IS '处理状态：0未处理/1已处理';

CREATE TABLE sys_changelog (
	id           BIGINT       PRIMARY KEY,
	version      VARCHAR(32)  NOT NULL,
	type         VARCHAR(16)  NOT NULL,
	title        VARCHAR(255) NOT NULL,
	content      TEXT,
	publish_time TIMESTAMP,
	sort         INT          NOT NULL DEFAULT 0,
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          NOT NULL DEFAULT 0
);
COMMENT ON TABLE sys_changelog IS '版本更新记录';
COMMENT ON COLUMN sys_changelog.type IS '类型：feature新增/optimize优化/fix修复';

-- 更新日志种子（首页卡即时展示）
INSERT INTO sys_changelog (id, version, type, title, content, publish_time, sort, create_time) VALUES
(1080000000000000001, 'v1.2.0', 'feature', '新增在线帮助文档与全局帮助抽屉', '<p>支持按页面绑定帮助文档，右侧抽屉随当前页展示。</p>', now(), 3, now()),
(1080000000000000002, 'v1.1.0', 'optimize', '表格支持自定义列并持久化', '<p>列顺序、显隐、宽度可保存，刷新与重登后保持。</p>', now(), 2, now()),
(1080000000000000003, 'v1.0.0', 'fix', '修复若干已知问题并发布首个稳定版', '<p>修复登录与权限相关问题，平台首个稳定版本。</p>', now(), 1, now());
