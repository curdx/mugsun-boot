-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G49 通知公告增强：可见范围 + 阅读记录/UV
ALTER TABLE sys_notice ADD all_visible INT DEFAULT 1 NOT NULL;
ALTER TABLE sys_notice ADD view_uv     INT DEFAULT 0 NOT NULL;
ALTER TABLE sys_notice ADD view_pv     INT DEFAULT 0 NOT NULL;
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
	is_deleted  INT    DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uk_notice_scope ON sys_notice_scope (notice_id, scope_type, scope_id);
CREATE INDEX idx_notice_scope_lookup ON sys_notice_scope (scope_type, scope_id);
COMMENT ON TABLE sys_notice_scope IS '通知可见范围（scope_type 1员工/2部门）';

-- 阅读记录（每用户一行，read_count 累加）
CREATE TABLE sys_notice_read (
	id          BIGINT PRIMARY KEY,
	notice_id   BIGINT NOT NULL,
	user_id     BIGINT NOT NULL,
	read_count  INT    DEFAULT 1 NOT NULL,
	first_time  TIMESTAMP,
	last_time   TIMESTAMP,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT    DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uk_notice_read ON sys_notice_read (notice_id, user_id);
COMMENT ON TABLE sys_notice_read IS '通知阅读记录（每用户一行，read_count 累加）';
