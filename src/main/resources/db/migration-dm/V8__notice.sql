-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_notice (
	id           BIGINT       PRIMARY KEY,
	tenant_id    VARCHAR(12),
	title        VARCHAR(255) NOT NULL,
	content      CLOB,
	category     VARCHAR(16),
	is_top       INT          DEFAULT 0 NOT NULL,
	release_time TIMESTAMP,
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_notice IS '通知公告';
COMMENT ON COLUMN sys_notice.category IS 'notice 通知 / announcement 公告';
COMMENT ON COLUMN sys_notice.is_top IS '是否置顶 0 否 1 是';
