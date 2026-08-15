-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- V56 生产就绪加固：存量数据回填 + 唯一约束补齐 + 查询索引 + 脏数据清洗
-- 背景：15 路对抗审查发现的数据层问题集中修复（详见审查报告）。全部幂等（ / 条件 UPDATE）。

-- ① sys_oauth_client 存量行 tenant_id 回填：G66 加列后存量为 NULL 形成「UI 不可见但仍可用」的鬼影凭据
UPDATE sys_oauth_client SET tenant_id = '000000' WHERE tenant_id IS NULL;

-- ② OAuth 客户端 redirect_uri 回填：fail-closed 改造后未登记 redirect_uri 的客户端禁止授权码模式，
--    存量两个 dev 客户端补登记前端调试回调（生产环境应改登真实客户端地址）
UPDATE sys_oauth_client SET redirect_uri = 'http://localhost:3006/#/oauth-debug'
WHERE (redirect_uri IS NULL OR redirect_uri = '') AND is_deleted = 0;

-- ③ 核心唯一约束（部分索引 WHERE is_deleted=0，与平台逻辑删除规约一致）
CREATE UNIQUE INDEX  uk_tenant_code ON sys_tenant (tenant_code);
CREATE UNIQUE INDEX  uk_param_key ON sys_param (param_key);
CREATE UNIQUE INDEX  uk_dict_code_key ON sys_dict (code, dict_key);
CREATE UNIQUE INDEX  uk_user_role ON sys_user_role (user_id, role_id);
CREATE UNIQUE INDEX  uk_role_menu ON sys_role_menu (role_id, menu_id);
CREATE UNIQUE INDEX  uk_role_dept ON sys_role_dept (role_id, dept_id);
CREATE UNIQUE INDEX  uk_user_tenant_phone ON sys_user (tenant_id, phone);

-- ④ uk_serial_record 改部分唯一：原全量唯一使逻辑删除的旧记录永久阻塞同 code+date 新记录
DROP INDEX  uk_serial_record;
CREATE UNIQUE INDEX uk_serial_record ON sys_serial_number_record (serial_code, record_date);

-- ⑤ 高频日志表查询索引（Flex 租户过滤 + id 倒序分页场景）
CREATE INDEX  idx_oper_log_tenant_id ON sys_oper_log (tenant_id, id DESC);
CREATE INDEX  idx_login_log_id ON sys_login_log (id DESC);
CREATE INDEX  idx_api_log_tenant_id ON sys_api_log (tenant_id, id DESC);

-- ⑥ 脱敏值污染清洗：sensitive1 的手机号曾被脱敏串写回（写门控已堵路径，存量置 NULL）
UPDATE sys_user SET phone = NULL WHERE phone LIKE '%*%';

-- ⑦ G85 篡改样本复原：验收期被改 IP 的操作日志记录恢复原值（record_hash 系按原值计算，复原后链验签通过）
UPDATE sys_oper_log SET ip = '0:0:0:0:0:0:0:1' WHERE id = 103432781770000138 AND ip = '9.9.9.9';

-- ⑧ 已逻辑删除租户级联清理：其下用户/角色/部门同步逻辑删除（防「租户恢复」场景旧账号静默复活）
UPDATE sys_user SET is_deleted = 1, update_time = SYSDATE
  WHERE is_deleted = 0 AND tenant_id IN (SELECT tenant_code FROM sys_tenant WHERE is_deleted = 1);
UPDATE sys_role SET is_deleted = 1, update_time = SYSDATE
  WHERE is_deleted = 0 AND tenant_id IN (SELECT tenant_code FROM sys_tenant WHERE is_deleted = 1);
UPDATE sys_dept SET is_deleted = 1, update_time = SYSDATE
  WHERE is_deleted = 0 AND tenant_id IN (SELECT tenant_code FROM sys_tenant WHERE is_deleted = 1);

-- ⑨ V39/V45/V46 硬编码雪花 ID 说明：种子菜单/授权的父级/角色锚定需待 DataInitializer 播种后按业务键修正
--    （迁移期锚点菜单尚不存在，无法在此修复；由 DataInitializer 启动时按 permission/role_code 幂等重锚）

-- ⑩ 孤儿历史密码清理：user_id 在 sys_user（含逻辑删除）完全不存在的审计残留
DELETE FROM sys_password_log WHERE user_id NOT IN (SELECT id FROM sys_user);
