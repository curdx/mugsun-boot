-- Mugsun 首次部署数据库初始化（PostgreSQL）
-- 用法：psql -U postgres -f scripts/init-db.sql
-- 说明：
--   1. 脚本幂等，可重复执行；
--   2. 账号密码与 application.yml 默认值一致（mugsun/mugsun），正式部署请改密码并同步 JDBC 环境变量；
--   3. CREATEDB 授权用途：应用首启自动创建埋点库（TrackFlywayConfig，本脚本已预建时为兜底）、
--      集成测试在同容器内建隔离库。

-- 应用账号（不存在才创建）
DO $$
BEGIN
	IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'mugsun') THEN
		CREATE USER mugsun WITH PASSWORD 'mugsun';
	END IF;
END
$$;

-- 主库 / 埋点库（CREATE DATABASE 不能在事务或函数块内执行，用 psql \gexec 条件创建）
SELECT 'CREATE DATABASE mugsun OWNER mugsun' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mugsun')\gexec
SELECT 'CREATE DATABASE mugsun_track OWNER mugsun' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mugsun_track')\gexec

-- 建库权限（应用首启自助创建埋点库 / 测试建库的兜底）
ALTER USER mugsun CREATEDB;
