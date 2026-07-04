ALTER TABLE sys_role ADD COLUMN data_scope INT NOT NULL DEFAULT 1;

COMMENT ON COLUMN sys_role.data_scope IS '数据范围：1 全部 / 2 本部门 / 3 仅本人';
