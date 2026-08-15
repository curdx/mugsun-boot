-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G41 全局水印 + 文件公私目录
-- 水印策略开关（后台可改，即时生效）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
 (900009, '全局水印开关', 'security.watermark.enabled', 'false', '开启后前端满屏水印(用户名+日期)', SYSDATE, 0);

-- 附件公私访问：public 直取 / private 需授权下载
ALTER TABLE sys_attach ADD access VARCHAR(16) DEFAULT 'private' NOT NULL;
COMMENT ON COLUMN sys_attach.access IS '访问级别：public 公开直取 / private 私有授权下载';
