-- G75 代码生成器企业级重写：双表元数据（gen_table 表级 + gen_column 字段级在线配置）
-- 替代原单表 gen_config（仅 3 字段），支撑字段级控件/字典/查询项配置与表结构增量同步保留已编辑配置。
CREATE TABLE gen_table (
	id              BIGINT       PRIMARY KEY,
	table_name      VARCHAR(128) NOT NULL,
	table_comment   VARCHAR(255),
	entity_name     VARCHAR(128),
	module_name     VARCHAR(64),
	business_name   VARCHAR(64),
	function_name   VARCHAR(64),
	function_author VARCHAR(64),
	base_package    VARCHAR(128),
	table_prefix    VARCHAR(64),
	gen_type        VARCHAR(8)   NOT NULL DEFAULT 'zip',
	parent_menu_id  BIGINT,
	options         VARCHAR(512),
	create_time     TIMESTAMP,
	update_time     TIMESTAMP,
	is_deleted      INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_gen_table_name ON gen_table (table_name) WHERE is_deleted = 0;

CREATE TABLE gen_column (
	id             BIGINT       PRIMARY KEY,
	table_id       BIGINT       NOT NULL,
	column_name    VARCHAR(128) NOT NULL,
	column_comment VARCHAR(255),
	column_type    VARCHAR(64),
	java_type      VARCHAR(64),
	java_field     VARCHAR(128),
	is_pk          INT          NOT NULL DEFAULT 0,
	is_increment   INT          NOT NULL DEFAULT 0,
	is_required    INT          NOT NULL DEFAULT 0,
	is_insert      INT          NOT NULL DEFAULT 1,
	is_edit        INT          NOT NULL DEFAULT 1,
	is_list        INT          NOT NULL DEFAULT 1,
	is_query       INT          NOT NULL DEFAULT 0,
	query_type     VARCHAR(16)  NOT NULL DEFAULT 'EQ',
	html_type      VARCHAR(32)  NOT NULL DEFAULT 'input',
	dict_type      VARCHAR(64),
	sort           INT          NOT NULL DEFAULT 0,
	create_time    TIMESTAMP,
	update_time    TIMESTAMP,
	is_deleted     INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_gen_column_table ON gen_column (table_id) WHERE is_deleted = 0;

-- 代码生成菜单权限码（配 GenController 写端点 @SaCheckPermission，挂系统管理下；admin 通配天然放行）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted) VALUES
(1075000000000000001, 102821679785000176, '代码生成', 'C', 'sys:gen:list',   90, now(), 0),
(1075000000000000002, 1075000000000000001, '导入表',  'F', 'sys:gen:import', 1,  now(), 0),
(1075000000000000003, 1075000000000000001, '配置',    'F', 'sys:gen:edit',   2,  now(), 0),
(1075000000000000004, 1075000000000000001, '预览生成','F', 'sys:gen:preview',3,  now(), 0)
ON CONFLICT (id) DO NOTHING;
