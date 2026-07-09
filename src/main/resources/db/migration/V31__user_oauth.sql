-- G53 社交登录：用户第三方账号绑定（微信/支付宝/QQ 等，回调后按 source+open_id 绑定/登录）
CREATE TABLE sys_user_oauth (
	id          BIGINT PRIMARY KEY,
	user_id     BIGINT NOT NULL,
	source      VARCHAR(32) NOT NULL,
	open_id     VARCHAR(128) NOT NULL,
	union_id    VARCHAR(128),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_user_oauth_source_openid ON sys_user_oauth (source, open_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_user_oauth IS '用户第三方账号绑定（社交登录 source+open_id 唯一）';
