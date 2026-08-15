-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- W5 登录体验增强：sys_user 补头像列 + 忘记密码邮件模板种子
-- avatar：个人中心头像（附件体系公开区 URL，实体未建模——SysUser 冻结，读写走 Db 行级 SQL）
-- forget_password 模板：忘记密码链路（/auth/forget-code）按 ${code} 渲染下发 6 位重置验证码

ALTER TABLE sys_user ADD avatar VARCHAR(255);
COMMENT ON COLUMN sys_user.avatar IS '头像 URL（个人中心上传，/system/file/upload 公开区产物）';

-- 忘记密码验证码邮件模板（与 login_2fa 同格式；id 段沿用 91xxxx 邮件模板序列）
INSERT INTO sys_mail_template (id, code, name, subject, content, status, create_time, is_deleted) VALUES
 (910002, 'forget_password', '忘记密码重置验证码', '【Mugsun】密码重置验证码', '您的密码重置验证码是 ${code}，5 分钟内有效，请勿泄露。如非本人操作请忽略。', 1, SYSDATE, 0);
