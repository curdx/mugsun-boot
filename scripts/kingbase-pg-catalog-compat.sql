-- 金仓（KingbaseES）PG 协议兼容补丁（开发/联调）
-- 社区 Flyway PostgreSQL 方言与 PG JDBC DatabaseMetaData 会硬查 pg_catalog.pg_*，
-- 金仓仅有 sys_catalog.sys_*。建同名 schema/视图，使 classpath:db/migration 可走同一套 Flyway。
-- 用法：docker cp 本文件进容器后 ksql -f；或见信创指南 §6。
-- 警告：非正式信创验收手段，勿当作生产 catalog 改造。

CREATE SCHEMA IF NOT EXISTS mugsun;
CREATE SCHEMA IF NOT EXISTS pg_catalog;

DROP VIEW IF EXISTS mugsun.pg_namespace;
DROP VIEW IF EXISTS public.pg_namespace;
CREATE VIEW mugsun.pg_namespace AS
	SELECT nspname, nspowner, nspacl FROM sys_catalog.sys_namespace;
CREATE VIEW public.pg_namespace AS
	SELECT nspname, nspowner, nspacl FROM sys_catalog.sys_namespace;

CREATE OR REPLACE VIEW pg_catalog.pg_namespace AS
	SELECT oid, nspname, nspowner, nspacl FROM sys_catalog.sys_namespace;
CREATE OR REPLACE VIEW pg_catalog.pg_class AS
	SELECT * FROM sys_catalog.sys_class;
CREATE OR REPLACE VIEW pg_catalog.pg_attribute AS
	SELECT * FROM sys_catalog.sys_attribute;
CREATE OR REPLACE VIEW pg_catalog.pg_type AS
	SELECT * FROM sys_catalog.sys_type;
CREATE OR REPLACE VIEW pg_catalog.pg_constraint AS
	SELECT * FROM sys_catalog.sys_constraint;
CREATE OR REPLACE VIEW pg_catalog.pg_index AS
	SELECT * FROM sys_catalog.sys_index;
CREATE OR REPLACE VIEW pg_catalog.pg_proc AS
	SELECT * FROM sys_catalog.sys_proc;
CREATE OR REPLACE VIEW pg_catalog.pg_database AS
	SELECT * FROM sys_catalog.sys_database;
CREATE OR REPLACE VIEW pg_catalog.pg_description AS
	SELECT * FROM sys_catalog.sys_description;
CREATE OR REPLACE VIEW pg_catalog.pg_depend AS
	SELECT * FROM sys_catalog.sys_depend;
CREATE OR REPLACE VIEW pg_catalog.pg_authid AS
	SELECT * FROM sys_catalog.sys_authid;
CREATE OR REPLACE VIEW pg_catalog.pg_roles AS
	SELECT * FROM sys_catalog.sys_roles;
