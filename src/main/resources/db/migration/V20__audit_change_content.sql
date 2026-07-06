-- G42 数据变更记录·字段级 diff：审计表增字段级变更内容
ALTER TABLE sys_data_audit ADD COLUMN change_content TEXT;
COMMENT ON COLUMN sys_data_audit.change_content IS '字段级变更内容 JSON：[{label,old,new}]';
