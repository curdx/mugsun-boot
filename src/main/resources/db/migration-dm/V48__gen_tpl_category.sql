-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G76 主子表 + 树表生成：gen_table 扩展模板类别与关联配置
-- tpl_category：crud（单表，默认）/ tree（树表）/ master（主子表一对多）
ALTER TABLE gen_table ADD tpl_category      VARCHAR(16)  DEFAULT 'crud' NOT NULL;
-- 树表：父级字段列名（如 parent_id），导入时自动识别
ALTER TABLE gen_table ADD tree_parent_field VARCHAR(64);
-- 主子表：子表名 + 子表中指向主表的外键列 + 子表功能名
ALTER TABLE gen_table ADD sub_table_name    VARCHAR(128);
ALTER TABLE gen_table ADD sub_join_field    VARCHAR(64);
