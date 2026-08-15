-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

ALTER TABLE sys_attach ADD base_path VARCHAR(255);

COMMENT ON COLUMN sys_attach.base_path IS '存储平台基础路径（授权流式下载定位文件用）';
