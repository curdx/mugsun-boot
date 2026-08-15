-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE sys_region (
	id          BIGINT       PRIMARY KEY,
	code        VARCHAR(20)  NOT NULL,
	parent_code VARCHAR(20)  DEFAULT '0' NOT NULL,
	name        VARCHAR(64)  NOT NULL,
	level       INT          DEFAULT 1 NOT NULL,
	sort        INT          DEFAULT 0 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

COMMENT ON TABLE sys_region IS '行政区划';
COMMENT ON COLUMN sys_region.code IS '区划编码';
COMMENT ON COLUMN sys_region.parent_code IS '父级编码，0 为顶级';
COMMENT ON COLUMN sys_region.level IS '层级：1省 2市 3区县';

-- 种子数据：省 / 市 / 区县 三级，用于懒加载树演示
INSERT INTO sys_region(id, code, parent_code, name, level, sort, is_deleted) VALUES
	(1500000000000000001, '110000', '0',      '北京市',   1, 1, 0),
	(1500000000000000002, '110100', '110000', '市辖区',   2, 1, 0),
	(1500000000000000003, '110101', '110100', '东城区',   3, 1, 0),
	(1500000000000000004, '110102', '110100', '西城区',   3, 2, 0),
	(1500000000000000010, '440000', '0',      '广东省',   1, 2, 0),
	(1500000000000000011, '440100', '440000', '广州市',   2, 1, 0),
	(1500000000000000012, '440103', '440100', '荔湾区',   3, 1, 0),
	(1500000000000000013, '440104', '440100', '越秀区',   3, 2, 0),
	(1500000000000000014, '440300', '440000', '深圳市',   2, 2, 0),
	(1500000000000000015, '440303', '440300', '罗湖区',   3, 1, 0),
	(1500000000000000020, '330000', '0',      '浙江省',   1, 3, 0),
	(1500000000000000021, '330100', '330000', '杭州市',   2, 1, 0),
	(1500000000000000022, '330102', '330100', '上城区',   3, 1, 0);
