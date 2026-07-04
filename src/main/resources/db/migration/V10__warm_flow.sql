CREATE TABLE flow_definition (
	id              int8         NOT NULL,
	flow_code       varchar(40)  NOT NULL,
	flow_name       varchar(100) NOT NULL,
	model_value     varchar(40)  NOT NULL DEFAULT 'CLASSICS',
	category        varchar(100),
	version         varchar(20)  NOT NULL,
	is_publish      int2         NOT NULL DEFAULT 0,
	form_custom     bpchar(1)    DEFAULT 'N',
	form_path       varchar(100),
	activity_status int2         NOT NULL DEFAULT 1,
	listener_type   varchar(100),
	listener_path   varchar(400),
	ext             varchar(500),
	create_time     timestamp,
	create_by       varchar(64)  DEFAULT '',
	update_time     timestamp,
	update_by       varchar(64)  DEFAULT '',
	del_flag        bpchar(1)    DEFAULT '0',
	tenant_id       varchar(40),
	CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_node (
	id              int8         NOT NULL,
	node_type       int2         NOT NULL,
	definition_id   int8         NOT NULL,
	node_code       varchar(100) NOT NULL,
	node_name       varchar(100),
	permission_flag varchar(200),
	node_ratio      varchar(200),
	coordinate      varchar(100),
	any_node_skip   varchar(100),
	listener_type   varchar(100),
	listener_path   varchar(400),
	form_custom     bpchar(1)    DEFAULT 'N',
	form_path       varchar(100),
	version         varchar(20)  NOT NULL,
	create_time     timestamp,
	create_by       varchar(64)  DEFAULT '',
	update_time     timestamp,
	update_by       varchar(64)  DEFAULT '',
	ext             text,
	del_flag        bpchar(1)    DEFAULT '0',
	tenant_id       varchar(40),
	CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_skip (
	id             int8         NOT NULL,
	definition_id  int8         NOT NULL,
	now_node_code  varchar(100) NOT NULL,
	now_node_type  int2,
	next_node_code varchar(100) NOT NULL,
	next_node_type int2,
	skip_name      varchar(100),
	skip_type      varchar(40),
	skip_condition varchar(200),
	coordinate     varchar(100),
	create_time    timestamp,
	create_by      varchar(64)  DEFAULT '',
	update_time    timestamp,
	update_by      varchar(64)  DEFAULT '',
	del_flag       bpchar(1)    DEFAULT '0',
	tenant_id      varchar(40),
	CONSTRAINT flow_skip_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_instance (
	id              int8         NOT NULL,
	definition_id   int8         NOT NULL,
	business_id     varchar(40)  NOT NULL,
	node_type       int2         NOT NULL,
	node_code       varchar(40)  NOT NULL,
	node_name       varchar(100),
	variable        text,
	flow_status     varchar(20)  NOT NULL,
	activity_status int2         NOT NULL DEFAULT 1,
	def_json        text,
	create_time     timestamp,
	create_by       varchar(64)  DEFAULT '',
	update_time     timestamp,
	update_by       varchar(64)  DEFAULT '',
	ext             varchar(500),
	del_flag        bpchar(1)    DEFAULT '0',
	tenant_id       varchar(40),
	CONSTRAINT flow_instance_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_task (
	id            int8         NOT NULL,
	definition_id int8         NOT NULL,
	instance_id   int8         NOT NULL,
	node_code     varchar(100) NOT NULL,
	node_name     varchar(100),
	node_type     int2         NOT NULL,
	flow_status   varchar(20)  NOT NULL,
	form_custom   bpchar(1)    DEFAULT 'N',
	form_path     varchar(100),
	create_time   timestamp,
	create_by     varchar(64)  DEFAULT '',
	update_time   timestamp,
	update_by     varchar(64)  DEFAULT '',
	del_flag      bpchar(1)    DEFAULT '0',
	tenant_id     varchar(40),
	CONSTRAINT flow_task_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_his_task (
	id               int8         NOT NULL,
	definition_id    int8         NOT NULL,
	instance_id      int8         NOT NULL,
	task_id          int8         NOT NULL,
	node_code        varchar(100),
	node_name        varchar(100),
	node_type        int2,
	target_node_code varchar(200),
	target_node_name varchar(200),
	approver         varchar(40),
	cooperate_type   int2         NOT NULL DEFAULT 0,
	collaborator     varchar(500),
	skip_type        varchar(10),
	flow_status      varchar(20)  NOT NULL,
	form_custom      bpchar(1)    DEFAULT 'N',
	form_path        varchar(100),
	ext              text,
	message          varchar(500),
	variable         text,
	create_time      timestamp,
	update_time      timestamp,
	del_flag         bpchar(1)    DEFAULT '0',
	tenant_id        varchar(40),
	CONSTRAINT flow_his_task_pkey PRIMARY KEY (id)
);

CREATE TABLE flow_user (
	id           int8        NOT NULL,
	type         bpchar(1)   NOT NULL,
	processed_by varchar(80),
	associated   int8        NOT NULL,
	create_time  timestamp,
	create_by    varchar(64) DEFAULT '',
	update_time  timestamp,
	update_by    varchar(64) DEFAULT '',
	del_flag     bpchar(1)   DEFAULT '0',
	tenant_id    varchar(40),
	CONSTRAINT flow_user_pk PRIMARY KEY (id)
);
CREATE INDEX user_processed_type ON flow_user USING btree (processed_by, type);
CREATE INDEX user_associated_idx ON flow_user USING btree (associated);

COMMENT ON TABLE flow_definition IS '流程定义';
COMMENT ON TABLE flow_node IS '流程节点';
COMMENT ON TABLE flow_skip IS '节点跳转';
COMMENT ON TABLE flow_instance IS '流程实例';
COMMENT ON TABLE flow_task IS '待办任务';
COMMENT ON TABLE flow_his_task IS '历史任务';
COMMENT ON TABLE flow_user IS '流程用户';
