-- V57 存量低代码表租户隔离补齐：gen 托管物理表统一补 tenant_id 列并回填平台租户
-- 背景：对抗审查发现低代码建表（V57 前）无 tenant_id 列，多租户共享读写违反隔离铁律；
--       DdlService.buildCreate 已改为新建默认携带，本迁移补齐存量。幂等（存在即跳过）。

DO $$
DECLARE t RECORD;
BEGIN
  FOR t IN SELECT table_name FROM gen_table WHERE is_deleted = 0 LOOP
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = t.table_name)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = current_schema() AND table_name = t.table_name
                         AND column_name = 'tenant_id') THEN
      EXECUTE format('ALTER TABLE %I ADD COLUMN tenant_id VARCHAR(12)', t.table_name);
      -- 存量数据归平台租户（演示数据平台持有；租户自此只见本租户行）
      EXECUTE format('UPDATE %I SET tenant_id = ''000000'' WHERE tenant_id IS NULL', t.table_name);
    END IF;
  END LOOP;
END $$;
