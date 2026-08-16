-- GIS 可选模块（默认开启）：底图供应商表 + 场景表 + 顶级菜单 + 参数开关
-- 无密钥也可登录；工作台空态引导去配置底图。关闭 gis.module.enabled 后菜单隐藏、接口拒绝。

CREATE TABLE gis_map_provider (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	provider    VARCHAR(32)  NOT NULL,
	enabled     INT          NOT NULL DEFAULT 1,
	api_key     VARCHAR(512),
	secret      VARCHAR(512),
	extra_json  TEXT,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_gis_map_provider ON gis_map_provider (tenant_id, provider) WHERE is_deleted = 0;

CREATE TABLE gis_scene (
	id          BIGINT       PRIMARY KEY,
	tenant_id   VARCHAR(12),
	name        VARCHAR(128) NOT NULL,
	scene_json  TEXT,
	status      INT          NOT NULL DEFAULT 1,
	remark      VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	is_deleted  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_gis_scene_tenant ON gis_scene (tenant_id) WHERE is_deleted = 0;

COMMENT ON TABLE gis_map_provider IS 'GIS 底图供应商密钥（租户隔离，密钥密文）';
COMMENT ON COLUMN gis_map_provider.provider IS 'tianditu/amap/baidu/google';
COMMENT ON TABLE gis_scene IS 'GIS 场景（相机、底图、图层 JSON）';

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, sort, icon, is_public, create_time, is_deleted)
SELECT 1094000000000000001, 0, '地理信息', '/gis', '/index/index', 'M', 6, 'ri:earth-line', 0, now(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/gis' AND is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, now(), 0
FROM (VALUES
	(1094000000000000002, '地图工作台', '/gis/workspace', '/gis/workspace', 'gis:workspace:list', 1, 'ri:map-2-line'),
	(1094000000000000003, '底图配置', '/gis/provider', '/gis/provider', 'gis:provider:list', 2, 'ri:key-2-line')
) AS v(id, name, path, component, perm, sort, icon)
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, v.parent, v.name, 'F', v.perm, v.sort, now(), 0
FROM (VALUES
	(1094000000000000004, 1094000000000000003, '保存底图', 'gis:provider:save', 1),
	(1094000000000000005, 1094000000000000003, '删除底图', 'gis:provider:remove', 2),
	(1094000000000000006, 1094000000000000002, '保存场景', 'gis:scene:save', 1),
	(1094000000000000007, 1094000000000000002, '删除场景', 'gis:scene:remove', 2)
) AS v(id, parent, name, perm, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.is_deleted = 0);

INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900030, '地理信息模块开关', 'gis.module.enabled', 'true', 'false 时隐藏 GIS 菜单并拒绝 /system/gis 接口；无需任何底图 Key 即可关', now(), 0)
ON CONFLICT (id) DO NOTHING;
