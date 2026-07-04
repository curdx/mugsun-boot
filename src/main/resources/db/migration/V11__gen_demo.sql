CREATE TABLE gen_product (
	id           BIGINT        PRIMARY KEY,
	product_name VARCHAR(128)  NOT NULL,
	price        NUMERIC(10,2),
	stock        INT           DEFAULT 0,
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT           NOT NULL DEFAULT 0
);

COMMENT ON TABLE gen_product IS '代码生成演示表-商品';
