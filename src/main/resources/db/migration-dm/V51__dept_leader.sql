-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G80 多候选人：部门负责人字段，供"发起人部门负责人"候选人解析（assignment 监听器读取）。
ALTER TABLE sys_dept ADD leader_id BIGINT;
