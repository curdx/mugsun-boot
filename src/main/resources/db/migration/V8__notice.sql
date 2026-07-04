CREATE TABLE sys_notice (
	id           BIGINT       PRIMARY KEY,
	tenant_id    VARCHAR(12),
	title        VARCHAR(255) NOT NULL,
	content      TEXT,
	category     VARCHAR(16),
	is_top       INT          NOT NULL DEFAULT 0,
	release_time TIMESTAMP,
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_notice IS '通知公告';
COMMENT ON COLUMN sys_notice.category IS 'notice 通知 / announcement 公告';
COMMENT ON COLUMN sys_notice.is_top IS '是否置顶 0 否 1 是';
