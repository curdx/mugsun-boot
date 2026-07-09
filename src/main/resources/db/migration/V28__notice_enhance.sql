-- G49 通知公告增强：可见范围 + 阅读记录/UV
ALTER TABLE sys_notice ADD COLUMN all_visible INT NOT NULL DEFAULT 1;
ALTER TABLE sys_notice ADD COLUMN view_uv     INT NOT NULL DEFAULT 0;
ALTER TABLE sys_notice ADD COLUMN view_pv     INT NOT NULL DEFAULT 0;
COMMENT ON COLUMN sys_notice.all_visible IS '1全部可见/0按范围';
COMMENT ON COLUMN sys_notice.view_uv IS '浏览用户数(去重)';
COMMENT ON COLUMN sys_notice.view_pv IS '浏览次数(累计)';

-- 可见范围（主体+关联，对齐 sys_message_user 风格）
CREATE TABLE sys_notice_scope (
	id          BIGINT PRIMARY KEY,
	notice_id   BIGINT NOT NULL,
	scope_type  INT    NOT NULL,
	scope_id    BIGINT NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_notice_scope ON sys_notice_scope (notice_id, scope_type, scope_id) WHERE is_deleted = 0;
CREATE INDEX idx_notice_scope_lookup ON sys_notice_scope (scope_type, scope_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_notice_scope IS '通知可见范围（scope_type 1员工/2部门）';

-- 阅读记录（每用户一行，read_count 累加）
CREATE TABLE sys_notice_read (
	id          BIGINT PRIMARY KEY,
	notice_id   BIGINT NOT NULL,
	user_id     BIGINT NOT NULL,
	read_count  INT    NOT NULL DEFAULT 1,
	first_time  TIMESTAMP,
	last_time   TIMESTAMP,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_notice_read ON sys_notice_read (notice_id, user_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_notice_read IS '通知阅读记录（每用户一行，read_count 累加）';
