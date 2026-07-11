-- G76 主子表 + 树表生成：gen_table 扩展模板类别与关联配置
-- tpl_category：crud（单表，默认）/ tree（树表）/ master（主子表一对多）
ALTER TABLE gen_table ADD COLUMN IF NOT EXISTS tpl_category      VARCHAR(16)  NOT NULL DEFAULT 'crud';
-- 树表：父级字段列名（如 parent_id），导入时自动识别
ALTER TABLE gen_table ADD COLUMN IF NOT EXISTS tree_parent_field VARCHAR(64);
-- 主子表：子表名 + 子表中指向主表的外键列 + 子表功能名
ALTER TABLE gen_table ADD COLUMN IF NOT EXISTS sub_table_name    VARCHAR(128);
ALTER TABLE gen_table ADD COLUMN IF NOT EXISTS sub_join_field    VARCHAR(64);
