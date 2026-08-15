-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE gen_product (
	id           BIGINT        PRIMARY KEY,
	product_name VARCHAR(128)  NOT NULL,
	price        NUMERIC(10,2),
	stock        INT           DEFAULT 0,
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT           DEFAULT 0 NOT NULL
);

COMMENT ON TABLE gen_product IS '代码生成演示表-商品';
