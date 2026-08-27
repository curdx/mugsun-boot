-- App 登录客户端：图形验证码仍开，避免 /auth/login + clientId=app 绕过 PC 图形码。
-- 移动端登录走 /app/auth/login + 滑块一次性 ticket。
INSERT INTO sys_client (id, client_id, client_name, captcha_enabled, max_online, token_timeout, status, create_time, is_deleted)
VALUES (7500000000000001, 'app', '移动工作台', 1, 0, 2592000, 1, now(), 0);
