-- 达梦/Oracle 语法占位（V1 认证基表示意）。
-- 完整 V1–V68 与 PG 目录对齐转换待达梦实例联调；当前仅保证类型/注释写法可参考，不足以单独完成全量 migrate。
-- 启用：spring.flyway.locations=classpath:db/migration-dm

CREATE TABLE sys_user (
	id          NUMBER(19)    PRIMARY KEY,
	username    VARCHAR2(64)  NOT NULL,
	password    VARCHAR2(100) NOT NULL,
	nickname    VARCHAR2(64),
	status      NUMBER(5)     DEFAULT 1 NOT NULL,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  NUMBER(10)    DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username);

COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.status IS '状态：1 启用 / 0 停用';
COMMENT ON COLUMN sys_user.is_deleted IS '逻辑删除：0 正常 / 1 删除';
