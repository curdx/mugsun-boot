-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G36 数据脱敏 + 字段加密：sys_user 增敏感字段
-- phone 明文存储、展示脱敏（@ColumnMask）；id_card 存 SM4 密文（TypeHandler），查询自动解密
ALTER TABLE sys_user ADD phone   VARCHAR(32);
ALTER TABLE sys_user ADD id_card VARCHAR(255);
COMMENT ON COLUMN sys_user.phone   IS '手机号（展示脱敏）';
COMMENT ON COLUMN sys_user.id_card IS '身份证号（SM4 加密存储）';
