ALTER TABLE sys_report ADD COLUMN charts TEXT;

COMMENT ON COLUMN sys_report.charts IS '多图表仪表盘配置 JSON：[{dataset,chartType,title}]';
