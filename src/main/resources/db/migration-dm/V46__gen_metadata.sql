-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

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
	gen_type        VARCHAR(8)   DEFAULT 'zip' NOT NULL,
	parent_menu_id  BIGINT,
	options         VARCHAR(512),
	create_time     TIMESTAMP,
	update_time     TIMESTAMP,
	is_deleted      INT          DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uk_gen_table_name ON gen_table (table_name);

CREATE TABLE gen_column (
	id             BIGINT       PRIMARY KEY,
	table_id       BIGINT       NOT NULL,
	column_name    VARCHAR(128) NOT NULL,
	column_comment VARCHAR(255),
	column_type    VARCHAR(64),
	java_type      VARCHAR(64),
	java_field     VARCHAR(128),
	is_pk          INT          DEFAULT 0 NOT NULL,
	is_increment   INT          DEFAULT 0 NOT NULL,
	is_required    INT          DEFAULT 0 NOT NULL,
	is_insert      INT          DEFAULT 1 NOT NULL,
	is_edit        INT          DEFAULT 1 NOT NULL,
	is_list        INT          DEFAULT 1 NOT NULL,
	is_query       INT          DEFAULT 0 NOT NULL,
	query_type     VARCHAR(16)  DEFAULT 'EQ' NOT NULL,
	html_type      VARCHAR(32)  DEFAULT 'input' NOT NULL,
	dict_type      VARCHAR(64),
	sort           INT          DEFAULT 0 NOT NULL,
	create_time    TIMESTAMP,
	update_time    TIMESTAMP,
	is_deleted     INT          DEFAULT 0 NOT NULL
);
CREATE INDEX idx_gen_column_table ON gen_column (table_id);

-- 代码生成菜单权限码（配 GenController 写端点 @SaCheckPermission，挂系统管理下；admin 通配天然放行）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted) VALUES
(1075000000000000001, 102821679785000176, '代码生成', 'C', 'sys:gen:list',   90, SYSDATE, 0),
(1075000000000000002, 1075000000000000001, '导入表',  'F', 'sys:gen:import', 1,  SYSDATE, 0),
(1075000000000000003, 1075000000000000001, '配置',    'F', 'sys:gen:edit',   2,  SYSDATE, 0),
(1075000000000000004, 1075000000000000001, '预览生成','F', 'sys:gen:preview',3,  SYSDATE, 0);
