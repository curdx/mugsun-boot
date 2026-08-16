-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

CREATE TABLE gis_map_provider (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	provider    VARCHAR(32)  NOT NULL,
	enabled     INT          DEFAULT 1 NOT NULL,
	api_key     VARCHAR(512),
	secret      VARCHAR(512),
	extra_json  CLOB,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_gis_map_provider ON gis_map_provider (tenant_id, provider);

CREATE TABLE gis_scene (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	name        VARCHAR(128) NOT NULL,
	scene_json  CLOB,
	status      INT          DEFAULT 1 NOT NULL,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          DEFAULT 0 NOT NULL
);

CREATE INDEX idx_gis_scene_tenant ON gis_scene (tenant_id);

COMMENT ON TABLE gis_map_provider IS 'GIS 底图供应商密钥（租户隔离，密钥密文）';
COMMENT ON COLUMN gis_map_provider.provider IS 'tianditu/amap/baidu/google';
COMMENT ON TABLE gis_scene IS 'GIS 场景（相机、底图、图层 JSON）';

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000001, 0, '地理信息', '/gis', '/index/index', 'M', 6, 'ri:earth-line', 0, SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/gis' AND is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000002, p.id, '地图工作台', '/gis/workspace', '/gis/workspace', 'C', 'gis:workspace:list', 1, 'ri:map-2-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/gis' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/gis/workspace' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000003, p.id, '底图配置', '/gis/provider', '/gis/provider', 'C', 'gis:provider:list', 2, 'ri:key-2-line', 0, SYSDATE, 0
FROM sys_menu p
WHERE p.path = '/gis' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = '/gis/provider' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1094000000000000004, 1094000000000000003, '保存底图', 'F', 'gis:provider:save', 1, SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'gis:provider:save' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1094000000000000005, 1094000000000000003, '删除底图', 'F', 'gis:provider:remove', 2, SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'gis:provider:remove' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1094000000000000006, 1094000000000000002, '保存场景', 'F', 'gis:scene:save', 1, SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'gis:scene:save' AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT 1094000000000000007, 1094000000000000002, '删除场景', 'F', 'gis:scene:remove', 2, SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = 'gis:scene:remove' AND x.is_deleted = 0);

INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted)
SELECT 900030, '地理信息模块开关', 'gis.module.enabled', 'true', 'false 时隐藏 GIS 菜单并拒绝接口', SYSDATE, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_param WHERE id = 900030);
