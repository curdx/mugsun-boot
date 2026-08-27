-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- GIS 可选模块（默认开启）：底图供应商表 + 场景表 + 顶级菜单 + 参数开关
-- 无密钥也可登录；工作台空态引导去配置底图。关闭 gis.module.enabled 后菜单隐藏、接口拒绝。

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
COMMENT ON TABLE gis_scene IS 'GIS 场景（相机、底图、图层 CLOB）';

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000001, 0, '地理信息', '/gis', '/index/index', 'M', 6, 'ri:earth-line', 0, SYSDATE, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/gis' AND is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, SYSDATE, 0
FROM (
	SELECT 1094000000000000002 AS id, '地图工作台' AS name, '/gis/workspace' AS path, '/gis/workspace' AS component, 'gis:workspace:list' AS perm, 1 AS sort, 'ri:map-2-line' AS icon FROM DUAL
	UNION ALL
	SELECT 1094000000000000003, '底图配置', '/gis/provider', '/gis/provider', 'gis:provider:list', 2, 'ri:key-2-line' FROM DUAL
) v
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, v.parent, v.name, 'F', v.perm, v.sort, SYSDATE, 0
FROM (
	SELECT 1094000000000000004 AS id, 1094000000000000003 AS parent, '保存底图' AS name, 'gis:provider:save' AS perm, 1 AS sort FROM DUAL
	UNION ALL
	SELECT 1094000000000000005, 1094000000000000003, '删除底图', 'gis:provider:remove', 2 FROM DUAL
	UNION ALL
	SELECT 1094000000000000006, 1094000000000000002, '保存场景', 'gis:scene:save', 1 FROM DUAL
	UNION ALL
	SELECT 1094000000000000007, 1094000000000000002, '删除场景', 'gis:scene:remove', 2 FROM DUAL
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.is_deleted = 0);

INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900030, '地理信息模块开关', 'gis.module.enabled', 'true', 'false 时隐藏 GIS 菜单并拒绝 /system/gis 接口；无需任何底图 Key 即可关', SYSDATE, 0);
