-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G76 代码生成器 树表 / 主子表 生成演示目标表（供导入→配置→生成→drop-in 验收）
-- 树表：分类（parent_id 自关联）
CREATE TABLE gen_category (
	id            BIGINT       PRIMARY KEY,
	parent_id     BIGINT       DEFAULT 0 NOT NULL,
	category_name VARCHAR(64),
	sort          INT          DEFAULT 0 NOT NULL,
	create_time   TIMESTAMP,
	update_time   TIMESTAMP,
	is_deleted    INT          DEFAULT 0 NOT NULL
);

-- 主子表：订单（主）+ 订单明细（子，order_id 外键）
CREATE TABLE gen_order (
	id          BIGINT         PRIMARY KEY,
	order_no    VARCHAR(64),
	amount      NUMERIC(12, 2),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT            DEFAULT 0 NOT NULL
);

CREATE TABLE gen_order_item (
	id           BIGINT       PRIMARY KEY,
	order_id     BIGINT,
	product_name VARCHAR(128),
	qty          INT,
	create_time  TIMESTAMP,
	update_time  TIMESTAMP,
	is_deleted   INT          DEFAULT 0 NOT NULL
);
