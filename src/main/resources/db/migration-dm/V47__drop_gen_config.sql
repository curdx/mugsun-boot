-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G75 代码生成器重写：移除旧生成器的单表配置 gen_config（已由 gen_table + gen_column 双表元数据取代）
DROP TABLE  gen_config;
