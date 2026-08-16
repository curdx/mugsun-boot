-- GIS 图层库 + 独立模块菜单（与埋点同级：工作台 / 图层 / 场景 / 底图）
CREATE TABLE gis_layer (
	id             BIGINT        PRIMARY KEY,
	tenant_id      VARCHAR(12),
	name           VARCHAR(128)  NOT NULL,
	kind           VARCHAR(32)   NOT NULL DEFAULT 'vector',
	crs            VARCHAR(32)   NOT NULL DEFAULT 'EPSG:4326',
	data_json      TEXT          NOT NULL,
	style_json     TEXT,
	feature_count  INT           NOT NULL DEFAULT 0,
	bbox           VARCHAR(128),
	status         INT           NOT NULL DEFAULT 1,
	remark         VARCHAR(255),
	create_time    TIMESTAMP,
	update_time    TIMESTAMP,
	is_deleted     INT           NOT NULL DEFAULT 0
);

CREATE INDEX idx_gis_layer_tenant ON gis_layer (tenant_id) WHERE is_deleted = 0;

COMMENT ON TABLE gis_layer IS '通用 GIS 图层（WGS84 GeoJSON，跨模块叠加）';
COMMENT ON COLUMN gis_layer.kind IS 'vector/heatmap';
COMMENT ON COLUMN gis_layer.crs IS '规范化后固定 EPSG:4326';

UPDATE sys_menu SET sort = 3, menu_name = '地理信息', icon = 'ri:earth-line', parent_id = 0, menu_type = 'M'
	WHERE path = '/gis' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE path = '/system' AND is_deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE path = '/saas' AND is_deleted = 0;
UPDATE sys_menu SET sort = 6 WHERE path = '/open-platform' AND is_deleted = 0;

UPDATE sys_menu SET sort = 1, menu_name = '地图工作台'
	WHERE path = '/gis/workspace' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4, menu_name = '底图配置'
	WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, now(), 0
FROM (VALUES
	(1094000000000000008, '图层', '/gis/layer', '/gis/layer', 'gis:layer:list', 2, 'ri:stack-line'),
	(1094000000000000009, '场景', '/gis/scene', '/gis/scene', 'gis:scene:list', 3, 'ri:landscape-line')
) AS v(id, name, path, component, perm, sort, icon)
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, v.parent, v.name, 'F', v.perm, v.sort, now(), 0
FROM (VALUES
	(1094000000000000010, 1094000000000000008, '保存图层', 'gis:layer:save', 1),
	(1094000000000000011, 1094000000000000008, '删除图层', 'gis:layer:remove', 2)
) AS v(id, parent, name, perm, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.is_deleted = 0);
