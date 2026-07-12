-- G80 多候选人：部门负责人字段，供"发起人部门负责人"候选人解析（assignment 监听器读取）。
ALTER TABLE sys_dept ADD COLUMN leader_id BIGINT;
