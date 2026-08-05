-- W2 登录日志增强：UA 解析（浏览器/操作系统）+ IP 归属地
-- browser/os 由登录写入时 Hutool UserAgentUtil 解析 user_agent 落列；login_location 由 ip2region 离线库解析
-- （开关 mugsun.ip2region.enabled 缺省关闭、xdb 外置，未开启/历史行一律为 NULL，列表直接展示空值）

ALTER TABLE sys_login_log ADD COLUMN browser        VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN os             VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN login_location VARCHAR(128);

COMMENT ON COLUMN sys_login_log.browser IS '浏览器（登录时 UA 解析落列）';
COMMENT ON COLUMN sys_login_log.os IS '操作系统（登录时 UA 解析落列）';
COMMENT ON COLUMN sys_login_log.login_location IS 'IP 归属地（ip2region 离线解析，开关关闭/内网/未命中为 NULL）';
