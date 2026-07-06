-- 单号生成器：规则定义表 + 生成记录表
CREATE TABLE sys_serial_number (
	id                BIGINT       PRIMARY KEY,
	code              VARCHAR(64)  NOT NULL,
	business_name     VARCHAR(64)  NOT NULL,
	format            VARCHAR(64)  NOT NULL,
	rule_type         VARCHAR(16)  NOT NULL,
	init_number       BIGINT       NOT NULL DEFAULT 0,
	step_random_range INT          NOT NULL DEFAULT 1,
	last_number       BIGINT,
	last_time         TIMESTAMP,
	remark            VARCHAR(255),
	create_time       TIMESTAMP,
	update_time       TIMESTAMP,
	is_deleted        INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_serial_code ON sys_serial_number (code) WHERE is_deleted = 0;

COMMENT ON TABLE sys_serial_number IS '单号生成规则';
COMMENT ON COLUMN sys_serial_number.format IS '格式：[yyyy]年 [mm]月 [dd]日 [n..n]数字位';
COMMENT ON COLUMN sys_serial_number.rule_type IS '周期：none/year/month/day';
COMMENT ON COLUMN sys_serial_number.step_random_range IS '步长随机上限，1 为固定步长 1';

CREATE TABLE sys_serial_number_record (
	id          BIGINT    PRIMARY KEY,
	serial_code VARCHAR(64) NOT NULL,
	record_date DATE      NOT NULL,
	last_number BIGINT    NOT NULL DEFAULT 0,
	last_time   TIMESTAMP NOT NULL,
	gen_count   BIGINT    NOT NULL DEFAULT 0,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_serial_record ON sys_serial_number_record (serial_code, record_date);

COMMENT ON TABLE sys_serial_number_record IS '单号生成记录（每业务每天一条）';

INSERT INTO sys_serial_number (id, code, business_name, format, rule_type, init_number, step_random_range, remark, create_time) VALUES
(1060000000000000001, 'ORDER',    '订单编号', 'DK[yyyy][mm][dd]NO[nnnnn]', 'day',  1000, 1, '按日重置', now()),
(1060000000000000002, 'CONTRACT', '合同编号', 'HT[yyyy][nnnnnn]',          'year', 0,    1, '按年重置', now());
