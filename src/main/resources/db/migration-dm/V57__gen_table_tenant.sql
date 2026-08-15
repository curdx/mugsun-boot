-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- V57 存量低代码表租户隔离补齐：gen 托管物理表统一补 tenant_id 列并回填平台租户
-- 背景：对抗审查发现低代码建表（V57 前）无 tenant_id 列，多租户共享读写违反隔离铁律；
--       DdlService.buildCreate 已改为新建默认携带，本迁移补齐存量。幂等（存在即跳过）。

-- skipped PostgreSQL DO block (达梦用 Java/应用层回填)
