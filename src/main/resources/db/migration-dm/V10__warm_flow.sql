-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE flow_definition (
	id              BIGINT         NOT NULL,
	flow_code       VARCHAR(40)  NOT NULL,
	flow_name       VARCHAR(100) NOT NULL,
	model_value     VARCHAR(40)  DEFAULT 'CLASSICS' NOT NULL,
	category        VARCHAR(100),
	version         VARCHAR(20)  NOT NULL,
	is_publish      SMALLINT         DEFAULT 0 NOT NULL,
	form_custom     CHAR(1)    DEFAULT 'N',
	form_path       VARCHAR(100),
	activity_status SMALLINT         DEFAULT 1 NOT NULL,
	listener_type   VARCHAR(100),
	listener_path   VARCHAR(400),
	ext             VARCHAR(500),
	create_time     TIMESTAMP,
	create_by       VARCHAR(64)  DEFAULT '',
	update_time     TIMESTAMP,
	update_by       VARCHAR(64)  DEFAULT '',
	del_flag        CHAR(1)    DEFAULT '0',
	tenant_id       VARCHAR(40),
	CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_node (
	id              BIGINT         NOT NULL,
	node_type       SMALLINT         NOT NULL,
	definition_id   BIGINT         NOT NULL,
	node_code       VARCHAR(100) NOT NULL,
	node_name       VARCHAR(100),
	permission_flag VARCHAR(200),
	node_ratio      VARCHAR(200),
	coordinate      VARCHAR(100),
	any_node_skip   VARCHAR(100),
	listener_type   VARCHAR(100),
	listener_path   VARCHAR(400),
	form_custom     CHAR(1)    DEFAULT 'N',
	form_path       VARCHAR(100),
	version         VARCHAR(20)  NOT NULL,
	create_time     TIMESTAMP,
	create_by       VARCHAR(64)  DEFAULT '',
	update_time     TIMESTAMP,
	update_by       VARCHAR(64)  DEFAULT '',
	ext             CLOB,
	del_flag        CHAR(1)    DEFAULT '0',
	tenant_id       VARCHAR(40),
	CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_skip (
	id             BIGINT         NOT NULL,
	definition_id  BIGINT         NOT NULL,
	now_node_code  VARCHAR(100) NOT NULL,
	now_node_type  SMALLINT,
	next_node_code VARCHAR(100) NOT NULL,
	next_node_type SMALLINT,
	skip_name      VARCHAR(100),
	skip_type      VARCHAR(40),
	skip_condition VARCHAR(200),
	coordinate     VARCHAR(100),
	create_time    TIMESTAMP,
	create_by      VARCHAR(64)  DEFAULT '',
	update_time    TIMESTAMP,
	update_by      VARCHAR(64)  DEFAULT '',
	del_flag       CHAR(1)    DEFAULT '0',
	tenant_id      VARCHAR(40),
	CONSTRAINT flow_skip_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_instance (
	id              BIGINT         NOT NULL,
	definition_id   BIGINT         NOT NULL,
	business_id     VARCHAR(40)  NOT NULL,
	node_type       SMALLINT         NOT NULL,
	node_code       VARCHAR(40)  NOT NULL,
	node_name       VARCHAR(100),
	variable        CLOB,
	flow_status     VARCHAR(20)  NOT NULL,
	activity_status SMALLINT         DEFAULT 1 NOT NULL,
	def_json        CLOB,
	create_time     TIMESTAMP,
	create_by       VARCHAR(64)  DEFAULT '',
	update_time     TIMESTAMP,
	update_by       VARCHAR(64)  DEFAULT '',
	ext             VARCHAR(500),
	del_flag        CHAR(1)    DEFAULT '0',
	tenant_id       VARCHAR(40),
	CONSTRAINT flow_instance_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_task (
	id            BIGINT         NOT NULL,
	definition_id BIGINT         NOT NULL,
	instance_id   BIGINT         NOT NULL,
	node_code     VARCHAR(100) NOT NULL,
	node_name     VARCHAR(100),
	node_type     SMALLINT         NOT NULL,
	flow_status   VARCHAR(20)  NOT NULL,
	form_custom   CHAR(1)    DEFAULT 'N',
	form_path     VARCHAR(100),
	create_time   TIMESTAMP,
	create_by     VARCHAR(64)  DEFAULT '',
	update_time   TIMESTAMP,
	update_by     VARCHAR(64)  DEFAULT '',
	del_flag      CHAR(1)    DEFAULT '0',
	tenant_id     VARCHAR(40),
	CONSTRAINT flow_task_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_his_task (
	id               BIGINT         NOT NULL,
	definition_id    BIGINT         NOT NULL,
	instance_id      BIGINT         NOT NULL,
	task_id          BIGINT         NOT NULL,
	node_code        VARCHAR(100),
	node_name        VARCHAR(100),
	node_type        SMALLINT,
	target_node_code VARCHAR(200),
	target_node_name VARCHAR(200),
	approver         VARCHAR(40),
	cooperate_type   SMALLINT         DEFAULT 0 NOT NULL,
	collaborator     VARCHAR(500),
	skip_type        VARCHAR(10),
	flow_status      VARCHAR(20)  NOT NULL,
	form_custom      CHAR(1)    DEFAULT 'N',
	form_path        VARCHAR(100),
	ext              CLOB,
	message          VARCHAR(500),
	variable         CLOB,
	create_time      TIMESTAMP,
	update_time      TIMESTAMP,
	del_flag         CHAR(1)    DEFAULT '0',
	tenant_id        VARCHAR(40),
	CONSTRAINT flow_his_task_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_user (
	id           BIGINT        NOT NULL,
	"TYPE"         CHAR(1)   NOT NULL,
	processed_by VARCHAR(80),
	associated   BIGINT        NOT NULL,
	create_time  TIMESTAMP,
	create_by    VARCHAR(64) DEFAULT '',
	update_time  TIMESTAMP,
	update_by    VARCHAR(64) DEFAULT '',
	del_flag     CHAR(1)   DEFAULT '0',
	tenant_id    VARCHAR(40),
	CONSTRAINT flow_user_pk PRIMARY KEY (id)
);
CREATE INDEX user_processed_type ON flow_user (processed_by, "TYPE");
CREATE INDEX user_associated_idx ON flow_user (associated);

COMMENT ON TABLE flow_definition IS '流程定义';
COMMENT ON TABLE flow_node IS '流程节点';
COMMENT ON TABLE flow_skip IS '节点跳转';
COMMENT ON TABLE flow_instance IS '流程实例';
COMMENT ON TABLE flow_task IS '待办任务';
COMMENT ON TABLE flow_his_task IS '历史任务';
COMMENT ON TABLE flow_user IS '流程用户';
