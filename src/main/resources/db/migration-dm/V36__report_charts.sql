-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

ALTER TABLE sys_report ADD charts CLOB;

COMMENT ON COLUMN sys_report.charts IS '多图表仪表盘配置 CLOB：[{dataset,chartType,title}]';
