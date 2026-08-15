-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- 在线帮助文档：目录树 + 文档(富文本+浏览量) + 页面绑定(按路由关联)
CREATE TABLE help_catalog (
	id          BIGINT       PRIMARY KEY,
	parent_id   BIGINT       DEFAULT 0 NOT NULL,
	name        VARCHAR(128) NOT NULL,
	sort        INT          DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);
COMMENT ON TABLE help_catalog IS '帮助文档目录（树形，parent_id=0 为根）';

CREATE TABLE help_doc (
	id          BIGINT        PRIMARY KEY,
	catalog_id  BIGINT        NOT NULL,
	title       VARCHAR(255)  NOT NULL,
	content     CLOB,
	view_count  BIGINT        DEFAULT 0 NOT NULL,
	sort        INT           DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT           DEFAULT 0 NOT NULL
);
CREATE INDEX idx_help_doc_catalog ON help_doc (catalog_id);
COMMENT ON TABLE help_doc IS '帮助文档（富文本内容，view_count 累计浏览量）';

CREATE TABLE help_page_binding (
	id          BIGINT        PRIMARY KEY,
	route_path  VARCHAR(255)  NOT NULL,
	doc_id      BIGINT        NOT NULL,
	sort        INT           DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT           DEFAULT 0 NOT NULL
);
CREATE INDEX idx_help_binding_route ON help_page_binding (route_path);
CREATE UNIQUE INDEX uk_help_binding ON help_page_binding (route_path, doc_id);
COMMENT ON TABLE help_page_binding IS '帮助文档与前端路由的绑定（一页可绑多文档）';

-- 示例种子：系统管理帮助 → 如何新增用户，绑定 /system/user
INSERT INTO help_catalog (id, parent_id, name, sort, create_time) VALUES
(1070000000000000001, 0, '系统管理帮助', 1, SYSDATE);
INSERT INTO help_doc (id, catalog_id, title, content, view_count, sort, create_time) VALUES
(1070000000000000101, 1070000000000000001, '如何新增用户',
 '<h3>新增用户步骤</h3><p>1. 进入「用户管理」页面；</p><p>2. 点击左上角<strong>新增用户</strong>按钮；</p><p>3. 填写用户名、昵称、手机号后保存即可。</p>', 0, 1, SYSDATE);
INSERT INTO help_page_binding (id, route_path, doc_id, sort, create_time) VALUES
(1070000000000000201, '/system/user', 1070000000000000101, 1, SYSDATE);
