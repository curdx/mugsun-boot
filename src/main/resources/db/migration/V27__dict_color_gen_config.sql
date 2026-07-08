-- G48 补漏：字典项标签颜色 + 代码生成配置持久化
ALTER TABLE sys_dict ADD COLUMN color VARCHAR(32);
ALTER TABLE sys_dict_biz ADD COLUMN color VARCHAR(32);
COMMENT ON COLUMN sys_dict.color IS '标签颜色（十六进制，前端着色）';

CREATE TABLE gen_config (
	id           BIGINT       PRIMARY KEY,
	table_name   VARCHAR(128) NOT NULL,
	base_package VARCHAR(255),
	table_prefix VARCHAR(64),
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_gen_config_table ON gen_config (table_name) WHERE is_deleted = 0;
COMMENT ON TABLE gen_config IS '代码生成表配置（按表名持久化）';
