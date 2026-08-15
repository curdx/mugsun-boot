-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G85 操作日志防篡改：哈希链 + SM2 签名（等保三级审计完整性）
ALTER TABLE sys_oper_log ADD prev_hash   VARCHAR(64);
ALTER TABLE sys_oper_log ADD record_hash VARCHAR(64);
ALTER TABLE sys_oper_log ADD sign        CLOB;

COMMENT ON COLUMN sys_oper_log.prev_hash   IS '前一条记录哈希（链式防篡改，首条为创世 0）';
COMMENT ON COLUMN sys_oper_log.record_hash IS '本条记录 SM3 哈希（含 prev_hash，任一字段改动即失配）';
COMMENT ON COLUMN sys_oper_log.sign        IS '本条记录哈希的 SM2 签名（验签防伪造）';
